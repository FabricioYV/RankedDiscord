package org.fabricioyv.command;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.fabricioyv.RankedDiscord;

import java.awt.*;

public class PrefixCommandHandler {
    private final RankedDiscord plugin;

    public PrefixCommandHandler(RankedDiscord plugin) {
        this.plugin = plugin;
    }

    public void handleCommand(MessageReceivedEvent event, String content) {
        String[] args = content.substring(1).split(" ");
        String command = args[0].toLowerCase();

        switch (command) {
            case "ip":
                handleIpCommand(event);
                break;
            case "info":
                handleInfoCommand(event);
                break;
            case "instrucciones8v8":
                handleInstrucciones8v8Command(event);
                break;
            case "instrucciones5v5":
                handleInstrucciones5v5Command(event);
                break;
            case "donacion":
            case "donar":
            case "paypal":
            case "apoyo":
                handleDonationCommand(event);
                break;
            case "yape":
                handleYapeCommand(event);
                break;
            case "stats":
                handleStatsCommand(event, args);
                break;
            default:
                // Comando no reconocido - no hacer nada
                break;
        }
    }

    private void handleIpCommand(MessageReceivedEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🌐 IP del Servidor")
                .setColor(new Color(0, 255, 0))
                .setDescription("**IP del Servidor:** `keke.sparked.network`\n\n" +
                        "**Para jugar Ranked:** `/server keke1`\n" +
                        "**Para jugar Mixed:** `/server mixed`")
                .setFooter("Keke Network • Únete ahora!", null)
                .setTimestamp(java.time.Instant.now());

        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }

    private void handleInfoCommand(MessageReceivedEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🎮 Información del Servidor")
                .setColor(new Color(255, 215, 0))
                .setDescription("¡Bienvenido a **Keke Network**!")
                .addField("🌐 IP del Servidor", "`keke.sparked.network`", false)
                .addField("🏆 Servidor Ranked", "`/server keke1`", true)
                .addField("🎯 Servidor Mixed", "`/server mixed`", true)
                .addField("📈 Características",
                        "• Sistema de ELO competitivo\n" +
                                "• Matchmaking balanceado\n" +
                                "• Rankings y estadísticas\n" +
                                "• Modo Mixed para práctica", false)
                .setFooter("Keke Network • ¡Disfruta del mejor PvP competitivo!", null)
                .setTimestamp(java.time.Instant.now());

        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }

    private void handleInstrucciones8v8Command(MessageReceivedEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("⚡ Instrucciones - Keke 8vs8")
                .setColor(new Color(231, 76, 60))
                .setDescription("**IP:** `keke2.sparked.network`\n\n" +
                        "**Para las Keke 8vs8, serán solamente de CTW (Capture The Wool)**\n" +
                        "Bot diferente = MMR separado de la cola 5v5\n\n" +
                        "**📋 Pasos para jugar:**")
                .addField("1️⃣ **Unirse a la Cola**",
                        "Únete al canal de voz **Queue 8v8** para entrar en cola.", false)
                .addField("2️⃣ **Formación de Partida**",
                        "Una vez se complete 16 jugadores:\n" +
                                "• Se creará un canal temporal **Partida (código de partida)**\n" +
                                "• Debes unirte **manualmente** al canal indicado\n" +
                                "• Se creará un canal que te etiquetará\n" +
                                "• **Si no ingresas al canal, se cancelará la partida**", false)
                .addField("3️⃣ **Pickeo de Equipos**",
                        "• Se seleccionan capitanes automáticamente\n" +
                                "• Inicia el pickeo de jugadores para formar equipos\n" +
                                "• **Sugerencia:** Los capitanes pueden guiarse de los roles seleccionados por el jugador", false)
                .addField("4️⃣ **Inicio de Partida**",
                        "• Únete al servidor `keke2.sparked.network`\n" +
                                "• **Canal \"Equipo 1\"** → **MC \"Team 1\"**\n" +
                                "• **Canal \"Equipo 2\"** → **MC \"Team 2\"**\n" +
                                "• El host iniciará la partida en el servidor", false)
                .addField("5️⃣ **Final de Partida**",
                        "Terminada la partida se actualizarán las stats las cuales podrás ver en ⁠comandos-8v8", false)
                .addField("⚠️ **Reglas Adicionales**",
                        "• Si no se definieran dos defensas, el capitán del equipo podrá designarlas\n" +
                                "• **Respetar los roles** designados durante la partida\n" +
                                "• **NO rushear sin armadura** o morir sin ningún propósito\n" +
                                "• Los roles son **rotativos** a excepción de las defensas\n" +
                                "• El incumplimiento se sancionará según el reglamento de rankeds", false)
                .setFooter("¡Estrategia y coordinación son clave en 8v8!", null)
                .setTimestamp(java.time.Instant.now());

        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }

