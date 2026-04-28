package com.example.saveposition;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class SavePositionPlugin extends JavaPlugin {

    private PositionStore store;

    @Override
    public void onEnable() {
        this.store = new PositionStore(this);
        this.store.load();

        PluginCommand command = getCommand("pos");
        if (command == null) {
            getLogger().severe("コマンド 'pos' が plugin.yml に登録されていません。");
            return;
        }
        PosCommand handler = new PosCommand(store);
        command.setExecutor(handler);
        command.setTabCompleter(handler);

        getLogger().info("SavePosition を有効化しました。");
    }

    @Override
    public void onDisable() {
        if (store != null) {
            store.save();
        }
        getLogger().info("SavePosition を無効化しました。");
    }
}
