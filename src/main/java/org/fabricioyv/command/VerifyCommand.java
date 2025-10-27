package org.fabricioyv.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.fabricioyv.RankedDiscord;
import org.fabricioyv.model.RankedPlayer;

import java.util.Random;

public class VerifyCommand implements CommandExecutor {
    private final RankedDiscord plugin;
    private final Random random = new Random();
    public VerifyCommand(RankedDiscord plugin) {
        this.plugin = plugin;

    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cEste comando solo puede ser usado por jugadores.");
            return true;
        }

        Player player = (Player) sender;

        // Verificar si es premium
        if (!player.isOnline()) {
            player.sendMessage("§cDebes tener una cuenta premium para usar este comando.");
            return true;
        }

        RankedPlayer rankedPlayer = plugin.getDatabaseManager().getPlayer(player.getUniqueId());

        if (rankedPlayer != null && rankedPlayer.isVerified()) {
            player.sendMessage("§cTu cuenta ya está verificada.");
            return true;
        }

        // Generar código de verificación
        String verificationCode = generateCode();
        long expiry = System.currentTimeMillis() + (5 * 60 * 1000); // 5 minutos

        if (rankedPlayer == null) {
            rankedPlayer = new RankedPlayer(player.getUniqueId(), player.getName());
        }

        rankedPlayer.setVerificationCode(verificationCode);
        rankedPlayer.setVerificationExpiry(expiry);

        if (rankedPlayer.getId() == 0) {
            plugin.getDatabaseManager().createPlayer(rankedPlayer);
        } else {
            plugin.getDatabaseManager().updatePlayer(rankedPlayer);
        }

        player.sendMessage("§a§lCÓDIGO DE VERIFICACIÓN");
        player.sendMessage("§7Ve al Discord y usa el comando: §f/verify " + verificationCode);
        player.sendMessage("§7El código expira en §c5 minutos§7.");

        return true;
    }

    private String generateCode() {
        return String.format("%08d", random.nextInt(100000000));
    }
}

