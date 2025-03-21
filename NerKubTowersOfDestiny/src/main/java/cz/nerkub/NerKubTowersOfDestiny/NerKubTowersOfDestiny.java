package cz.nerkub.NerKubTowersOfDestiny;

import cz.nerkub.NerKubTowersOfDestiny.Commands.CommandHandler;
import cz.nerkub.NerKubTowersOfDestiny.Commands.TowersTabCompleter;
import cz.nerkub.NerKubTowersOfDestiny.CustomFiles.CustomConfig;
import cz.nerkub.NerKubTowersOfDestiny.Expansions.TowersOfDestinyPlaceholderExpansion;
import cz.nerkub.NerKubTowersOfDestiny.GUI.VoteGUI;
import cz.nerkub.NerKubTowersOfDestiny.Items.VoteItem;
import cz.nerkub.NerKubTowersOfDestiny.Listeners.*;
import cz.nerkub.NerKubTowersOfDestiny.Managers.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;


public final class NerKubTowersOfDestiny extends JavaPlugin {

	private CustomConfig messages;
	private CustomConfig modes;
	private CustomConfig votegui;

	private DatabaseManager databaseManager;
	private ArenaManager arenaManager;
	private GameManager gameManager;
	private VoteManager voteManager;
	private CommandHandler commandHandler;
	private PlayerMoveListener playerMoveListener;
	private EndLocationManager endLocationManager;
	private VoteItem voteItem;

	@Override
	public void onEnable() {
		saveDefaultConfig();
		messages = new CustomConfig("messages", "messages.yml", this);
		messages.saveConfig();
		modes = new CustomConfig("modes", "modes.yml", this);
		modes.saveConfig();
		votegui = new CustomConfig("gui", "votegui.yml", this);
		votegui.saveConfig();

		updateMainConfig();
		messages.updateConfig();
		modes.updateConfig();
		votegui.updateConfig();

		int pluginId = 25004;
		Metrics metrics = new Metrics(this, pluginId);

		Bukkit.getConsoleSender().sendMessage("");
		Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&3|\\   |  | /	&aPlugin: &6NerKub TowersOfDestiny"));
		Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&3| \\  |  |/	&aVersion: &bv1.0.2"));
		Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&3|  \\ |  |\\	&aAuthor: &3NerKub Studio"));
		Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&3|   \\|  | \\	&aPremium: &cThis plugin is not a premium resource."));
		Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', " "));
		Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&3| Visit our Discord for more! &ahttps://discord.gg/YXm26egK6g"));
		Bukkit.getConsoleSender().sendMessage("");

		endLocationManager = new EndLocationManager(this);
		databaseManager = new DatabaseManager(this);

		// 🛠 Inicializujeme ArenaManager (zatím bez GameManageru)
		arenaManager = new ArenaManager(this, databaseManager, gameManager);

		// 🛠 Nejdříve vytvoříme GameManager bez VoteManageru a VoteItemu
		gameManager = new GameManager(this, arenaManager, databaseManager, voteManager, voteItem);

		// 🛠 Teď inicializujeme VoteManager a VoteItem
		voteManager = new VoteManager(this, gameManager);
		voteItem = new VoteItem(this, voteManager);

		// 🛠 Teď, když máme VoteManager a VoteItem, aktualizujeme GameManager
		gameManager.setVoteManager(voteManager);
		gameManager.setVoteItem(voteItem);

		// 🛠 Teď můžeme aktualizovat ArenaManager, aby měl přístup k GameManageru
		arenaManager = new ArenaManager(this, databaseManager, gameManager);

		// Registrace PlaceholderAPI, pokud je povolena
		if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
			new TowersOfDestinyPlaceholderExpansion(this, databaseManager, gameManager).register();
			if (getConfig().getBoolean("debug")) {
				Bukkit.getLogger().info("PlaceholderAPI úspěšně registrována.");
			}
		} else {
			if (getConfig().getBoolean("debug")) {
				Bukkit.getLogger().warning("PlaceholderAPI není dostupná.");
			}
		}

		// 🛠 Registrace eventů
		getServer().getPluginManager().registerEvents(voteItem, this);
		getServer().getPluginManager().registerEvents(new VoteGUI(this, voteManager), this);
		getServer().getPluginManager().registerEvents(new PlayerDeathListener(this, gameManager), this);
		getServer().getPluginManager().registerEvents(new PlayerJoinListener(databaseManager), this);
		getServer().getPluginManager().registerEvents(new BlockPlaceListener(this, databaseManager, gameManager), this);

		commandHandler = new CommandHandler(this, databaseManager, arenaManager, gameManager);

		PluginCommand towersCommand = getCommand("towers");
		if (towersCommand != null) {
			towersCommand.setExecutor(commandHandler);
			towersCommand.setTabCompleter(new TowersTabCompleter(arenaManager));
		}

		this.playerMoveListener = new PlayerMoveListener(this, arenaManager, gameManager);
		getServer().getPluginManager().registerEvents(new EventListeners(this, commandHandler), this);
		getServer().getPluginManager().registerEvents(playerMoveListener, this);

		getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

		Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
				getMessages().getConfig().getString("prefix") + "&aTowers of Destiny successfully launched!"));
	}


	@Override
	public void onDisable() {
		String prefix = getConfig().getString("prefix");
		// ✅ Zavření databázového připojení při vypnutí
		if (databaseManager != null) {
			databaseManager.close();
			Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
					getMessages().getConfig().getString("database.close")));
		}

		Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
				getMessages().getConfig().getString("prefix") + "&cTowers of Destiny successfully turned off!"));

		saveConfig();
		getMessages().saveConfig();
		getModes().saveConfig();

	}

	public PlayerMoveListener getPlayerMoveListener() {
		return playerMoveListener;
	}

	public EndLocationManager getEndLocationManager() {
		return endLocationManager;
	}

	public void updateMainConfig() {
		getConfig().options().copyDefaults(true);
		saveConfig();
	}

	public CustomConfig getMessages() {
		return messages;
	}

	public CustomConfig getModes() {
		return modes;
	}

	public CustomConfig getVoteGui() {
		return votegui;
	}
}
