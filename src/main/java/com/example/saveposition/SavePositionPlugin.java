package com.example.saveposition;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class SavePositionPlugin extends JavaPlugin {

    private PositionStore store;
    private GotoTracker tracker;

    @Override
    public void onEnable() {
        this.store = new PositionStore(this);
        this.store.load();

        this.tracker = new GotoTracker(this);
        getServer().getPluginManager().registerEvents(tracker, this);

        PluginCommand command = getCommand("pos");
        if (command == null) {
            getLogger().severe("コマンド 'pos' が plugin.yml に登録されていません。");
            return;
        }
        PosCommand handler = new PosCommand(store, tracker);
        command.setExecutor(handler);
        command.setTabCompleter(handler);

        getLogger().info("SavePosition を有効化しました。");
    }

    @Override
    public void onDisable() {
        if (tracker != null) {
            tracker.stopAll();
        }
        if (store != null) {
            store.save();
        }
        getLogger().info("SavePosition を無効化しました。");
    }
}
