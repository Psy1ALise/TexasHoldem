package pokerserver;

import java.util.function.Supplier;

public class CountdownTimer extends Thread {
    private int timeRemaining;
    private boolean running;
    private PokerServer server;
    private Supplier<Integer> playerCountSupplier;

    public CountdownTimer(PokerServer server, int initialTime, Supplier<Integer> playerCountSupplier) {
        this.server = server;
        this.timeRemaining = initialTime;
        this.running = true;
        this.playerCountSupplier = playerCountSupplier;
    }

    public void addTime(int additionalTime) {
        timeRemaining += additionalTime;
    }

    public void stopTimer() {
        running = false;
    }

    public boolean isRunning() {
        return running;
    }
    
    @Override
    public void run() {
        int prevPlayerCount = playerCountSupplier.get();
        while (running && timeRemaining > 0) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            int currentPlayerCount = playerCountSupplier.get();
            if (currentPlayerCount < 2) {
                stopTimer();
                return;
            }
            if (currentPlayerCount > prevPlayerCount) {
                timeRemaining += 2 * (currentPlayerCount - prevPlayerCount);
            }
            prevPlayerCount = currentPlayerCount;
            timeRemaining--;
        }
        if (running) {
            server.startNewRound();
        }
    }
}
