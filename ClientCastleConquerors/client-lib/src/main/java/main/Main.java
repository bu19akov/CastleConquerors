package main;

import java.util.Scanner;

import messagesbase.messagesfromclient.EMove;
import messagesbase.messagesfromclient.PlayerRegistration;
import messagesbase.messagesfromserver.*;

public class Main {
	private static ClientNetwork clientNetwork;
	
	public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String gameID = "", playerID = "";
        FullMap map;

        // Step 1: Prompt the user to initialize the game or provide a gameID.
        System.out.println("Press Enter to initialize the game or enter the gameID to join an existing game.");
        gameID = scanner.nextLine();

        if (gameID.isEmpty()) {
            try {
            	clientNetwork = new ClientNetwork("http://localhost:18235");
                gameID = clientNetwork.retrieveUniqueGameIdentifier();
                System.out.println("Game initialized. GameID: " + gameID);
            } catch (Exception e) {
                System.out.println("Error initializing game: " + e.getMessage());
                return;
            }
        } else {
        	try {
        		clientNetwork = new ClientNetwork("http://localhost:18235", gameID);
        	} catch (Exception e) {
                System.out.println("Error initializing game: " + e.getMessage());
                return;
            }
        }

        // Step 2: Register the player.
        System.out.println("Enter your username:");
        String username = scanner.nextLine();

        try {
            playerID = clientNetwork.sendPlayerRegistration(new PlayerRegistration(username));
            System.out.println("Player registered. PlayerID: " + playerID);

            // Step 3: Display the full map.
            map = clientNetwork.retrieveMapState(playerID);
            printMapWithChars(map);
            System.out.println("Waiting for the other player...");
        } catch (Exception e) {
            System.out.println("Error registering player: " + e.getMessage());
            return;
        }

        while (true) {
            try {
                PlayerState playerState = clientNetwork.getPlayerState(playerID);
                
                if (playerState.getState() == EPlayerGameState.Won) {
                	System.out.println("Congratulations! You have won!");
                	break;
                } else if (playerState.getState() == EPlayerGameState.Lost) {
                	System.out.println("It's always hard to say... But you have lost...");
                	break;
                } else if (playerState.getState() == EPlayerGameState.MustAct) {
                    System.out.println("It's your turn! Enter a move (Up, Down, Left, Right):");
                    String moveString = scanner.nextLine();

                    EMove move = EMove.valueOf(moveString);
                    clientNetwork.sendPlayerMove(playerID, move);

                    // Display the updated map.
                    map = clientNetwork.retrieveMapState(playerID);
                    printMapWithChars(map);

                } else if (playerState.getState() == EPlayerGameState.MustWait) {
                    Thread.sleep(1000); // Sleep for 1 second before checking again.
                }

            } catch (Exception e) {
                System.out.println("Error during gameplay: " + e.getMessage());
                return;
            }
        }
	}

    private static int getMapMaxX(FullMap map) {
        int maxX = 0;
        for (FullMapNode node : map.getMapNodes()) {
            if (node.getX() > maxX) maxX = node.getX();
        }
        return maxX;
    }

    private static int getMapMaxY(FullMap map) {
        int maxY = 0;
        for (FullMapNode node : map.getMapNodes()) {
            if (node.getX() > maxY) maxY = node.getY();
        }
        return maxY;
    }


    //DEBUG

    public static void printMapWithChars(FullMap map) {
        for (int i = 0; i <= getMapMaxY(map); i++) {
            for (int j = 0; j <= getMapMaxX(map); j++) {
                System.out.print(translateMapNode(map.get(j, i).get()) + " ");
            }
            System.out.println();
        }
    }


    public static String translateMapNode(FullMapNode node) {
        if (node.getPlayerPositionState() == EPlayerPositionState.MyPlayerPosition)
            return "*";
        if (node.getPlayerPositionState() == EPlayerPositionState.EnemyPlayerPosition)
            return "E";
        if (node.getTreasureState() == ETreasureState.MyTreasureIsPresent)
            return "T";
        if (node.getFortState() == EFortState.MyFortPresent)
            return "f";
        if (node.getFortState() == EFortState.EnemyFortPresent)
            return "F";
        if (node.getTerrain() == ETerrain.Water)
            return "W";
        if (node.getTerrain() == ETerrain.Mountain)
            return "M";
        if (node.getTerrain() == ETerrain.Grass)
            return "G";
        return " ";
    }
}
