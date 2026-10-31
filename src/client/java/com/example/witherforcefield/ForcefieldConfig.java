package com.example.witherforcefield;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Settings, saved to config/witherforcefield.json in your game folder.
 * Edit the file and run /forcefield reload, or use the /forcefield commands.
 */
public class ForcefieldConfig {

	/** Turn the forcefield on or off. */
	public boolean enabled = true;

	/**
	 * Permanent mode: a full-body forcefield that is always on (no armor needed) and doesn't move.
	 * While this is true, "speed" is ignored.
	 */
	public boolean permanent = false;

	/** Swirl speed. 1.0 = same as the Wither, 0 = frozen, 3.0 = three times faster. */
	public float speed = 1.0f;

	/** Color as #RRGGBB. #808080 is vanilla's grey; the swirl texture is multiplied by this color. */
	public String color = "#808080";

	/** Parsed form of {@link #color} as ARGB (not saved to the file). */
	private transient int tint = 0xFF808080;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("witherforcefield.json");
	private static ForcefieldConfig instance = new ForcefieldConfig();

	public static ForcefieldConfig get() {
		return instance;
	}

	public int tint() {
		return this.tint;
	}

	/** Reads the file (creating it with defaults if missing). Bad values fall back to defaults. */
	public static void load() {
		ForcefieldConfig loaded = new ForcefieldConfig();
		if (Files.exists(FILE)) {
			try {
				ForcefieldConfig fromFile = GSON.fromJson(Files.readString(FILE), ForcefieldConfig.class);
				if (fromFile != null) {
					loaded = fromFile;
				}
			} catch (Exception e) {
				System.err.println("[witherforcefield] Could not read config, using defaults: " + e);
			}
		}
		loaded.speed = clampSpeed(loaded.speed);
		if (!loaded.setColor(loaded.color)) {
			loaded.setColor("#808080");
		}
		instance = loaded;
		save();
	}

	public static void save() {
		try {
			Files.writeString(FILE, GSON.toJson(instance));
		} catch (IOException e) {
			System.err.println("[witherforcefield] Could not save config: " + e);
		}
	}

	public static void resetToDefaults() {
		instance = new ForcefieldConfig();
		save();
	}

	public void setSpeed(float value) {
		this.speed = clampSpeed(value);
	}

	/** Accepts "#RRGGBB" or "RRGGBB". Returns false (and changes nothing) if it isn't valid. */
	public boolean setColor(String hex) {
		if (hex == null) {
			return false;
		}
		String h = hex.trim();
		if (h.startsWith("#")) {
			h = h.substring(1);
		}
		if (h.length() != 6) {
			return false;
		}
		try {
			int rgb = Integer.parseInt(h, 16);
			this.tint = 0xFF000000 | rgb;
			this.color = "#" + h.toUpperCase();
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private static float clampSpeed(float v) {
		if (Float.isNaN(v)) {
			return 1.0f;
		}
		return Math.max(0.0f, Math.min(10.0f, v));
	}
}
