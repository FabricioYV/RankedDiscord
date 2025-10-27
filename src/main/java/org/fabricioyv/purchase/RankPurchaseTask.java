package org.fabricioyv.purchase;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.fabricioyv.RankedDiscord;
import org.fabricioyv.database.DBManager;
import net.dv8tion.jda.api.entities.Role;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Task que se ejecuta cada cierto tiempo para procesar compras de rangos
 * que han sido confirmadas desde la aplicación web.
 */
public class RankPurchaseTask extends BukkitRunnable {

    private final RankedDiscord plugin;
    private final DBManager dbManager;
    private final int maxPurchasesPerCycle;

    public RankPurchaseTask(RankedDiscord plugin, DBManager dbManager, int maxPurchasesPerCycle) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        this.maxPurchasesPerCycle = maxPurchasesPerCycle;
    }

    @Override
    public void run() {
        try {
            // ⚠️ LOGGING INICIAL PARA DEBUG
            plugin.getLogger().info("§e🔄 Iniciando ciclo de verificación de compras...");
            plugin.getLogger().info("§7📋 Configuración: maxPurchasesPerCycle=" + maxPurchasesPerCycle);

            // Obtener compras pendientes
            List<Purchase> pendingPurchases = getPendingPurchases();

            if (pendingPurchases.isEmpty()) {
                plugin.getLogger().info("§7😴 No hay compras pendientes en este ciclo");
                return; // No hay compras pendientes
            }

            plugin.getLogger().info("§e📦 Procesando " + pendingPurchases.size() + " compras pendientes...");

            // Procesar cada compra en el hilo principal del servidor
            for (Purchase purchase : pendingPurchases) {
                plugin.getLogger().info("§7⚙️ Programando procesamiento de compra ID: " + purchase.id);

                // Ejecutar en el hilo principal de Bukkit
                Bukkit.getScheduler().runTask(plugin, () -> {
                    processPurchase(purchase);
                });
            }

            plugin.getLogger().info("§a✅ Todas las compras han sido programadas para procesamiento");

        } catch (Exception e) {
            plugin.getLogger().severe("❌ Error en RankPurchaseTask: " + e.getMessage());
            plugin.getLogger().severe("❌ StackTrace completo:");
            e.printStackTrace();
        }
    }

    /**
     * Obtiene las compras pendientes desde la base de datos
     */
    private List<Purchase> getPendingPurchases() {
        List<Purchase> purchases = new ArrayList<>();

        // ⚠️ LOGGING PARA DEBUG
        plugin.getLogger().info("§e🔍 Buscando compras con status='paid'...");
        plugin.getLogger().info("§7🔧 Conectando a base de datos...");

        // 🔧 QUERY SIMPLIFICADA CON LEFT JOIN (más tolerante)
        String query = """
            SELECT 
                p.id,
                p.minecraft_uuid,
                p.product_id,
                p.product_name,
                rp.minecraft_username,
                rp.discord_id,
                sp.command_template,
                sp.name as product_name
            FROM purchases p
            LEFT JOIN ranked_players rp ON p.minecraft_uuid = rp.minecraft_uuid
            LEFT JOIN store_products sp ON p.product_id = sp.id
            WHERE p.status = 'paid'
            ORDER BY p.id DESC
            LIMIT ?
            """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            plugin.getLogger().info("§7✓ Conexión establecida, ejecutando query...");

            stmt.setInt(1, maxPurchasesPerCycle);
            ResultSet rs = stmt.executeQuery();

            // ⚠️ CONTADOR PARA DEBUG
            int count = 0;

            while (rs.next()) {
                count++;
                Purchase purchase = new Purchase();
                purchase.id = rs.getInt("id");
                purchase.minecraftUuid = rs.getString("minecraft_uuid");
                purchase.productId = rs.getInt("product_id");
                purchase.minecraftUsername = rs.getString("minecraft_username");
                purchase.discordId = rs.getString("discord_id");
                purchase.commandTemplate = rs.getString("command_template");
                purchase.productName = rs.getString("product_name");

                // ⚠️ LOGGING DETALLADO DE CADA COMPRA ENCONTRADA
                plugin.getLogger().info("§a✓ Compra encontrada: ID=" + purchase.id +
                    ", UUID=" + purchase.minecraftUuid +
                    ", User=" + purchase.minecraftUsername +
                    ", Product=" + purchase.productName +
                    ", Command=" + purchase.commandTemplate);

                purchases.add(purchase);
            }

            // ⚠️ RESULTADO FINAL
            if (count == 0) {
                plugin.getLogger().warning("§c⚠️ No se encontraron compras con status='paid'");
                plugin.getLogger().warning("§c🔍 Verificando si existen compras en general...");

                // Query adicional para verificar si hay compras en la tabla
                String checkQuery = "SELECT COUNT(*) as total FROM purchases";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
                     ResultSet checkRs = checkStmt.executeQuery()) {
                    if (checkRs.next()) {
                        int totalPurchases = checkRs.getInt("total");
                        plugin.getLogger().info("§7📊 Total de compras en la tabla: " + totalPurchases);
                    }
                }

                // Query para verificar compras con status 'paid'
                String paidQuery = "SELECT COUNT(*) as paid_total FROM purchases WHERE status = 'paid'";
                try (PreparedStatement paidStmt = conn.prepareStatement(paidQuery);
                     ResultSet paidRs = paidStmt.executeQuery()) {
                    if (paidRs.next()) {
                        int paidTotal = paidRs.getInt("paid_total");
                        plugin.getLogger().info("§7📊 Compras con status='paid': " + paidTotal);
                    }
                }

            } else {
                plugin.getLogger().info("§a✅ Total compras encontradas: " + count);
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Error obteniendo compras pendientes: " + e.getMessage());
            plugin.getLogger().severe("❌ SQLException Code: " + e.getErrorCode());
            plugin.getLogger().severe("❌ SQLState: " + e.getSQLState());
            e.printStackTrace();
        }

        plugin.getLogger().info("§7🏁 Finalizando búsqueda de compras. Encontradas: " + purchases.size());
        return purchases;
    }

    /**
     * Procesa una compra individual
     */
    private void processPurchase(Purchase purchase) {
        try {
            // Reemplazar {username} en el comando
            String command = purchase.commandTemplate.replace("{username}", purchase.minecraftUsername);

            // Validar que el comando no esté vacío
            if (command == null || command.trim().isEmpty()) {
                markPurchaseAsFailed(purchase.id, "Comando vacío o inválido");
                return;
            }

            plugin.getLogger().info("§a⚡ Ejecutando comando: /" + command);

            // Ejecutar el comando desde la consola
            boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

            if (success) {
                // Asignar rol de Discord "Sponsor" si el jugador tiene Discord vinculado
                assignSponsorRole(purchase);

                // Marcar como aplicada
                markPurchaseAsApplied(purchase.id);

                // Registrar en logs
                logPurchaseAction(purchase.id, "applied",
                    "Rango aplicado exitosamente: /" + command);

                // Notificar al jugador si está online
                notifyPlayerIfOnline(purchase.minecraftUsername, purchase.productName);

                plugin.getLogger().info("§a✅ Rango aplicado: /" + command + " (Purchase ID: " + purchase.id + ")");

            } else {
                // Marcar como fallida
                markPurchaseAsFailed(purchase.id, "El comando no se ejecutó correctamente");
                plugin.getLogger().warning("§c❌ Error ejecutando comando: /" + command + " (Purchase ID: " + purchase.id + ")");
            }

        } catch (Exception e) {
            markPurchaseAsFailed(purchase.id, "Excepción: " + e.getMessage());
            plugin.getLogger().severe("§c❌ Error procesando compra " + purchase.id + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Marca una compra como aplicada exitosamente
     */
    private void markPurchaseAsApplied(int purchaseId) {
        String query = "UPDATE purchases SET status = 'applied', applied_at = NOW() WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, purchaseId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Error marcando compra como aplicada: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Marca una compra como fallida
     */
    private void markPurchaseAsFailed(int purchaseId, String errorMessage) {
        String query = "UPDATE purchases SET error_message = ?, execution_attempts = execution_attempts + 1 WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, errorMessage);
            stmt.setInt(2, purchaseId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Error marcando compra como fallida: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Registra una acción en el log de compras
     */
    private void logPurchaseAction(int purchaseId, String action, String details) {
        String query = "INSERT INTO purchase_logs (purchase_id, action, details, created_at) VALUES (?, ?, ?, NOW())";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, purchaseId);
            stmt.setString(2, action);
            stmt.setString(3, details);
            stmt.executeUpdate();

        } catch (SQLException e) {
            plugin.getLogger().severe("❌ Error registrando log de compra: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Notifica al jugador si está online
     */
    private void notifyPlayerIfOnline(String username, String productName) {
        Player player = Bukkit.getPlayer(username);
        if (player != null && player.isOnline()) {
            player.sendMessage("§a§l✅ §7¡Tu rango ha sido aplicado exitosamente!");
            player.sendMessage("§7Producto: §e" + productName);
            player.sendMessage("§7Gracias por tu compra en §e§lKEKE RANKEDS§7!");
        }
    }

    /**
     * Asigna el rol de Discord "Sponsor" al jugador que realizó la compra
     */
    private void assignSponsorRole(Purchase purchase) {
        try {
            // Verificar si el jugador tiene Discord vinculado
            if (purchase.discordId == null || purchase.discordId.trim().isEmpty()) {
                plugin.getLogger().info("§7Usuario " + purchase.minecraftUsername + " no tiene Discord vinculado, omitiendo asignación de rol Sponsor");
                return;
            }

            // Obtener el bot de Discord
            var discordBot = plugin.getDiscordBot();
            if (discordBot == null || discordBot.getJDA() == null) {
                plugin.getLogger().warning("§c⚠️ Bot de Discord no disponible, no se puede asignar rol Sponsor");
                return;
            }

            var jda = discordBot.getJDA();

            // Obtener todos los guilds (servidores) donde está el bot
            for (var guild : jda.getGuilds()) {
                // Buscar el rol "Sponsor" por ID
                Role sponsorRole = guild.getRoleById("1413243740231041174");

                if (sponsorRole == null) {
                    continue; // Este servidor no tiene el rol Sponsor, probar el siguiente
                }

                // Buscar al miembro en este servidor
                guild.retrieveMemberById(purchase.discordId).queue(
                    member -> {
                        // Verificar si ya tiene el rol
                        if (member.getRoles().contains(sponsorRole)) {
                            plugin.getLogger().info("§7Usuario " + purchase.minecraftUsername + " ya tiene el rol Sponsor");
                            return;
                        }

                        // Asignar el rol Sponsor
                        guild.addRoleToMember(member, sponsorRole).queue(
                            success -> {
                                plugin.getLogger().info("§a✅ Rol 'Sponsor' asignado exitosamente a " + purchase.minecraftUsername + " (Discord: " + member.getUser().getName() + ")");

                                // Registrar en logs
                                logPurchaseAction(purchase.id, "discord_role_assigned",
                                    "Rol Sponsor asignado en Discord a " + member.getUser().getName());
                            },
                            error -> {
                                plugin.getLogger().warning("§c❌ Error asignando rol Sponsor a " + purchase.minecraftUsername + ": " + error.getMessage());

                                // Registrar el error en logs
                                logPurchaseAction(purchase.id, "discord_role_error",
                                    "Error asignando rol Sponsor: " + error.getMessage());
                            }
                        );
                    },
                    error -> {
                        // El usuario no está en este servidor, intentar el siguiente
                        plugin.getLogger().info("§7Usuario " + purchase.minecraftUsername + " no encontrado en servidor: " + guild.getName());
                    }
                );
            }

        } catch (Exception e) {
            plugin.getLogger().severe("§c❌ Error en assignSponsorRole para " + purchase.minecraftUsername + ": " + e.getMessage());
            e.printStackTrace();

            // Registrar el error en logs
            logPurchaseAction(purchase.id, "discord_role_exception",
                "Excepción asignando rol Sponsor: " + e.getMessage());
        }
    }

    /**
     * Clase interna para representar una compra
     */
    private static class Purchase {
        int id;
        String minecraftUuid;
        int productId;
        String minecraftUsername;
        String discordId;
        String commandTemplate;
        String productName;
    }
}
