package main;

import messagesbase.messagesfromclient.PlayerRegistration;
import messagesbase.messagesfromserver.FullMap;
import messagesbase.messagesfromserver.FullMapNode;

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
            map = clientNetwork.retrieveMapState(playerID1);

            for (FullMapNode node : map.getMapNodes()) {
                System.out.print(node.toString());
            }

        } catch (Exception exception) {
            System.out.println(exception.getMessage());
        }
        System.out.println(gameID + " " + playerID1);
    }
}
