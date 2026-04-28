package com.example.saveposition;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class PositionStore {
    private final Plugin plugin;
    private final File file;

    private final Map<UUID, PlayerData> data = new LinkedHashMap<>();

    public PositionStore(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "positions.yml");
    }

    public void load() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("データフォルダの作成に失敗しました: " + plugin.getDataFolder());
        }
        data.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = config.getConfigurationSection("players");
        if (root == null) {
            return;
        }
        for (String uuidStr : root.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("不正な UUID をスキップしました: " + uuidStr);
                continue;
            }
            ConfigurationSection playerSection = root.getConfigurationSection(uuidStr);
            if (playerSection == null) continue;
            String name = playerSection.getString("name", uuidStr);
            PlayerData pd = new PlayerData(name);
            ConfigurationSection positionsSection = playerSection.getConfigurationSection("positions");
            if (positionsSection != null) {
                for (String posName : positionsSection.getKeys(false)) {
                    ConfigurationSection posSection = positionsSection.getConfigurationSection(posName);
                    if (posSection == null) continue;
                    pd.positions.put(posName, SavedPosition.read(posName, posSection));
                }
            }
            data.put(uuid, pd);
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection root = config.createSection("players");
        for (var entry : data.entrySet()) {
            ConfigurationSection playerSection = root.createSection(entry.getKey().toString());
            playerSection.set("name", entry.getValue().name);
            ConfigurationSection positionsSection = playerSection.createSection("positions");
            for (var posEntry : entry.getValue().positions.entrySet()) {
                ConfigurationSection posSection = positionsSection.createSection(posEntry.getKey());
                posEntry.getValue().writeTo(posSection);
            }
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("positions.yml の保存に失敗しました: " + e.getMessage());
        }
    }

    public Optional<SavedPosition> get(UUID owner, String name) {
        PlayerData pd = data.get(owner);
        if (pd == null) return Optional.empty();
        return Optional.ofNullable(pd.positions.get(name));
    }

    public List<SavedPosition> listOwn(UUID owner) {
        PlayerData pd = data.get(owner);
        if (pd == null) return List.of();
        return new ArrayList<>(pd.positions.values());
    }

    public void put(UUID owner, String ownerName, SavedPosition position) {
        PlayerData pd = data.computeIfAbsent(owner, k -> new PlayerData(ownerName));
        pd.name = ownerName;
        pd.positions.put(position.name(), position);
        save();
    }

    public boolean remove(UUID owner, String name) {
        PlayerData pd = data.get(owner);
        if (pd == null) return false;
        if (pd.positions.remove(name) != null) {
            save();
            return true;
        }
        return false;
    }

    public List<SharedEntry> listAllShared() {
        List<SharedEntry> result = new ArrayList<>();
        for (var entry : data.entrySet()) {
            for (SavedPosition pos : entry.getValue().positions.values()) {
                if (pos.shared()) {
                    result.add(new SharedEntry(entry.getKey(), entry.getValue().name, pos));
                }
            }
        }
        return result;
    }

    public Optional<UUID> findUuidByName(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        for (var entry : data.entrySet()) {
            if (entry.getValue().name.toLowerCase(Locale.ROOT).equals(lower)) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    public List<SavedPosition> listSharedBy(UUID owner) {
        PlayerData pd = data.get(owner);
        if (pd == null) return List.of();
        return pd.positions.values().stream().filter(SavedPosition::shared).toList();
    }

    public List<String> knownPlayerNames() {
        return data.values().stream().map(p -> p.name).toList();
    }

    public record SharedEntry(UUID owner, String ownerName, SavedPosition position) {}

    private static final class PlayerData {
        String name;
        final Map<String, SavedPosition> positions = new LinkedHashMap<>();

        PlayerData(String name) {
            this.name = name;
        }
    }
}
