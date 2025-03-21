package cz.nerkub.NerKubTowersOfDestiny.Managers;

import cz.nerkub.NerKubTowersOfDestiny.NerKubTowersOfDestiny;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

public class EndLocationManager implements Listener {
	private final NerKubTowersOfDestiny plugin;
	private Location endLocation;

	public EndLocationManager(NerKubTowersOfDestiny plugin) {
		this.plugin = plugin;
		this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	public void loadEndLocation() {
		if (plugin.getConfig().contains("end_location")) {
			String worldName = plugin.getConfig().getString("end_location.world");
			double x = plugin.getConfig().getDouble("end_location.x");
			double y = plugin.getConfig().getDouble("end_location.y");
			double z = plugin.getConfig().getDouble("end_location.z");
			float yaw = (float) plugin.getConfig().getDouble("end_location.yaw");
			float pitch = (float) plugin.getConfig().getDouble("end_location.pitch");

			World world = Bukkit.getWorld(worldName);
			if (world != null) {
				endLocation = new Location(world, x, y, z, yaw, pitch);
				if (plugin.getConfig().getBoolean("debug")) {
					System.out.println("✅ EndLocation načtena: " + endLocation);
				}
			} else {
				if (plugin.getConfig().getBoolean("debug")) {
					System.out.println("❌ Chyba: Svět '" + worldName + "' nebyl nalezen! Čekám na načtení světa...");
				}
			}
		} else {
			endLocation = Bukkit.getWorlds().get(0).getSpawnLocation();
		}
	}

	@EventHandler
	public void onWorldLoad(WorldLoadEvent event) {
		String worldName = event.getWorld().getName();
		String savedWorld = plugin.getConfig().getString("end_location.world");

		if (savedWorld != null && savedWorld.equals(worldName)) {
			if (plugin.getConfig().getBoolean("debug")) {
				System.out.println("🌍 Svět '" + worldName + "' byl načten! Aktualizuji EndLocation.");
			}
			loadEndLocation(); // ✅ Znovu načteme EndLocation po načtení světa
		}
	}

	public void setEndLocation(Location location) {
		endLocation = location;
		plugin.getConfig().set("end_location.world", location.getWorld().getName());
		plugin.getConfig().set("end_location.x", location.getX());
		plugin.getConfig().set("end_location.y", location.getY());
		plugin.getConfig().set("end_location.z", location.getZ());
		plugin.getConfig().set("end_location.yaw", location.getYaw());
		plugin.getConfig().set("end_location.pitch", location.getPitch());
		plugin.saveConfig();
		if (plugin.getConfig().getBoolean("debug")) {
			System.out.println("✅ EndLocation uložena: " + location);
		}
	}

	public Location getEndLocation() {
		if (endLocation == null) {
			if (plugin.getConfig().getBoolean("debug")) {
				if (plugin.getConfig().getBoolean("debug")) {
					System.out.println("⚠ EndLocation není načtena! Používám výchozí spawn.");
				}
			}
			return Bukkit.getWorlds().get(0).getSpawnLocation();
		}
		return endLocation;
	}
}
