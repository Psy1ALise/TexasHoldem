package pokerserver;

public class Card {
    private final Suit suit;
    private final Rank rank;

    public Card(Suit suit, Rank rank) {
        this.suit = suit;
        this.rank = rank;
    }

    public Suit getSuit() {
        return suit;
    }

    public Rank getRank() {
        return rank;
    }

    @Override
    public String toString() {
        return suit + " " + rank;
    }
    
    public int getIntSuit() {
    	return suit.getSuit();
    }
    
    public int getIntRank() {
    	return rank.getRank();
    }
}

