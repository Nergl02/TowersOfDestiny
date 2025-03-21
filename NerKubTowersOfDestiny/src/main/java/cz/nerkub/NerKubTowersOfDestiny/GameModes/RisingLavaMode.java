package cz.nerkub.NerKubTowersOfDestiny.GameModes;

import cz.nerkub.NerKubTowersOfDestiny.GameModes.GameModeBase;
import cz.nerkub.NerKubTowersOfDestiny.Managers.ArenaManager;
import cz.nerkub.NerKubTowersOfDestiny.Managers.DatabaseManager;
import cz.nerkub.NerKubTowersOfDestiny.NerKubTowersOfDestiny;
import org.bukkit.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class RisingLavaMode implements GameModeBase {
	private final NerKubTowersOfDestiny plugin;
	private final ArenaManager arenaManager;
	private final DatabaseManager databaseManager;
	private final Random random = new Random();
	private BukkitTask lavaTask; // 🔴 Uložíme úkol do proměnné, abychom ho mohli zrušit

	public RisingLavaMode(NerKubTowersOfDestiny plugin, ArenaManager arenaManager, DatabaseManager databaseManager) {
		this.plugin = plugin;
		this.arenaManager = arenaManager;
		this.databaseManager = databaseManager;
	}

	@Override
	public void startMode(String arenaName) {
		String prefix = plugin.getMessages().getConfig().getString("prefix");
		String worldName = databaseManager.getArenaWorld(arenaName);
		World world = Bukkit.getWorld(worldName);

		if (world == null) {
			System.out.println("⚠ Chyba: Svět " + worldName + " neexistuje nebo není načten!");
			return;
		}

		Location boundary1 = databaseManager.getArenaBoundary1(arenaName);
		Location boundary2 = databaseManager.getArenaBoundary2(arenaName);
		if (boundary1 == null || boundary2 == null) {
			System.out.println("⚠ Chyba: Nelze načíst hranice arény pro " + arenaName);
			return;
		}

		int minX = Math.min(boundary1.getBlockX(), boundary2.getBlockX());
		int maxX = Math.max(boundary1.getBlockX(), boundary2.getBlockX());
		int minZ = Math.min(boundary1.getBlockZ(), boundary2.getBlockZ());
		int maxZ = Math.max(boundary1.getBlockZ(), boundary2.getBlockZ());
		int minY = Math.min(boundary1.getBlockY(), boundary2.getBlockY());
		int maxY = Math.max(boundary1.getBlockY(), boundary2.getBlockY());

		int lavaSpeed = plugin.getConfig().getInt("game.rising_lava_speed", 5);
		double lavaCoverage = plugin.getConfig().getDouble("game.lava_coverage", 0.5);

		Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', prefix +
				plugin.getModes().getConfig().getString("rising-lava.start-message").replace("%arenaname%", arenaName)));

		lavaTask = new BukkitRunnable() {
			int lavaY = minY;

			@Override
			public void run() {
				if (lavaY >= maxY) {
					cancel();
					return;
				}

				for (int x = minX; x <= maxX; x++) {
					for (int z = minZ; z <= maxZ; z++) {
						if (random.nextDouble() <= lavaCoverage) { // 50 % pokrytí
							Location lavaLocation = new Location(world, x, lavaY, z);
							if (lavaLocation.getBlock().getType() == Material.AIR) {
								lavaLocation.getBlock().setType(Material.LAVA);
							}
						}
					}
				}

				lavaY++;
			}
		}.runTaskTimer(plugin, 0L, Math.max(1, 100L - lavaSpeed)); // Čím vyšší speed, tím rychlejší
	}

	public void stopMode(String arenaName) {
		if (lavaTask != null) {
			lavaTask.cancel();
			lavaTask = null;
		}
		if (plugin.getConfig().getBoolean("debug")) {
			System.out.println("🛑 Rising Lava mód byl zastaven.");
		}

		removeLava(arenaName); // ✅ Po zastavení odstraníme lávu
	}


	public void removeLava(String arenaName) {
		String worldName = databaseManager.getArenaWorld(arenaName);
		World world = Bukkit.getWorld(worldName);
		if (world == null) {
			if (plugin.getConfig().getBoolean("debug")) {
				System.out.println("❌ Chyba: Svět " + worldName + " neexistuje!");
			}
			return;
		}

		Location boundary1 = databaseManager.getArenaBoundary1(arenaName);
		Location boundary2 = databaseManager.getArenaBoundary2(arenaName);
		if (boundary1 == null || boundary2 == null) {
			if (plugin.getConfig().getBoolean("debug")) {
				System.out.println("❌ Chyba: Hranice arény " + arenaName + " nejsou definovány!");
			}
			return;
		}

		int minX = Math.min(boundary1.getBlockX(), boundary2.getBlockX()) - 10;
		int maxX = Math.max(boundary1.getBlockX(), boundary2.getBlockX()) + 10;
		int minY = 1; // 🌍 Začneme od Y=1, aby se láva odstranila až k bedrocku
		int maxY = Math.max(boundary1.getBlockY(), boundary2.getBlockY()) + 10;
		int minZ = Math.min(boundary1.getBlockZ(), boundary2.getBlockZ()) - 10;
		int maxZ = Math.max(boundary1.getBlockZ(), boundary2.getBlockZ()) + 10;

		if (plugin.getConfig().getBoolean("debug")) {
			System.out.println("🛠️ Odstraňuji lávu z arény '" + arenaName + "'...");
		}

		for (int x = minX; x <= maxX; x++) {
			for (int y = minY; y <= maxY; y++) { // ✅ Procházíme až k Y=1
				for (int z = minZ; z <= maxZ; z++) {
					Location loc = new Location(world, x, y, z);
					if (loc.getBlock().getType() == Material.LAVA) {
						removeLavaRecursively(loc); // 🔄 BFS pro odstranění lávy
					}
				}
			}
		}

		if (plugin.getConfig().getBoolean("debug")) {
			System.out.println("✅ Láva byla odstraněna z arény '" + arenaName + "' a jejího okolí.");
		}
	}

	private void removeLavaRecursively(Location location) {
		World world = location.getWorld();
		if (world == null) return;

		Queue<Location> queue = new LinkedList<>();
		queue.add(location);

		while (!queue.isEmpty()) {
			Location current = queue.poll();
			if (current.getBlock().getType() == Material.LAVA) {
				current.getBlock().setType(Material.AIR);

				// Přidáme sousední bloky do fronty pro odstranění
				queue.add(current.clone().add(1, 0, 0));
				queue.add(current.clone().add(-1, 0, 0));
				queue.add(current.clone().add(0, 1, 0));
				queue.add(current.clone().add(0, -1, 0));
				queue.add(current.clone().add(0, 0, 1));
				queue.add(current.clone().add(0, 0, -1));
			}
		}
	}


}
