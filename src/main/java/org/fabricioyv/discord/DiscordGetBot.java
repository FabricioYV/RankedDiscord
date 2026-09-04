package org.fabricioyv.discord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.internal.utils.JDALogger;
import org.fabricioyv.RankedDiscord;
import org.fabricioyv.command.PrefixCommandHandler;
import org.fabricioyv.model.RankedPlayer;

import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.UUID;



public class DiscordGetBot extends ListenerAdapter {
    private JDA jda;
    private final RankedDiscord plugin;
    private final PrefixCommandHandler prefixCommandHandler;

    public DiscordGetBot(RankedDiscord plugin) {
        this.plugin = plugin;
        this.prefixCommandHandler = new PrefixCommandHandler(plugin);
    }

    public void initialize(String token) {
        try {
            JDALogger.setFallbackLoggerEnabled(false); // Desactivar logger por defecto de JDA

            jda = JDABuilder.createDefault(token)
                    .enableIntents(
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.MESSAGE_CONTENT
                    )
                    .addEventListeners(this)
                    .build();

            jda.awaitReady();

            // Registrar comando slash
            jda.updateCommands().addCommands(
                    Commands.slash("verify", "Verificar tu cuenta de Minecraft")
                            .addOption(OptionType.STRING, "code", "Código de verificación de Minecraft", true),
                    Commands.slash("stats", "Ver estadísticas de un jugador")
                            .addOption(OptionType.STRING, "player", "Nombre del jugador (opcional)", false),
                    Commands.slash("top", "Ver el top de jugadores por ELO")
                            .addOption(OptionType.INTEGER, "limit", "Cantidad de jugadores a mostrar (1-20)", false),
                    Commands.slash("ranks", "Ver información sobre todos los rangos disponibles")
            ).queue();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "verify":
                handleVerifyCommand(event);
                break;
            case "stats":
                handleStatsCommand(event);
                break;
            case "top":
                handleTopCommand(event);
                break;
            case "ranks":
                handleRanksCommand(event);
                break;
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {

        // Ignorar mensajes de bots
        if (event.getAuthor().isBot()) return;

        String content = event.getMessage().getContentRaw();

        // Verificar si el mensaje empieza con el prefix !
        if (content.startsWith("!")) {
            prefixCommandHandler.handleCommand(event, content);
        }
    }

    private void handleRanksCommand(SlashCommandInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🎖️ Sistema de Rangos - Ranked Discord")
                .setColor(new Color(255, 215, 0))
                .setDescription("**Progresa a través de estos rangos ganando ELO en partidas competitivas:**\n")
                .addField("💚 **ESMERALDA**",
                        "💚 Esmeralda: **1200+ ELO**", false)
                .addField("💎 **DIAMANTE**",
                        "💎 Diamante I: **1100-1199 ELO**\n" +
                                "💎 Diamante II: **1000-1099 ELO**\n" +
                                "💎 Diamante III: **900-999 ELO**", false)
                .addField("🟡 **ORO**",
                        "🟡 Oro I: **800-899 ELO**\n" +
                                "🟡 Oro II: **700-799 ELO**\n" +
                                "🟡 Oro III: **600-699 ELO**", false)
                .addField("⚪ **HIERRO**",
                        "⚪ Hierro I: **500-599 ELO**\n" +
                                "⚪ Hierro II: **400-499 ELO**\n" +
                                "⚪ Hierro III: **300-399 ELO**", false)
                .addField("🟤 **COBRE**",
                        "🟤 Cobre I: **200-299 ELO**\n" +
                                "🟤 Cobre II: **100-199 ELO**\n" +
                                "🟤 Cobre III: **0-99 ELO** *(ELO Inicial)*", false)
                .setFooter("¡Los nuevos jugadores empiezan con 0 ELO (Cobre III)!", null)
                .setTimestamp(java.time.Instant.now());

        event.replyEmbeds(embed.build()).queue();
    }

    private void handleVerifyCommand(SlashCommandInteractionEvent event) {
        String code = event.getOption("code").getAsString();
        User discordUser = event.getUser();

        RankedPlayer player = plugin.getDatabaseManager().getPlayerByVerificationCode(code);

        if (player == null) {
            event.reply("❌ **Código inválido o expirado**\nVerifica que hayas escrito el código correctamente.")
                    .setEphemeral(true).queue();
            return;
        }

        if (player.isVerified()) {
            event.reply("❌ **Esta cuenta ya está verificada**")
                    .setEphemeral(true).queue();
            return;
        }

        // Verificar cuenta
        player.setDiscordId(discordUser.getId());
        player.setVerified(true);
        player.setVerificationCode(null);
        player.setVerificationExpiry(0);

        plugin.getDatabaseManager().updatePlayer(player);

        assignQueueRole(event, discordUser);
        updateUserNickname(event, player);

        event.reply("✅ **¡Cuenta verificada exitosamente!**\n" +
                        "Tu cuenta de Minecraft `" + player.getMinecraftUsername() + "` " +
                        "ha sido vinculada con tu Discord.\n\n" +
                        "**Estadísticas iniciales:**\n" +
                        "ELO: " + player.getElo() + "\n" +
                        "✅ Victorias: " + player.getWins() + "\n" +
                        "❌ Derrotas: " + player.getLosses() + "\n" +
                        "Partidas jugadas: " + player.getGamesPlayed())
                .setEphemeral(true).queue();
    }

    private void updateUserNickname(SlashCommandInteractionEvent event, RankedPlayer player) {
        try {
            if (event.getGuild() == null) return;

            String minecraftName = player.getMinecraftUsername();
            int elo = player.getElo();

            // NICKNAME SIN EMOJI - Solo nombre y ELO
            String newNickname = String.format("%s [%d]", minecraftName, elo);

            // Verificar límite de caracteres de Discord (32 caracteres máximo)
            if (newNickname.length() > 32) {
                // Si es muy largo, usar formato más corto
                newNickname = String.format("%s[%d]", minecraftName, elo);

                // Si aún es muy largo, truncar el nombre
                if (newNickname.length() > 32) {
                    int maxNameLength = 32 - String.valueOf(elo).length() - 3; // 3 para "[" + "]"
                    String truncatedName = minecraftName.substring(0, Math.min(minecraftName.length(), maxNameLength));
                    newNickname = String.format("%s[%d]", truncatedName, elo);
                }
            }

            final String finalNickname = newNickname;

            event.getGuild().retrieveMember(event.getUser()).queue(
                    member -> {
                        member.modifyNickname(finalNickname).queue(
                                success -> plugin.getLogger().info("✅ Nickname actualizado: " + finalNickname),
                                error -> plugin.getLogger().warning("⚠️ No se pudo actualizar el nickname: " + error.getMessage())
                        );
                    },
                    error -> plugin.getLogger().warning("⚠️ No se pudo obtener el miembro para actualizar nickname")
            );

        } catch (Exception e) {
            plugin.getLogger().warning("Error actualizando nickname: " + e.getMessage());
        }
    }

    private void handleStatsCommand(SlashCommandInteractionEvent event) {
        String playerName = event.getOption("player") != null ?
                event.getOption("player").getAsString() : null;

        RankedPlayer player = null;
        if (playerName != null) {
            // Buscar directamente en la base de datos por nombre
            player = plugin.getDatabaseManager().getPlayerByMinecraftName(playerName);
            if (player == null) {
                event.reply("❌ **Jugador no encontrado**\nEl jugador `" + playerName + "` no ha entrado nunca al servidor o no está verificado.").setEphemeral(true).queue();
                return;
            }
        } else {
            player = plugin.getDatabaseManager().getPlayerByDiscordId(event.getUser().getId());
            if (player == null) {
                event.reply("❌ **No estás registrado**\nDebes verificar tu cuenta primero usando `/verify` en Minecraft.").setEphemeral(true).queue();
                return;
            }
        }

        int rankPosition = plugin.getDatabaseManager().getPlayerRankPosition(player.getDiscordId());
        double winRate = player.getGamesPlayed() > 0 ?
                (double) player.getWins() / player.getGamesPlayed() * 100 : 0.0;
        double kdRatio = player.getTotalDeaths() > 0 ?
                (double) player.getTotalKills() / player.getTotalDeaths() : player.getTotalKills();
        String detailedRank = getDetailedRank(player.getElo());
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📊 Estadísticas de " + player.getMinecraftUsername())
                .setThumbnail("https://mc-heads.net/avatar/" + player.getMinecraftUsername() + "/128")
                .setColor(getRankColor(player.getElo()))
                .addField(" ELO", String.valueOf(player.getElo()), true)
                .addField(" Posición", "#" + rankPosition, true)
                .addField("️ Rango", detailedRank, false)
                .addField("✅ Victorias", String.valueOf(player.getWins()), true)
                .addField("❌ Derrotas", String.valueOf(player.getLosses()), true)
                .addField(" Partidas", String.valueOf(player.getGamesPlayed()), true)
                .addField("📈 Winrate", String.format("%.1f%%", winRate), true)
                .addField("⚔ Kills", String.valueOf(player.getTotalKills()), true)
                .addField(" Muertes", String.valueOf(player.getTotalDeaths()), true)
                .addField(" K/D Ratio", String.format("%.2f", kdRatio), true)
                .addField(" Estado", player.isInMatch() ? "🔴 En partida" : "🟢 Disponible", true)
                .setFooter("Ranked Discord • Sistema de Estadísticas", null)
                .setTimestamp(java.time.Instant.now());

        event.replyEmbeds(embed.build()).queue();
    }

    private void handleTopCommand(SlashCommandInteractionEvent event) {
        int limit = event.getOption("limit") != null ?
                event.getOption("limit").getAsInt() : 10;

        // Limitar entre 1 y 20
        limit = Math.max(1, Math.min(20, limit));

        List<RankedPlayer> topPlayers = plugin.getDatabaseManager().getTopPlayersByElo(limit);

        if (topPlayers.isEmpty()) {
            event.reply("❌ **No hay jugadores registrados**\nNo se encontraron jugadores verificados.")
                    .setEphemeral(true).queue();
            return;
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🏆 Top " + limit + " Jugadores - Ranking ELO")
                .setColor(Color.YELLOW)
                .setThumbnail("https://mc-heads.net/avatar/" + topPlayers.get(0).getMinecraftUsername() + "/128")
                .setFooter("Ranked Discord • Top Players", null)
                .setTimestamp(java.time.Instant.now());

        StringBuilder description = new StringBuilder();

        for (int i = 0; i < topPlayers.size(); i++) {
            RankedPlayer player = topPlayers.get(i);
            String medal = getMedalEmoji(i + 1);
            String rank = getRankByElo(player.getElo());
            String rankEmoji = getRankEmoji(player.getElo());

            double winRate = player.getGamesPlayed() > 0 ?
                    (double) player.getWins() / player.getGamesPlayed() * 100 : 0.0;

            description.append(String.format(
                    "%s **#%d** %s `%s`\n" +
                            "🏆 **%d ELO** %s %s\n" +
                            "📊 %d/%d/%d (%.1f%% WR)\n\n",
                    medal, i + 1, player.getMinecraftUsername(),
                    player.isInMatch() ? "🔴" : "🟢",
                    player.getElo(), rankEmoji, rank,
                    player.getWins(), player.getLosses(), player.getGamesPlayed(), winRate
            ));
        }

        embed.setDescription(description.toString());
        event.replyEmbeds(embed.build()).queue();
    }

    private String getMedalEmoji(int position) {
        return switch (position) {
            case 1 -> "🥇";
            case 2 -> "🥈";
            case 3 -> "🥉";
            default -> "🏅";
        };
    }

    private String getRankByElo(int elo) {
        if (elo >= 1200) return "Esmeralda";
        if (elo >= 1100) return "Diamante I";
        if (elo >= 1000) return "Diamante II";
        if (elo >= 900) return "Diamante III";
        if (elo >= 800) return "Oro I";
        if (elo >= 700) return "Oro II";
        if (elo >= 600) return "Oro III";
        if (elo >= 500) return "Hierro I";
        if (elo >= 400) return "Hierro II";
        if (elo >= 300) return "Hierro III";
        if (elo >= 200) return "Cobre I";
        if (elo >= 100) return "Cobre II";
        return "Cobre III";                    // ELO inicial
    }

    private String getRankEmoji(int elo) {
        if (elo >= 1200) return "💚";     // Esmeralda
        if (elo >= 1100) return "💎";     // Diamante I
        if (elo >= 1000) return "💎";     // Diamante II
        if (elo >= 900) return "💎";      // Diamante III
        if (elo >= 800) return "🟡";     // Oro I
        if (elo >= 700) return "🟡";     // Oro II
        if (elo >= 600) return "🟡";      // Oro III
        if (elo >= 500) return "⚪";      // Hierro I
        if (elo >= 400) return "⚪";      // Hierro II
        if (elo >= 300) return "⚪";      // Hierro III
        if (elo >= 200) return "🟤";      // Cobre I
        if (elo >= 100) return "🟤";      // Cobre II
        return "🟤";                      // Cobre III (inicial)
    }

    private Color getRankColor(int elo) {
        if (elo >= 1200) return new Color(34, 139, 34);    // Verde Esmeralda
        if (elo >= 1100) return new Color(185, 242, 255);  // Azul Diamante I
        if (elo >= 1000) return new Color(135, 206, 235);  // Azul Diamante II
        if (elo >= 900) return new Color(70, 130, 180);   // Azul Diamante III
        if (elo >= 800) return new Color(255, 215, 0);    // Dorado I
        if (elo >= 700) return new Color(218, 165, 32);   // Dorado II
        if (elo >= 600) return new Color(184, 134, 11);    // Dorado III
        if (elo >= 500) return new Color(192, 192, 192);   // Plateado Hierro I
        if (elo >= 400) return new Color(169, 169, 169);   // Plateado Hierro II
        if (elo >= 300) return new Color(128, 128, 128);   // Plateado Hierro III
        if (elo >= 200) return new Color(205, 127, 50);    // Cobre I
        if (elo >= 100) return new Color(160, 82, 45);     // Cobre II
        return new Color(139, 69, 19);                     // Cobre III
    }

    private String getNextRankInfo(int elo) {
        if (elo >= 1200) return "🏆 **¡Rango Máximo Alcanzado!**";
        if (elo >= 1100) return "📈 **" + (1200 - elo) + " ELO** para Esmeralda";
        if (elo >= 1000) return "📈 **" + (1100 - elo) + " ELO** para Diamante I";
        if (elo >= 900) return "📈 **" + (1000 - elo) + " ELO** para Diamante II";
        if (elo >= 800) return "📈 **" + (900 - elo) + " ELO** para Diamante III";
        if (elo >= 700) return "📈 **" + (800 - elo) + " ELO** para Oro I";
        if (elo >= 600) return "📈 **" + (700 - elo) + " ELO** para Oro II";
        if (elo >= 500) return "📈 **" + (600 - elo) + " ELO** para Oro III";
        if (elo >= 400) return "📈 **" + (500 - elo) + " ELO** para Hierro I";
        if (elo >= 300) return "📈 **" + (400 - elo) + " ELO** para Hierro II";
        if (elo >= 200) return "📈 **" + (300 - elo) + " ELO** para Hierro III";
        if (elo >= 100) return "📈 **" + (200 - elo) + " ELO** para Cobre I";
        return "📈 **" + (100 - elo) + " ELO** para Cobre II";
    }

    private String getDetailedRank(int elo) {
        String rank = getRankByElo(elo);
        String emoji = getRankEmoji(elo);
        String nextRankInfo = getNextRankInfo(elo);

        return emoji + " **" + rank + "**" + (nextRankInfo.isEmpty() ? "" : "\n" + nextRankInfo);
    }

    public void shutdown() {
        if (jda != null) {
            jda.shutdown();
        }
    }

    /**
     * Obtiene la instancia JDA del bot para uso externo
     */
    public JDA getJDA() {
        return jda;
    }

    private void assignQueueRole(SlashCommandInteractionEvent event, User user) {
        try {
            // Obtener el nombre del rol desde la configuración
            String queueRoleName = plugin.getConfig().getString("discord.queue_role_name", "Queue");

            // Obtener el guild (servidor) donde se ejecutó el comando
            if (event.getGuild() == null) {
                plugin.getLogger().warning("No se pudo asignar el rol - comando ejecutado fuera de un servidor");
                return;
            }

            // Buscar el rol por nombre
            var roles = event.getGuild().getRolesByName(queueRoleName, true);

            if (roles.isEmpty()) {
                plugin.getLogger().warning("No se encontró el rol '" + queueRoleName + "' en el servidor");
                return;
            }

            var queueRole = roles.get(0);

            event.getGuild().retrieveMember(user).queue(
                    member -> {
                        // Verificar si ya tiene el rol
                        if (member.getRoles().contains(queueRole)) {
                            plugin.getLogger().info("El usuario " + user.getName() + " ya tiene el rol Queue");
                            return;
                        }

                        // Asignar el rol
                        event.getGuild().addRoleToMember(member, queueRole).queue(
                                success -> plugin.getLogger().info("✅ Rol 'Queue' asignado exitosamente a " + user.getName()),
                                error -> plugin.getLogger().severe("❌ Error al asignar el rol Queue: " + error.getMessage())
                        );
                    },
                    error -> {
                        plugin.getLogger().warning("❌ No se pudo obtener el miembro " + user.getName() + ": " + error.getMessage());
                        plugin.getLogger().warning("Posibles causas:");
                        plugin.getLogger().warning("- El usuario no está en el servidor");
                        plugin.getLogger().warning("- El bot no tiene permisos para ver miembros");
                        plugin.getLogger().warning("- El usuario tiene configuración de privacidad estricta");
                    }
            );

        } catch (Exception e) {
            plugin.getLogger().severe("Error al intentar asignar el rol Queue: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Helper to fetch latest UUID from Mojang API
    private UUID fetchUuidFromMojang(String playerName) {
        try {
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + playerName);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int status = conn.getResponseCode();
            if (status != 200) {
                return null;
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            conn.disconnect();

            String json = content.toString();
            if (!json.contains("\"id\"")) return null;
            String id = json.split("\"id\":\"")[1].split("\"")[0];
            return UUID.fromString(id.replaceFirst(
                "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                "$1-$2-$3-$4-$5"
            ));
        } catch (Exception e) {
            return null;
        }
    }
}
