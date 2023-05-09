package pokerserver;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class PokerServer extends JFrame implements Runnable {

	private static final int WIDTH = 400;
	private static final int HEIGHT = 300;
	static final int MAX_PLAYERS = 6;

	List<PrintWriter> clients = new ArrayList<>();
	JTextArea serverTextArea;
	PlayerSeat[] seats;
	private int serverPort;
	Set<String> uniqueIDs = new HashSet<>();
	CountdownTimer countdownTimer;

	private boolean roundInProgress = false;
	private PokerGame currentGame;

	public PokerServer() {
		super("Poker Server");
		setSize(WIDTH, HEIGHT);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		createMenu();
		serverTextArea = new JTextArea();
		add(new JScrollPane(serverTextArea));
		setVisible(true);
		seats = new PlayerSeat[MAX_PLAYERS];
		clients = new ArrayList<>();
		uniqueIDs = new HashSet<>();
		setRoundInProgress(false);
	}

	private void createMenu() {
		JMenuBar menuBar = new JMenuBar();
		JMenu menu = new JMenu("Server Options");
		JMenuItem startItem = new JMenuItem("Start server");
		startItem.addActionListener((e) -> startServer());
		menu.add(startItem);
		JMenuItem stopItem = new JMenuItem("Stop server");
		stopItem.addActionListener((e) -> stopServer());
		menu.add(stopItem);
		JMenuItem clearItem = new JMenuItem("Clear server output");
		clearItem.addActionListener((e) -> serverTextArea.setText(""));
		menu.add(clearItem);
		JMenuItem exitItem = new JMenuItem("Exit");
		exitItem.addActionListener((e) -> System.exit(0));
		menu.add(exitItem);
		menuBar.add(menu);
		setJMenuBar(menuBar);
	}

	private void startServer() {
		String portNumber = JOptionPane.showInputDialog(this, "Enter server port number:");
		serverPort = Integer.parseInt(portNumber);
		Thread serverThread = new Thread(this);
		serverThread.start();
		serverTextArea.append("Poker server started on port " + serverPort + "\n");
	}

	private void stopServer() {
		serverTextArea.append("Poker server stopped\n");
	}

	@Override
	public void run() {
		try {
			ServerSocket serverSocket = new ServerSocket(serverPort);
			setTitle("Poker Server: Port " + serverPort);
			while (true) {
				Socket clientSocket = serverSocket.accept();
				ClientHandler clientHandler = new ClientHandler(this, clientSocket);
				Thread clientThread = new Thread(clientHandler);
				clientThread.start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	String getSeatInfo() {
		StringBuilder seatInfo = new StringBuilder();
		synchronized (seats) {
			for (int i = 0; i < MAX_PLAYERS; i++) {
				if (seats[i] != null && seats[i].isOccupied) {
					seatInfo.append(i).append(",").append(seats[i].playerId).append(";");
				}
			}
		}
		return seatInfo.toString();
	}

	void broadcast(String message) {
		for (PrintWriter client : clients) {
			client.println(message);
		}
	}

	public void startNewRound() {
//		// Stop the countdown timer
//		if (countdownTimer != null) {
//			countdownTimer.stopTimer();
//			countdownTimer = null;
//		}

		// Start the game logic
	    setRoundInProgress(true);

	    // Initialize the PokerGame instance
	    currentGame = new PokerGame(this);

	    // Add connected players to the game
	    synchronized (seats) {
	        for (PlayerSeat seat : seats) {
	            if (seat != null && seat.isOccupied) {
	                Player player = new Player(seat.playerId, seat.seatNumber, 200);
	                currentGame.addPlayer(player);
	            }
	        }
	    }

	    broadcast("GAME_START");
	    serverTextArea.append("Game Started\n");

	    // Sort players by their seat number
	    currentGame.sortPlayersBySeat();
	    // Initialize the seats in game
//	    currentGame.initOccupiedSeats();

	    // Initialize the dealer position after all players are added
	    if (currentGame.getRoundId() == 0) {
	        currentGame.setRandomDealer();
	        serverTextArea.append("Dealer is " + currentGame.getDealerPosition() + "\n");
	    } else {
	        currentGame.setNextDealer();
	        serverTextArea.append("Dealer is " + currentGame.getDealerPosition() + "\n");
	    }

	    // Game Actions
	    synchronized(currentGame) {
		    currentGame.playerActions();
	    }
	    

	   
	}

	public boolean isRoundInProgress() {
		return roundInProgress;
	}

	public void setRoundInProgress(boolean roundInProgress) {
		this.roundInProgress = roundInProgress;
	}

	public static void main(String[] args) {
		PokerServer pokerServer = new PokerServer();
	}

	public PokerGame getCurrentGame() {
		return currentGame;
	}
}
