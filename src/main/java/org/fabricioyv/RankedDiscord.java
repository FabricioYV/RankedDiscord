package org.fabricioyv;

import org.bukkit.plugin.java.JavaPlugin;
import org.fabricioyv.command.VerifyCommand;
import org.fabricioyv.command.SetWelcomeCommand;
import org.fabricioyv.database.DBManager;
import org.fabricioyv.discord.DiscordGetBot;
import org.fabricioyv.PlayerJoinListener;

public final class RankedDiscord extends JavaPlugin {
    private DBManager DBManager;
    private DiscordGetBot discordGetBot;

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();

        DBManager = new DBManager();
        boolean dbOk = DBManager.initializeDatabase(
                getConfig().getString("database.host"),
                getConfig().getInt("database.port"),
                getConfig().getString("database.database"),
                getConfig().getString("database.username"),
                getConfig().getString("database.password")
        );

        if (!dbOk) {
            getLogger().severe("❌ No se pudo inicializar la base de datos. Deshabilitando RankedDiscord...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        //Initialize Discord Bot
        discordGetBot = new DiscordGetBot(this);
        discordGetBot.initialize(getConfig().getString("discord.token"));
        //Register commands
        getCommand("verify").setExecutor(new VerifyCommand(this));
        getCommand("setwelcome").setExecutor(new SetWelcomeCommand(this));
        //Register event listeners
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        getLogger().info("RankedDiscord habilitado correctamente!");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic

        if(discordGetBot != null) {
            discordGetBot.shutdown();
        }
        if(DBManager != null) {
            DBManager.close();
        }
        getLogger().info("RankedDiscord deshabilitado correctamente!");
    }

    public DBManager getDatabaseManager() {
        return DBManager;
    }

    /**
     * Obtiene la instancia del bot de Discord
     */
    public DiscordGetBot getDiscordBot() {
        return discordGetBot;
    }
}
