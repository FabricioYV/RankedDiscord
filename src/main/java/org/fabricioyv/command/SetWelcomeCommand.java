package org.fabricioyv.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;

public class SetWelcomeCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final File customMessagesFile;
    private final YamlConfiguration customMessagesConfig;
    private final String requiredPermission = "rankeddiscord.setwelcome";

    public SetWelcomeCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        this.customMessagesFile = new File(plugin.getDataFolder(), "welcome_messages.yml");
        this.customMessagesConfig = YamlConfiguration.loadConfiguration(customMessagesFile);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Solo jugadores pueden usar este comando.");
            return true;
        }
        Player player = (Player) sender;
        boolean hasPermission = player.hasPermission(requiredPermission);
        if (!hasPermission) {
            player.sendMessage("No tienes permiso para modificar tu mensaje de bienvenida.");
            return true;
        }
        if (args.length == 0) {
            player.sendMessage("Uso: /setwelcome <mensaje>");
            return true;
        }
        String message = String.join(" ", args);
        // Soporte para códigos de color de Minecraft usando '&'
        message = message.replaceAll("&([0-9a-fk-or])", "§$1");
        customMessagesConfig.set(player.getUniqueId().toString(), message);
        try {
            customMessagesConfig.save(customMessagesFile);
            player.sendMessage("Mensaje de bienvenida personalizado guardado.");
        } catch (IOException e) {
            player.sendMessage("Error al guardar el mensaje. Contacta a un administrador.");
        }
        return true;
    }
}
