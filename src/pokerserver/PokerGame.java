package pokerserver;

import java.util.*;

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
		// Evaluate hands and determine the winner(s)
		// Get the list of players who haven't folded
		List<Player> activePlayers = new ArrayList<>();
		for (Player player : players) {
			if (!player.getFold()) {
				activePlayers.add(player);
			}
		}

		// Determine the winner
		Player winner = HandEvaluator.evaluateHands(activePlayers, communityCards);
		server.broadcast("WINNER " + winner.getId());

		// Transfer the pot to the winner
		winner.addChips(pot);
		pot = 0;

		// Reset for the next hand
		for (Player player : players) {
			player.resetHand();
		}
		communityCards.clear();
	}
}