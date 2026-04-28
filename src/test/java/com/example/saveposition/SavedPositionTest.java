package com.example.saveposition;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SavedPositionTest {

    @Test
    void writeThenReadProducesEqualValue() {
        UUID worldId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        SavedPosition original = new SavedPosition(
                "home", worldId, "world",
                12.5, 64.0, -7.25,
                90f, 15f,
                true, 1_700_000_000L
        );

        YamlConfiguration config = new YamlConfiguration();
        var section = config.createSection("position");
        original.writeTo(section);

        SavedPosition reloaded = SavedPosition.read(original.name(), section);

        assertEquals(original, reloaded);
    }

    @Test
    void readSurvivesYamlSerializationRoundTrip() {
        UUID worldId = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
        SavedPosition original = new SavedPosition(
                "spawn", worldId, "world_nether",
                0.0, 70.0, 0.0,
                0f, 0f,
                false, 42L
        );

        YamlConfiguration write = new YamlConfiguration();
        original.writeTo(write.createSection("p"));
        String yaml = write.saveToString();

        YamlConfiguration read = new YamlConfiguration();
        try {
            read.loadFromString(yaml);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        SavedPosition reloaded = SavedPosition.read("spawn", read.getConfigurationSection("p"));

        assertEquals(original, reloaded);
    }

    @Test
    void withSharedReturnsNewInstanceWithFlippedFlag() {
        SavedPosition original = new SavedPosition(
                "p", new UUID(0L, 0L), "world",
                1.0, 2.0, 3.0, 0f, 0f, false, 0L
        );
        SavedPosition shared = original.withShared(true);

        assertNotSame(original, shared);
        assertFalse(original.shared());
        assertTrue(shared.shared());
        // Other fields unchanged
        assertEquals(original.name(), shared.name());
        assertEquals(original.x(), shared.x());
    }
}