    private void handleInstrucciones5v5Command(MessageReceivedEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🎯 Instrucciones - Keke 5vs5")
                .setColor(new Color(155, 89, 182))
                .setDescription("**IP:** `keke1.sparked.network`\n\n" +
                        "**📋 Pasos para jugar:**")
                .addField("1️⃣ **Conectarse al Servidor**",
                        "Para unirte a cola primero debes estar en el servidor de Minecraft.", false)
                .addField("2️⃣ **Unirse a la Cola**",
                        "Una vez en el servidor de MC, únete al canal de voz **Ranked Automáticas (5v5)**.\n" +
                                "• Actualmente, solo puede haber una cola activa a la vez\n" +
                                "• No te dejará unirte a cola si no ingresas primero al servidor de MC", false)
                .addField("3️⃣ **Durante la Partida**",
                        "Para empezar la partida debes mantenerte conectado en Minecraft y en la llamada a la vez, caso contrario se cancelará la partida.", false)
                .addField("4️⃣ **Votación de Mapa**",
                        "Vota el mapa que prefieras con el comando `/votemap` (solo disponible cuando la partida esté por empezar).", false)
                .addField("5️⃣ **Final de Partida**",
                        "Al terminar, se actualizarán tus estadísticas y serás movido/a a la sala de espera para que puedas unirte de nuevo a la cola.", false)
                .addField("⚠️ **Nota Importante**",
                        "Si ya hay 10 jugadores en una partida, únete a la cola de 8v8 para completarla, ya que no se pueden ejecutar dos partidas simultáneamente.", false)
                .addField("📊 **Comandos Útiles**",
                        "• `/stats` - Ver tus estadísticas\n" +
                                "• `/top` - Ver el ranking\n" +
                                "• `/ranks` - Ver información de rangos", false)
                .addField("🐛 **Reportes**",
                        "Cualquier reporte, sugerencia o bug en ⁠reporte-bugs", false)
                .setFooter("¡Buena suerte en tus partidas ranked!", null)
                .setTimestamp(java.time.Instant.now());

        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }

    private void handleDonationCommand(MessageReceivedEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("💖 Donaciones - ¡Apoya al Servidor!")
                .setColor(new Color(0, 123, 255))
                .setDescription("¡Gracias por considerar apoyar nuestro servidor! Tu contribución nos ayuda a mantener y mejorar la experiencia de juego para toda la comunidad.")
                .addField("💳 **¿Cómo donar?**",
                        "Puedes hacer una donación segura a través de PayPal haciendo clic en el enlace de abajo:", false)
                .addField("🔗 **Enlace de Donación**",
                        "[**Donar via PayPal**](https://www.paypal.com/paypalme/fabricioyv)\n" +
                                "`https://www.paypal.com/paypalme/fabricioyv`", false)
                .addField("🎁 **¿Para qué se usan las donaciones?**",
                        "• Mantenimiento y hosting de servidores\n" +
                                "• Para que CyDarkCat pueda comer\n" +
                                "• Desarrollo de nuevas características(duos y picks)\n" +
                                "• Herramientas y recursos para el staff\n" +
                                "• Eventos especiales y premios", false)
                .addField("❤️ **Beneficios de donar**",
                        "• Reconocimiento especial en el servidor\n" +
                                "• Acceso a canales exclusivos\n" +
                                "• Rol especial de Donador\n" +
                                "• Prioridad en soporte técnico\n" +
                                "• Participación en decisiones del servidor", false)
                .addField("📝 **Notas importantes**",
                        "• Todas las donaciones son **100% voluntarias**\n" +
                                "• No se requiere donar para disfrutar del servidor\n" +
                                "• Las donaciones no son reembolsables\n" +
                                "• Agradecemos cualquier cantidad, sin importar el monto", false)
                .setFooter("¡Tu apoyo hace posible que este servidor siga creciendo! 🚀", null)
                .setTimestamp(java.time.Instant.now());

        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }

