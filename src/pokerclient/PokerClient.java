package pokerclient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class PokerClient extends JFrame implements Runnable {
	// UI components for Texas Hold'em Poker game
	private static final int WIDTH = 1200;
	private static final int HEIGHT = 900;
	private JPanel tablePanel;
	private JTextArea logTextArea;

	private JLabel[] communityCards;
	private JLabel[] playerHoldCards;
	private JLabel potSizeLabel;

	// Player action button
	private JButton foldButton;
	private JButton callButton;
	private JButton raiseButton;
	private JButton allInButton;
	private JSpinner raiseAmountSpinner;

	// Networking and game state management
	private PrintWriter out;
	private BufferedReader in;
	private String serverAddress;
	private int serverPort;
	private String id;

	// PokerClient constructor
	public PokerClient() {
		super("Poker Client");
		// Set up UI components for Poker game
		setupUI();
		// Connect to server and initialize game state
		connectToServer();
		setWindowTitle(id);

		this.setResizable(false);

		// When window is close then disconnect from the server
		shutdownHook();
	}

	private void setupUI() {
		this.setSize(WIDTH, HEIGHT);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// Set up the poker table and layout
		tablePanel = new JPanel(null) {
			private Image backgroundImage = new ImageIcon(getClass().getResource("/img/blank.png")).getImage();

			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				g.drawImage(backgroundImage, 0, 0, this.getWidth(), this.getHeight(), this);
			}
		};
		this.add(tablePanel);

		// Create player labels
		JLabel player1 = createPlayerLabel(" ");
		player1.setBackground(Color.WHITE);
		JLabel player2 = createPlayerLabel(" ");
		player2.setBackground(Color.WHITE);
		JLabel player3 = createPlayerLabel(" ");
		player3.setBackground(Color.WHITE);
		JLabel player4 = createPlayerLabel(" ");
		player4.setBackground(Color.WHITE);
		JLabel player5 = createPlayerLabel(" ");
		player5.setBackground(Color.WHITE);
		JLabel player6 = createPlayerLabel(" ");
		player6.setBackground(Color.WHITE);

		// Position player labels from top right count clockwise
		player1.setBounds(750, 50, 100, 50);
		player2.setBounds(950, 300, 100, 50);
		player3.setBounds(750, 550, 100, 50);
		player4.setBounds(350, 550, 100, 50);
		player5.setBounds(150, 300, 100, 50);
		player6.setBounds(350, 50, 100, 50);

		// Add player labels to the tablePanel
		tablePanel.add(player1);
		tablePanel.add(player2);
		tablePanel.add(player3);
		tablePanel.add(player4);
		tablePanel.add(player5);
		tablePanel.add(player6);

		// Set up action buttons and raise amount spinner
		foldButton = new JButton("Fold");
		callButton = new JButton("Check/Call");
		raiseButton = new JButton("Raise");
		allInButton = new JButton("All In");
		SpinnerNumberModel spinnerModel = new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1);
		raiseAmountSpinner = new JSpinner(spinnerModel);

		// Position the action buttons and raise amount spinner
		foldButton.setBounds(400, 700, 100, 50);
		callButton.setBounds(550, 700, 100, 50);
		raiseButton.setBounds(700, 700, 100, 50);
		raiseAmountSpinner.setBounds(800, 700, 100, 50);
		allInButton.setBounds(950, 700, 100, 50);

		// Add action buttons and raise amount spinner to the tablePanel
		tablePanel.add(foldButton);
		tablePanel.add(callButton);
		tablePanel.add(raiseButton);
		tablePanel.add(allInButton);
		tablePanel.add(raiseAmountSpinner);

		// Add listeners for the buttons (to be implemented later)
		foldButton.addActionListener(e -> handleFoldAction());
		callButton.addActionListener(e -> handleCallAction());
		raiseButton.addActionListener(e -> handleRaiseAction());
		allInButton.addActionListener(e -> handleAllInAction());

		// Create a log panel for the clients
		logTextArea = new JTextArea();
		logTextArea.setEditable(false);
		logTextArea.setLineWrap(true);
		logTextArea.setWrapStyleWord(true);
		JScrollPane logScrollPane = new JScrollPane(logTextArea);
		logScrollPane.setBounds(50, 700, 250, 150);

		// Add log panel for clients
		tablePanel.add(logScrollPane);

		// Set up community cards, player hole cards, pot size label, and player actions
		// ...

		this.setVisible(true);
	}

	private JLabel createPlayerLabel(String text) {
		JLabel label = new JLabel(text);
		label.setHorizontalAlignment(SwingConstants.CENTER);
		label.setVerticalAlignment(SwingConstants.CENTER);
		label.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
		label.setOpaque(true); // Add this line to make the background color visible
		return label;
	}

	public void setWindowTitle(String id) {
		this.setTitle("Poker Client: " + id);
	}

	private void connectToServer() {
		JTextField serverAddressField = new JTextField();
		JTextField portField = new JTextField();
		JTextField idField = new JTextField();

		JPanel inputPanel = new JPanel(new GridLayout(0, 2));
		inputPanel.add(new JLabel("Server IP address:"));
		inputPanel.add(serverAddressField);
		inputPanel.add(new JLabel("Port Number:"));
		inputPanel.add(portField);
		inputPanel.add(new JLabel("Name or ID:"));
		inputPanel.add(idField);

		int result = JOptionPane.showConfirmDialog(this, inputPanel, "Connect to server", JOptionPane.OK_CANCEL_OPTION);
		if (result == JOptionPane.OK_OPTION) {
			serverAddress = serverAddressField.getText();
			serverPort = Integer.parseInt(portField.getText());
			id = idField.getText();

			try {
				Socket socket = new Socket(serverAddress, serverPort);
				out = new PrintWriter(socket.getOutputStream(), true);
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				out.println(id); // send the chosen ID to the server
				new Thread(this).start();
			} catch (UnknownHostException e) {
				showError("Unknown host: " + serverAddress);
				System.exit(1);
			} catch (IOException e) {
				showError("Could not connect to server: " + e.getMessage());
				System.exit(1);
			}
		} else {
			System.exit(0);
		}
	}

	// Game-related methods
	private void sendPlayerAction(String action) {
		if (out != null) {
			out.println(action);
			out.flush();
		}
	}

	private void handleFoldAction() {
		sendPlayerAction("FOLD");
	}

	private void handleCallAction() {
		sendPlayerAction("CALL");
	}

	private void handleRaiseAction() {
		int raiseAmount = (int) raiseAmountSpinner.getValue();
		sendPlayerAction("RAISE " + raiseAmount);
	}

	private void handleAllInAction() {
		sendPlayerAction("ALL_IN");
	}

	// Networking-related methods
	private void updateAssignedSeat(int assignedSeat) {
		SwingUtilities.invokeLater(() -> {
			// Find the JLabel for the assigned seat
			Component seatLabel = tablePanel.getComponent(assignedSeat);
			if (seatLabel instanceof JLabel) {
				((JLabel) seatLabel).setText("You");
			}
		});
	}

	private void updateOtherSeats(String seatInfo) {
		SwingUtilities.invokeLater(() -> {
			// Update player labels for occupied seats based on seat information
			String[] seatData = seatInfo.split(";");
			for (String data : seatData) {
				if (!data.isEmpty()) {
					String[] seatInfoParts = data.split(",");
					int seatIndex = Integer.parseInt(seatInfoParts[0].trim());
					String playerId = seatInfoParts[1].trim();

					Component seatLabel = tablePanel.getComponent(seatIndex);
					if (seatLabel instanceof JLabel) {
						JLabel label = (JLabel) seatLabel;
						// Skip updating the label for the client's own seat
						if (!playerId.equals(id)) {
							label.setText(playerId);
						} else {
							label.setText("You");
						}
					}
				}
			}
		});
	}

	private void updateLog(String message) {
		SwingUtilities.invokeLater(() -> {
			logTextArea.append(message + "\n");
		});
	}

	// A hook handle the window close event
	private void shutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			if (out != null) {
				out.println("DISCONNECT");
				out.flush();
			}
		}));
	}

	private void showError(String message) {
		JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
	}

	@Override
	public void run() {
		// Listen for updates from the server and update the game state accordingly

		try {
			// Read the seat assignment information
			String response = in.readLine();
			// Check if ID is duplicate then assign a seat
			if (response.equals("DUPLICATE_ID")) {
				showError("Duplicate ID. Please use a unique ID.");
				System.exit(1);
			} else if (response.startsWith("ASSIGNED")) {
				int assignedSeat = Integer.parseInt(response.split(" ")[1]);
				// Call a method to update the UI
				updateAssignedSeat(assignedSeat);
			} else if (response.equals("REFUSED")) {
				showError("Connection refused. The table is full.");
				System.exit(1);
			}
			while (true) {
				response = in.readLine();
				if (response.startsWith("SEATINFO")) {
					String seatInfo = response.substring(8);
					updateOtherSeats(seatInfo);
				} else if (response.startsWith("PLAYER_JOINED")) {
					String playerName = response.split(" ")[1];
					updateLog(playerName + " has joined the game.");
				} else if (response.startsWith("GAME_START")) {
					updateLog("The game has started.");
				} else if (response.startsWith("PLAYER_DISCONNECTED")) {
					String playerName = response.split(" ")[1];
					updateLog(playerName + " has left the game.");
				} else if (response.startsWith("COMMUNITY_CARDS")) {
					// Update community cards based on the server's message
				} else if (response.startsWith("HOLE_CARDS")) {
					// Update player hole cards based on the server's message
				} else if (response.startsWith("POT_SIZE")) {
					// Update the pot size label based on the server's message
				} else if (response.startsWith("PLAYER_ACTION")) {
					// Update the log and/or UI based on the action taken by another player
				}

			}

		} catch (IOException e) {
			showError("Error reading from server: " + e.getMessage());
			System.exit(1);
		}
	}

	public static void main(String[] args) {
		PokerClient pokerClient = new PokerClient();
	}
}
