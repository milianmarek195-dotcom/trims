package de.armortrim.plugin.manager;

import de.armortrim.plugin.ArmorTrimPlugin;
import org.bukkit.Color;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Speichert pro Spieler:
 *  - Letzte Leder-Farbe (R, G, B)
 *  - Letztes Trim-Pattern
 *  - Letztes Trim-Material
 */
public class PlayerDataManager {

    private final ArmorTrimPlugin plugin;
    private final File dataFile;
    private FileConfiguration data;

    public PlayerDataManager(ArmorTrimPlugin plugin) {
        this.plugin   = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        load();
    }

    private void load() {
        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Konnte playerdata.yml nicht erstellen: " + e.getMessage());
            }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void save() {
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Konnte playerdata.yml nicht speichern: " + e.getMessage());
        }
    }

    // ── Leder-Farbe ───────────────────────────────────────────

    public void saveLeatherColor(UUID uuid, int r, int g, int b) {
        String path = "players." + uuid + ".leather-color";
        data.set(path + ".r", r);
        data.set(path + ".g", g);
        data.set(path + ".b", b);
        save();
    }

    public Color loadLeatherColor(UUID uuid) {
        String path = "players." + uuid + ".leather-color";
        if (!data.contains(path)) return null;
        int r = data.getInt(path + ".r", 160);
        int g = data.getInt(path + ".g", 101);
        int b = data.getInt(path + ".b", 64);
        return Color.fromRGB(r, g, b);
    }

    // ── Letzter Trim ──────────────────────────────────────────

    public void saveLastPattern(UUID uuid, String patternKey) {
        data.set("players." + uuid + ".last-pattern", patternKey);
        save();
    }

    public String loadLastPattern(UUID uuid) {
        return data.getString("players." + uuid + ".last-pattern", null);
    }

    public void saveLastMaterial(UUID uuid, String materialKey) {
        data.set("players." + uuid + ".last-material", materialKey);
        save();
    }

    public String loadLastMaterial(UUID uuid) {
        return data.getString("players." + uuid + ".last-material", null);
    }
}
