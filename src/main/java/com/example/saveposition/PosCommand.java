package com.example.saveposition;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class PosCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of(
            "save", "list", "show", "delete", "share", "unshare", "shared"
    );

    private final PositionStore store;

    public PosCommand(PositionStore store) {
        this.store = store;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "save" -> handleSave(sender, args);
            case "list" -> handleList(sender);
            case "show" -> handleShow(sender, args);
            case "delete", "remove", "del" -> handleDelete(sender, args);
            case "share" -> handleShareToggle(sender, args, true);
            case "unshare" -> handleShareToggle(sender, args, false);
            case "shared" -> handleShared(sender, args);
            default -> { sendHelp(sender); yield true; }
        };
    }

    private boolean handleSave(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return true;
        if (args.length < 2) {
            sender.sendMessage(Component.text("使用法: /pos save <名前>", NamedTextColor.YELLOW));
            return true;
        }
        String name = args[1];
        boolean previouslyShared = store.get(player.getUniqueId(), name)
                .map(SavedPosition::shared)
                .orElse(false);
        Location loc = player.getLocation();
        SavedPosition pos = SavedPosition.fromLocation(name, loc, previouslyShared, System.currentTimeMillis());
        store.put(player.getUniqueId(), player.getName(), pos);
        sender.sendMessage(Component.text("座標を保存しました: " + name, NamedTextColor.GREEN));
        sender.sendMessage(formatLine(pos));
        return true;
    }

    private boolean handleList(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player == null) return true;
        List<SavedPosition> positions = store.listOwn(player.getUniqueId());
        if (positions.isEmpty()) {
            sender.sendMessage(Component.text("保存された座標はありません。", NamedTextColor.GRAY));
            return true;
        }
        sender.sendMessage(Component.text("=== 保存座標一覧 (" + positions.size() + ") ===", NamedTextColor.AQUA));
        for (SavedPosition pos : positions) {
            sender.sendMessage(formatLine(pos));
        }
        return true;
    }

    private boolean handleShow(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return true;
        if (args.length < 2) {
            sender.sendMessage(Component.text("使用法: /pos show <名前>", NamedTextColor.YELLOW));
            return true;
        }
        Optional<SavedPosition> pos = store.get(player.getUniqueId(), args[1]);
        if (pos.isEmpty()) {
            sender.sendMessage(Component.text("見つかりません: " + args[1], NamedTextColor.RED));
            return true;
        }
        sender.sendMessage(formatLine(pos.get()));
        return true;
    }

    private boolean handleDelete(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return true;
        if (args.length < 2) {
            sender.sendMessage(Component.text("使用法: /pos delete <名前>", NamedTextColor.YELLOW));
            return true;
        }
        if (store.remove(player.getUniqueId(), args[1])) {
            sender.sendMessage(Component.text("削除しました: " + args[1], NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("見つかりません: " + args[1], NamedTextColor.RED));
        }
        return true;
    }

    private boolean handleShareToggle(CommandSender sender, String[] args, boolean shared) {
        Player player = requirePlayer(sender);
        if (player == null) return true;
        String label = shared ? "share" : "unshare";
        if (args.length < 2) {
            sender.sendMessage(Component.text("使用法: /pos " + label + " <名前>", NamedTextColor.YELLOW));
            return true;
        }
        Optional<SavedPosition> existing = store.get(player.getUniqueId(), args[1]);
        if (existing.isEmpty()) {
            sender.sendMessage(Component.text("見つかりません: " + args[1], NamedTextColor.RED));
            return true;
        }
        SavedPosition updated = existing.get().withShared(shared);
        store.put(player.getUniqueId(), player.getName(), updated);
        sender.sendMessage(Component.text(
                args[1] + " を " + (shared ? "共有しました" : "非共有にしました") + "。",
                shared ? NamedTextColor.GREEN : NamedTextColor.YELLOW
        ));
        return true;
    }

    private boolean handleShared(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            String targetName = args[1];
            Optional<UUID> uuidOpt = resolveUuid(targetName);
            if (uuidOpt.isEmpty()) {
                sender.sendMessage(Component.text(
                        "プレイヤーが見つかりません: " + targetName, NamedTextColor.RED));
                return true;
            }
            List<SavedPosition> list = store.listSharedBy(uuidOpt.get());
            if (list.isEmpty()) {
                sender.sendMessage(Component.text(
                        targetName + " の共有座標はありません。", NamedTextColor.GRAY));
                return true;
            }
            sender.sendMessage(Component.text(
                    "=== " + targetName + " の共有座標 (" + list.size() + ") ===", NamedTextColor.AQUA));
            for (SavedPosition pos : list) {
                sender.sendMessage(formatLine(pos));
            }
            return true;
        }

        List<PositionStore.SharedEntry> all = store.listAllShared();
        if (all.isEmpty()) {
            sender.sendMessage(Component.text("共有された座標はありません。", NamedTextColor.GRAY));
            return true;
        }
        sender.sendMessage(Component.text(
                "=== 共有座標一覧 (" + all.size() + ") ===", NamedTextColor.AQUA));
        for (PositionStore.SharedEntry entry : all) {
            sender.sendMessage(Component.text(" [" + entry.ownerName() + "] ", NamedTextColor.GOLD)
                    .append(formatLine(entry.position())));
        }
        return true;
    }

    private Optional<UUID> resolveUuid(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return Optional.of(online.getUniqueId());
        return store.findUuidByName(name);
    }

    @Nullable
    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player p) return p;
        sender.sendMessage(Component.text("このコマンドはプレイヤーのみ実行できます。", NamedTextColor.RED));
        return null;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== SavePosition コマンド ===", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("/pos save <名前>          現在地を保存", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/pos list                 自分の保存一覧", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/pos show <名前>          座標を表示", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/pos delete <名前>        削除", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/pos share <名前>         共有 ON", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/pos unshare <名前>       共有 OFF", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/pos shared [プレイヤー]  共有座標一覧", NamedTextColor.GRAY));
    }

    private Component formatLine(SavedPosition pos) {
        Component flag = pos.shared()
                ? Component.text(" [共有]", NamedTextColor.GREEN)
                : Component.empty();
        return Component.text(" - " + pos.name(), NamedTextColor.WHITE)
                .append(flag)
                .append(Component.text(" " + formatCoords(pos), NamedTextColor.GRAY));
    }

    private String formatCoords(SavedPosition pos) {
        return String.format(Locale.ROOT, "(%s: %.1f, %.1f, %.1f)",
                pos.worldName(), pos.x(), pos.y(), pos.z());
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return filterPrefix(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            return switch (sub) {
                case "show", "delete", "remove", "del", "share", "unshare" -> {
                    if (sender instanceof Player player) {
                        List<String> names = store.listOwn(player.getUniqueId()).stream()
                                .map(SavedPosition::name)
                                .toList();
                        yield filterPrefix(names, args[1]);
                    }
                    yield List.of();
                }
                case "shared" -> {
                    List<String> names = new ArrayList<>(store.knownPlayerNames());
                    Bukkit.getOnlinePlayers().forEach(p -> {
                        if (!names.contains(p.getName())) names.add(p.getName());
                    });
                    yield filterPrefix(names, args[1]);
                }
                default -> List.of();
            };
        }
        return List.of();
    }

    private List<String> filterPrefix(List<String> values, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(v -> v.toLowerCase(Locale.ROOT).startsWith(lower))
                .toList();
    }
}
