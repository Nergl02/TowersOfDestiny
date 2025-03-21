package cz.nerkub.NerKubTowersOfDestiny.Listeners;

import cz.nerkub.NerKubTowersOfDestiny.Managers.ArenaManager;
import cz.nerkub.NerKubTowersOfDestiny.Managers.DatabaseManager;
import cz.nerkub.NerKubTowersOfDestiny.Managers.GameManager;
import cz.nerkub.NerKubTowersOfDestiny.NerKubTowersOfDestiny;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerEventListener implements Listener {
	private final NerKubTowersOfDestiny plugin;
	private final ArenaManager arenaManager;
	private final GameManager gameManager;
	private final DatabaseManager databaseManager;

	public PlayerEventListener(NerKubTowersOfDestiny plugin, ArenaManager arenaManager, GameManager gameManager, DatabaseManager databaseManager) {
		this.plugin = plugin;
		this.arenaManager = arenaManager;
		this.gameManager = gameManager;
		this.databaseManager = databaseManager;
	}


	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		Location to = event.getTo();

	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		Player player = event.getEntity();
		Player killer = player.getKiller();
		String arenaName = gameManager.getPlayerArena(player);

		if (arenaName == null) return; // ✅ Hráč není v aréně

		String deathMessage = ChatColor.RED + "💀 " + player.getName() + " byl eliminován!";
		if (killer != null) {
			deathMessage += " ⚔️ " + ChatColor.YELLOW + "Zabit hráčem " + ChatColor.GOLD + killer.getName();
		}

		// ✅ Pošleme zprávu všem hráčům v aréně
		for (Player p : gameManager.getPlayersInArena(arenaName)) {
			p.sendMessage(deathMessage);
		}

		// ✅ Teleportujeme hráče do středu arény a přepneme na Spectator
		Location arenaCenter = arenaManager.getArenaCenter(arenaName);
		if (arenaCenter != null) {
			Bukkit.getScheduler().runTaskLater(plugin, () -> {
				player.teleport(arenaCenter);
				player.setGameMode(GameMode.SPECTATOR);
				player.sendMessage(ChatColor.GRAY + "👁 Sleduješ hru z výšky. Počkej na konec!");
			}, 1L); // ✅ Malý delay pro stabilní teleport
		}

	}

}
