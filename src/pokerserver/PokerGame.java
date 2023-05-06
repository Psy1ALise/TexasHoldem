package pokerserver;

import java.awt.Component;
import java.util.*;

public class PokerGame{
	
	private int gameId;
    private static int nextGameId = 0;
    
    private List<Player> players;
    private List<Card> communityCards;
    private int dealerPosition;
    private int currentPlayer;
    private int currentBet;
    private int pot;
    

    public PokerGame() {
        // Initialize game state
        gameId = nextGameId++;
        players = new ArrayList<>();
        communityCards = new ArrayList<>();
        pot = 0;
        currentBet = 0;
        
    }
    
    public int getGameId() {
        return gameId;
    }
    
    public void addPlayer(Player player) {
        players.add(player);
    }

    public void removePlayer(Player player) {
        players.remove(player);
    }
    
    public List<Player> getPlayers() {
        return players;
    }
    
    void setRandomDealer() {
        dealerPosition = new Random().nextInt(players.size());
    }
    
    public void startNewHand() {
        // Start a new hand (deal cards, set blinds, etc.)
    }

    public void handleAction(Player player, String action, int amount) {
        // Handle player actions (fold, call, raise, all-in)
    }

    private void dealCards() {
        // Deal cards to players and the community
    }

    private void advanceRound() {
        // Move to the next round (pre-flop, flop, turn, river, showdown)
    }

    private void resolveHand() {
        // Evaluate hands and determine the winner(s)
    }
        
}