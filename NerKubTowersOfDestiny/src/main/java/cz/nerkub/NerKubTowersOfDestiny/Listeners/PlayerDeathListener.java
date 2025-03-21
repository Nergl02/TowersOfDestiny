package cz.nerkub.NerKubTowersOfDestiny.Listeners;

import cz.nerkub.NerKubTowersOfDestiny.Managers.GameManager;
import cz.nerkub.NerKubTowersOfDestiny.NerKubTowersOfDestiny;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener implements Listener {
	private final NerKubTowersOfDestiny plugin;
	private final GameManager gameManager;

	public PlayerDeathListener(NerKubTowersOfDestiny plugin, GameManager gameManager) {
		this.plugin = plugin;
		this.gameManager = gameManager;
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		String prefix = plugin.getMessages().getConfig().getString("prefix");
		Player player = event.getEntity();

		// ✅ Zjistíme, zda je hráč v nějaké aréně
		String arenaName = gameManager.getPlayerArena(player);
		if (arenaName == null) {
			return; // 🚀 Pokud není v aréně, neřešíme ho
		}

		// ✅ Oznámíme eliminaci
		event.setDeathMessage(null); // ❌ Zabráníme výpisu defaultní zprávy o smrti
		player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
				plugin.getMessages().getConfig().getString("game.elimination")));

		// ✅ Eliminujeme hráče (nastavíme spectator mód)
		gameManager.eliminatePlayer(player);
	}
}
