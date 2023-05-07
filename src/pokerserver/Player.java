package pokerserver;

import java.util.*;

public class Player {
    private String id;
    private List<Card> hand;
    private int seat;
    private int chips;
    private boolean isFolded;
    private boolean isAllIn;

    public Player(String id, int seat, int initialChips) {
        this.id = id;
        this.seat = seat;
        this.chips = initialChips;
        this.hand = new ArrayList<>();
        this.isFolded = false;
        this.isAllIn = false;
    }
    

    // Getters and setters for the properties

    public void dealCard(Card card) {
        hand.add(card);
    }
    
    void bet(int betAmount) {
        this.chips -= betAmount;
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
}
