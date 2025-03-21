package cz.nerkub.NerKubTowersOfDestiny.Expansions;

import cz.nerkub.NerKubTowersOfDestiny.Managers.DatabaseManager;
import cz.nerkub.NerKubTowersOfDestiny.Managers.GameManager;
import cz.nerkub.NerKubTowersOfDestiny.NerKubTowersOfDestiny;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class TowersOfDestinyPlaceholderExpansion extends PlaceholderExpansion{

	private final NerKubTowersOfDestiny plugin;
	private final DatabaseManager databaseManager;
	private final GameManager gameManager;

	public TowersOfDestinyPlaceholderExpansion(NerKubTowersOfDestiny plugin, DatabaseManager databaseManager, GameManager gameManager) {
		this.plugin = plugin;
		this.databaseManager = databaseManager;
		this.gameManager = gameManager;
	}

	@Override
	public @NotNull String getIdentifier() {
		return "towers";
	}

	@Override
	public @NotNull String getAuthor() {
		return "Nergl02";
	}

	@Override
	public @NotNull String getVersion() {
		return plugin.getDescription().getVersion();
	}

	@Override
	public boolean persist() {
		return true;
	}

	@Override
	public boolean canRegister() {
		return true;
	}

	@Override
	public String onPlaceholderRequest(Player player, String params) {
		if (gameManager == null || databaseManager == null) {
			System.out.println("⚠ DEBUG: gameManager nebo databaseManager je null!");
			return "";
		}

		// 🌍 Dynamické placeholdery s arénou
		if (params.contains("_")) {
			String[] parts = params.split("_", 2); // Rozdělení podle prvního podtržítka
			if (parts.length == 2) {
				String type = parts[0]; // Např. "players", "maxplayers", "mode"
				String arenaName = parts[1]; // Název arény

				System.out.println("✅ DEBUG: Placeholder požadavek -> Typ: " + type + ", Aréna: " + arenaName);

				if (!gameManager.getActiveArenas().contains(arenaName)) {
					System.out.println("⚠ DEBUG: Aréna '" + arenaName + "' neexistuje!");
					return "0"; // Aréna neexistuje, takže vrátíme výchozí hodnotu
				}

				return switch (type.toLowerCase()) {
					case "players" -> String.valueOf(gameManager.getPlayerCount(arenaName));
					case "maxplayers" -> String.valueOf(gameManager.getMaxPlayers(arenaName));
					case "mode" -> gameManager.getCurrentMode(arenaName);
					default -> null;
				};
			}
		}

		// 🌟 Statické placeholdery pro hráče (fungují i když není v aréně)
		UUID playerUUID = (player != null) ? player.getUniqueId() : null;

		return switch (params) {
			case "arena" -> (playerUUID != null) ? gameManager.getPlayerArena(player) : "None";
			case "players" -> (playerUUID != null) ? String.valueOf(gameManager.getPlayerCount(gameManager.getPlayerArena(player))) : "0";
			case "maxplayers" -> (playerUUID != null) ? String.valueOf(gameManager.getMaxPlayers(gameManager.getPlayerArena(player))) : "0";
			case "mode" -> (playerUUID != null) ? gameManager.getCurrentMode(gameManager.getPlayerArena(player)) : "Unknown";
			case "kills" -> (playerUUID != null) ? String.valueOf(databaseManager.getStat(playerUUID, "kills")) : "0";
			case "deaths" -> (playerUUID != null) ? String.valueOf(databaseManager.getStat(playerUUID, "deaths")) : "0";
			case "gamesplayed" -> (playerUUID != null) ? String.valueOf(databaseManager.getStat(playerUUID, "games_played")) : "0";
			case "wins" -> (playerUUID != null) ? String.valueOf(databaseManager.getStat(playerUUID, "wins")) : "0";
			case "losses" -> (playerUUID != null) ? String.valueOf(databaseManager.getStat(playerUUID, "losses")) : "0";
			case "highestkillstreak" -> (playerUUID != null) ? String.valueOf(databaseManager.getStat(playerUUID, "highest_killstreak")) : "0";
			default -> null;
		};
	}

}

