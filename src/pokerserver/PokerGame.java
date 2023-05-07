package pokerserver;

import java.util.*;

public class PokerGame {

	private int roundId;
	private static int nextRoundId = 0;

	private Deck deck;
	private List<Player> players;
	private static int dealerPosition;
	private List<Card> communityCards;

	private Player currentPlayer;
	private int currentBet;
	private int pot;

	public PokerGame() {
		// Initialize game state
		roundId = nextRoundId++;
		deck = new Deck();
		players = new ArrayList<>();
		dealerPosition = -1;
		communityCards = new ArrayList<>();
		currentBet = 0;
		pot = 0;
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
		// Blind bets
		int currentDealerIndex = findPlayerIndexBySeat(dealerPosition);
		// Start with the player on the left of the big blind
		int currentPlayerIndex = (currentDealerIndex + 3) % players.size(); 
		boolean blindBetCompleted = false;
		while (!blindBetCompleted) {
	        currentPlayer = players.get(currentPlayerIndex);

	        if (!currentPlayer.getFold() && !currentPlayer.getAllIn()) {
	            String action = getPlayerAction(currentPlayer);
	            int amount = getBetAmount(currentPlayer, action);

	            handleAction(currentPlayer, action, amount);

	            if (isPlayerTurnCompleted()) {
	            	blindBetCompleted = true;
	                break;
	            }
	        }

	        currentPlayerIndex = getNextPlayerIndex(currentPlayerIndex);
	    }
		

		// Flop bets

		// Turn bets

		// River bets
	}
	private boolean isPlayerTurnCompleted() {
		// TODO Auto-generated method stub
		return false;
	}

	private int getBetAmount(Player currentPlayer2, String action) {
		// TODO Auto-generated method stub
		return 0;
	}

	private String getPlayerAction(Player currentPlayer2) {
		// TODO Auto-generated method stub
		return null;
	}

	private void handleAction(Player player, String action, int amount) {
		// Handle player actions (fold, call, raise, all-in)
		if (true) {
			
		}
	}

	private void startNewHand() {
		// Start a new hand (deal cards, set blinds, etc.)
	}

	

	void sortPlayersBySeat() {
		Collections.sort(players,
				(player1, player2) -> Integer.compare(player1.getSeatNumber(), player2.getSeatNumber()));
	}

	private int getNextPlayerIndex(int currentIndex) {
		return (currentIndex + 1) % players.size();
	}

	private void advanceRound() {
		// Move to the next round (pre-flop, flop, turn, river, showdown)
	}

	private void resolveHand() {
		// Evaluate hands and determine the winner(s)
	}

}