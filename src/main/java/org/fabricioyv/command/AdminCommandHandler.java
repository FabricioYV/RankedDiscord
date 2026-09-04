package org.fabricioyv.command;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.fabricioyv.RankedDiscord;
import org.fabricioyv.model.RankedPlayer;

import java.util.UUID;

public class AdminCommandHandler {
    private final RankedDiscord plugin;

    public AdminCommandHandler(RankedDiscord plugin) {
        this.plugin = plugin;
    }

    public void handleAdminCommand(MessageReceivedEvent event, String content) {
        // Verificar si el usuario tiene rol ADMIN o DEV
        if (!hasAdminOrDevRole(event)) {
            event.getChannel().sendMessage("❌ **No tienes permisos para usar este comando**").queue();
            return;
        }

        String[] args = content.substring(1).split(" ");
        String command = args[0].toLowerCase();

        switch (command) {
            case "setwins":
                handleSetWins(event, args);
                break;
            case "setlosses":
                handleSetLosses(event, args);
                break;
            case "setelo":
                handleSetElo(event, args);
                break;
            case "addwins":
                handleAddWins(event, args);
                break;
            case "addlosses":
                handleAddLosses(event, args);
                break;
            case "addelo":
                handleAddElo(event, args);
                break;
            case "removeelo":
                handleRemoveElo(event, args);
                break;
            case "resetmatches":
                handleResetMatches(event);
                break;
            case "resetallstats":
                handleResetAllStats(event);
                break;
            case "adminhelp":
                handleAdminHelp(event);
                break;
            default:
                break;
        }
    }

    private boolean hasAdminOrDevRole(MessageReceivedEvent event) {
        if (event.getGuild() == null) return false;

        return event.getMember().getRoles().stream()
                .anyMatch(role -> role.getName().equalsIgnoreCase("ADMIN") ||
                                role.getName().equalsIgnoreCase("DEV"));
    }

    private boolean hasAdminRole(MessageReceivedEvent event) {
        if (event.getGuild() == null) return false;

        return event.getMember().getRoles().stream()
                .anyMatch(role -> role.getName().equalsIgnoreCase("ADMIN"));
    }

    private RankedPlayer getPlayerByName(String playerName) {
        UUID uuid = plugin.getDatabaseManager().getUuidByMinecraftName(playerName);
        if (uuid == null) return null;
        return plugin.getDatabaseManager().getPlayer(uuid);
    }

    private void handleSetWins(MessageReceivedEvent event, String[] args) {
        if (args.length != 3) {
            event.getChannel().sendMessage("❌ **Uso:** `!setwins <jugador> <cantidad>`").queue();
            return;
        }
        String playerName = args[1];
        int wins;
        try {
            wins = Integer.parseInt(args[2]);
            if (wins < 0) {
                event.getChannel().sendMessage("❌ **Las victorias no pueden ser negativas**").queue();
                return;
            }
        } catch (NumberFormatException e) {
            event.getChannel().sendMessage("❌ **Cantidad inválida**").queue();
            return;
        }
        RankedPlayer player = getPlayerByName(playerName);
        if (player == null) {
            event.getChannel().sendMessage("❌ **Jugador no encontrado:** `" + playerName + "`").queue();
            return;
        }
        int oldWins = player.getWins();
        player.setWins(wins);
        player.setGamesPlayed(player.getWins() + player.getLosses());
        plugin.getDatabaseManager().updatePlayer(player);
        event.getChannel().sendMessage("✅ **Victorias actualizadas para " + playerName + "**\n" +
                "Anterior: " + oldWins + " → Nuevo: " + wins).queue();
        plugin.getLogger().info("ADMIN: " + event.getAuthor().getName() + " cambió las victorias de " +
                playerName + " de " + oldWins + " a " + wins);
    }

