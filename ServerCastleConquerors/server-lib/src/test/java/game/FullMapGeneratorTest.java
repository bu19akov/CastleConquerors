package game;

import messagesbase.messagesfromserver.*;
import testResult.TestResultLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import main.game.FullMapGenerator;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;

@ExtendWith(TestResultLogger.class)
public class FullMapGeneratorTest {

    private FullMapGenerator generator;

    @BeforeEach
    public void setup() {
        generator = new FullMapGenerator();
    }

    @Test
    public void noInput_generateFullMap_returnsValidFullMap() {
        FullMap map = generator.generateFullMap();

        assertFalse(map.isEmpty(), "Expected a populated map");
    }

    @Test
    public void defaultState_resetFullMap_mapIsEmpty() {
        FullMap map = generator.generateFullMap();
        assertFalse(map.isEmpty(), "Expected a populated map");

        generator.resetFullMap();
        assertTrue(map.isEmpty(), "Expected an empty map after reset");
    }

    @Test
    public void noInput_generateHalfMap_returnsHalfMapOfSize5x10() {
        FullMap halfMap = generator.generateHalfMap();

        int width = 0;
        int height = 0;

        for (FullMapNode node : halfMap) {
            if (node.getY() > height) {
                height = node.getY();
            }
            if (node.getX() > width) {
                width = node.getX();
            }
        }

        assertEquals(FullMapGenerator.getHeight(), height + 1, "Expected a height of 5");
        assertEquals(FullMapGenerator.getWidth(), width + 1, "Expected a width of 10");
    }

    @Test
    public void waterAndGrass_floodFill_returnsFalse() {
        FullMap map = new FullMap();
        for (int i = 0; i < 10; i++) {
            map.addDefaultForTerrain(i < 5 ? ETerrain.Grass : ETerrain.Water, i, 0);
        }
        generator.populateFullMapForFloodFill(map);
        assertFalse(generator.floodFill(), "Expected no isolated islands");
    }

    @Test
    public void grassSurroundedByWater_floodFill_returnsTrue() {
        FullMap map = new FullMap();

        // Setting the base terrain to grass.
        for (int x = 0; x < FullMapGenerator.getWidth(); x++) {
            for (int y = 0; y < FullMapGenerator.getHeight(); y++) {
                map.addDefaultForTerrain(ETerrain.Grass, x, y);
            }
        }

        // Surrounding the terrain at position (4,2) with water on all four sides.
        map.addDefaultForTerrain(ETerrain.Water, 3, 2);
        map.addDefaultForTerrain(ETerrain.Water, 5, 2);
        map.addDefaultForTerrain(ETerrain.Water, 4, 1);
        map.addDefaultForTerrain(ETerrain.Water, 4, 3);

        generator.populateFullMapForFloodFill(map);
        
        assertTrue(generator.floodFill(), "Expected an unreachable terrain to be detected");
    }

    @Test
    public void grassNode_placeItemOnGrass_replacesWithTreasure() {
        FullMap halfMap = new FullMap();
        halfMap.addDefaultForTerrain(ETerrain.Grass, 0, 0);
        generator.placeItemOnGrass(halfMap, ETreasureState.MyTreasureIsPresent, 1);

        Optional<FullMapNode> node = halfMap.get(0, 0);
        assertTrue(node.isPresent());
        assertEquals(node.get().getTreasureState(), ETreasureState.MyTreasureIsPresent);
    }

    @Test
    public void grassNode_placeItemOnGrass_replacesWithFort() {
        FullMap halfMap = new FullMap();
        halfMap.addDefaultForTerrain(ETerrain.Grass, 0, 0);
        generator.placeItemOnGrass(halfMap, EFortState.MyFortPresent, 1);

        Optional<FullMapNode> node = halfMap.get(0, 0);
        assertTrue(node.isPresent());
        assertEquals(node.get().getFortState(), EFortState.MyFortPresent);
    }
}

