package cz.nerkub.NerKubTowersOfDestiny.Commands;

import cz.nerkub.NerKubTowersOfDestiny.Managers.ArenaManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TowersTabCompleter implements TabCompleter {
	private final ArenaManager arenaManager;

	public TowersTabCompleter(ArenaManager arenaManager) {
		this.arenaManager = arenaManager;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		List<String> completions = new ArrayList<>();
		Set<String> arenas = arenaManager.getAllArenas();

		if (args.length == 1) {
			List<String> commands = List.of("setendlocation", "setup", "createarena", "addspawn", "join", "leave", "removearena", "forcestart", "reload");
			completions.addAll(filterByInput(commands, args[0]));
		} else if (args.length == 2) {
			if (args[0].equalsIgnoreCase("addspawn") ||
					args[0].equalsIgnoreCase("join") ||
					args[0].equalsIgnoreCase("removearena") ||
					args[0].equalsIgnoreCase("forcestart")) {
				completions.addAll(filterByInput(new ArrayList<>(arenas), args[1]));
			}
		}

		return completions;
	}

	// ✅ Pomocná metoda na filtraci podle vstupu
	private List<String> filterByInput(List<String> list, String input) {
		return list.stream()
				.filter(option -> option.toLowerCase().startsWith(input.toLowerCase()))
				.collect(Collectors.toList());
	}
}