    private void handleSetLosses(MessageReceivedEvent event, String[] args) {
        if (args.length != 3) {
            event.getChannel().sendMessage("❌ **Uso:** `!setlosses <jugador> <cantidad>`").queue();
            return;
        }
        String playerName = args[1];
        int losses;
        try {
            losses = Integer.parseInt(args[2]);
            if (losses < 0) {
                event.getChannel().sendMessage("❌ **Las derrotas no pueden ser negativas**").queue();
                return;
            }
        } catch (NumberFormatException e) {
            event.getChannel().sendMessage("❌ **Cantidad inválida**").queue();
            return;
        }
        RankedPlayer player = getPlayerByName(playerName);
        if (player == null) {
            event.getChannel().sendMessage("❌ **Jugador no encontrado:** `" + playerName + "`").queue();
            return;
        }
        int oldLosses = player.getLosses();
        player.setLosses(losses);
        player.setGamesPlayed(player.getWins() + player.getLosses());
        plugin.getDatabaseManager().updatePlayer(player);
        event.getChannel().sendMessage("✅ **Derrotas actualizadas para " + playerName + "**\n" +
                "Anterior: " + oldLosses + " → Nuevo: " + losses).queue();
        plugin.getLogger().info("ADMIN: " + event.getAuthor().getName() + " cambió las derrotas de " +
                playerName + " de " + oldLosses + " a " + losses);
    }

    private void handleSetElo(MessageReceivedEvent event, String[] args) {
        if (args.length != 3) {
            event.getChannel().sendMessage("❌ **Uso:** `!setelo <jugador> <elo>`").queue();
            return;
        }

        String playerName = args[1];
        int elo;

        try {
            elo = Integer.parseInt(args[2]);
            if (elo < 0) {
                event.getChannel().sendMessage("❌ **El ELO no puede ser negativo**").queue();
                return;
            }
        } catch (NumberFormatException e) {
            event.getChannel().sendMessage("❌ **ELO inválido**").queue();
            return;
        }

        RankedPlayer player = getPlayerByName(playerName);
        if (player == null) {
            event.getChannel().sendMessage("❌ **Jugador no encontrado:** `" + playerName + "`").queue();
            return;
        }

        int oldElo = player.getElo();
        player.setElo(elo);
        plugin.getDatabaseManager().updatePlayer(player);

        event.getChannel().sendMessage("✅ **ELO actualizado para " + playerName + "**\n" +
                "Anterior: " + oldElo + " → Nuevo: " + elo).queue();

        plugin.getLogger().info("ADMIN: " + event.getAuthor().getName() + " cambió el ELO de " +
                playerName + " de " + oldElo + " a " + elo);
    }

    private void handleAddWins(MessageReceivedEvent event, String[] args) {
        if (args.length != 3) {
            event.getChannel().sendMessage("❌ **Uso:** `!addwins <jugador> <cantidad>`").queue();
            return;
        }

        String playerName = args[1];
        int winsToAdd;

        try {
            winsToAdd = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            event.getChannel().sendMessage("❌ **Cantidad inválida**").queue();
            return;
        }

        RankedPlayer player = getPlayerByName(playerName);
        if (player == null) {
            event.getChannel().sendMessage("❌ **Jugador no encontrado:** `" + playerName + "`").queue();
            return;
        }

        int oldWins = player.getWins();
        int newWins = Math.max(0, oldWins + winsToAdd);
        player.setWins(newWins);
        player.setGamesPlayed(player.getWins() + player.getLosses());
        plugin.getDatabaseManager().updatePlayer(player);

        event.getChannel().sendMessage("✅ **Victorias modificadas para " + playerName + "**\n" +
                "Anterior: " + oldWins + " → Nuevo: " + newWins + " (" +
                (winsToAdd > 0 ? "+" : "") + winsToAdd + ")").queue();

        plugin.getLogger().info("ADMIN: " + event.getAuthor().getName() + " modificó las victorias de " +
                playerName + " en " + winsToAdd + " (" + oldWins + " → " + newWins + ")");
    }

