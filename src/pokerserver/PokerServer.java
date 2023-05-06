package pokerserver;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class PokerServer extends JFrame implements Runnable {

	private static final int WIDTH = 400;
	private static final int HEIGHT = 300;
	private static final int MAX_PLAYERS = 6;

	private List<PrintWriter> clients = new ArrayList<>();
	private JTextArea serverTextArea;
	private PlayerSeat[] seats;
	private int serverPort;
	private Set<String> uniqueIDs = new HashSet<>();
	private CountdownTimer countdownTimer;
	
	private boolean gameInProgress = false;
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
		setGameInProgress(false);
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

				// Create a new thread to handle the client's connection
				Thread clientThread = new Thread(() -> handleClientConnection(clientSocket));
				clientThread.start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void handleClientConnection(Socket clientSocket) {
		try {
			// Create input and output streams for the client's socket
			BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

			// Read the client's ID from the input stream
			String id = in.readLine();
			serverTextArea.append("Client " + id + " connected\n");
			broadcast("PLAYER_JOINED " + id);
			// Add the client's output stream to the list of clients
			synchronized (clients) {
				clients.add(out);
			}
			// Check for duplicate IDs and handle accordingly
			synchronized (uniqueIDs) {
				if (uniqueIDs.contains(id)) {
					out.println("DUPLICATE_ID");
					clientSocket.close();
					return;
				} else {
					uniqueIDs.add(id);
				}
			}
			// Seat Handling
			int assignedSeat = -1;
			List<Integer> availableSeats = new ArrayList<>();
			synchronized (seats) {
				for (int i = 0; i < MAX_PLAYERS; i++) {
					if (seats[i] == null) {
						seats[i] = new PlayerSeat(i);
					}
					if (!seats[i].isOccupied) {
						availableSeats.add(i);
					}
				}
				if (!availableSeats.isEmpty()) {
					int randomIndex = new Random().nextInt(availableSeats.size());
					assignedSeat = availableSeats.get(randomIndex);
					seats[assignedSeat].assignSeat(id);
				}
			}
			if (assignedSeat == -1) {
				out.println("REFUSED");
				clientSocket.close();
				return;
			} else {
				// Broadcast the updated seat information to all clients
				broadcast("SEATINFO " + getSeatInfo());
				out.println("ASSIGNED " + (assignedSeat));
				out.println("SEATINFO " + getSeatInfo());
			}
			synchronized (uniqueIDs) {
			    int playerCount = uniqueIDs.size();
			    if (playerCount >= 2 && countdownTimer != null && !countdownTimer.isRunning()) {
			        countdownTimer = new CountdownTimer(this, 10, () -> uniqueIDs.size());
			        countdownTimer.start();
			    }
			}
			// TODO
			// Handle the "DISCONNECT" message
			while (true) {
				String message = in.readLine();
				if (message == null || message.equals("DISCONNECT")) {
					synchronized (seats) {
						seats[assignedSeat].releaseSeat();
					}
					serverTextArea.append("Client " + id + " disconnected\n");
					broadcast("PLAYER_DISCONNECTED " + id);
					broadcast("SEATINFO " + getSeatInfo());
					break;
				}
			}

			// Remove the client's output stream from the list of clients and the unique ID
	        synchronized (clients) {
	            clients.remove(out);
	        }
	        synchronized (uniqueIDs) {
	            uniqueIDs.remove(id);
	        }

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String getSeatInfo() {
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

	private void broadcast(String message) {
		for (PrintWriter client : clients) {
			client.println(message);
		}
	}
	
	public void startGame() {
	    // Stop the countdown timer
	    if (countdownTimer != null) {
	        countdownTimer.stopTimer();
	        countdownTimer = null;
	    }

	    // Start the game logic
	    setGameInProgress(true);

	    // Initialize the PokerGame instance
	    currentGame = new PokerGame();

	    // Add connected players to the game
	    synchronized (seats) {
	        for (PlayerSeat seat : seats) {
	            if (seat != null && seat.isOccupied) {
	                Player player = new Player(seat.playerId, seat.seatNumber, initialChips);
	                currentGame.addPlayer(player);
	            }
	        }
	    }

	    // Initialize the dealer position after all players are added
	    if (currentGame.getGameId() == 0) {
	        currentGame.setRandomDealer();
	    }

	    broadcast("GAME_START");
	    serverTextArea.append("Game Started\n");
	}

	public static void main(String[] args) {
		PokerServer pokerServer = new PokerServer();
	}

	public boolean isGameInProgress() {
		return gameInProgress;
	}

	public void setGameInProgress(boolean gameInProgress) {
		this.gameInProgress = gameInProgress;
	}
}
