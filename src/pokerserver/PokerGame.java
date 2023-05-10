package pokerserver;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PokerGame {

	PokerServer server;
	private int roundId;
	private static int nextRoundId = 0;

	private Deck deck;
	private List<Player> players;
	private static int dealerPosition;
	private List<Card> communityCards;

	private Player currentPlayer;
	private int currentBet;
	private int pot;
	private Object actionLock = new Object();
	private String playerAction = null;

	public PokerGame(PokerServer server) {
		// Initialize game state
		this.server = server;
		roundId = nextRoundId++;
		deck = new Deck();
		players = new ArrayList<>();
		dealerPosition = -1;
		communityCards = new ArrayList<>();
		currentBet = 0;
		pot = 0;
		actionLock = new Object();
		playerAction = null;
	}

	int getRoundId() {
		return roundId;
	}

	void addPlayer(Player player) {
		players.add(player);
	}

	void removePlayer(Player player) {
		players.remove(player);
	}

	List<Player> getPlayers() {
		return players;
	}

	int getDealerPosition() {
		return dealerPosition;
	}

	private int findPlayerIndexBySeat(int seatNumber) {
		for (int i = 0; i < players.size(); i++) {
			if (players.get(i).getSeatNumber() == seatNumber) {
				return i;
			}
		}
		return -1;
	}

	void setRandomDealer() {
		int randomIndex = new Random().nextInt(players.size());
		dealerPosition = players.get(randomIndex).getSeatNumber();
	}

	void setNextDealer() {
		int currentDealerIndex = findPlayerIndexBySeat(dealerPosition);
		int nextDealerIndex = (currentDealerIndex + 1) % players.size();
		dealerPosition = players.get(nextDealerIndex).getSeatNumber();
	}

	void dealCards() {
		// Deal one card at a time for two rounds to each player
		int currentDealerIndex = findPlayerIndexBySeat(dealerPosition);
		int currentPlayerIndex = (currentDealerIndex + 1) % players.size();

		for (int dealRound = 0; dealRound < 2; dealRound++) {
			for (int i = 0; i < players.size(); i++) {
				players.get(currentPlayerIndex).dealCard(deck.dealCard());
				currentPlayerIndex = getNextPlayerIndex(currentPlayerIndex);
			}
		}

		// Burn a card before dealing the flop
		deck.dealCard();

		// Deal three community cards for the flop
		for (int i = 0; i < 3; i++) {
			communityCards.add(deck.dealCard());
		}

		// Burn a card before dealing the turn
		deck.dealCard();

		// Deal one community card for the turn
		communityCards.add(deck.dealCard());

		// Burn a card before dealing the river
		deck.dealCard();

		// Deal one community card for the river
		communityCards.add(deck.dealCard());
	}

	void blindBets() {
		int currentDealerIndex = findPlayerIndexBySeat(dealerPosition);
		int smallBlindIndex = (currentDealerIndex + 1) % players.size();
		int bigBlindIndex = (currentDealerIndex + 2) % players.size();

		Player smallBlindPlayer = players.get(smallBlindIndex);
		Player bigBlindPlayer = players.get(bigBlindIndex);

		// For player at the small blind index they have to bet 1
		smallBlindPlayer.bet(1);
		// For player at the big blind index they have to bet 2
		bigBlindPlayer.bet(2);
	}

	void playerActions() {
		// Deal Cards and set Blinds
		dealCards();
		blindBets();
		currentBet = 2;
		// Determine the starting player index for pre-flop
		int currentDealerIndex = findPlayerIndexBySeat(dealerPosition);
		int startPlayerIndex = (currentDealerIndex + 3) % players.size();
		bettingRound(startPlayerIndex);
		currentBet = 0;
		// Check if there is only one player active
		if (getNumberOfActivePlayers() == 1) {
			resolveHand();
			return;
		}

		// Starting from flop, actions start from the player on the left of the dealer
		startPlayerIndex = (currentDealerIndex + 1) % players.size();
		for (int i = 0; i < 3; i++) { // Loop for flop, turn, and river
			bettingRound(startPlayerIndex);
			currentBet = 0;
			if (getNumberOfActivePlayers() == 1) {
				resolveHand();
				return;
			}
		}

		// Finally, resolve the hand
		resolveHand();
	}

	private boolean bettingRound(int startPlayerIndex) {
		int currentBet = 0;
		while (true) {
			boolean actionTaken = false;
			for (int i = 0; i < players.size(); i++) {
				int currentPlayerIndex = (startPlayerIndex + i) % players.size();
				Player currentPlayer = players.get(currentPlayerIndex);

				if (currentPlayer.getFold() || currentPlayer.getAllIn()) {
					continue;
				}

				String playerAction = waitForPlayerAction();
				handleAction(currentPlayer.getId(), playerAction);

				if (playerAction.startsWith("RAISE")) {
					currentBet = Integer.parseInt(playerAction.substring("RAISE".length()).trim());
					actionTaken = true;
				} else if (playerAction.startsWith("ALL_IN")) {
					currentBet = currentPlayer.getChips();
					actionTaken = true;
				}
			}

			if (!actionTaken) {
				break;
			}
		}

		return getNumberOfActivePlayers() > 1;
	}

	void handleAction(String playerId, String action) {
		// Find the player with the given ID
		Player player = getPlayerById(playerId);
		if (player == null) {
			return;
		}

		synchronized (actionLock) {
			// Handle player actions (fold, call, raise, all-in)
			if (action.startsWith("FOLD")) {
				player.setFold();
				player.resetHand();
			} else if (action.startsWith("CALL")) {
				player.call(currentBet);
			} else if (action.startsWith("RAISE")) {
				int raiseAmount = Integer.parseInt(action.substring("RAISE".length()).trim());
				player.bet(raiseAmount);
			} else if (action.startsWith("ALL_IN")) {
				player.bet(player.getChips());
			}

			// Notify the game loop that the player's action has been processed
			playerAction = action;
			actionLock.notifyAll();
		}
	}

	String waitForPlayerAction() {
		synchronized (actionLock) {
			while (playerAction == null) {
				try {
					actionLock.wait();
				} catch (InterruptedException e) {
					// Handle interruption
				}
			}
			String action = playerAction;
			playerAction = null; // Reset for the next player's turn
			return action;
		}
	}

	private boolean isPlayerActionCompleted() {
		long distinctBets = players.stream().filter(p -> !p.getFold() && !p.getAllIn()).mapToInt(Player::getSinkValue)
				.distinct().count();
		return distinctBets == 1;
	}

	private void updateCurrentBet(int amount) {
		currentBet = Math.max(currentBet, amount);
	}

	private Player getPlayerById(String playerId) {
		return players.stream().filter(p -> p.getId().equals(playerId)).findFirst().orElse(null);
	}

	private int getNumberOfActivePlayers() {
		return (int) players.stream().filter(p -> !p.getFold()).count();
	}

	private void startNewHand() {
		// Start a new hand (deal cards, set blinds, etc.)
	}

	Player getCurrentPlayer() {
		return currentPlayer;
	}

	void sortPlayersBySeat() {
		Collections.sort(players,
				(player1, player2) -> Integer.compare(player1.getSeatNumber(), player2.getSeatNumber()));
	}

	private int getNextPlayerIndex(int currentIndex) {
		return (currentIndex + 1) % players.size();
	}

	private void resolveHand() {
		// Get the list of players who haven't folded
		List<Player> activePlayers = new ArrayList<>();
		for (Player player : players) {
			if (!player.getFold()) {
				activePlayers.add(player);
			}
		}
		List<Player> bestPlayers = new ArrayList<>();
		if (activePlayers.size() > 1) {
			PokerHandResolver resolver = new PokerHandResolver();
			HandRank bestHandRank = null;
			List<List<Card>> winningHands = new ArrayList<>();

			// Evaluate hands and determine the winner(s)
			for (Player player : activePlayers) {
				List<Card> playerCards = new ArrayList<>(player.getHand());
				playerCards.addAll(communityCards);
				HandRank playerRank = resolver.getBestRank(playerCards);

				if (bestHandRank == null || playerRank.compareTo(bestHandRank) > 0) {
					bestPlayers.clear();
					bestPlayers.add(player);
					winningHands.clear();
					winningHands.add(playerCards);
					bestHandRank = playerRank;
				} else if (playerRank.compareTo(bestHandRank) == 0) {
					bestPlayers.add(player);
					winningHands.add(playerCards);
				}
			}

			if (bestPlayers.size() > 1) {
				// Tierbreakers
				switch (bestHandRank) {
				case HIGH_CARD:
					handleHandsWithMultiples(1, bestPlayers, winningHands);
					break;
				case PAIR:
					handleHandsWithMultiples(2, bestPlayers, winningHands);
					break;
				case TWO_PAIR:
					handleTwoPairAndFullHouse(bestHandRank, bestPlayers, winningHands);
					break;
				case THREE_OF_A_KIND:
					handleHandsWithMultiples(3, bestPlayers, winningHands);
					break;
				case STRAIGHT:
					handleStraight(bestPlayers, winningHands);
					break;
				case FLUSH:
					handleFlush(bestPlayers, winningHands);
					break;
				case FULL_HOUSE:
					handleTwoPairAndFullHouse(bestHandRank, bestPlayers, winningHands);
					break;
				case FOUR_OF_A_KIND:
					handleHandsWithMultiples(4, bestPlayers, winningHands);
					break;
				case STRAIGHT_FLUSH:
					handleStraightFlush(bestPlayers, winningHands);
					break;
				case ROYAL_FLUSH:
					break;
				default:

				}
			}

		} else {
			bestPlayers = activePlayers;
		}

		// TODO: Side Pot need to be done
		// Announce the winner and transfer the pot
		for (Player player : bestPlayers) {
			server.broadcast("WINNER " + player.getId());
			player.addChips(pot / bestPlayers.size());
		}
		pot = 0;

		// Reset for the next hand
		for (Player player : players) {
			player.resetHand();
		}
		communityCards.clear();
	}

	private void handleHandsWithMultiples(int numOfMultiples, List<Player> bestPlayers, List<List<Card>> winningHands) {
		Rank hiCardRank = null;
		List<Player> stillTiedPlayers = new ArrayList<>();
		List<List<Card>> stillTiedHands = new ArrayList<>();

		for (List<Card> hand : winningHands) {
			Rank cardRank = (numOfMultiples == 1) ? highCard(hand) : findHighestRankWithCount(hand, numOfMultiples);

			if (hiCardRank == null || cardRank.compareTo(hiCardRank) > 0) {
				hiCardRank = cardRank;
				stillTiedPlayers.clear();
				stillTiedHands.clear();
				addPlayerAndHandToLists(bestPlayers, winningHands, hand, stillTiedPlayers, stillTiedHands);
			} else if (cardRank.equals(hiCardRank)) {
				addPlayerAndHandToLists(bestPlayers, winningHands, hand, stillTiedPlayers, stillTiedHands);
			}
		}

		if (stillTiedPlayers.size() > 1) {
			// Remove the cards in the arrayList of with the hiCardRank then find the hiCard
			// of the kicker
			for (List<Card> hand : stillTiedHands) {
				final Rank finalCardRank = hiCardRank;
				hand.removeIf(card -> card.getRank().equals(finalCardRank));
			}

			// Now find the high card from the remaining cards in each hand
			findHighKickers(bestPlayers, stillTiedHands, stillTiedPlayers);
		} else {
			bestPlayers = stillTiedPlayers;
		}
	}

	private void handleStraight(List<Player> bestPlayers, List<List<Card>> winningHands) {
		List<Player> stillTiedPlayers = new ArrayList<>();
		Rank highestStraightRank = null;

		for (List<Card> hand : winningHands) {
			List<Rank> ranks = hand.stream().map(Card::getRank).distinct().sorted(Comparator.reverseOrder())
					.collect(Collectors.toList());
			Rank topRankInStraight = findStraight(ranks);

			if (topRankInStraight != null) {
				if (highestStraightRank == null || topRankInStraight.compareTo(highestStraightRank) > 0) {
					highestStraightRank = topRankInStraight;
					stillTiedPlayers.clear();
					stillTiedPlayers.add(bestPlayers.get(winningHands.indexOf(hand)));
				} else if (topRankInStraight.equals(highestStraightRank)) {
					stillTiedPlayers.add(bestPlayers.get(winningHands.indexOf(hand)));
				}
			}
		}

		bestPlayers.clear();
		bestPlayers.addAll(stillTiedPlayers);
	}

	private Rank findStraight(List<Rank> ranks) {
		for (int i = 0; i <= ranks.size() - 5;) {
			if (ranks.get(i).ordinal() - 1 == ranks.get(i + 1).ordinal()) {
				if (ranks.get(i + 1).ordinal() - 1 == ranks.get(i + 2).ordinal()
						&& ranks.get(i + 2).ordinal() - 1 == ranks.get(i + 3).ordinal()
						&& ranks.get(i + 3).ordinal() - 1 == ranks.get(i + 4).ordinal()) {
					return ranks.get(i);
				} else if (i < ranks.size() - 5) {
					// restart from next index
					continue;
				} else {
					break;
				}
			}
			i++;
		}
		if (ranks.containsAll(Arrays.asList(Rank.ACE, Rank.TWO, Rank.THREE, Rank.FOUR, Rank.FIVE))) {
			return Rank.FIVE;
		}
		return null;
	}

	private void handleFlush(List<Player> bestPlayers, List<List<Card>> winningHands) {
	    Rank hiCardRank = null;
	    Player bestPlayer = null;

	    for (List<Card> hand : winningHands) {
	        Map<Suit, List<Card>> suitToListMap = hand.stream().collect(Collectors.groupingBy(Card::getSuit));

	        Suit flushSuit = suitToListMap.entrySet().stream()
	                .filter(entry -> entry.getValue().size() >= 5)
	                .map(Map.Entry::getKey)
	                .findFirst()
	                .orElse(null);

	        if (flushSuit != null) {
	            List<Card> flushCards = suitToListMap.get(flushSuit);
	            flushCards.sort(Comparator.comparing(Card::getRank).reversed());
	            Rank cardRank = flushCards.get(0).getRank();

	            if (hiCardRank == null || cardRank.compareTo(hiCardRank) > 0) {
	                hiCardRank = cardRank;
	                bestPlayer = bestPlayers.get(winningHands.indexOf(hand));
	            }
	        }
	    }

	    bestPlayers.clear();
	    if (bestPlayer != null) {
	        bestPlayers.add(bestPlayer);
	    }
	}

	private void handleStraightFlush(List<Player> bestPlayers, List<List<Card>> winningHands) {
	    List<Player> stillTiedPlayers = new ArrayList<>();
	    List<List<Card>> stillTiedHands = new ArrayList<>();
	    Rank hiStraightRank = null;

	    for (List<Card> hand : winningHands) {
	        Map<Suit, List<Card>> suitToListMap = hand.stream().collect(Collectors.groupingBy(Card::getSuit));
	        Suit flushSuit = suitToListMap.entrySet().stream()
	                .filter(entry -> entry.getValue().size() >= 5)
	                .map(Map.Entry::getKey)
	                .findFirst()
	                .orElse(null);

	        if (flushSuit != null) {
	            List<Card> flushCards = suitToListMap.get(flushSuit);
	            List<Rank> sortedRanks = flushCards.stream()
	                    .map(Card::getRank)
	                    .sorted(Comparator.reverseOrder())
	                    .collect(Collectors.toList());
	            Rank straightRank = findStraight(sortedRanks);

	            if (straightRank != null) {
	                if (hiStraightRank == null || straightRank.compareTo(hiStraightRank) > 0) {
	                    hiStraightRank = straightRank;
	                    stillTiedPlayers.clear();
	                    stillTiedHands.clear();
	                    addPlayerAndHandToLists(bestPlayers, winningHands, flushCards, stillTiedPlayers, stillTiedHands);
	                } else if (straightRank.equals(hiStraightRank)) {
	                    addPlayerAndHandToLists(bestPlayers, winningHands, flushCards, stillTiedPlayers, stillTiedHands);
	                }
	            }
	        }
	    }
	}


	private void handleTwoPairAndFullHouse(HandRank handRank, List<Player> bestPlayers, List<List<Card>> winningHands) {
		List<Player> stillTiedPlayers = new ArrayList<>();
		List<List<Card>> stillTiedHands = new ArrayList<>();

		Rank hiRank1 = null;
		Rank hiRank2 = null;

		for (List<Card> hand : winningHands) {
			Rank rank1 = findHighestRankWithCount(hand, handRank == HandRank.TWO_PAIR ? 2 : 3);
			hand.removeIf(card -> card.getRank().equals(rank1));
			Rank rank2 = findHighestRankWithCount(hand, 2);

			if (hiRank1 == null || rank1.compareTo(hiRank1) > 0
					|| (rank1.equals(hiRank1) && rank2.compareTo(hiRank2) > 0)) {
				hiRank1 = rank1;
				hiRank2 = rank2;
				stillTiedPlayers.clear();
				stillTiedHands.clear();
				addPlayerAndHandToLists(bestPlayers, winningHands, hand, stillTiedPlayers, stillTiedHands);
			} else if (rank1.equals(hiRank1) && rank2.equals(hiRank2)) {
				addPlayerAndHandToLists(bestPlayers, winningHands, hand, stillTiedPlayers, stillTiedHands);
			}
		}

		if (stillTiedPlayers.size() > 1 && handRank == HandRank.TWO_PAIR) {
			for (List<Card> hand : stillTiedHands) {
				final Rank finalRank1 = hiRank1;
				final Rank finalRank2 = hiRank2;
				hand.removeIf(card -> card.getRank().equals(finalRank1) || card.getRank().equals(finalRank2));
			}
			findHighKickers(bestPlayers, stillTiedHands, stillTiedPlayers);
		} else {
			bestPlayers.clear();
			bestPlayers.addAll(stillTiedPlayers);
		}
	}

	private Rank findHighestRankWithCount(List<Card> hand, int count) {
		return hand.stream().collect(Collectors.groupingBy(Card::getRank, Collectors.counting())).entrySet().stream()
				.filter(entry -> entry.getValue() == count).max(Map.Entry.comparingByKey()).get().getKey();
	}

	private void addPlayerAndHandToLists(List<Player> bestPlayers, List<List<Card>> winningHands, List<Card> hand,
			List<Player> stillTiedPlayers, List<List<Card>> stillTiedHands) {
		stillTiedPlayers.add(bestPlayers.get(winningHands.indexOf(hand)));
		stillTiedHands.add(new ArrayList<>(hand));
	}

	private void findHighKickers(List<Player> bestPlayers, List<List<Card>> stillTiedHands,
			List<Player> stillTiedPlayers) {
		// Make a copy of hands to avoid modifying the original
		List<List<Card>> copiedHands = stillTiedHands.stream().map(ArrayList::new).collect(Collectors.toList());

		// Identify the ranks common to all hands, sorted in descending order
		Set<Rank> commonRanks = EnumSet.allOf(Rank.class);
		for (List<Card> hand : copiedHands) {
			commonRanks.retainAll(hand.stream().map(Card::getRank).collect(Collectors.toSet()));
		}
		List<Rank> sortedCommonRanks = new ArrayList<>(commonRanks);
		sortedCommonRanks.sort(Comparator.reverseOrder());

		// Sequentially remove the highest common ranks from each hand
		for (Rank commonRank : sortedCommonRanks) {
			for (List<Card> hand : copiedHands) {
				// Loop through the hand and remove the first card with the common rank
				Iterator<Card> iterator = hand.iterator();
				while (iterator.hasNext()) {
					Card card = iterator.next();
					if (card.getRank().equals(commonRank)) {
						iterator.remove();
						break;
					}
				}
			}
		}

		// Now find the high card from the remaining cards in each hand
		Rank hiKickerRank = null;
		for (List<Card> hand : copiedHands) {
			Rank kickerRank = highCard(hand);
			if (hiKickerRank == null || kickerRank.compareTo(hiKickerRank) > 0) {
				hiKickerRank = kickerRank;
				bestPlayers.clear();
				bestPlayers.add(stillTiedPlayers.get(copiedHands.indexOf(hand)));
			} else if (kickerRank.compareTo(hiKickerRank) == 0) {
				bestPlayers.add(stillTiedPlayers.get(copiedHands.indexOf(hand)));
			}
		}
	}

	public Rank highCard(List<Card> cards) {
		return cards.stream().max(Comparator.comparing(Card::getRank)).orElse(null).getRank();
	}

}