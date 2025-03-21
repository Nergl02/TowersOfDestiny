package cz.nerkub.NerKubTowersOfDestiny.Managers;

import org.bukkit.Material;

public class ArenaBlock {
	private final String worldName;
	private final int x, y, z;
	private final Material material;
	private final String blockData;

	public ArenaBlock(String worldName, int x, int y, int z, Material material, String blockData) {
		this.worldName = worldName;
		this.x = x;
		this.y = y;
		this.z = z;
		this.material = material;
		this.blockData = blockData;
	}

	public String getWorldName() {
		return worldName;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getZ() {
		return z;
	}

	public Material getMaterial() {
		return material;
	}

	public String getBlockData() {
		return blockData;
	}
}


