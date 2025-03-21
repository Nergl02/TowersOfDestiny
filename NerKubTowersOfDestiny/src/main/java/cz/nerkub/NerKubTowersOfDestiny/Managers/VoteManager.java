package cz.nerkub.NerKubTowersOfDestiny.Managers;

import cz.nerkub.NerKubTowersOfDestiny.GUI.VoteGUI;
import cz.nerkub.NerKubTowersOfDestiny.NerKubTowersOfDestiny;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;

public class VoteManager {
	private final NerKubTowersOfDestiny plugin;
	private final GameManager gameManager;
	private final Map<String, Map<String, Integer>> votes = new HashMap<>(); // Ukládá hlasy pro každou arénu
	private final List<String> gameModes = Arrays.asList("Rising Lava", "Player Swapper", "Item Shuffle");

	public VoteManager(NerKubTowersOfDestiny plugin, GameManager gameManager) {
		this.plugin = plugin;
		this.gameManager = gameManager;
	}

	public void openVoteMenu(Player player) {
		VoteGUI voteGUI = new VoteGUI(plugin, this); // ✅ Přidáme `plugin`
		voteGUI.open(player);
	}

	public void voteForMode(Player player, String arenaName, String mode) {
		String prefix = plugin.getMessages().getConfig().getString("prefix");
		mode = ChatColor.stripColor(mode); // ✅ Zbavíme se emoji a barvy

		votes.putIfAbsent(arenaName, new HashMap<>());
		Map<String, Integer> arenaVotes = votes.get(arenaName);
		if (plugin.getConfig().getBoolean("debug")) {
			System.out.println("📊 DEBUG: Před hlasováním: " + arenaVotes);
		}

		// ✅ Přidáme hlas
		arenaVotes.put(mode, arenaVotes.getOrDefault(mode, 0) + 1);

		if (plugin.getConfig().getBoolean("debug")) {
			System.out.println("📊 DEBUG: Po hlasování: " + arenaVotes);
		}
		player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
				plugin.getMessages().getConfig().getString("vote.made").replace("%mode%", mode)));
	}




	public String getWinningMode(String arenaName) {
		if (!votes.containsKey(arenaName) || votes.get(arenaName).isEmpty()) {
			if (plugin.getConfig().getBoolean("debug")) {
				System.out.println("⚠ DEBUG: Žádné hlasy pro arénu " + arenaName + ", výchozí mód.");
			}
			return "Default";
		}

		Map<String, Integer> arenaVotes = votes.get(arenaName);

		// 📌 Debug: Výpis aktuálních hlasů
		if (plugin.getConfig().getBoolean("debug")) {
			System.out.println("📊 DEBUG: Hlasy v aréně " + arenaName + ": " + arenaVotes);
		}

		// ✅ Najdeme mód s nejvyšším počtem hlasů
		String winningMode = arenaVotes.entrySet()
				.stream()
				.max(Map.Entry.comparingByValue())
				.map(Map.Entry::getKey)
				.orElse("Default");

		// ✅ Odstraníme emoji a barevné formátování
		winningMode = ChatColor.stripColor(winningMode);

		if (plugin.getConfig().getBoolean("debug")) {
			System.out.println("🏆 DEBUG: Vítězný mód po úpravě: " + winningMode);
		}
		return winningMode;
	}

	// ✅ Vrátí mód, pro který hráč hlasoval
	public String getPlayerVote(Player player, String arenaName) {
		if (!votes.containsKey(arenaName)) {
			return null;
		}

		for (Map.Entry<String, Integer> entry : votes.get(arenaName).entrySet()) {
			if (entry.getValue() > 0) {
				return entry.getKey();
			}
		}

		return null;
	}

	// ✅ Odebere hráči hlas z předchozího módu
	public void removeVote(Player player, String arenaName, String mode) {
		if (!votes.containsKey(arenaName)) {
			return;
		}

		Map<String, Integer> arenaVotes = votes.get(arenaName);
		if (arenaVotes.containsKey(mode)) {
			int currentVotes = arenaVotes.get(mode);
			if (currentVotes > 1) {
				arenaVotes.put(mode, currentVotes - 1);
			} else {
				arenaVotes.remove(mode);
			}
		}
	}


	public void resetVotes(String arenaName) {
		votes.remove(arenaName);
	}

	public String getArena(Player player) {
		return gameManager.getPlayerArena(player);
	}
}