    private void handleYapeCommand(MessageReceivedEvent event) {
        try {
            // Crear embed con información de Yape
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("📱 Yape - ¡Apoya al Servidor!")
                    .setColor(new Color(158, 0, 93)) // Color morado de Yape
                    .setDescription("¡Gracias por querer apoyar nuestro servidor con Yape! 🇵🇪")
                    .addField("💜 **¿Cómo donar con Yape?**",
                            "1️⃣ Abre tu aplicación Yape\n" +
                                    "2️⃣ Escanea el código QR de la imagen\n" +
                                    "3️⃣ Ingresa el monto que desees donar\n" +
                                    "4️⃣ Confirma la transferencia", false)
                    .addField("🎁 **Tu apoyo nos ayuda con:**",
                            "• Mantenimiento de servidores\n" +
                                    "• Mejoras y nuevas características\n" +
                                    "• Eventos especiales\n" +
                                    "• Para que CyDarkCat pueda comer 🐱", false)
                    .addField("📝 **Nota importante**",
                            "Si realizas una donación por Yape, puedes contactar al staff para recibir tu rol de Donador especial ❤️", false)
                    .setFooter("¡Gracias por tu apoyo desde Perú! 🇵🇪", null)
                    .setTimestamp(java.time.Instant.now());

            // Obtener la imagen del QR de Yape desde los recursos
            var inputStream = getClass().getClassLoader().getResourceAsStream("yape.png");

            if (inputStream != null) {
                // Si existe la imagen, enviarla con el embed
                embed.setImage("attachment://yape.png");
                event.getChannel().sendMessageEmbeds(embed.build())
                        .addFiles(FileUpload.fromData(inputStream, "yape.png"))
                        .queue();
            } else {
                // Si no existe la imagen, enviar solo el embed con instrucciones
                embed.addField("⚠️ **Código QR no disponible**",
                        "Por favor contacta al staff para obtener el código QR de Yape.", false);
                event.getChannel().sendMessageEmbeds(embed.build()).queue();

                // Log para el administrador
                System.err.println("❌ Archivo yape.png no encontrado en resources/");
            }

        } catch (Exception e) {
            // En caso de error, enviar mensaje de error
            event.getChannel().sendMessage("❌ **Error al cargar el código QR de Yape.** " +
                    "Por favor contacta al staff para obtener la información de donación.").queue();

            System.err.println("❌ Error en handleYapeCommand: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleStatsCommand(MessageReceivedEvent event, String[] args) {
        String playerName;
        if (args.length > 1) {
            playerName = args[1];
        } else {
            // Si no se especifica nombre, usar el nombre vinculado al usuario de Discord (puedes ajustar esto según tu lógica)
            event.getChannel().sendMessage("❌ Debes especificar el nombre del jugador. Ejemplo: `/stats CyDarkCat`").queue();
            return;
        }

        // Obtener UUID actual desde la API de Mojang
        String uuid = getUUIDFromMojang(playerName);
        if (uuid == null) {
            event.getChannel().sendMessage("❌ No se pudo encontrar el UUID para el jugador: " + playerName).queue();
            return;
        }



        // Buscar en la base de datos por UUID
        java.util.UUID uuidObj;
        try {
            uuidObj = java.util.UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            event.getChannel().sendMessage("❌ UUID inválido para el jugador: " + playerName).queue();
            return;
        }
        org.fabricioyv.model.RankedPlayer rankedPlayer = plugin.getDatabaseManager().getPlayerByMinecraftUuid(uuidObj);
        if (rankedPlayer == null) {
            event.getChannel().sendMessage("❌ No se encontraron estadísticas para el jugador: " + playerName).queue();
            return;
        }

        // Enviar embed con estadísticas (manteniendo el formato actual)
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📊 Estadísticas de " + playerName)
                .setColor(new Color(54, 125, 255))
                .addField("UUID", uuid, false)
                .addField("ELO", String.valueOf(rankedPlayer.getElo()), true)
                .addField("Victorias", String.valueOf(rankedPlayer.getWins()), true)
                .addField("Derrotas", String.valueOf(rankedPlayer.getLosses()), true)
                .addField("Partidas", String.valueOf(rankedPlayer.getGamesPlayed()), true)
                .setFooter("Ranked Discord • Sistema Competitivo", null)
                .setTimestamp(java.time.Instant.now());

        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }

    // Utilidad para obtener el UUID actual de Mojang
    private String getUUIDFromMojang(String playerName) {
        try {
            java.net.URL url = new java.net.URL("https://api.mojang.com/users/profiles/minecraft/" + playerName);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int status = conn.getResponseCode();
            if (status != 200) return null;

            java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            conn.disconnect();

            // Parsear JSON
            String json = content.toString();
            int idIndex = json.indexOf("\"id\":");
            if (idIndex == -1) return null;
            String idValue = json.split("\"id\":\"")[1].split("\"")[0];
            // Mojang devuelve el UUID sin guiones, lo formateamos
            return idValue.replaceFirst(
                    "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                    "$1-$2-$3-$4-$5"
            );
        } catch (Exception e) {
            System.err.println("[DEBUG] Error obteniendo UUID de Mojang: " + e.getMessage());
            return null;
        }
    }


}
