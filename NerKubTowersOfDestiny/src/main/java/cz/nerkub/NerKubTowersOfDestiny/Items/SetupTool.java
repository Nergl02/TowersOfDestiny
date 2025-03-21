package cz.nerkub.NerKubTowersOfDestiny.Items;

import cz.nerkub.NerKubTowersOfDestiny.NerKubTowersOfDestiny;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class SetupTool {

	private final NerKubTowersOfDestiny plugin;

	private boolean isActive;
	private Location boundary1;
	private Location boundary2;

	public SetupTool(NerKubTowersOfDestiny plugin) {
		this.plugin = plugin;
		this.isActive = false;
		this.boundary1 = null;
		this.boundary2 = null;
	}

	public void activate() {
		this.isActive = true;
	}

	public void deactivate() {
		this.isActive = false;
	}

	public boolean isActive() {
		return isActive;
	}

	// ✅ Označení hranice s Location
	public void markBoundary(Location location) {
		if (!isActive) {
			if (plugin.getConfig().getBoolean("debug")) {
				System.out.println("SetupTool není aktivní. Nejprve jej aktivujte.");
			}
			return;
		}

		if (boundary1 == null) {
			boundary1 = location;
			if (plugin.getConfig().getBoolean("debug")) {
				System.out.println("🔹 První hranice byla označena na pozici: " + formatLocation(boundary1));
			}
		} else if (boundary2 == null) {
			boundary2 = location;
			if (plugin.getConfig().getBoolean("debug")) {
				System.out.println("🔸 Druhá hranice byla označena na pozici: " + formatLocation(boundary2));
			}
		} else {
			if (plugin.getConfig().getBoolean("debug")) {
				System.out.println("⚠️ Obě hranice jsou již nastaveny.");
			}
		}
	}

	public Location getBoundary1() {
		return boundary1;
	}

	public Location getBoundary2() {
		return boundary2;
	}

	public void resetBoundaries() {
		boundary1 = null;
		boundary2 = null;
	}

	// ✅ Pomocná metoda pro formátování souřadnic
	private String formatLocation(Location loc) {
		return loc.getWorld().getName() + ", " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
	}

	// ✅ Vrací ItemStack (Setup Tool) pro hráče
	public ItemStack getItem() {
		ItemStack tool = new ItemStack(Material.GOLDEN_HOE);
		ItemMeta meta = tool.getItemMeta();
		if (meta != null) {
			meta.setDisplayName(ChatColor.GOLD + "Setup Tool");
			tool.setItemMeta(meta);
		}
		return tool;
	}
}
