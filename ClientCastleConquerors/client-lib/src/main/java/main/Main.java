package main;

import messagesbase.messagesfromclient.EMove;
import messagesbase.messagesfromclient.PlayerRegistration;
import messagesbase.messagesfromserver.*;

public class Main {
    public static void main(String[] args) {
        ClientNetwork clientNetwork = new ClientNetwork("http://localhost:18235");
        String gameID = "", playerID1 = "", playerID2 = "";
        FullMap map;

        try {
            gameID = clientNetwork.retrieveUniqueGameIdentifier();
            playerID1 = clientNetwork.sendPlayerRegistration(
                    new PlayerRegistration("test_username1")
            );
            playerID2 = clientNetwork.sendPlayerRegistration(
                    new PlayerRegistration("test_username2")
            );


            System.out.println("Player1 POV: ");
            map = clientNetwork.retrieveMapState(playerID1);
            printMapWithChars(map);

            System.out.println("Moving left");
            clientNetwork.sendPlayerMove(playerID1, EMove.Left);
            clientNetwork.sendPlayerMove(playerID2, EMove.Left);
            clientNetwork.sendPlayerMove(playerID1, EMove.Left);
            clientNetwork.sendPlayerMove(playerID2, EMove.Left);
            map = clientNetwork.retrieveMapState(playerID1);
            printMapWithChars(map);



        } catch (Exception exception) {
            System.out.println(exception.getMessage());
        }
        System.out.println(gameID + " " + playerID1);
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
            return "P";
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
