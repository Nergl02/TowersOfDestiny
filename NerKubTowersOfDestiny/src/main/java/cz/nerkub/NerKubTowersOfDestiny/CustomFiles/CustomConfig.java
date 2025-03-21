package cz.nerkub.NerKubTowersOfDestiny.CustomFiles;

import cz.nerkub.NerKubTowersOfDestiny.NerKubTowersOfDestiny;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.file.Files;

public class CustomConfig {

	private final NerKubTowersOfDestiny plugin;
	private File file;
	private FileConfiguration customConfig;
	private final String fileName;

	public CustomConfig(String directory, String fileName, NerKubTowersOfDestiny plugin){
		this.plugin = plugin;
		this.fileName = fileName;
		if (directory == null || directory.isEmpty()) {
			file = new File(plugin.getDataFolder(), fileName);
		} else {
			File dir = new File(plugin.getDataFolder(), directory);
			if (!dir.exists()) {
				dir.mkdirs();
			}

			file = new File(dir, fileName);
		}

		if (!file.exists()) {
			try (InputStream in = plugin.getResource(fileName)) {
				if (in != null) {
					Files.copy(in, file.toPath());
				} else {
					file.createNewFile();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		customConfig = YamlConfiguration.loadConfiguration(file);

	}

	public FileConfiguration getConfig() {
		return customConfig;
	}

	public void saveConfig() {
		if (file == null || customConfig == null) return;

		try (Writer writer = new FileWriter(file)) {
			writer.write(customConfig.saveToString());  // Ručně zapíšeme YAML obsah
		} catch (IOException e) {
			plugin.getLogger().severe("❌ Error while saving " + file.getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
	}

	public void reloadConfig() {
		customConfig = YamlConfiguration.loadConfiguration(file);
	}

	// 🔹 Automatická aktualizace chybějících hodnot
	public void updateConfig() {
		InputStream defaultConfigStream = plugin.getResource(fileName);
		if (defaultConfigStream != null) {
			YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultConfigStream));

			boolean updated = false;
			for (String key : defaultConfig.getKeys(true)) {
				if (!customConfig.contains(key)) {
					customConfig.set(key, defaultConfig.get(key));
					updated = true;
				}
			}

			if (updated) {
				saveConfig();
				plugin.getLogger().info("✅ Config file " + fileName + " has been updated with missing values!");
			}
		}
	}
}
