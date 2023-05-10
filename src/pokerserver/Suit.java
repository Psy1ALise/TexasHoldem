package pokerserver;

public enum Suit {
    HEART(0),
    SPADE(1),
    CLUB(2),
    DIAMOND(3);

    private final int suit;

    Suit(int suit) {
        this.suit = suit;
    }

    public int getSuit() {
        return suit;
    }
}
