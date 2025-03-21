package cz.nerkub.NerKubTowersOfDestiny.Listeners;

import cz.nerkub.NerKubTowersOfDestiny.Managers.ArenaManager;
import cz.nerkub.NerKubTowersOfDestiny.Managers.DatabaseManager;
import cz.nerkub.NerKubTowersOfDestiny.Managers.GameManager;
import cz.nerkub.NerKubTowersOfDestiny.NerKubTowersOfDestiny;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockPlaceListener implements Listener {
	private final NerKubTowersOfDestiny plugin;
	private final DatabaseManager databaseManager;
	private final GameManager gameManager;

	public BlockPlaceListener(NerKubTowersOfDestiny plugin, DatabaseManager databaseManager, GameManager gameManager) {
		this.plugin = plugin;
		this.databaseManager = databaseManager;
		this.gameManager = gameManager;
	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		Player player = event.getPlayer();
		Location blockLocation = event.getBlockPlaced().getLocation();
		String prefix = plugin.getMessages().getConfig().getString("prefix");

		// 🏰 Získání arény hráče
		String arenaName = gameManager.getPlayerArena(player);
		if (arenaName == null) return; // ✅ Pokud hráč není v aréně, ignorujeme

		// 📏 Získání hranic arény
		Location boundary1 = databaseManager.getArenaBoundary1(arenaName);
		Location boundary2 = databaseManager.getArenaBoundary2(arenaName);

		if (boundary1 == null || boundary2 == null) {
			return; // ✅ Pokud nemáme hranice, neblokujeme
		}

		// 🔍 Kontrola, zda je blok uvnitř hranic
		if (!isInsideBoundary(blockLocation, boundary1, boundary2)) {
			event.setCancelled(true);
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
					plugin.getMessages().getConfig().getString("game.build-outside-arena")));
		}
	}

	private boolean isInsideBoundary(Location loc, Location boundary1, Location boundary2) {
		int minX = Math.min(boundary1.getBlockX(), boundary2.getBlockX());
		int maxX = Math.max(boundary1.getBlockX(), boundary2.getBlockX());
		int minY = Math.min(boundary1.getBlockY(), boundary2.getBlockY());
		int maxY = Math.max(boundary1.getBlockY(), boundary2.getBlockY());
		int minZ = Math.min(boundary1.getBlockZ(), boundary2.getBlockZ());
		int maxZ = Math.max(boundary1.getBlockZ(), boundary2.getBlockZ());

		return loc.getBlockX() >= minX && loc.getBlockX() <= maxX &&
				loc.getBlockY() >= minY && loc.getBlockY() <= maxY &&
				loc.getBlockZ() >= minZ && loc.getBlockZ() <= maxZ;
	}
}
