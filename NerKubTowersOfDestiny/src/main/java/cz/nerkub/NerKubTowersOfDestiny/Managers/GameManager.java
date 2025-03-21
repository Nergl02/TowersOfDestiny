package cz.nerkub.NerKubTowersOfDestiny.Managers;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import cz.nerkub.NerKubTowersOfDestiny.GameModes.GameModeBase;
import cz.nerkub.NerKubTowersOfDestiny.GameModes.ItemShuffleMode;
import cz.nerkub.NerKubTowersOfDestiny.GameModes.PlayerSwapperMode;
import cz.nerkub.NerKubTowersOfDestiny.GameModes.RisingLavaMode;
import cz.nerkub.NerKubTowersOfDestiny.Items.VoteItem;
import cz.nerkub.NerKubTowersOfDestiny.NerKubTowersOfDestiny;
import cz.nerkub.NerKubTowersOfDestiny.Utils.RandomItemGenerator;
import org.bukkit.*;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;


import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class GameManager {
	private final NerKubTowersOfDestiny plugin;
	private final ArenaManager arenaManager;
	private final DatabaseManager databaseManager;
	private VoteItem voteItem;
	private VoteManager voteManager;
	private final Map<String, List<Player>> activeArenas; // ✅ Uloží hráče v každé aréně
	private final Map<String, GameState> arenaStates; // ✅ Uloží stav každé arény
	private final RandomItemGenerator randomItemGenerator;
	private final Map<String, BukkitTask> itemTasks = new HashMap<>();
	private final Map<String, BukkitTask> countdownTasks = new HashMap<>();
	private final Map<String, List<Player>> spectators = new HashMap<>();
	private final Map<String, Integer> countdownTimers = new HashMap<>();
	private final Map<String, GameModeBase> gameModes = new HashMap<>();


	public GameManager(NerKubTowersOfDestiny plugin, ArenaManager arenaManager, DatabaseManager databaseManager, VoteManager voteManager, VoteItem voteItem) {
		this.plugin = plugin;
		this.arenaManager = arenaManager;
		this.databaseManager = databaseManager;
		this.voteManager = voteManager;
		this.randomItemGenerator = new RandomItemGenerator();
		this.activeArenas = new HashMap<>();
		this.arenaStates = new HashMap<>();
		this.voteItem = voteItem;

		// ✅ Přidání herních módů do mapy
		gameModes.put("Rising Lava", new RisingLavaMode(plugin, arenaManager, databaseManager));
		gameModes.put("Player Swapper", new PlayerSwapperMode(plugin, arenaManager, this));
		gameModes.put("Item Shuffle", new ItemShuffleMode(plugin, this));

		if (plugin.getConfig().getBoolean("debug")) {
			System.out.println("📜 DEBUG: Herní módy byly načteny: " + gameModes.keySet());
		}

	}

	// ✅ Metoda pro aktivaci arény
	public void activateArena(String arenaName) {
		if (!activeArenas.containsKey(arenaName)) {
			activeArenas.put(arenaName, new ArrayList<>());
			arenaStates.put(arenaName, GameState.WAITING); // ✅ Nastaví stav na WAITING
			if (plugin.getConfig().getBoolean("debug")) {
				System.out.println("✅ Aréna '" + arenaName + "' byla aktivována.");
			}
		}
	}

	public boolean joinGame(Player player, String arenaName) {
		String prefix = plugin.getMessages().getConfig().getString("prefix");
		if (arenaManager == null) {
			player.sendMessage(ChatColor.RED + "❌ Chyba: ArenaManager není inicializován!");
			return false;
		}

		// ✅ Pokud je hráč už v této aréně, ukončíme metodu a nic dalšího nevypisujeme
		if (activeArenas.containsKey(arenaName) && activeArenas.get(arenaName).contains(player)) {
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
					plugin.getMessages().getConfig().getString("game.already-joined")));
			return false;
		}

		activeArenas.putIfAbsent(arenaName, new ArrayList<>());
		arenaStates.putIfAbsent(arenaName, GameState.WAITING);

		if (arenaStates.get(arenaName) != GameState.WAITING && arenaStates.get(arenaName) != GameState.COUNTDOWN) {
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
					plugin.getMessages().getConfig().getString("game.in-game")));
			return false;
		}

		List<Location> spawnPoints = arenaManager.getSpawnPoints(arenaName);
		if (spawnPoints.isEmpty()) {
			player.sendMessage(ChatColor.RED + "❌ Tato aréna nemá nastavené spawnpointy!");
			return false;
		}

		int maxPlayers = spawnPoints.size();
		int minPlayers = getMinPlayers(arenaName);
		List<Player> players = activeArenas.get(arenaName);

		if (players.size() >= maxPlayers) {
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
					plugin.getMessages().getConfig().getString("game.full-arena")));
			return false;
		}

		Location spawnLocation = spawnPoints.get(players.size());
		player.teleport(spawnLocation);
		players.add(player);

		plugin.getPlayerMoveListener().freezePlayer(player);
		voteItem.giveVoteItem(player);

		for (Player p : players) {
			p.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
					plugin.getMessages().getConfig().getString("game.join-all").replace("%player%", player.getName()).replace("%current_players%", String.valueOf(players.size())).replace("%max_players%", String.valueOf(maxPlayers))));
		}

		// ✅ Pokud už odpočet běží, informujeme hráče o zbývajícím čase
		if (arenaStates.get(arenaName) == GameState.COUNTDOWN) {
			int remainingTime = countdownTimers.getOrDefault(arenaName, 10); // Výchozí hodnota 10 sekund
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
					plugin.getMessages().getConfig().getString("game.countdown-already-running").replace("%countdown%", String.valueOf(remainingTime))));
			return true;
		}

		// ✅ Pokud odpočet ještě neběží a je dost hráčů, spustíme ho
		if (players.size() >= minPlayers) {
			startCountdown(arenaName);
		}

		return true;
	}


	public boolean leaveGame(Player player) {
		String prefix = plugin.getMessages().getConfig().getString("prefix");
		boolean useBungee = plugin.getConfig().getBoolean("use_bungeecord", false);
		String bungeeServer = plugin.getConfig().getString("bungee_server", "Hub");

		for (String arena : activeArenas.keySet()) {
			List<Player> players = activeArenas.get(arena);
			if (players.remove(player)) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
						plugin.getMessages().getConfig().getString("game.leave").replace("%arenaname%", arena)));
				// ✅ Odemknout hráče, aby se mohl hýbat!
				plugin.getPlayerMoveListener().unfreezePlayer(player);

				// ✅ Informujeme ostatní hráče
				for (Player p : players) {
					p.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
							plugin.getMessages().getConfig().getString("game.leave-all")
									.replace("%player%", player.getName())
									.replace("%current_players%", String.valueOf(players.size()))
									.replace("%max_players%", String.valueOf(getMaxPlayers(arena)))));
				}

				// ✅ Portujeme hráče na endLocation nebo Bungee server
				if (useBungee) {
					sendToBungeeServer(player, bungeeServer, true);
				} else {
					player.teleport(plugin.getEndLocationManager().getEndLocation());
				}

				VoteItem.removeVoteItem(player);

				// ✅ Zrušit countdown, pokud je hráčů méně než minimum
				int minPlayers = getMinPlayers(arena);
				if (players.size() < minPlayers) {
					cancelCountdown(arena);
				}

				return true;
			}
		}
		return false;
	}


	// ✅ Přidáváme metodu pro získání seznamu aktivních arén
	public Set<String> getActiveArenas() {
		return activeArenas.keySet();
	}

	// ✅ Získání počtu hráčů v aréně
	public int getPlayerCount(String arena) {
		return activeArenas.getOrDefault(arena, new ArrayList<>()).size();
	}

	public int getMaxPlayers(String arena) {
		return arenaManager.getSpawnPoints(arena).size();
	}

	public int getMinPlayers(String arena) {
		return Math.max(2, (int) Math.ceil(getMaxPlayers(arena) * 0.5)); // ✅ Min. 2 hráči nebo 50% míst
	}


	// ✅ Získání stavu hry
	public GameState getGameState(String arena) {
		return arenaStates.getOrDefault(arena, GameState.WAITING);
	}

	private Location parseLocation(String locString) {
		try {
			String[] parts = locString.split(", ");
			World world = Bukkit.getWorld(parts[0]); // ✅ Svět musí být uložený v databázi
			double x = Double.parseDouble(parts[1]);
			double y = Double.parseDouble(parts[2]);
			double z = Double.parseDouble(parts[3]);

			if (world == null) {
				System.out.println("❌ Svět " + parts[0] + " nebyl nalezen!");
				return null;
			}

			return new Location(world, x, y, z);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public void startCountdown(String arenaName) {
		String prefix = plugin.getMessages().getConfig().getString("prefix");
		if (arenaStates.get(arenaName) != GameState.WAITING) return;

		// ❗ Kontrola, zda už nějaký odpočet neběží
		if (countdownTasks.containsKey(arenaName)) {
			if (plugin.getConfig().getBoolean("debug")) {
				System.out.println("⏳ [DEBUG] Odpočet už běží pro arénu: " + arenaName);
			}
			return;
		}
		if (plugin.getConfig().getBoolean("debug")) {
			System.out.println("🚀 [DEBUG] Odpočet spuštěn pro arénu: " + arenaName);
		}

		BukkitTask task = new BukkitRunnable() {
			int countdown = 10;

			@Override
			public void run() {
				if (plugin.getConfig().getBoolean("debug")) {
					System.out.println("⏳ [DEBUG] Odpočet: " + countdown + " sekund pro arénu: " + arenaName);
				}

				if (countdown <= 0) {
					if (plugin.getConfig().getBoolean("debug")) {
						System.out.println("🏁 [DEBUG] Odpočet dokončen, hra startuje pro arénu: " + arenaName);
					}
					startGame(arenaName, false);
					countdownTasks.remove(arenaName); // ✅ Po startu hry odstraníme task
					cancel();
					return;
				}

				// ✅ Kontrola, zda počet hráčů neklesl pod minimum
				int minPlayers = getMinPlayers(arenaName);
				int currentPlayers = activeArenas.get(arenaName).size();
				if (currentPlayers < minPlayers) {
					if (plugin.getConfig().getBoolean("debug")) {
						System.out.println("❌ [DEBUG] Odpočet zrušen, málo hráčů (" + currentPlayers + "/" + minPlayers + ")");
					}
					cancelCountdown(arenaName);
					for (Player player : activeArenas.get(arenaName)) {
						player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
								plugin.getMessages().getConfig().getString("game.not-enough-players-player-quit")));
					}
					return;
				}

				// ✅ Odeslat zprávu hráčům v aréně
				for (Player player : activeArenas.get(arenaName)) {
					player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
							plugin.getMessages().getConfig().getString("game.countdown").replace("%countdown%", String.valueOf(countdown))));
					player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
				}

				countdown--;
			}
		}.runTaskTimer(plugin, 0L, 20L);

		countdownTasks.put(arenaName, task); // ✅ Uložíme task do mapy
	}


	public void startGame(String arenaName, boolean forceStart) {
		String prefix = plugin.getMessages().getConfig().getString("prefix");

		// Kontrola, zda aréna vůbec existuje v `arenaStates`
		if (!arenaStates.containsKey(arenaName)) {
			return;
		}


		// Kontrola, zda je aréna ve stavu WAITING
		if (arenaStates.get(arenaName) != GameState.WAITING) {
			return;
		}

		List<Player> players = activeArenas.get(arenaName);
		List<Location> spawnPoints = arenaManager.getSpawnPoints(arenaName);
		int maxPlayers = spawnPoints.size();

		String selectedMode = voteManager.getWinningMode(arenaName);

		for (Player player : players) {
			VoteItem.removeVoteItem(player);
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
					plugin.getMessages().getConfig().getString("game.mode-start").replace("%arenaname%", arenaName).replace("%mode%", selectedMode)));
			databaseManager.updateStat(player.getUniqueId(), "games_played", 1);
		}

		applyGameMode(arenaName, selectedMode);

		voteManager.resetVotes(arenaName);
		if (plugin.getConfig().getBoolean("debug")) {
			System.out.println("🔄 DEBUG: Hlasy pro arénu " + arenaName + " byly resetovány.");
		}

		double requiredPercentage = plugin.getConfig().getDouble("game.required_player_percentage", 50.0) / 100.0;
		int minPlayers = (int) Math.ceil(maxPlayers * requiredPercentage);
		minPlayers = Math.max(minPlayers, 2);

		if (!forceStart && players.size() < minPlayers) {
			for (Player player : players) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
						plugin.getMessages().getConfig().getString("game.not-enough-players").replace("%min_players%", String.valueOf(minPlayers))));
			}
			return;
		}

		// ✅ Nastavení hry jako "RUNNING"
		arenaStates.put(arenaName, GameState.RUNNING);

		plugin.getPlayerMoveListener().unfreezePlayers();

		// ✅ Informování hráčů o startu
		for (Player player : players) {
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
					plugin.getMessages().getConfig().getString("game.start")));
			player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
		}

		// ✅ Odemknutí pohybu ihned po odpočtu
		plugin.getPlayerMoveListener().unfreezePlayers();

		// ✅ Interval generování itemů z config.yml
		int itemInterval = plugin.getConfig().getInt("game.item_spawn_interval", 3) * 20; // Převod sekund na ticky

		// ✅ Zahájení cyklického rozdávání náhodných itemů
		// ✅ Pokud běží Item Shuffle, tak **nedávat itemy**
		if (!selectedMode.equalsIgnoreCase("Item Shuffle")) {
			BukkitTask itemTask = new BukkitRunnable() {
				@Override
				public void run() {
					if (arenaStates.get(arenaName) != GameState.RUNNING) {
						cancel();
						return;
					}

					// ✅ Projdeme všechny hráče v aréně a dáme jim item
					for (Player player : activeArenas.get(arenaName)) {
						// ✅ Pokud je hráč ve spectatormódu, nedáme mu item
						if (spectators.containsKey(arenaName) && spectators.get(arenaName).contains(player)) {
							continue;
						}

						ItemStack randomItem = randomItemGenerator.generateRandomItem();
						if (randomItem != null) {
							player.getInventory().addItem(randomItem);
						}
					}
				}
			}.runTaskTimer(plugin, 0L, itemInterval); // Každých X sekund podle configu

			// ✅ Uložíme tento úkol do mapy `itemTasks`
			itemTasks.put(arenaName, itemTask);
		}
	}


	public void endGame(String arenaName, Player winner) {
		String prefix = plugin.getMessages().getConfig().getString("prefix");
		List<Player> players = activeArenas.get(arenaName);
		List<Player> specPlayers = spectators.getOrDefault(arenaName, new ArrayList<>());
		boolean useBungee = plugin.getConfig().getBoolean("use_bungeecord", false);
		String bungeeServer = plugin.getConfig().getString("bungee_server", "Hub");

		voteManager.resetVotes(arenaName);

		if (winner != null) {
			// ✅ Správně přičíst pouze jednu výhru vítězi
			databaseManager.updateStat(winner.getUniqueId(), "wins", 1);

			for (Player player : players) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
						plugin.getMessages().getConfig().getString("game.winner-all")
								.replace("%player%", winner.getName())
								.replace("%arenaname%", arenaName)));
			}
		} else {
			for (Player player : players) {
				player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
						plugin.getMessages().getConfig().getString("game.no-winner")
								.replace("%arenaname%", arenaName)));
			}
		}

		// ✅ Přičtení proher všem hráčům kromě vítěze
		for (Player player : players) {
			if (winner == null) {
				// ✅ Pokud není vítěz, všem hráčům se přičte prohra
				databaseManager.updateStat(player.getUniqueId(), "losses", 1);
				System.out.println("❌ Přičítám prohru hráči: " + player.getName());
			}
		}


		// ✅ Po 3 sekundách teleport všech hráčů na end location
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			for (Player player : players) {
				if (useBungee) {
					sendToBungeeServer(player, bungeeServer, false);
				} else {
					player.teleport(plugin.getEndLocationManager().getEndLocation());
					player.setGameMode(GameMode.SURVIVAL);
				}
			}

			// ✅ Teleport všech spectatorů
			for (Player spectator : specPlayers) {
				if (useBungee) {
					sendToBungeeServer(spectator, bungeeServer, false);
				} else {
					spectator.teleport(plugin.getEndLocationManager().getEndLocation());
					spectator.setGameMode(GameMode.SURVIVAL);
				}
			}

			// ✅ Vyčistíme seznam spectatorů
			activeArenas.remove(arenaName);
			spectators.remove(arenaName);

			// ✅ Zastavení Rising Lava módu
			if (gameModes.get("Rising Lava") instanceof RisingLavaMode lavaMode) {
				lavaMode.stopMode(arenaName);
			}

			// ✅ Resetujeme arénu
			arenaManager.resetArena(arenaName);
			arenaStates.put(arenaName, GameState.WAITING);

		}, plugin.getConfig().getInt("game.celebration_time") * 20L);
	}


	public String getCurrentMode(String arenaName) {
		GameModeBase mode = gameModes.get(voteManager.getWinningMode(arenaName));
		return mode != null ? mode.getClass().getSimpleName().replace("Mode", "") : "Default";
	}


	/**
	 * 🎆 Oslavy vítěze (ohňostroj, částice, titulky)
	 */
	private void celebrateWinner(String arenaName, Player winner, Location arenaCenter) {
		if (winner == null || arenaCenter == null) {
			if (plugin.getConfig().getBoolean("debug")) {
				System.out.println("❌ DEBUG: Oslava vítěze nebyla spuštěna, winner nebo arenaCenter je null!");
			}
			return;
		}

		if (plugin.getConfig().getBoolean("debug")) {
			System.out.println("🎆 DEBUG: Spouštíme oslavy pro vítěze " + winner.getName());
		}

		winner.getInventory().clear();

		// ✅ Přidáme vítěze do spectator seznamu
		spectators.putIfAbsent(arenaName, new ArrayList<>());
		spectators.get(arenaName).add(winner);

		// ✅ Zvuk pro vítěze
		winner.playSound(winner.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
		winner.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, plugin.getConfig().getInt("game.celebration_time"), 1, false, false));
		winner.setNoDamageTicks(60);
		winner.getInventory().clear();

		// ✅ Spustíme ohňostroje
		for (int i = 0; i < plugin.getConfig().getInt("game.celebration_time"); i++) {
			Bukkit.getScheduler().runTaskLater(plugin, () -> {
				if (plugin.getConfig().getBoolean("debug")) {
					System.out.println("🎆 DEBUG: Spouštím ohňostroj nad hráčem " + winner.getName());
				}
				spawnFireworks(winner.getLocation().add(0, 5, 0));
			}, i * 20L);
		}

		// ✅ Titulek pro všechny hráče
		for (Player player : Bukkit.getOnlinePlayers()) {
			player.sendTitle(ChatColor.translateAlternateColorCodes('&',
					plugin.getMessages().getConfig().getString("game.winner-title").replace("%player%", winner.getName())),
					ChatColor.translateAlternateColorCodes('&',
							plugin.getMessages().getConfig().getString("game.winner-subtitle")), 10, 60, 10);
		}

		// ✅ Po 3 sekundách teleport všech hráčů a ukončení hry
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			if (plugin.getConfig().getBoolean("debug")) {
				System.out.println("🏆 DEBUG: Konec oslav, ukončujeme hru a teleportujeme hráče.");
			}
			winner.removePotionEffect(PotionEffectType.GLOWING);
			activeArenas.getOrDefault(arenaName, new ArrayList<>()).remove(winner);
			endGame(arenaName, winner);
		}, plugin.getConfig().getInt("game.celebration_time") * 20L);
	}


	private void spawnFireworks(Location location) {
		Bukkit.getScheduler().runTask(plugin, () -> {
			Firework firework = (Firework) location.getWorld().spawnEntity(location, EntityType.FIREWORK_ROCKET);
			FireworkMeta meta = firework.getFireworkMeta();

			// ✅ Náhodná barva ohňostroje
			FireworkEffect effect = FireworkEffect.builder()
					.withColor(Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.PURPLE, Color.ORANGE)
					.with(FireworkEffect.Type.BALL_LARGE)
					.withFlicker()
					.withTrail()
					.build();

			meta.addEffect(effect);
			meta.setPower(0); // ✅ Síla 0 = okamžitý výbuch
			firework.setFireworkMeta(meta);

			// ✅ Nastavení ohňostroje, aby neublížil vítězi
			firework.setInvulnerable(true);

			// ✅ Okamžitě odpálit ohňostroj po 1 ticku
			Bukkit.getScheduler().runTaskLater(plugin, firework::detonate, 1L);
		});
	}


	public void eliminatePlayer(Player player) {
		String prefix = plugin.getMessages().getConfig().getString("prefix");
		String arenaName = getPlayerArena(player);
		if (arenaName == null) return;

		List<Player> players = activeArenas.get(arenaName);
		if (!players.contains(player)) return;

		players.remove(player);
		player.getInventory().clear();

		// ✅ Přidání do spectator seznamu
		spectators.putIfAbsent(arenaName, new ArrayList<>());
		spectators.get(arenaName).add(player);

		// ✅ Přepnutí do spectator módu
		player.setGameMode(GameMode.SPECTATOR);

		// ✅ Zvýšení počtu smrtí
		databaseManager.updateStat(player.getUniqueId(), "deaths", 1);
		databaseManager.updateStat(player.getUniqueId(), "losses", 1);

		// ✅ Získání posledního útočníka (pokud existuje)
		Player killer = getLastDamager(player);
		String killerName = (killer != null) ? killer.getName() : ChatColor.translateAlternateColorCodes('&', prefix +
				plugin.getMessages().getConfig().getString("game.self-elimination").replace("%player%", player.getName()));

		if (killer != null) {
			databaseManager.updateStat(killer.getUniqueId(), "kills", 1);

			// ✅ Killstreak kontrola
			int currentKillstreak = databaseManager.getStat(killer.getUniqueId(), "highest_killstreak") + 1;
			databaseManager.setStat(killer.getUniqueId(), "highest_killstreak", Math.max(currentKillstreak, databaseManager.getStat(killer.getUniqueId(), "highest_killstreak")));
		}

		// ✅ Výběr náhodné eliminační zprávy
		List<String> messages = plugin.getMessages().getConfig().getConfigurationSection("game.elimination-messages")
				.getValues(false).values().stream().map(Object::toString).collect(Collectors.toList());
		String message = messages.isEmpty() ? "%victim% byl vyřazen!" : messages.get(new Random().nextInt(messages.size()));

		// ✅ Nahrazení placeholderů
		message = message.replace("%victim%", player.getName()).replace("%killer%", killerName);

		// ✅ Oznámení všem hráčům v aréně
		for (Player p : players) {
			p.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + message));
		}

		// ✅ Teleport do středu arény
		Location center = arenaManager.getArenaCenter(arenaName);
		if (center != null) {
			player.teleport(center);
		}

		checkForLastPlayer(arenaName);
	}

	// ✅ Metoda pro získání posledního útočníka
	private Player getLastDamager(Player player) {
		if (player.getLastDamageCause() instanceof EntityDamageByEntityEvent) {
			EntityDamageByEntityEvent event = (EntityDamageByEntityEvent) player.getLastDamageCause();
			if (event.getDamager() instanceof Player) {
				return (Player) event.getDamager();
			}
		}
		return null;
	}



	public String getPlayerArena(Player player) {
		for (Map.Entry<String, List<Player>> entry : activeArenas.entrySet()) {
			if (entry.getValue().contains(player)) {
				return entry.getKey();
			}
		}
		return null;
	}

	public List<Player> getPlayersInArena(String arenaName) {
		return activeArenas.getOrDefault(arenaName, new ArrayList<>());
	}


	public void cancelCountdown(String arenaName) {
		if (countdownTasks.containsKey(arenaName)) {
			if (plugin.getConfig().getBoolean("debug")) {
				System.out.println("⏹ [DEBUG] Odpočet zrušen pro arénu: " + arenaName);
			}
			countdownTasks.get(arenaName).cancel();
			countdownTasks.remove(arenaName);
		}
		arenaStates.put(arenaName, GameState.WAITING);
	}


	private void sendToBungeeServer(Player player, String server, boolean autoJoin) {
		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		out.writeUTF("Connect");
		out.writeUTF(server);
		player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());

		// ✅ Pošleme extra zprávu pro automatické připojení do arény
		if (autoJoin) {
			Bukkit.getScheduler().runTaskLater(plugin, () -> {
				ByteArrayDataOutput out2 = ByteStreams.newDataOutput();
				out2.writeUTF("Forward");
				out2.writeUTF(server);
				out2.writeUTF("TowersJoin");

				ByteArrayOutputStream msgBytes = new ByteArrayOutputStream();
				DataOutputStream msgOut = new DataOutputStream(msgBytes);
				try {
					msgOut.writeUTF(player.getUniqueId().toString());
				} catch (IOException e) {
					e.printStackTrace();
				}

				out2.writeShort(msgBytes.toByteArray().length);
				out2.write(msgBytes.toByteArray());
				player.sendPluginMessage(plugin, "BungeeCord", out2.toByteArray());
			}, 40L); // ✅ Po 2 sekundách, aby se hráč stihl připojit
		}
	}

	public void checkForLastPlayer(String arenaName) {
		List<Player> players = activeArenas.getOrDefault(arenaName, new ArrayList<>());

		// ✅ Filtrujeme pouze hráče, kteří NEJSOU ve spectator módu
		List<Player> activePlayers = new ArrayList<>();
		for (Player player : players) {
			if (!spectators.getOrDefault(arenaName, new ArrayList<>()).contains(player)) {
				activePlayers.add(player);
			}
		}

		if (plugin.getConfig().getBoolean("debug")) {
			System.out.println("🔍 DEBUG: Po eliminaci zbývá v aréně '" + arenaName + "' aktivních hráčů: " + activePlayers.size());
		}

		if (activePlayers.size() == 1) {
			Player winner = activePlayers.get(0);
			if (plugin.getConfig().getBoolean("debug")) {
				System.out.println("🏆 DEBUG: Oslavujeme vítěze " + winner.getName() + " v aréně " + arenaName);
			}
			celebrateWinner(arenaName, winner, arenaManager.getArenaCenter(arenaName));

		} else if (activePlayers.isEmpty()) {
			if (plugin.getConfig().getBoolean("debug")) {
				System.out.println("❌ DEBUG: Aréna " + arenaName + " je prázdná, končíme hru bez vítěze.");
			}
			endGame(arenaName, null);
		}
	}

	public void setVoteManager(VoteManager voteManager) {
		this.voteManager = voteManager;
	}

	public void setVoteItem(VoteItem voteItem) {
		this.voteItem = voteItem;
	}

	public void applyGameMode(String arenaName, String mode) {
		mode = ChatColor.stripColor(mode); // ✅ Odstraníme emoji a barvy

		if (!gameModes.containsKey(mode)) {
			if (plugin.getConfig().getBoolean("debug")) {
				System.out.println("⚠ Chyba: Neznámý herní mód: " + mode);
			}
			return;
		}

		if (plugin.getConfig().getBoolean("debug")) {
			System.out.println("🚀 DEBUG: Spouštím mód: " + mode);
		}
		gameModes.get(mode).startMode(arenaName);
	}

	public NerKubTowersOfDestiny getPlugin() {
		return plugin;
	}

}
