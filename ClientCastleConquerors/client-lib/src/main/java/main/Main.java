package main;

import messagesbase.messagesfromclient.PlayerRegistration;

public class Main {
    public static void main(String[] args) {
        ClientNetwork clientNetwork = new ClientNetwork("http://localhost:18235");
        String gameID = "", playerID = "";

        try {
             gameID = clientNetwork.retrieveUniqueGameIdentifier();
             playerID = clientNetwork.sendPlayerRegistration(
                    new PlayerRegistration("test_username")
            );
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
        }
        System.out.println(gameID +" " + playerID);
    }
}