    private void handleAddLosses(MessageReceivedEvent event, String[] args) {
        if (args.length != 3) {
            event.getChannel().sendMessage("❌ **Uso:** `!addlosses <jugador> <cantidad>`").queue();
            return;
        }

        String playerName = args[1];
        int lossesToAdd;

        try {
            lossesToAdd = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            event.getChannel().sendMessage("❌ **Cantidad inválida**").queue();
            return;
        }

        RankedPlayer player = getPlayerByName(playerName);
        if (player == null) {
            event.getChannel().sendMessage("❌ **Jugador no encontrado:** `" + playerName + "`").queue();
            return;
        }

        int oldLosses = player.getLosses();
        int newLosses = Math.max(0, oldLosses + lossesToAdd);
        player.setLosses(newLosses);
        player.setGamesPlayed(player.getWins() + player.getLosses());
        plugin.getDatabaseManager().updatePlayer(player);

        event.getChannel().sendMessage("✅ **Derrotas modificadas para " + playerName + "**\n" +
                "Anterior: " + oldLosses + " → Nuevo: " + newLosses + " (" +
                (lossesToAdd > 0 ? "+" : "") + lossesToAdd + ")").queue();

        plugin.getLogger().info("ADMIN: " + event.getAuthor().getName() + " modificó las derrotas de " +
                playerName + " en " + lossesToAdd + " (" + oldLosses + " → " + newLosses + ")");
    }

    private void handleAddElo(MessageReceivedEvent event, String[] args) {
        if (args.length != 3) {
            event.getChannel().sendMessage("❌ **Uso:** `!addelo <jugador> <cantidad>`").queue();
            return;
        }

        String playerName = args[1];
        int eloToAdd;

        try {
            eloToAdd = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            event.getChannel().sendMessage("❌ **Cantidad inválida**").queue();
            return;
        }

        RankedPlayer player = getPlayerByName(playerName);
        if (player == null) {
            event.getChannel().sendMessage("❌ **Jugador no encontrado:** `" + playerName + "`").queue();
            return;
        }

        int oldElo = player.getElo();
        int newElo = Math.max(0, oldElo + eloToAdd);
        player.setElo(newElo);
        plugin.getDatabaseManager().updatePlayer(player);

        event.getChannel().sendMessage("✅ **ELO modificado para " + playerName + "**\n" +
                "Anterior: " + oldElo + " → Nuevo: " + newElo + " (" +
                (eloToAdd > 0 ? "+" : "") + eloToAdd + ")").queue();

        plugin.getLogger().info("ADMIN: " + event.getAuthor().getName() + " modificó el ELO de " +
                playerName + " en " + eloToAdd + " (" + oldElo + " → " + newElo + ")");
    }

    private void handleRemoveElo(MessageReceivedEvent event, String[] args) {
        if (args.length != 3) {
            event.getChannel().sendMessage("❌ **Uso:** `!removeelo <jugador> <cantidad>`").queue();
            return;
        }

        String playerName = args[1];
        int eloToRemove;

        try {
            eloToRemove = Integer.parseInt(args[2]);
            if (eloToRemove < 0) {
                event.getChannel().sendMessage("❌ **La cantidad a remover debe ser positiva**").queue();
                return;
            }
        } catch (NumberFormatException e) {
            event.getChannel().sendMessage("❌ **Cantidad inválida**").queue();
            return;
        }

        RankedPlayer player = getPlayerByName(playerName);
        if (player == null) {
            event.getChannel().sendMessage("❌ **Jugador no encontrado:** `" + playerName + "`").queue();
            return;
        }

        int oldElo = player.getElo();
        int newElo = Math.max(0, oldElo - eloToRemove);
        player.setElo(newElo);
        plugin.getDatabaseManager().updatePlayer(player);

        event.getChannel().sendMessage("✅ **ELO reducido para " + playerName + "**\n" +
                "Anterior: " + oldElo + " → Nuevo: " + newElo + " (-" + eloToRemove + ")").queue();

        plugin.getLogger().info("ADMIN: " + event.getAuthor().getName() + " redujo el ELO de " +
                playerName + " en " + eloToRemove + " (" + oldElo + " → " + newElo + ")");
    }

