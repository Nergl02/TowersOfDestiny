package cz.nerkub.NerKubTowersOfDestiny.Utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomItemGenerator {
	private final List<Material> validItems = new ArrayList<>();
	private final Random random = new Random();

	public RandomItemGenerator() {
		for (Material material : Material.values()) {
			if (isValid(material)) {
				validItems.add(material);
			}
		}
	}

	private boolean isValid(Material material) {
		// ❌ Vyloučené bloky a itemy
		return material.isItem() // ✅ Zajistí, že to není blok bez item verze
				&& !material.name().contains("COMMAND")
				&& material != Material.BARRIER
				&& material != Material.STRUCTURE_BLOCK
				&& material != Material.JIGSAW
				&& material != Material.WITHER_SPAWN_EGG
				&& material != Material.ENDER_DRAGON_SPAWN_EGG;
	}

	public ItemStack generateRandomItem() {
		if (validItems.isEmpty()) {
			System.out.println("❌ Chyba: Seznam platných itemů je prázdný!");
			return new ItemStack(Material.STONE); // ✅ Výchozí item pro případ chyby
		}

		Material randomMaterial = validItems.get(random.nextInt(validItems.size()));

		return new ItemStack(randomMaterial, 1);
	}
}
