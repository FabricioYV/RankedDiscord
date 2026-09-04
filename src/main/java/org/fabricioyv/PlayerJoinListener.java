package org.fabricioyv;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;

public class PlayerJoinListener implements Listener {
    private final JavaPlugin plugin;
    private final File customMessagesFile;
    private final YamlConfiguration customMessagesConfig;
    private final String generalWelcome = "§a¡Bienvenido al servidor!";
    private final String welcomePermission = "rankeddiscord.seewelcome";

    public PlayerJoinListener(JavaPlugin plugin) {
        this.plugin = plugin;
        this.customMessagesFile = new File(plugin.getDataFolder(), "welcome_messages.yml");
        this.customMessagesConfig = YamlConfiguration.loadConfiguration(customMessagesFile);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String customMessage = customMessagesConfig.getString(player.getUniqueId().toString());
        String messageToBroadcast = (customMessage != null && !customMessage.isEmpty()) ? customMessage : generalWelcome;

        // Enviar mensaje solo a jugadores con permiso
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.hasPermission(welcomePermission)) {
                onlinePlayer.sendMessage(messageToBroadcast);
            }
        }
    }
}
