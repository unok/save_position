package com.example.saveposition;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class GotoTracker implements Listener {

    private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};
    private static final long UPDATE_PERIOD_TICKS = 5L;
    private static final double FULL_BAR_DISTANCE = 500.0;

    private final Plugin plugin;
    private final Map<UUID, ActiveGuide> active = new HashMap<>();
    private BukkitTask updater;

    public GotoTracker(Plugin plugin) {
        this.plugin = plugin;
    }

    public void start(Player player, SavedPosition target) {
        ActiveGuide existing = active.get(player.getUniqueId());
        if (existing != null) {
            existing.target = target;
            updateBar(player, existing);
            ensureUpdaterRunning();
            return;
        }
        BossBar bar = BossBar.bossBar(
                Component.text("…"),
                1.0f,
                BossBar.Color.YELLOW,
                BossBar.Overlay.PROGRESS
        );
        ActiveGuide guide = new ActiveGuide(bar, target);
        active.put(player.getUniqueId(), guide);
        player.showBossBar(bar);
        updateBar(player, guide);
        ensureUpdaterRunning();
    }

    public boolean stop(Player player) {
        ActiveGuide removed = active.remove(player.getUniqueId());
        if (removed == null) return false;
        player.hideBossBar(removed.bar);
        if (active.isEmpty() && updater != null) {
            updater.cancel();
            updater = null;
        }
        return true;
    }

    public void stopAll() {
        for (var entry : active.entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p != null) p.hideBossBar(entry.getValue().bar);
        }
        active.clear();
        if (updater != null) {
            updater.cancel();
            updater = null;
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        stop(event.getPlayer());
    }

    public boolean isActive(UUID playerId) {
        return active.containsKey(playerId);
    }

    private void ensureUpdaterRunning() {
        if (updater != null) return;
        updater = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, UPDATE_PERIOD_TICKS, UPDATE_PERIOD_TICKS);
    }

    private void tick() {
        if (active.isEmpty()) return;
        active.forEach((uuid, guide) -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) return;
            updateBar(p, guide);
        });
    }

    private void updateBar(Player player, ActiveGuide guide) {
        SavedPosition target = guide.target;
        World targetWorld = Bukkit.getWorld(target.worldId());
        Location loc = player.getLocation();
        if (targetWorld == null) {
            guide.bar.name(Component.text(
                    "目的地『" + target.name() + "』のワールド (" + target.worldName() + ") が見つかりません",
                    NamedTextColor.RED));
            guide.bar.progress(0.0f);
            return;
        }
        if (!targetWorld.equals(loc.getWorld())) {
            guide.bar.name(Component.text(
                    String.format(Locale.ROOT,
                            "目的地『%s』は別ワールド %s (%.0f, %.0f, %.0f)",
                            target.name(),
                            target.worldName(),
                            target.x(), target.y(), target.z()),
                    NamedTextColor.GOLD));
            guide.bar.progress(0.0f);
            return;
        }
        double dx = target.x() - loc.getX();
        double dz = target.z() - loc.getZ();
        double dist = Math.hypot(dx, dz);
        double bearing = Math.toDegrees(Math.atan2(-dx, dz));
        double relative = ((bearing - loc.getYaw()) % 360.0 + 540.0) % 360.0 - 180.0;
        int idx = ((int) Math.round(relative / 45.0) % 8 + 8) % 8;
        String arrow = ARROWS[idx];
        long distInt = Math.round(dist);
        guide.bar.name(Component.text(
                String.format(Locale.ROOT, "%s 目的地『%s』まで %dm",
                        arrow, target.name(), distInt),
                NamedTextColor.AQUA));
        float progress = (float) Math.max(0.0, Math.min(1.0, 1.0 - dist / FULL_BAR_DISTANCE));
        guide.bar.progress(progress);
    }

    private static final class ActiveGuide {
        final BossBar bar;
        SavedPosition target;

        ActiveGuide(BossBar bar, SavedPosition target) {
            this.bar = bar;
            this.target = target;
        }
    }
}
