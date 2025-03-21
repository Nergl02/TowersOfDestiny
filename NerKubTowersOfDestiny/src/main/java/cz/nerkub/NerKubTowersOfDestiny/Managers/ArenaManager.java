package cz.nerkub.NerKubTowersOfDestiny.Managers;

import cz.nerkub.NerKubTowersOfDestiny.NerKubTowersOfDestiny;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class ArenaManager {
	private final NerKubTowersOfDestiny plugin;
	private final DatabaseManager databaseManager;
	private final GameManager gameManager;

	public ArenaManager(NerKubTowersOfDestiny plugin, DatabaseManager databaseManager, GameManager gameManager) {
		this.plugin = plugin;
		this.databaseManager = databaseManager;
		this.gameManager = gameManager;
	}

	// Metoda pro vytvoření nové arény
	public void createArena(String name, Location boundary1, Location boundary2) {
		try {
			String worldName = boundary1.getWorld().getName(); // ✅ Správný svět
			String boundary1Str = worldName + ", " + boundary1.getBlockX() + ", " + boundary1.getBlockY() + ", " + boundary1.getBlockZ();
			String boundary2Str = worldName + ", " + boundary2.getBlockX() + ", " + boundary2.getBlockY() + ", " + boundary2.getBlockZ();

			databaseManager.createArena(name, boundary1Str, boundary2Str, worldName);
			if (plugin.getConfig().getBoolean("debug")) {
				System.out.println("✅ Aréna '" + name + "' byla vytvořena ve světě '" + worldName + "'.");
			}
			saveArenaBlocks(name);
		} catch (SQLException e) {
			e.printStackTrace();
			if (plugin.getConfig().getBoolean("debug")) {
				System.out.println("❌ Chyba při vytváření arény: " + e.getMessage());
			}
		}
	}

	// Metoda pro přidání spawnpointu do arény
	public void addSpawnPoint(String arenaName, String worldName, Location location) {
		try {
			double x = location.getX();
			double y = location.getY();
			double z = location.getZ();
			float yaw = location.getYaw();
			float pitch = location.getPitch();

			databaseManager.addSpawnPoint(arenaName, worldName, x, y, z, yaw, pitch);
			if (plugin.getConfig().getBoolean("debug")) {
				System.out.println("✅ Spawnpoint přidán: " + x + ", " + y + ", " + z + " (Yaw: " + yaw + ", Pitch: " + pitch + ") do arény " + arenaName + ".");
			}
		} catch (SQLException e) {
			e.printStackTrace();
			if (plugin.getConfig().getBoolean("debug")) {
				System.out.println("❌ Chyba při přidávání spawnpointu: " + e.getMessage());
			}
		}
	}


	// ✅ Převod String souřadnic na Bukkit Location
	public List<Location> getSpawnPoints(String arenaName) {
		List<Location> spawnLocations = new ArrayList<>();

		// 🔍 Debug: Ověření databázového připojení
		if (databaseManager == null) {
			if (plugin.getConfig().getBoolean("debug")) {
				System.out.println("❌ Chyba: DatabaseManager není inicializován v ArenaManager!");
			}
			return spawnLocations;
		}

		String sql = "SELECT world_name, x, y, z, yaw, pitch FROM spawnpoints WHERE arena_name = ?";

		try (Connection conn = databaseManager.getConnection();  // ✅ Oprava volání getConnection()
			 PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setString(1, arenaName);

			try (ResultSet resultSet = pstmt.executeQuery()) {
				while (resultSet.next()) {
					String worldName = resultSet.getString("world_name");
					World world = Bukkit.getWorld(worldName);
					if (world == null) {
						if (plugin.getConfig().getBoolean("debug")) {
							System.out.println("⚠️ Svět '" + worldName + "' nebyl nalezen, přeskočeno.");
						}
						continue;
					}

					double x = resultSet.getDouble("x");
					double y = resultSet.getDouble("y");
					double z = resultSet.getDouble("z");
					float yaw = resultSet.getFloat("yaw");
					float pitch = resultSet.getFloat("pitch");

					spawnLocations.add(new Location(world, x, y, z, yaw, pitch));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			if (plugin.getConfig().getBoolean("debug")) {
				System.out.println("❌ Chyba při načítání spawnpointů: " + e.getMessage());
			}
		}
		return spawnLocations;
	}


	public String findBestArena() {
		String bestArena = null;
		int maxPlayers = -1;

		for (String arena : getAllArenas()) { // ✅ Používáme všechny arény, nejen aktivní
			GameState state = gameManager.getGameState(arena);
			int players = gameManager.getPlayerCount(arena);
			int maxSlots = gameManager.getMaxPlayers(arena);

			if (state == GameState.WAITING && players < maxSlots) {
				if (players > maxPlayers) {
					bestArena = arena;
					maxPlayers = players;
				}
			}
		}

		// ✅ Pokud žádná aréna nemá hráče, vezmeme první dostupnou
		if (bestArena == null) {
			for (String arena : getAllArenas()) {
				if (gameManager.getGameState(arena) == GameState.WAITING) {
					return arena;
				}
			}
		}

		return bestArena;
	}

	public Set<String> getAllArenas() {
		return new HashSet<>(databaseManager.getAllArenas()); // ✅ Získá seznam všech arén z databáze
	}

	public int getArenaMinY(String arenaName) {
		String boundary1, boundary2;

		try (Connection conn = databaseManager.getConnection();
			 PreparedStatement pstmt = conn.prepareStatement("SELECT boundary1, boundary2 FROM arenas WHERE name = ?")) {
			pstmt.setString(1, arenaName);

			try (ResultSet resultSet = pstmt.executeQuery()) {
				if (resultSet.next()) {
					boundary1 = resultSet.getString("boundary1");
					boundary2 = resultSet.getString("boundary2");

					if (boundary1 != null && boundary2 != null) {
						int y1 = Integer.parseInt(boundary1.split(", ")[1]); // ✅ Y souřadnice první hranice
						int y2 = Integer.parseInt(boundary2.split(", ")[1]); // ✅ Y souřadnice druhé hranice
						return Math.min(y1, y2); // ✅ Vrátí nejnižší Y
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			if (plugin.getConfig().getBoolean("debug")) {
				System.out.println("❌ Chyba při získávání minimální Y souřadnice arény: " + e.getMessage());
			}
		}

		return 0; // ✅ Výchozí hodnota, pokud něco selže
	}

	public void saveArenaBlocks(String arenaName) {
		String worldName = databaseManager.getArenaWorld(arenaName); // ✅ Získáme správný svět
		World world = Bukkit.getWorld(worldName);
		if (world == null) {
			if (plugin.getConfig().getBoolean("debug")) {
				System.out.println("❌ Chyba: Svět arény '" + worldName + "' neexistuje!");
			}
			return;
		}

		Location boundary1 = databaseManager.getArenaBoundary1(arenaName);
		Location boundary2 = databaseManager.getArenaBoundary2(arenaName);
		if (boundary1 == null || boundary2 == null) {
			if (plugin.getConfig().getBoolean("debug")) {
				System.out.println("❌ Chyba: Hraniční souřadnice arény '" + arenaName + "' nejsou nastaveny!");
			}
			return;
		}

		if (plugin.getConfig().getBoolean("debug")) {
			System.out.println("🔍 Ukládání bloků pro arénu '" + arenaName + "' ve světě: " + world.getName());
		}

		int minX = Math.min(boundary1.getBlockX(), boundary2.getBlockX());
		int maxX = Math.max(boundary1.getBlockX(), boundary2.getBlockX());
		int minY = Math.min(boundary1.getBlockY(), boundary2.getBlockY());
		int maxY = Math.max(boundary1.getBlockY(), boundary2.getBlockY());
		int minZ = Math.min(boundary1.getBlockZ(), boundary2.getBlockZ());
		int maxZ = Math.max(boundary1.getBlockZ(), boundary2.getBlockZ());

		List<SavedBlockData> blockDataList = new ArrayList<>();

		for (int x = minX; x <= maxX; x++) {
			for (int y = minY; y <= maxY; y++) {
				for (int z = minZ; z <= maxZ; z++) {
					Location loc = new Location(world, x, y, z);
					Block block = loc.getBlock();
					Material material = block.getType();

					if (material != Material.AIR) { // ✅ Ukládáme jen bloky, které nejsou vzduch
						String blockData = block.getBlockData().getAsString();
						blockDataList.add(new SavedBlockData(arenaName, world.getName(), x, y, z, material, blockData));
						if (plugin.getConfig().getBoolean("debug")) {
							System.out.println("✔ Uložen blok: " + material.name() + " na " + x + ", " + y + ", " + z);
						}
					}
				}
			}
		}

		if (!blockDataList.isEmpty()) {
			databaseManager.saveBlockBatch(blockDataList);
			if (plugin.getConfig().getBoolean("debug")) {
				System.out.println("✅ Bloky arény '" + arenaName + "' byly uloženy.");
			}
		} else {
			if (plugin.getConfig().getBoolean("debug")) {
				System.out.println("⚠ Žádné bloky k uložení pro arénu '" + arenaName + "'.");
			}
		}
	}

	public void restoreArenaBlocks(String arenaName) {
		List<SavedBlockData> blocks = databaseManager.getArenaBlocks(arenaName);
		String worldName = databaseManager.getArenaWorld(arenaName); // ✅ Opraveno
		World world = Bukkit.getWorld(worldName);

		if (world == null) {
			if (plugin.getConfig().getBoolean("debug")) {
				System.out.println("❌ Svět arény '" + worldName + "' nebyl nalezen!");
			}
			return;
		}

		for (SavedBlockData blockData : blocks) {
			Location loc = new Location(world, blockData.getX(), blockData.getY(), blockData.getZ());
			Block block = loc.getBlock();

			block.setType(blockData.getMaterial(), false);

			if (blockData.getBlockData() != null && !blockData.getBlockData().isEmpty()) {
				try {
					BlockData data = Bukkit.createBlockData(blockData.getBlockData());
					block.setBlockData(data, false);
				} catch (IllegalArgumentException e) {
					if (plugin.getConfig().getBoolean("debug")) {
						System.out.println("❌ Chyba při nastavování BlockData: " + e.getMessage());
					}
				}
			}
		}
		if (plugin.getConfig().getBoolean("debug")) {
			System.out.println("✅ Bloky arény '" + arenaName + "' byly obnoveny.");
		}
	}


	public void resetArena(String arenaName) {

		clearArenaBlocks(arenaName);
		// ✅ 2️⃣ Obnovíme původní stav bloků z databáze
		restoreArenaBlocks(arenaName);

		// ✅ 3️⃣ Odstraníme entity (moby, dropnuté itemy apod.)
		removeEntitiesInArena(arenaName);

		if (plugin.getConfig().getBoolean("debug")) {
			System.out.println("✅ Aréna '" + arenaName + "' byla úspěšně resetována.");
		}
	}

	private void removeEntitiesInArena(String arenaName) {
		World world = Bukkit.getWorld(databaseManager.getArenaWorld(arenaName));
		if (world == null) return;

		Location boundary1 = databaseManager.getArenaBoundary1(arenaName);
		Location boundary2 = databaseManager.getArenaBoundary2(arenaName);

		if (boundary1 == null || boundary2 == null) return;

		int minX = Math.min(boundary1.getBlockX(), boundary2.getBlockX());
		int maxX = Math.max(boundary1.getBlockX(), boundary2.getBlockX());
		int minY = Math.min(boundary1.getBlockY(), boundary2.getBlockY());
		int maxY = Math.max(boundary1.getBlockY(), boundary2.getBlockY());
		int minZ = Math.min(boundary1.getBlockZ(), boundary2.getBlockZ());
		int maxZ = Math.max(boundary1.getBlockZ(), boundary2.getBlockZ());

		world.getEntities().stream()
				.filter(e -> e.getLocation().getBlockX() >= minX && e.getLocation().getBlockX() <= maxX)
				.filter(e -> e.getLocation().getBlockY() >= minY && e.getLocation().getBlockY() <= maxY)
				.filter(e -> e.getLocation().getBlockZ() >= minZ && e.getLocation().getBlockZ() <= maxZ)
				.forEach(e -> e.remove()); // ✅ Odstraníme entity v aréně
	}


	private void clearArenaBlocks(String arenaName) {
		World world = Bukkit.getWorld(databaseManager.getArenaWorld(arenaName));
		if (world == null) return;

		Location boundary1 = databaseManager.getArenaBoundary1(arenaName);
		Location boundary2 = databaseManager.getArenaBoundary2(arenaName);

		if (boundary1 == null || boundary2 == null) return;

		int minX = Math.min(boundary1.getBlockX(), boundary2.getBlockX());
		int maxX = Math.max(boundary1.getBlockX(), boundary2.getBlockX());
		int minY = Math.min(boundary1.getBlockY(), boundary2.getBlockY());
		int maxY = Math.max(boundary1.getBlockY(), boundary2.getBlockY());
		int minZ = Math.min(boundary1.getBlockZ(), boundary2.getBlockZ());
		int maxZ = Math.max(boundary1.getBlockZ(), boundary2.getBlockZ());

		for (int x = minX; x <= maxX; x++) {
			for (int y = minY; y <= maxY; y++) {
				for (int z = minZ; z <= maxZ; z++) {
					Location loc = new Location(world, x, y, z);
					loc.getBlock().setType(Material.AIR); // ✅ Vymazání všech bloků v aréně
				}
			}
		}
	}

	public boolean removeArena(String arenaName) {
		try {
			databaseManager.deleteArenaBlocks(arenaName); // ✅ Smaže bloky arény
			databaseManager.removeArena(arenaName); // ✅ Smaže samotnou arénu
			if (plugin.getConfig().getBoolean("debug")) {
				System.out.println("✅ Aréna '" + arenaName + "' byla úspěšně odstraněna.");
			}
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			if (plugin.getConfig().getBoolean("debug")) {
				System.out.println("❌ Chyba při odstraňování arény '" + arenaName + "': " + e.getMessage());
			}
			return false;
		}
	}

	public Location getArenaCenter(String arenaName) {
		Location boundary1 = databaseManager.getArenaBoundary1(arenaName);
		Location boundary2 = databaseManager.getArenaBoundary2(arenaName);

		if (boundary1 == null || boundary2 == null) return null;

		World world = boundary1.getWorld();
		double centerX = (boundary1.getX() + boundary2.getX()) / 2;
		double centerY = (boundary1.getY() + boundary2.getY()) / 2;
		double centerZ = (boundary1.getZ() + boundary2.getZ()) / 2;

		return new Location(world, centerX, centerY, centerZ);
	}


}
