package org.fabricioyv.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.fabricioyv.model.RankedPlayer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DBManager {
    private HikariDataSource dataSource;

    public void initializeDatabase(String host, int port, String database, String username, String password) {
        try {
            HikariConfig config = new HikariConfig();

            // URL con configuraciones anti-timeout
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database +
                    "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" +
                    "&autoReconnect=true&useUnicode=true&characterEncoding=UTF-8" +
                    "&cachePrepStmts=true&useServerPrepStmts=true&rewriteBatchedStatements=true");

            config.setUsername(username);
            config.setPassword(password);

            // CONFIGURACIONES MEJORADAS PARA EVITAR TIMEOUTS
            config.setMaximumPoolSize(8);           // Reducido de 10 a 8
            config.setMinimumIdle(2);               // Mínimo 2 conexiones activas
            config.setConnectionTimeout(20000);     // Reducido a 20 segundos
            config.setIdleTimeout(300000);          // 5 minutos (reducido de 600000)
            config.setMaxLifetime(900000);          // 15 minutos (reducido de 1800000)
            config.setLeakDetectionThreshold(30000); // Reducido a 30 segundos

            // PROPIEDADES ADICIONALES PARA ESTABILIDAD
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("maintainTimeStats", "false");
            config.addDataSourceProperty("useLocalSessionState", "true");
            config.addDataSourceProperty("elideSetAutoCommits", "true");

            // TEST DE CONEXIÓN MEJORADO
            config.setConnectionTestQuery("SELECT 1");
            config.setValidationTimeout(5000);

            this.dataSource = new HikariDataSource(config);

            // Probar conexión inicial con retry
            testConnection();
            createTables();

            System.out.println("✅ Conexión a MySQL exitosa con pool configurado!");

        } catch (Exception e) {
            System.err.println("❌ Error conectando a MySQL:");
            System.err.println("Host: " + host + ":" + port);
            System.err.println("Database: " + database);
            System.err.println("Usuario: " + username);
            e.printStackTrace();
        }
    }

    private void testConnection() throws SQLException {
        int maxRetries = 3;
        SQLException lastException = null;

        for (int i = 0; i < maxRetries; i++) {
            try (Connection conn = dataSource.getConnection()) {
                if (conn.isValid(5)) {
                    System.out.println("✅ Test de conexión exitoso (intento " + (i + 1) + ")");
                    return;
                }
            } catch (SQLException e) {
                lastException = e;
                System.err.println("⚠️ Test de conexión fallido (intento " + (i + 1) + "): " + e.getMessage());

                if (i < maxRetries - 1) {
                    try {
                        Thread.sleep(2000); // Esperar 2 segundos antes del siguiente intento
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new SQLException("Conexión interrumpida", ie);
                    }
                }
            }
        }

        throw new SQLException("No se pudo establecer conexión después de " + maxRetries + " intentos", lastException);
    }

    private void createTables() {
        String createPlayerTable = """
        
                CREATE TABLE IF NOT EXISTS ranked_players (
            id INT AUTO_INCREMENT PRIMARY KEY,
            minecraft_uuid VARCHAR(36) NOT NULL UNIQUE,
            minecraft_username VARCHAR(16) NOT NULL,
            discord_id VARCHAR(20),
            is_verified TINYINT(1) DEFAULT 0,
            elo INT DEFAULT 0,
            mmr DOUBLE DEFAULT 950.0,
            is_in_match TINYINT(1) DEFAULT 0,
            current_match_id VARCHAR(50),
            wins INT DEFAULT 0,
            losses INT DEFAULT 0,
            games_played INT DEFAULT 0,
            total_kills INT DEFAULT 0,
            total_deaths INT DEFAULT 0,
            verification_code VARCHAR(8),
            verification_expiry BIGINT,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            INDEX idx_discord_id (discord_id),
            INDEX idx_elo (elo),
            INDEX idx_mmr (mmr),
            INDEX idx_in_match (is_in_match),
            INDEX idx_minecraft_uuid (minecraft_uuid)
        )
        """;

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createPlayerTable);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public RankedPlayer getPlayer(UUID uuid) {
        String query = "SELECT * FROM ranked_players WHERE minecraft_uuid = ?";
        // Log para consola Bukkit (si está disponible)
        try {
            org.bukkit.Bukkit.getConsoleSender().sendMessage("§e[DEBUG] Buscando UUID: " + uuid.toString());
        } catch (Throwable ignored) {}
        // Log para consola estándar (siempre)
        System.out.println("[DEBUG] Buscando UUID: " + uuid.toString());
        for (int retry = 0; retry < 2; retry++) {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {

                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    try {
                        org.bukkit.Bukkit.getConsoleSender().sendMessage("§a[DEBUG] Jugador encontrado en la base de datos: " + rs.getString("minecraft_username"));
                    } catch (Throwable ignored) {}
                    System.out.println("[DEBUG] Jugador encontrado en la base de datos: " + rs.getString("minecraft_username"));
                    return mapResultSetToPlayer(rs);
                } else {
                    try {
                        org.bukkit.Bukkit.getConsoleSender().sendMessage("§c[DEBUG] No se encontró jugador con UUID: " + uuid.toString());
                    } catch (Throwable ignored) {}
                    System.out.println("[DEBUG] No se encontró jugador con UUID: " + uuid.toString());
                }
                return null; // No encontrado, pero consulta exitosa

            } catch (SQLException e) {
                try {
                    org.bukkit.Bukkit.getConsoleSender().sendMessage("§c❌ Error en getPlayer (intento " + (retry + 1) + "): " + e.getMessage());
                } catch (Throwable ignored) {}
                System.out.println("[DEBUG] ❌ Error en getPlayer (intento " + (retry + 1) + "): " + e.getMessage());

                if (retry == 1) { // Último intento
                    e.printStackTrace();
                    return null;
                }

                // Esperar antes de reintentar
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
        return null;
    }
    public List<RankedPlayer> getTopPlayersByElo(int limit) {
        String query = "SELECT * FROM ranked_players WHERE is_verified = TRUE ORDER BY elo DESC LIMIT ?";
        List<RankedPlayer> players = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                players.add(mapResultSetToPlayer(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return players;
    }
    public RankedPlayer getPlayerByDiscordId(String discordId) {
        String query = "SELECT * FROM ranked_players WHERE discord_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, discordId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToPlayer(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    public RankedPlayer getPlayerByMinecraftName(String minecraftName) {
        String query = "SELECT * FROM ranked_players WHERE minecraft_username = ? AND is_verified = TRUE";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, minecraftName);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToPlayer(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    public int getPlayerRankPosition(String discordId) {
        String query = """
        SELECT COUNT(*) + 1 as
                position
        FROM
                ranked_players 
        WHERE
                is_verified = TRUE 
        AND elo > (SELECT elo FROM ranked_players WHERE discord_id = ?)
    """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, discordId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("position");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public void createPlayer(RankedPlayer rankedPlayer) {
        String query = """
            INSERT INTO ranked_players (minecraft_uuid, minecraft_username, elo, verification_code, verification_expiry)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, rankedPlayer.getMinecraftUuid().toString());
            stmt.setString(2, rankedPlayer.getMinecraftUsername());
            stmt.setInt(3, rankedPlayer.getElo());
            stmt.setString(4, rankedPlayer.getVerificationCode());
            stmt.setLong(5, rankedPlayer.getVerificationExpiry());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private RankedPlayer mapResultSetToPlayer(ResultSet rs) throws SQLException {
        RankedPlayer player = new RankedPlayer();
        player.setId(rs.getInt("id"));
        player.setMinecraftUuid(UUID.fromString(rs.getString("minecraft_uuid")));
        player.setMinecraftUsername(rs.getString("minecraft_username"));
        player.setDiscordId(rs.getString("discord_id"));
        player.setVerified(rs.getBoolean("is_verified"));
        player.setElo(rs.getInt("elo"));
        player.setMmr(rs.getDouble("mmr")); // AÑADIDO: Mapeo del MMR desde la base de datos
        player.setInMatch(rs.getBoolean("is_in_match"));
        player.setCurrentMatchId(rs.getString("current_match_id"));
        player.setWins(rs.getInt("wins"));
        player.setLosses(rs.getInt("losses"));
        player.setGamesPlayed(rs.getInt("games_played"));
        player.setTotalKills(rs.getInt("total_kills"));
        player.setTotalDeaths(rs.getInt("total_deaths"));
        player.setVerificationCode(rs.getString("verification_code"));
        player.setVerificationExpiry(rs.getLong("verification_expiry"));
        return player;
    }

    public void updatePlayer(RankedPlayer rankedPlayer) {
        String query = "UPDATE ranked_players SET minecraft_username = ?, discord_id = ?, is_verified = ?, elo = ?, mmr = ?, is_in_match = ?, current_match_id = ?, wins = ?, losses = ?, games_played = ?, total_kills = ?, total_deaths = ?, verification_code = ?, verification_expiry = ? WHERE minecraft_uuid = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, rankedPlayer.getMinecraftUsername());
            stmt.setString(2, rankedPlayer.getDiscordId());
            stmt.setBoolean(3, rankedPlayer.isVerified());
            stmt.setInt(4, rankedPlayer.getElo());
            stmt.setDouble(5, rankedPlayer.getMmr()); // AÑADIDO: Actualizar MMR en la base de datos
            stmt.setBoolean(6, rankedPlayer.isInMatch());
            stmt.setString(7, rankedPlayer.getCurrentMatchId());
            stmt.setInt(8, rankedPlayer.getWins());
            stmt.setInt(9, rankedPlayer.getLosses());
            stmt.setInt(10, rankedPlayer.getGamesPlayed());
            stmt.setInt(11, rankedPlayer.getTotalKills());
            stmt.setInt(12, rankedPlayer.getTotalDeaths());
            stmt.setString(13, rankedPlayer.getVerificationCode());
            stmt.setLong(14, rankedPlayer.getVerificationExpiry());
            stmt.setString(15, rankedPlayer.getMinecraftUuid().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public RankedPlayer getPlayerByVerificationCode(String code) {
        String query = "SELECT * FROM ranked_players WHERE verification_code = ? AND verification_expiry > ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, code);
            stmt.setLong(2, System.currentTimeMillis());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToPlayer(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    return null;
    }
    public int resetAllMatches() {
        String query = "UPDATE ranked_players SET is_in_match = FALSE, current_match_id = NULL WHERE is_in_match = TRUE";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            return stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error reseteando partidas: " + e.getMessage());
        }
    }

    public int resetAllPlayerStats() {
        String query = """
                UPDATE ranked_players 
                SET elo = 0,
                    wins = 0, 
                    losses = 0, 
                    games_played = 0, 
                    total_kills = 0, 
                    total_deaths = 0, 
                    mmr = 950.0
                WHERE is_verified = TRUE
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            return stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error reseteando estadísticas de jugadores: " + e.getMessage());
        }
    }

    /**
         * Obtiene el UUID almacenado en la base de datos para un nom
     ecraft.
         * NOTA: Este método NO consulta Mojang, solo devuelve el UUID registrado en
      datos.
         * Si el jugador cambió de nombre, este método puede devolver un UUID desac
     o nulo.
         * Para obtener el UUID actual, usa
     Mojang.
         */
    public UUID getUuidByMinecraftName(String minecraftName) {
        String query = "SELECT minecraft_uuid FROM ranked_players WHERE minecraft_username = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, minecraftName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return UUID.fromString(rs.getString("minecraft_uuid"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Busca un jugador por su UUID de Minecraft (para /stats por nombre)
     */
    public RankedPlayer getPlayerByMinecraftUuid(UUID uuid) {
        return getPlayer(uuid);
    }

    /**
     * Obtiene una conexión del pool para uso externo
     * IMPORTANTE: Debe usarse con try-with-resources para cerrar automáticamente
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            System.out.println("🔄 Cerrando pool de conexiones...");
            dataSource.close();
            System.out.println("✅ Pool cerrado correctamente");
        }
    }
}
