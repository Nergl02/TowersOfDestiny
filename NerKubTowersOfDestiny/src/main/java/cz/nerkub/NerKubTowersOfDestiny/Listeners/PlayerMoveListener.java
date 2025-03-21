package cz.nerkub.NerKubTowersOfDestiny.Listeners;

import cz.nerkub.NerKubTowersOfDestiny.Managers.ArenaManager;
import cz.nerkub.NerKubTowersOfDestiny.Managers.GameManager;
import cz.nerkub.NerKubTowersOfDestiny.NerKubTowersOfDestiny;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerMoveListener implements Listener {
	private final NerKubTowersOfDestiny plugin;
	private final ArenaManager arenaManager;
	private final GameManager gameManager;
	private final Map<UUID, Location> frozenPlayers = new HashMap<>();

	public PlayerMoveListener(NerKubTowersOfDestiny plugin, ArenaManager arenaManager, GameManager gameManager) {
		this.plugin = plugin;
		this.arenaManager = arenaManager;
		this.gameManager = gameManager;
	}

	// ✅ Zamrazí hráče na jeho aktuální pozici
	public void freezePlayer(Player player) {
		String prefix = plugin.getMessages().getConfig().getString("prefix");
		frozenPlayers.put(player.getUniqueId(), player.getLocation());
		player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
				plugin.getMessages().getConfig().getString("game.frozen")));
	}

	public void unfreezePlayer(Player player) {
		frozenPlayers.remove(player.getUniqueId());
	}

	// ✅ Odemkne všechny hráče
	public void unfreezePlayers() {
		String prefix = plugin.getMessages().getConfig().getString("prefix");
		for (UUID playerId : frozenPlayers.keySet()) {
			Player player = Bukkit.getPlayer(playerId);
			if (player != null) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
						plugin.getMessages().getConfig().getString("game.unfrozen")));
			}
		}
		frozenPlayers.clear();
	}

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();

		// ✅ 1️⃣ Kontrola zamrznutých hráčů
		if (frozenPlayers.containsKey(player.getUniqueId())) {
			Location freezeLocation = frozenPlayers.get(player.getUniqueId());

			if (event.getTo() != null && (event.getTo().getBlockX() != freezeLocation.getBlockX() ||
					event.getTo().getBlockY() != freezeLocation.getBlockY() ||
					event.getTo().getBlockZ() != freezeLocation.getBlockZ())) {
				player.teleport(freezeLocation);
			}
			return; // ✅ Pokud je zmražený, neřešíme další pohyb
		}

		// ✅ 2️⃣ Detekce pádu pod arénu -> eliminace
		String arenaName = gameManager.getPlayerArena(player);
		if (arenaName == null) return;

		int minY = arenaManager.getArenaMinY(arenaName); // ✅ Získáme spodní hranici arény
		if (player.getLocation().getBlockY() < minY) {
			gameManager.eliminatePlayer(player);
		}
	}
}
