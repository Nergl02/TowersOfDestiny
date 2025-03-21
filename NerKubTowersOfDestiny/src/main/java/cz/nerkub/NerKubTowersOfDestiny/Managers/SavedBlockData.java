package cz.nerkub.NerKubTowersOfDestiny.Managers;

import org.bukkit.Material;

public class SavedBlockData {
	private final String arenaName;
	private final String worldName;
	private final double x, y, z;  // ✅ Používáme double pro přesnost
	private final Material material;
	private final String blockData;

	public SavedBlockData(String arenaName, String worldName, double x, double y, double z, Material material, String blockData) {
		this.arenaName = arenaName;
		this.worldName = worldName;
		this.x = x;
		this.y = y;
		this.z = z;
		this.material = material;
		this.blockData = blockData;
	}

	public String getArenaName() {
		return arenaName;
	}

	public String getWorldName() {
		return worldName;
	}

	public int getX() {  // ✅ Převádíme na int, protože SQL chce INTEGER
		return (int) Math.round(x);
	}

	public int getY() {
		return (int) Math.round(y);
	}

	public int getZ() {
		return (int) Math.round(z);
	}

	public Material getMaterial() {
		return material;
	}

	public String getBlockData() {
		return blockData;
	}
}
