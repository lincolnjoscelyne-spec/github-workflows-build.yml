package com.example.dupebutton;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

/** Tiny properties-file config stored at config/dupebutton.properties. */
public final class DupeConfig {
	private DupeConfig() {}

	/** Whether the "Dupe" button is drawn on chest screens. Toggled by the rebindable key. */
	public static boolean showButton = true;

	/** Ticks to wait between the held click and closing the screen (20 ticks = 1 second). */
	public static int delayTicks = 2;

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("dupebutton.properties");
	}

	public static void load() {
		Path path = path();
		if (!Files.exists(path)) {
			save();
			return;
		}
		Properties props = new Properties();
		try (Reader reader = Files.newBufferedReader(path)) {
			props.load(reader);
			showButton = Boolean.parseBoolean(props.getProperty("showButton", "true"));
			delayTicks = Math.max(0, Integer.parseInt(props.getProperty("delayTicks", "2").trim()));
		} catch (IOException | NumberFormatException e) {
			DupeButtonClient.LOGGER.warn("Could not read config, using defaults", e);
		}
	}

	public static void save() {
		Properties props = new Properties();
		props.setProperty("showButton", Boolean.toString(showButton));
		props.setProperty("delayTicks", Integer.toString(delayTicks));
		try (Writer writer = Files.newBufferedWriter(path())) {
			props.store(writer, "Dupe Button config");
		} catch (IOException e) {
			DupeButtonClient.LOGGER.warn("Could not save config", e);
		}
	}
}
