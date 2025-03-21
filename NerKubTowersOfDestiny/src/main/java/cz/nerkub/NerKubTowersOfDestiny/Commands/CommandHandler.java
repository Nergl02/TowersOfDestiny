package cz.nerkub.NerKubTowersOfDestiny.Commands;

import cz.nerkub.NerKubTowersOfDestiny.Items.SetupTool;
import cz.nerkub.NerKubTowersOfDestiny.Managers.ArenaManager;
import cz.nerkub.NerKubTowersOfDestiny.Managers.DatabaseManager;
import cz.nerkub.NerKubTowersOfDestiny.Managers.GameManager;

import cz.nerkub.NerKubTowersOfDestiny.NerKubTowersOfDestiny;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandHandler implements CommandExecutor {
	private final NerKubTowersOfDestiny plugin;
	private final DatabaseManager databaseManager;
	private final ArenaManager arenaManager;
	private final GameManager gameManager;
	private final SetupTool setupTool;
	private boolean setupMode = false;

	// ✅ Správný konstruktor bez vnořené třídy GameManager
	public CommandHandler(NerKubTowersOfDestiny plugin, DatabaseManager databaseManager, ArenaManager arenaManager, GameManager gameManager) {
		this.plugin = plugin;
		this.databaseManager = databaseManager;
		this.arenaManager = arenaManager;
		this.gameManager = gameManager;
		this.setupTool = new SetupTool(plugin);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "❌ Only player can use this command!");
			return true;
		}

		Player player = (Player) sender;

		if (args.length == 0) {
			sendHelpMessage(player);
			return true;
		}

		switch (args[0].toLowerCase()) {
			case "setup":
				return toggleSetupMode(player);
			case "createarena":
				return handleCreateArenaCommand(player, args);
			case "addspawn":
				return handleAddSpawnCommand(player, args);
			case "join":
				return handleJoinGame(player, args);
			case "leave":
				return handleLeaveGame(player);
			case "removearena":
				return handleRemoveArenaCommand(player, args);
			case "forcestart":
				return handleForceStart(player, args);
			case "setendlocation":
				return handleSetEndLocation(player);
			case "reload":
				return handleReloadCommand(player);
			default:
				sendHelpMessage(player);
				return true;
		}
	}

	private void sendHelpMessage(Player player) {
		player.sendMessage(ChatColor.GOLD + "------ Towers Of Destiny Příkazy ------");
		player.sendMessage(ChatColor.YELLOW + "/towers setup" + ChatColor.GRAY + " - Aktivuje Setup Tool");
		player.sendMessage(ChatColor.YELLOW + "/towers createarena <name>" + ChatColor.GRAY + " - Vytvoří arénu");
		player.sendMessage(ChatColor.YELLOW + "/towers addspawn <arenaName>" + ChatColor.GRAY + " - Přidá spawn point");
		player.sendMessage(ChatColor.YELLOW + "/towers join <arenaName>" + ChatColor.GRAY + " - Připojí se do hry");
		player.sendMessage(ChatColor.YELLOW + "/towers leave" + ChatColor.GRAY + " - Odejít ze hry");
	}

	private boolean toggleSetupMode(Player player) {
		String prefix = plugin.getMessages().getConfig().getString("prefix");
		if (setupMode) {
			setupMode = false;
			setupTool.deactivate();
			player.getInventory().remove(setupTool.getItem());
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
					plugin.getMessages().getConfig().getString("setup-tool.deactivated")));
		} else {
			setupMode = true;
			setupTool.activate();
			player.getInventory().addItem(setupTool.getItem());
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
					plugin.getMessages().getConfig().getString("setup-tool.activated")));
		}
		return true;
	}

	private boolean handleCreateArenaCommand(Player player, String[] args) {
		String prefix = plugin.getMessages().getConfig().getString("prefix");
		if (args.length < 2) {
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
					plugin.getMessages().getConfig().getString("arena.usage-create")));
			return true;
		}

		if (setupTool.getBoundary1() == null || setupTool.getBoundary2() == null) {
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
					plugin.getMessages().getConfig().getString("arena.no-boundaries-set")));
			return true;
		}

		String name = args[1];
		arenaManager.createArena(name, setupTool.getBoundary1(), setupTool.getBoundary2());
		setupTool.resetBoundaries();
		player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
				plugin.getMessages().getConfig().getString("arena.created").replace("%arenaname%", name)));
		return true;
	}

	private boolean handleAddSpawnCommand(Player player, String[] args) {
		String prefix = plugin.getMessages().getConfig().getString("prefix");
		if (args.length < 2) {
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
					plugin.getMessages().getConfig().getString("arena.usage-add-spawn")));
			return true;
		}

		String arenaName = args[1];
		String worldName = player.getWorld().getName();
		Location location = player.getLocation(); // ✅ Použijeme Location objekt přímo

		arenaManager.addSpawnPoint(arenaName, worldName, location); // ✅ Předáme Location místo stringu

		player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
				plugin.getMessages().getConfig().getString("arena.spawn-added").replace("%arenaname%", arenaName)));

		return true;
	}


	private boolean handleJoinGame(Player player, String[] args) {
		String arenaName;
		String prefix = plugin.getMessages().getConfig().getString("prefix");

		if (args.length > 1) {
			arenaName = args[1];
		} else {
			arenaName = arenaManager.findBestArena();
		}

		if (arenaName == null) {
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
					plugin.getMessages().getConfig().getString("arena.no-available-arena")));
			return true;
		}

		// ✅ Kontrola, zda už je hráč v aréně
		if (gameManager.getPlayerArena(player) != null) {
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
					plugin.getMessages().getConfig().getString("game.already-joined")));
			return true;
		}

		boolean success = gameManager.joinGame(player, arenaName);
		if (success) {
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
					plugin.getMessages().getConfig().getString("game.join").replace("%arenaname%", arenaName)));
		}
		return true;
	}


	private boolean handleRemoveArenaCommand(Player player, String[] args) {
		String prefix = plugin.getMessages().getConfig().getString("prefix");
		if (args.length < 2) {
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
					plugin.getMessages().getConfig().getString("arena.usage-remove")));
			return true;
		}

		String arenaName = args[1];

		if (!arenaManager.getAllArenas().contains(arenaName)) {
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
					plugin.getMessages().getConfig().getString("arena.invalid-remove").replace("%arenaname%", arenaName)));
			return true;
		}

		boolean success = arenaManager.removeArena(arenaName);

		if (success) {
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
					plugin.getMessages().getConfig().getString("arena.removed").replace("%arenaname%", arenaName)));
		}

		return true;
	}

	private boolean handleForceStart(Player player, String[] args) {
		String prefix = plugin.getMessages().getConfig().getString("prefix");
		if (!player.hasPermission("towers.admin")) {
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
					plugin.getMessages().getConfig().getString("no-permission")));
			return true;
		}

		if (args.length < 2) {
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
					plugin.getMessages().getConfig().getString("arena.usage-force-start")));
			return true;
		}

		String arenaName = args[1];

		if (!gameManager.getActiveArenas().contains(arenaName)) {
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
					plugin.getMessages().getConfig().getString("arena.invalid-force-start").replace("%arenaname%", arenaName)));
			return true;
		}

		gameManager.startGame(arenaName, true); // ✅ Nucený start bez čekání a odpočtu
		player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
				plugin.getMessages().getConfig().getString("arena.force-start").replace("%arenaname%", arenaName)));
		return true;
	}

	private boolean handleSetEndLocation(Player player) {
		String prefix = plugin.getMessages().getConfig().getString("prefix");
		if (!player.hasPermission("towers.admin")) {
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
					plugin.getMessages().getConfig().getString("no-permission")));
			return true;
		}

		Location location = player.getLocation();
		plugin.getEndLocationManager().setEndLocation(location);

		player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
				plugin.getMessages().getConfig().getString("arena.set-end-lobby")
						.replace("%x%", String.valueOf(location.getBlockX()))
						.replace("%y%", String.valueOf(location.getBlockY()))
						.replace("%z%", String.valueOf(location.getBlockZ()))));

		return true;
	}

	private boolean handleReloadCommand(Player player) {
		String prefix = plugin.getMessages().getConfig().getString("prefix");
		if (!player.hasPermission("towers.admin")) {
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
					plugin.getMessages().getConfig().getString("no-permission")));
			return true;
		}

		long start = System.currentTimeMillis();

		plugin.reloadConfig(); // 🔄 Reload config.yml
		plugin.getMessages().reloadConfig(); // 🔄 Reload messages.yml
		plugin.getModes().reloadConfig(); // 🔄 Reload modes.yml
		plugin.getVoteGui().reloadConfig();

		long end = System.currentTimeMillis();
		long duration = end - start;

		player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
				plugin.getMessages().getConfig().getString("reload").replace("%duration%", String.valueOf(duration))));


		return true;
	}


	private boolean handleLeaveGame(Player player) {
		return gameManager.leaveGame(player);
	}

	public boolean isSetupToolActive() {
		return setupMode;
	}

	public void markBoundary(Location location) {
		setupTool.markBoundary(location); // ✅ Předáváme Location, ne String
	}

}
