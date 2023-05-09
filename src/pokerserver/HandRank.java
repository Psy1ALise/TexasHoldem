package pokerserver;

public enum HandRank {
    HIGHCARD("High Card"),
    PAIR("Pair"),
    TWOPAIR("Two Pair"),
    THREEOFAKIND("Three of a Kind"),
    STRAIGHT("Straight"),
    FLUSH("Flush"),
    FULLHOUSE("Full House"),
    FOUROFAKIND("Four of a Kind"),
    STRAIGHTFLUSH("Straight Flush"),
    ROYALFLUSH("Royal Flush");

    private final String name;

    HandRank(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
