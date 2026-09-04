package org.fabricioyv.model;

import java.util.UUID;

public class RankedPlayer {
    private int id;
    private UUID minecraftUuid;
    private String minecraftUsername;
    private String discordId;
    private boolean isVerified;
    private int elo;
    private double mmr; // Campo MMR añadido
    private boolean isInMatch;
    private String currentMatchId;
    private int wins;
    private int losses;
    private int gamesPlayed;
    private int totalKills;
    private int totalDeaths;
    private String verificationCode;
    private long verificationExpiry;

    // Constructor vacío
    public RankedPlayer() {
        this.mmr = 950.0; // Valor por defecto
    }

    // Constructor completo
    public RankedPlayer(UUID minecraftUuid, String minecraftUsername) {
        this.minecraftUuid = minecraftUuid;
        this.minecraftUsername = minecraftUsername;
        this.isVerified = false;
        this.elo = 0;
        this.mmr = 950.0; // Valor por defecto
        this.isInMatch = false;
        this.wins = 0;
        this.losses = 0;
        this.gamesPlayed = 0;
        this.currentMatchId = null;
    }

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public UUID getMinecraftUuid() { return minecraftUuid; }
    public void setMinecraftUuid(UUID minecraftUuid) { this.minecraftUuid = minecraftUuid; }

    public String getMinecraftUsername() { return minecraftUsername; }
    public void setMinecraftUsername(String minecraftUsername) { this.minecraftUsername = minecraftUsername; }

    public String getDiscordId() { return discordId; }
    public void setDiscordId(String discordId) { this.discordId = discordId; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }

    public int getElo() { return elo; }
    public void setElo(int elo) { this.elo = elo; }

    public double getMmr() { return mmr; }
    public void setMmr(double mmr) { this.mmr = mmr; }

    public boolean isInMatch() { return isInMatch; }
    public void setInMatch(boolean inMatch) { isInMatch = inMatch; }

    public int getWins() { return wins; }
    public void setWins(int wins) { this.wins = wins; }

    public String getCurrentMatchId() { return currentMatchId; }
    public void setCurrentMatchId(String currentMatchId) { this.currentMatchId = currentMatchId; }

    public int getLosses() { return losses; }
    public void setLosses(int losses) { this.losses = losses; }

    public int getGamesPlayed() { return gamesPlayed; }
    public void setGamesPlayed(int gamesPlayed) { this.gamesPlayed = gamesPlayed; }

    public String getVerificationCode() { return verificationCode; }
    public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }

    public long getVerificationExpiry() { return verificationExpiry; }
    public void setVerificationExpiry(long verificationExpiry) { this.verificationExpiry = verificationExpiry; }

    public int getTotalKills() { return totalKills; }
    public void setTotalKills(int totalKills) { this.totalKills = totalKills; }

    public int getTotalDeaths() { return totalDeaths; }
    public void setTotalDeaths(int totalDeaths) { this.totalDeaths = totalDeaths; }
}