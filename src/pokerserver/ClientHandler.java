package pokerserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ClientHandler implements Runnable {
	private PokerServer server;
	private Socket clientSocket;
	private String id;
	
	public ClientHandler(PokerServer server, Socket clientSocket) {
		this.server = server;
		this.clientSocket = clientSocket;
	}

	@Override
	public void run() {
		try {
			// Create input and output streams for the client's socket
			BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

			// Read the client's ID from the input stream
			String id = in.readLine();
			server.serverTextArea.append("Client " + id + " connected\n");
			server.broadcast("PLAYER_JOINED " + id);
			// Add the client's output stream to the list of clients
			synchronized (server.clients) {
				server.clients.add(out);
			}
			// Check for duplicate IDs and handle accordingly
			synchronized (server.uniqueIDs) {
				if (server.uniqueIDs.contains(id)) {
					out.println("DUPLICATE_ID");
					clientSocket.close();
					return;
				} else {
					server.uniqueIDs.add(id);
				}
			}
			// Seat Handling
			int assignedSeat = -1;
			List<Integer> availableSeats = new ArrayList<>();
			synchronized (server.seats) {
				// Create a available seat with seat number 0-5 for clients, if the seat number
				// is occupied then skip
				for (int i = 0; i < server.MAX_PLAYERS; i++) {
					if (server.seats[i] == null) {
						server.seats[i] = new PlayerSeat(i);
					}
					if (!server.seats[i].isOccupied) {
						availableSeats.add(i);
					}
				}
				if (!availableSeats.isEmpty()) {
					int randomIndex = new Random().nextInt(availableSeats.size());
					assignedSeat = availableSeats.get(randomIndex);
					server.seats[assignedSeat].assignSeat(id); // Assign a random available seat to the connected client
				}
			}
			if (assignedSeat == -1) {
				out.println("REFUSED");
				clientSocket.close();
				return;
			} else {
				// Broadcast the updated seat information to all clients
				server.broadcast("SEATINFO " + server.getSeatInfo());
				out.println("ASSIGNED " + assignedSeat);
				out.println("SEATINFO " + server.getSeatInfo());
			}
			synchronized (server.uniqueIDs) {
				int playerCount = server.uniqueIDs.size();
//				if (playerCount >= 2 && server.countdownTimer != null && !server.countdownTimer.isRunning()) {
//					server.countdownTimer = new CountdownTimer(server, 10, () -> server.uniqueIDs.size());
//					server.countdownTimer.start();
//				}
				if (playerCount >= 2 && !server.isRoundInProgress()) {
					server.serverTextArea.append("abcd\n");
					server.startNewRound();
				}
			}
			// TODO
			// Handle the "DISCONNECT" message
			while (true) {
				String message = in.readLine();
				if (message == null || message.equals("DISCONNECT")) {
					synchronized (server.seats) {
						server.seats[assignedSeat].releaseSeat();
					}
					server.serverTextArea.append("Client " + id + " disconnected\n");
					server.broadcast("PLAYER_DISCONNECTED " + id);
					server.broadcast("SEATINFO " + server.getSeatInfo());
					break;
				} else if (message.startsWith("ACTION")) {
                    handlePlayerAction(message);
                }
			}

			// Remove the client's output stream from the list of clients and the unique ID
			synchronized (server.clients) {
				server.clients.remove(out);
			}
			synchronized (server.uniqueIDs) {
				server.uniqueIDs.remove(id);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void handlePlayerAction(String message) {
	    // Extract the player action from the message, e.g., "ACTION FOLD"
	    String action = message.substring("ACTION".length()).trim();

	    // Get the current game
	    PokerGame game = server.getCurrentGame();

	    // Check if it is the current player's turn
	    Player currentPlayer = game.getCurrentPlayer();
	    
	    if (currentPlayer.getId().equals(id)) {
	        synchronized (game) {
	            // Update the game state with the player's action
	            game.handleAction(id, action);

	            // Notify the server that the action has been processed
	            game.notifyAll();
	        }
	    } else if ("FOLD".equals(action)) {
	        // If it's not the current player's turn, but they're folding (probably due to disconnect),
	        // then handle the action anyway
	        synchronized (game) {
	            game.handleAction(id, action);
	            game.notifyAll();
	        }
	    } 
	    // Ignore all other actions from players whose turn it is not
	}

}
