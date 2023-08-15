package game;

import java.util.Optional;
import java.util.Random;
import messagesbase.messagesfromserver.*;

public class FullMapGenerator {
    private static final int WIDTH = 10;
    private static final int HEIGHT = 5;
    private static final int MIN_MOUNTAINS = 5;
    private static final int MAX_MOUNTAINS = 19;
    private static final int NUM_WATER = 7;
    private static final Random random = new Random();
    private FullMap fullMap = new FullMap();

    public FullMap generateFullMap() {
        mergeMaps(generateHalfMap(), generateHalfMap());
        return this.fullMap;
    }

    private void mergeMaps(FullMap firstHalf, FullMap secondHalf) {
        placeTreasureAndFort(firstHalf, 1);
        placeTreasureAndFort(secondHalf, 2);
        mergeHalfMapsBasedOnPosition(firstHalf, secondHalf);
    }

    private void mergeHalfMapsBasedOnPosition(FullMap firstHalf, FullMap secondHalf) {
        switch (random.nextInt(4)) {
            case 0:
                addNodes(firstHalf);
                addNodes(secondHalf, 0, 5);
                break;
            case 1:
                addNodes(firstHalf);
                addNodes(secondHalf, 10, 0);
                break;
            case 2: 
                addNodes(secondHalf);
                addNodes(firstHalf, 0, 5);
                break;
            case 3:
                addNodes(secondHalf);
                addNodes(firstHalf, 10, 0);
                break;
        }
    }

    private void addNodes(FullMap halfMap, int shiftX, int shiftY) {
        for (FullMapNode node : halfMap.getMapNodes()) {
            int newX = node.getX() + shiftX;
            int newY = node.getY() + shiftY;
            fullMap.add(new FullMapNode(node.getTerrain(), 
            							node.getPlayerPositionState(), 
            							node.getTreasureState(), 
            							node.getFortState(), 
            							newX, 
            							newY, 
            							node.getOwnedByPlayer()));
            updateMapDimensions(newX, newY);
        }
    }

    private void updateMapDimensions(int newX, int newY) {
        if (newX > fullMap.getMaxX()) {
            fullMap.setMaxX(newX);
        }
        if (newY > fullMap.getMaxY()) {
            fullMap.setMaxY(newY);
        }
    }

    private void addNodes(FullMap halfMap) {
        addNodes(halfMap, 0, 0);
    }

    private FullMap generateHalfMap() {
        FullMap halfMap = new FullMap();
        initializeHalfMapWithGrass(halfMap);
        populateRandomTerrain(halfMap, ETerrain.Mountain, randomMountainsCount());
        populateRandomTerrain(halfMap, ETerrain.Water, NUM_WATER);
        return halfMap;
    }

    private void initializeHalfMapWithGrass(FullMap halfMap) {
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                halfMap.addDefaultForTerrain(ETerrain.Grass, x, y);
            }
        }
    }

    private int randomMountainsCount() {
        return random.nextInt(MAX_MOUNTAINS - MIN_MOUNTAINS + 1) + MIN_MOUNTAINS;
    }

    private void populateRandomTerrain(FullMap halfMap, ETerrain terrain, int count) {
        while (count != 0) {
            int y = random.nextInt(HEIGHT);
            int x = random.nextInt(WIDTH);

            if (placeTerrainIfPossible(halfMap, x, y, terrain)) {
                count--;
            }
        }
    }

    private boolean placeTerrainIfPossible(FullMap halfMap, int x, int y, ETerrain terrain) {
        Optional<FullMapNode> optionalNode = halfMap.get(x, y);
        if (optionalNode.isPresent() && optionalNode.get().getTerrain() == ETerrain.Grass) {
            halfMap.addDefaultForTerrain(terrain, x, y);
            return true;
        }
        return false;
    }

    private void placeTreasureAndFort(FullMap halfMap, int ownedByPlayer) {
        placeItemOnGrass(halfMap, ETreasureState.MyTreasureIsPresent, ownedByPlayer);
        placeItemOnGrass(halfMap, EFortState.MyFortPresent, ownedByPlayer);
    }

    private void placeItemOnGrass(FullMap halfMap, Enum<?> item, int ownedByPlayer) {
        while (true) {
            int x = random.nextInt(WIDTH);
            int y = random.nextInt(HEIGHT);
            Optional<FullMapNode> optionalNode = halfMap.get(x, y);
            
            if (canPlaceItemOnNode(item, optionalNode)) {
                replaceNodeWithNewItem(halfMap, item, x, y, ownedByPlayer);
                break;
            }
        }
    }

    private boolean canPlaceItemOnNode(Enum<?> item, Optional<FullMapNode> optionalNode) {
        if (!optionalNode.isPresent()) {
            return false;
        }

        FullMapNode node = optionalNode.get();
        return node.getTerrain() == ETerrain.Grass && 
        	   node.getTreasureState() == ETreasureState.NoOrUnknownTreasureState && 
        	   node.getFortState() == EFortState.NoOrUnknownFortState;
    }

    private void replaceNodeWithNewItem(FullMap halfMap, Enum<?> item, int x, int y, int ownedByPlayer) {
        halfMap.remove(halfMap.get(x, y).get());

        if (item == ETreasureState.MyTreasureIsPresent) {
            halfMap.add(new FullMapNode(ETerrain.Grass, 
            							EPlayerPositionState.NoPlayerPresent, 
            							ETreasureState.MyTreasureIsPresent, 
            							EFortState.NoOrUnknownFortState, 
            							x, 
            							y, 
            							ownedByPlayer));
        } else if (item == EFortState.MyFortPresent) {
            halfMap.add(new FullMapNode(ETerrain.Grass, 
            							EPlayerPositionState.MyPlayerPosition, 
            							ETreasureState.NoOrUnknownTreasureState, 
            							EFortState.MyFortPresent, 
            							x, 
            							y, 
            							ownedByPlayer));
        }
    }

    public static int getWidth() {
        return WIDTH;
    }

    public static int getHeight() {
        return HEIGHT;
    }

    public static int getMinMountains() {
        return MIN_MOUNTAINS;
    }

    public static int getMaxMountains() {
        return MAX_MOUNTAINS;
    }
}
