package cz.nerkub.NerKubTowersOfDestiny.Listeners;

import cz.nerkub.NerKubTowersOfDestiny.Commands.CommandHandler;
import cz.nerkub.NerKubTowersOfDestiny.NerKubTowersOfDestiny;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class EventListeners implements Listener {

	private final NerKubTowersOfDestiny plugin;
	private final CommandHandler commandHandler;

	public EventListeners(NerKubTowersOfDestiny plugin, CommandHandler commandHandler) {
		this.plugin = plugin;
		this.commandHandler = commandHandler;
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		// ✅ Zabráníme dvojímu vyvolání eventu pro offhand
		if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) {
			return;
		}

		String prefix = plugin.getMessages().getConfig().getString("prefix");
		Player player = event.getPlayer();
		Action action = event.getAction();
		Block clickedBlock = event.getClickedBlock();
		ItemStack itemInHand = player.getInventory().getItemInMainHand();

		if (!commandHandler.isSetupToolActive() || itemInHand.getType() != Material.GOLDEN_HOE) {
			return;
		}

		if (clickedBlock == null) {
			return;
		}

		Location location = clickedBlock.getLocation();
		String formattedLocation = location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();

		if (action == Action.LEFT_CLICK_BLOCK) {
			commandHandler.markBoundary(location);
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
					plugin.getMessages().getConfig().getString("setup-tool.first-boundary").replace("%location%", formattedLocation)));
			event.setCancelled(true);
		}

		if (action == Action.RIGHT_CLICK_BLOCK) {
			commandHandler.markBoundary(location);
			player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix +
					plugin.getMessages().getConfig().getString("setup-tool.second-boundary").replace("%location%", formattedLocation)));
			event.setCancelled(true);
		}
	}

}
