package cz.nerkub.NerKubTowersOfDestiny.Listeners;

import cz.nerkub.NerKubTowersOfDestiny.Managers.DatabaseManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

	private final DatabaseManager databaseManager;

	public PlayerJoinListener(DatabaseManager databaseManager) {
		this.databaseManager = databaseManager;
	}


	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		databaseManager.createPlayerIfNotExists(player.getUniqueId(), player.getName()); // ✅ Registrace hráče při připojení
	}
}
