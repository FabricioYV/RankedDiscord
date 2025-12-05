package org.fabricioyv;

import org.bukkit.plugin.java.JavaPlugin;
import org.fabricioyv.command.VerifyCommand;
import org.fabricioyv.command.SetWelcomeCommand;
import org.fabricioyv.database.DBManager;
import org.fabricioyv.discord.DiscordGetBot;
import org.fabricioyv.PlayerJoinListener;
import org.fabricioyv.purchase.RankPurchaseTask;

public final class RankedDiscord extends JavaPlugin {
    private DBManager DBManager;
    private DiscordGetBot discordGetBot;
    private RankPurchaseTask rankPurchaseTask; // Nuevo campo para el task de compras

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

        // Inicializar el sistema de compras de rangos
        initializePurchaseSystem();

        getLogger().info("RankedDiscord habilitado correctamente!");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic

        // Cancelar task de compras si está ejecutándose
        if (rankPurchaseTask != null) {
            rankPurchaseTask.cancel();
            getLogger().info("✅ Task de compras cancelado correctamente");
        }

        if(discordGetBot != null) {
            discordGetBot.shutdown();
        }
        if(DBManager != null) {
            DBManager.close();
        }
        getLogger().info("RankedDiscord deshabilitado correctamente!");
    }

    /**
     * Inicializa el sistema de compras de rangos
     */
    private void initializePurchaseSystem() {
        // Verificar si el sistema de compras está habilitado
        boolean storeEnabled = getConfig().getBoolean("store.enabled", true);

        if (!storeEnabled) {
            getLogger().info("§7Sistema de compras deshabilitado en la configuración");
            return;
        }

        // Obtener configuración del sistema de compras
        int checkInterval = getConfig().getInt("store.check-interval", 30); // segundos
        int maxPurchasesPerCycle = getConfig().getInt("store.max-purchases-per-cycle", 10);

        // Crear y programar el task
        rankPurchaseTask = new RankPurchaseTask(this, DBManager, maxPurchasesPerCycle);

        // Convertir segundos a ticks (20 ticks = 1 segundo)
        long intervalTicks = checkInterval * 20L;

        // Iniciar el task asíncrono (se ejecuta cada X segundos)
        rankPurchaseTask.runTaskTimerAsynchronously(this, 60L, intervalTicks); // Esperar 3 segundos antes de empezar

        getLogger().info("§a✅ Sistema de compras inicializado correctamente");
        getLogger().info("§7• Intervalo de revisión: " + checkInterval + " segundos");
        getLogger().info("§7• Máximo compras por ciclo: " + maxPurchasesPerCycle);
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
