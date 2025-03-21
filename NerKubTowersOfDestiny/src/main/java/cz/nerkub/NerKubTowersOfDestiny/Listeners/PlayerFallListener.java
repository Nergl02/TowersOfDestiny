package cz.nerkub.NerKubTowersOfDestiny.Listeners;

import cz.nerkub.NerKubTowersOfDestiny.Managers.ArenaManager;
import cz.nerkub.NerKubTowersOfDestiny.Managers.GameManager;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.List;

public class PlayerFallListener implements Listener {
	private final GameManager gameManager;
	private final ArenaManager arenaManager;

	public PlayerFallListener(GameManager gameManager, ArenaManager arenaManager) {
		this.gameManager = gameManager;
		this.arenaManager = arenaManager;
	}

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		String arenaName = gameManager.getPlayerArena(player);
		if (arenaName == null) return; // ✅ Hráč není v aréně

		double minY = arenaManager.getArenaMinY(arenaName);
		if (player.getLocation().getY() < minY) { // ✅ Hráč padl pod minimální Y
			event.setCancelled(true); // ✅ Zamezí dalšímu pohybu dolů

			player.setGameMode(GameMode.SPECTATOR); // ✅ Přepneme do spectator módu
			player.sendMessage(ChatColor.RED + "❌ Spadl jsi mimo arénu a byl jsi eliminován!");

			// ✅ Teleportujeme hráče do středu arény nad arénu
			Location center = arenaManager.getArenaCenter(arenaName);
			if (center != null) {
				center.setY(center.getY() + 5); // Zvýšíme nad arénu
				player.teleport(center);
			}

			// ✅ Informujeme ostatní hráče v aréně
			List<Player> players = gameManager.getPlayersInArena(arenaName);
			for (Player p : players) {
				p.sendMessage(ChatColor.YELLOW + player.getName() + ChatColor.RED + " byl eliminován!");
			}

			// ✅ Kontrola, zda zbývá poslední hráč
			if (players.size() == 1) {
				Player winner = players.get(0);
				gameManager.endGame(arenaName, winner);
			} else if (players.isEmpty()) {
				gameManager.endGame(arenaName, null);
			}
		}
	}
}
