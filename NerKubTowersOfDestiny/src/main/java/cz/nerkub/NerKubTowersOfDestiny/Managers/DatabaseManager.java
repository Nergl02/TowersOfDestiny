package cz.nerkub.NerKubTowersOfDestiny.Managers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import cz.nerkub.NerKubTowersOfDestiny.NerKubTowersOfDestiny;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DatabaseManager {
	private final NerKubTowersOfDestiny plugin;
	private HikariDataSource dataSource;

	public DatabaseManager(NerKubTowersOfDestiny plugin) {
		this.plugin = plugin;
		String prefix = plugin.getMessages().getConfig().getString("prefix");
		FileConfiguration config = plugin.getConfig();
		String host = config.getString("database.host");
		String port = config.getString("database.port");
		String username = config.getString("database.username");
		String password = config.getString("database.password");
		String dbName = config.getString("database.db_name");


		String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + dbName + "?useSSL=false&autoReconnect=true";

		HikariConfig hikariConfig = new HikariConfig();
		hikariConfig.setJdbcUrl(jdbcUrl);
		hikariConfig.setUsername(username);
		hikariConfig.setPassword(password);
		hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
		hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
		hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

		dataSource = new HikariDataSource(hikariConfig);

		// ✅ Debug: Test připojení k databázi
		try (Connection conn = getConnection()) {
			System.out.println(ChatColor.translateAlternateColorCodes('&', prefix +
					plugin.getMessages().getConfig().getString("database.connect")));
		} catch (SQLException e) {
			System.out.println(ChatColor.translateAlternateColorCodes('&', prefix +
					plugin.getMessages().getConfig().getString("database.invalid-connection").replace("%error%", e.getMessage())));
			e.printStackTrace();
		}

		initializeDatabase();
	}

	private void initializeDatabase() {
		try (Connection conn = getConnection();
			 Statement stmt = conn.createStatement()) {

			// ✅ Tabulka ARENAS
			String createArenasTable = "CREATE TABLE IF NOT EXISTS arenas (" +
					"name VARCHAR(255) PRIMARY KEY," +
					"boundary1 VARCHAR(255)," +
					"boundary2 VARCHAR(255)," +
					"world_name VARCHAR(255) NOT NULL" + // ✅ Přidán svět arény
					")";
			stmt.execute(createArenasTable);

			// ✅ Tabulka SPAWNPOINTS
			String createSpawnPointsTable = "CREATE TABLE IF NOT EXISTS spawnpoints (" +
					"id INT AUTO_INCREMENT PRIMARY KEY," +
					"arena_name VARCHAR(255)," +
					"world_name VARCHAR(255)," +
					"x DOUBLE," +
					"y DOUBLE," +
					"z DOUBLE," +
					"yaw FLOAT," +
					"pitch FLOAT," +
					"FOREIGN KEY (arena_name) REFERENCES arenas(name)" +
					")";
			stmt.execute(createSpawnPointsTable);

			// ✅ Tabulka ARENA BLOCKS pro ukládání bloků k resetu
			String createArenaBlocksTable = "CREATE TABLE IF NOT EXISTS arena_blocks (" +
					"id INT AUTO_INCREMENT PRIMARY KEY," +
					"arena_name VARCHAR(255)," +
					"world_name VARCHAR(255)," +
					"x INT," +
					"y INT," +
					"z INT," +
					"material VARCHAR(255)," +
					"block_data TEXT," +  // ✅ Opravený sloupec pro block data
					"FOREIGN KEY (arena_name) REFERENCES arenas(name)" +
					")";
			stmt.execute(createArenaBlocksTable);

			String createPlayerStatsTable = "CREATE TABLE IF NOT EXISTS playerstats (" +
					"uuid VARCHAR(36) PRIMARY KEY," +
					"name VARCHAR(16) NOT NULL," +
					"kills INT DEFAULT 0," +
					"deaths INT DEFAULT 0," +
					"games_played INT DEFAULT 0," +
					"wins INT DEFAULT 0," +
					"losses INT DEFAULT 0," +
					"highest_killstreak INT DEFAULT 0" +
					")";
			stmt.execute(createPlayerStatsTable);

		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println("❌ Chyba při inicializaci databáze: " + e.getMessage());
		}
	}


	public Connection getConnection() throws SQLException {
		if (dataSource == null) {
			throw new SQLException("❌ Database connection pool is not initialized!");
		}
		return dataSource.getConnection();
	}


	public void createArena(String name, String boundary1, String boundary2, String worldName) throws SQLException {
		String sql = "INSERT INTO arenas (name, boundary1, boundary2, world_name) VALUES (?, ?, ?, ?)";
		try (Connection conn = getConnection();
			 PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, name);
			pstmt.setString(2, boundary1);
			pstmt.setString(3, boundary2);
			pstmt.setString(4, worldName); // ✅ Ukládáme správný svět
			pstmt.executeUpdate();
		}
	}

	public void addSpawnPoint(String arenaName, String worldName, double x, double y, double z, float yaw, float pitch) throws SQLException {
		String sql = "INSERT INTO spawnpoints (arena_name, world_name, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?)";
		try (Connection conn = getConnection();
			 PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, arenaName);
			pstmt.setString(2, worldName);
			pstmt.setDouble(3, x);
			pstmt.setDouble(4, y);
			pstmt.setDouble(5, z);
			pstmt.setFloat(6, yaw);
			pstmt.setFloat(7, pitch);
			pstmt.executeUpdate();
		}
	}


	public boolean removeArena(String arenaName) throws SQLException {
		try (Connection conn = getConnection()) {
			// ✅ Nejprve smažeme bloky uložené v `arena_blocks`
			String deleteBlocksSQL = "DELETE FROM arena_blocks WHERE arena_name = ?";
			try (PreparedStatement pstmt = conn.prepareStatement(deleteBlocksSQL)) {
				pstmt.setString(1, arenaName);
				pstmt.executeUpdate();
			}

			// ✅ Pak smažeme spawnpointy
			String deleteSpawnpointsSQL = "DELETE FROM spawnpoints WHERE arena_name = ?";
			try (PreparedStatement pstmt = conn.prepareStatement(deleteSpawnpointsSQL)) {
				pstmt.setString(1, arenaName);
				pstmt.executeUpdate();
			}

			// ✅ Nakonec smažeme samotnou arénu
			String deleteArenaSQL = "DELETE FROM arenas WHERE name = ?";
			try (PreparedStatement pstmt = conn.prepareStatement(deleteArenaSQL)) {
				pstmt.setString(1, arenaName);
				int affectedRows = pstmt.executeUpdate();
				return affectedRows > 0; // ✅ Vrací `true`, pokud byla aréna odstraněna
			}
		}
	}


	public List<String> getArenaSpawnPoints(String arenaName) {
		List<String> spawnPoints = new ArrayList<>();
		String sql = "SELECT location FROM spawnpoints WHERE arena_name = ?";

		try (Connection conn = getConnection();
			 PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, arenaName);
			try (ResultSet resultSet = pstmt.executeQuery()) {
				while (resultSet.next()) {
					spawnPoints.add(resultSet.getString("location"));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println("❌ Chyba při načítání spawnpointů: " + e.getMessage());
		}
		return spawnPoints;
	}

	public String getArenaWorld(String arenaName) {
		String sql = "SELECT world_name FROM arenas WHERE name = ?";
		try (Connection conn = getConnection();
			 PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, arenaName);
			try (ResultSet resultSet = pstmt.executeQuery()) {
				if (resultSet.next()) {
					String worldName = resultSet.getString("world_name"); // ✅ Správně načítáme svět
					if (plugin.getConfig().getBoolean("debug")) {
						System.out.println("🔍 Svět načten pro arénu '" + arenaName + "': " + worldName);
					}
					return worldName;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			if (plugin.getConfig().getBoolean("debug")) {
				System.out.println("❌ Chyba při načítání světa arény: " + e.getMessage());
			}
		}
		return "world"; // ✅ Výchozí hodnota jen jako záložní řešení!
	}


	public List<String> getAllArenas() {
		List<String> arenas = new ArrayList<>();
		String sql = "SELECT name FROM arenas";

		try (Connection conn = getConnection();
			 PreparedStatement pstmt = conn.prepareStatement(sql);
			 ResultSet resultSet = pstmt.executeQuery()) {

			while (resultSet.next()) {
				arenas.add(resultSet.getString("name"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
			if (plugin.getConfig().getBoolean("debug")) {
				System.out.println("❌ Chyba při načítání seznamu arén: " + e.getMessage());
			}
		}
		return arenas;
	}

	public List<SavedBlockData> getArenaBlocks(String arenaName) {
		List<SavedBlockData> blocks = new ArrayList<>();
		String sql = "SELECT world_name, x, y, z, material, block_data FROM arena_blocks WHERE arena_name = ?";

		try (Connection conn = getConnection();
			 PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, arenaName);
			try (ResultSet resultSet = pstmt.executeQuery()) {
				while (resultSet.next()) {
					String worldName = resultSet.getString("world_name");
					double x = resultSet.getDouble("x");
					double y = resultSet.getDouble("y");
					double z = resultSet.getDouble("z");
					String materialName = resultSet.getString("material");
					String blockData = resultSet.getString("block_data");

					Material material = Material.getMaterial(materialName);
					if (material == null) continue; // ✅ Ochrana proti neplatným blokům

					// ✅ Přidáváme chybějící `arenaName`
					blocks.add(new SavedBlockData(arenaName, worldName, x, y, z, material, blockData));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			if (plugin.getConfig().getBoolean("debug")) {
				System.out.println("❌ Chyba při načítání bloků pro arénu '" + arenaName + "': " + e.getMessage());
			}
		}
		return blocks;
	}


	public void deleteArenaBlocks(String arenaName) {
		String sql = "DELETE FROM arena_blocks WHERE arena_name = ?";
		try (Connection conn = getConnection();
			 PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, arenaName);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			if (plugin.getConfig().getBoolean("debug")) {
				System.out.println("❌ Chyba při mazání bloků arény: " + e.getMessage());
			}
		}
	}

	public Location getArenaBoundary1(String arenaName) {
		return getArenaBoundary(arenaName, "boundary1");
	}

	public Location getArenaBoundary2(String arenaName) {
		return getArenaBoundary(arenaName, "boundary2");
	}

	private Location getArenaBoundary(String arenaName, String boundaryColumn) {
		String sql = "SELECT " + boundaryColumn + " FROM arenas WHERE name = ?";
		try (Connection conn = getConnection();
			 PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, arenaName);
			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					String boundaryData = rs.getString(boundaryColumn);
					if (plugin.getConfig().getBoolean("debug")) {
						if (plugin.getConfig().getBoolean("debug")) {
							System.out.println("🔍 DEBUG: Načtená hranice pro " + boundaryColumn + ": " + boundaryData);
						}
					}
					return parseLocation(boundaryData);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			if (plugin.getConfig().getBoolean("debug")) {
				if (plugin.getConfig().getBoolean("debug")) {
					System.out.println("❌ Chyba při načítání hranice arény: " + e.getMessage());
				}
			}
		}
		return null;
	}


	private Location parseLocation(String locString) {
		if (locString == null || locString.isEmpty()) {
			if (plugin.getConfig().getBoolean("debug")) {
				if (plugin.getConfig().getBoolean("debug")) {
					System.out.println("❌ Chyba: Prázdná nebo neplatná souřadnice!");
				}
			}
			return null;
		}

		try {
			String[] parts = locString.split(", ");

			// ✅ Ověření, že máme alespoň 4 části (world, x, y, z)
			if (parts.length < 4) {
				if (plugin.getConfig().getBoolean("debug")) {
					System.out.println("❌ Chyba: Nesprávný formát souřadnic! (" + locString + ")");
				}
				return null;
			}

			World world = Bukkit.getWorld(parts[0]);
			double x = Double.parseDouble(parts[1]);
			double y = Double.parseDouble(parts[2]);
			double z = Double.parseDouble(parts[3]);

			if (world == null) {
				if (plugin.getConfig().getBoolean("debug")) {
					System.out.println("❌ Chyba: Svět '" + parts[0] + "' nebyl nalezen!");
				}
				return null;
			}

			return new Location(world, x, y, z);
		} catch (NumberFormatException e) {
			if (plugin.getConfig().getBoolean("debug")) {
				System.out.println("❌ Chyba: Špatný formát čísel v souřadnicích (" + locString + ")");
			}
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public void saveBlockBatch(List<SavedBlockData> blocks) {
		String sql = "INSERT INTO arena_blocks (arena_name, world_name, x, y, z, material, block_data) VALUES (?, ?, ?, ?, ?, ?, ?)";

		try (Connection conn = getConnection();
			 PreparedStatement pstmt = conn.prepareStatement(sql)) {

			for (SavedBlockData block : blocks) {
				pstmt.setString(1, block.getArenaName());
				pstmt.setString(2, block.getWorldName());
				pstmt.setInt(3, block.getX()); // ✅ Nyní správně převedeno na int
				pstmt.setInt(4, block.getY());
				pstmt.setInt(5, block.getZ());
				pstmt.setString(6, block.getMaterial().name());
				pstmt.setString(7, block.getBlockData());
				pstmt.addBatch(); // ✅ Přidáme do dávky
			}

			pstmt.executeBatch(); // ✅ Spustíme dávkový insert
		} catch (SQLException e) {
			e.printStackTrace();
			if (plugin.getConfig().getBoolean("debug")) {
			System.out.println("❌ Chyba při dávkovém ukládání bloků: " + e.getMessage());
			}
		}
	}

	public void createPlayerIfNotExists(UUID uuid, String playerName) {
		try (Connection conn = getConnection();
			 PreparedStatement stmt = conn.prepareStatement(
					 "INSERT INTO playerstats (uuid, name, kills, deaths, wins, games_played) " +
							 "VALUES (?, ?, 0, 0, 0, 0) ON DUPLICATE KEY UPDATE name = VALUES(name)"
			 )) {

			stmt.setString(1, uuid.toString());
			stmt.setString(2, playerName);
			stmt.executeUpdate();

			if (plugin.getConfig().getBoolean("debug")) {
				System.out.println("✅ Hráč " + playerName + " byl přidán do databáze.");
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public int getStat(UUID uuid, String column) {
		try (Connection conn = getConnection();
			 PreparedStatement stmt = conn.prepareStatement("SELECT " + column + " FROM playerstats WHERE uuid = ?")) {
			stmt.setString(1, uuid.toString());
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				return rs.getInt(column);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public void updateStat(UUID uuid, String column, int amount) {
		try (Connection conn = getConnection();
			 PreparedStatement stmt = conn.prepareStatement("UPDATE playerstats SET " + column + " = " + column + " + ? WHERE uuid = ?")) {
			stmt.setInt(1, amount);
			stmt.setString(2, uuid.toString());
			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void setStat(UUID uuid, String column, int value) {
		try (Connection conn = getConnection();
			 PreparedStatement stmt = conn.prepareStatement("UPDATE playerstats SET " + column + " = ? WHERE uuid = ?")) {
			stmt.setInt(1, value);
			stmt.setString(2, uuid.toString());
			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}


	public void close() {
		if (dataSource != null && !dataSource.isClosed()) {
			dataSource.close();
		}
	}
}