    private void handleResetMatches(MessageReceivedEvent event) {
        try {
            int playersReset = plugin.getDatabaseManager().resetAllMatches();

            event.getChannel().sendMessage("✅ **Reset de partidas completado**\n" +
                    "Jugadores afectados: " + playersReset + "\n" +
                    "Todos los jugadores han sido marcados como disponibles.").queue();

            plugin.getLogger().info("ADMIN: " + event.getAuthor().getName() +
                    " reseteó el estado de partidas. Jugadores afectados: " + playersReset);

        } catch (Exception e) {
            event.getChannel().sendMessage("❌ **Error al resetear partidas:** " + e.getMessage()).queue();
            plugin.getLogger().severe("Error en resetMatches: " + e.getMessage());
        }
    }

    private void handleResetAllStats(MessageReceivedEvent event) {
        // Verificar si el usuario tiene rol ADMIN específicamente
        if (!hasAdminRole(event)) {
            event.getChannel().sendMessage("❌ **Solo usuarios con rol ADMIN pueden usar este comando**").queue();
            return;
        }

        // Verificar si el usuario es el ID específico autorizado (config.yml -> discord.super-admin-user-id)
        String authorizedUserId = plugin.getConfig().getString("discord.super-admin-user-id", "");
        if (authorizedUserId.isEmpty() || !event.getAuthor().getId().equals(authorizedUserId)) {
            event.getChannel().sendMessage("❌ **No tienes autorización para usar este comando**").queue();
            return;
        }

        try {
            int playersUpdated = plugin.getDatabaseManager().resetAllPlayerStats();

            event.getChannel().sendMessage("✅ **Reset de estadísticas completado**\n" +
                    "Jugadores afectados: " + playersUpdated + "\n" +
                    "**Estadísticas reiniciadas:**\n" +
                    "• ELO: 0\n" +
                    "• Victorias: 0\n" +
                    "• Derrotas: 0\n" +
                    "• Partidas jugadas: 0\n" +
                    "• Kills totales: 0\n" +
                    "• Muertes totales: 0\n" +
                    "• MMR: 950.0").queue();

            plugin.getLogger().info("ADMIN AUTORIZADO: " + event.getAuthor().getName() + " (" + event.getAuthor().getId() + ")" +
                    " reinició las estadísticas de todos los jugadores. Jugadores afectados: " + playersUpdated);

        } catch (Exception e) {
            event.getChannel().sendMessage("❌ **Error al resetear estadísticas:** " + e.getMessage()).queue();
            plugin.getLogger().severe("Error en resetAllStats: " + e.getMessage());
        }
    }

    private void handleAdminHelp(MessageReceivedEvent event) {
        String helpMessage = """
                🔧 **Comandos de Administración**
                
                **📊 Gestión de Estadísticas:**
                • `!setwins <jugador> <cantidad>` - Establecer victorias
                • `!setlosses <jugador> <cantidad>` - Establecer derrotas
                • `!setelo <jugador> <elo>` - Establecer ELO
                
                **➕ Modificar Valores:**
                • `!addwins <jugador> <cantidad>` - Añadir/quitar victorias
                • `!addlosses <jugador> <cantidad>` - Añadir/quitar derrotas
                • `!addelo <jugador> <cantidad>` - Añadir ELO
                • `!removeelo <jugador> <cantidad>` - Quitar ELO
                
                **🔄 Utilidades:**
                • `!resetmatches` - Resetear estado "en partida" de todos los jugadores
                • `!resetallstats` - Reiniciar estadísticas de todos los jugadores
                • `!adminhelp` - Mostrar esta ayuda
                
                **ℹ️ Notas:**
                • Los valores no pueden ser negativos
                • Los cambios se registran en los logs
                • Solo usuarios con rol "ADMIN" o "DEV" pueden usar estos comandos
                """;

        event.getChannel().sendMessage(helpMessage).queue();
    }

    private boolean hasDevRole(MessageReceivedEvent event) {
        if (event.getGuild() == null) return false;

        return event.getMember().getRoles().stream()
                .anyMatch(role -> role.getName().equalsIgnoreCase("DEV"));
    }
}
