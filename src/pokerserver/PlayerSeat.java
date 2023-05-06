package pokerserver;

public class PlayerSeat {
    int seatNumber;
    String playerId;
    boolean isOccupied;

    public PlayerSeat(int seatNumber) {
        this.seatNumber = seatNumber;
        this.playerId = null;
        this.isOccupied = false;
    }

    public void assignSeat(String playerId) {
        this.playerId = playerId;
        this.isOccupied = true;
    }

    public void releaseSeat() {
        this.playerId = null;
        this.isOccupied = false;
    }
}