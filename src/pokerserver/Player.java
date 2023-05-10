package pokerserver;

import java.util.*;

public class Player {
    private String id;
    private List<Card> hand;
    private int seat;
    private int chips;
    private boolean isFolded;
    private boolean isAllIn;
    private int sinkValue;

    public Player(String id, int seat, int initialChips) {
        this.id = id;
        this.seat = seat;
        this.chips = initialChips;
        this.hand = new ArrayList<>();
        this.isFolded = false;
        this.isAllIn = false;
    }
    

    // Getters and setters for the properties

    public String getId() {
    	return id;
    }
    
    public void dealCard(Card card) {
        hand.add(card);
    }
    
    void bet(int betAmount) {
        if (betAmount >= chips) { // Player goes all-in
            betAmount = chips;
            setAllIn();
        }
        chips -= betAmount;
        sinkValue += betAmount;
    }

    void call(int currentBet) {
        int callAmount = currentBet - sinkValue;
        if (callAmount >= chips) { // Player goes all-in
            callAmount = chips;
            setAllIn();
        }
        bet(callAmount);
    }
    
    int getSinkValue() {
    	return sinkValue;
    }
    
    public boolean getFold() {
    	return isFolded;
    }
    
    public void setFold() {
        isFolded = true;
    }
    
    public boolean getAllIn() {
    	return isAllIn;
    }
    public void setAllIn() {
        isAllIn = true;
    }

    public void resetHand() {
        hand.clear();
        isFolded = false;
        isAllIn = false;
    }

    public int getChips() {
    	return chips;
    }
    
    public void addChips(int amount) {
        chips += amount;
    }

    public void removeChips(int amount) {
        chips -= amount;
    }


	public int getSeatNumber() {
		return seat;
	}


	List<Card> getHand() {
		return hand;
	}
}
