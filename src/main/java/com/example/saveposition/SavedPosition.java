package com.example.saveposition;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.UUID;

public record SavedPosition(
        String name,
        UUID worldId,
        String worldName,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        boolean shared,
        long createdAt
) {
    public static SavedPosition fromLocation(String name, Location loc, boolean shared, long createdAt) {
        World world = loc.getWorld();
        UUID worldId = world != null ? world.getUID() : new UUID(0L, 0L);
        String worldName = world != null ? world.getName() : "unknown";
        return new SavedPosition(
                name, worldId, worldName,
                loc.getX(), loc.getY(), loc.getZ(),
                loc.getYaw(), loc.getPitch(),
                shared, createdAt
        );
    }

    public SavedPosition withShared(boolean newShared) {
        return new SavedPosition(name, worldId, worldName, x, y, z, yaw, pitch, newShared, createdAt);
    }

    public void writeTo(ConfigurationSection section) {
        section.set("worldId", worldId.toString());
        section.set("worldName", worldName);
        section.set("x", x);
        section.set("y", y);
        section.set("z", z);
        section.set("yaw", yaw);
        section.set("pitch", pitch);
        section.set("shared", shared);
        section.set("createdAt", createdAt);
    }

    public static SavedPosition read(String name, ConfigurationSection section) {
        String worldIdStr = section.getString("worldId");
        UUID worldId;
        try {
            worldId = worldIdStr != null ? UUID.fromString(worldIdStr) : new UUID(0L, 0L);
        } catch (IllegalArgumentException e) {
            worldId = new UUID(0L, 0L);
        }
        String worldName = section.getString("worldName", "unknown");
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw");
        float pitch = (float) section.getDouble("pitch");
        boolean shared = section.getBoolean("shared");
        long createdAt = section.getLong("createdAt");
        return new SavedPosition(name, worldId, worldName, x, y, z, yaw, pitch, shared, createdAt);
    }
}
