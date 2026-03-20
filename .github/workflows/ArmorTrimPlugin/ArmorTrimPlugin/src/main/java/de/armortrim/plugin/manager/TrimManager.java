package de.armortrim.plugin.manager;

import de.armortrim.plugin.ArmorTrimPlugin;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TrimManager {

    private final ArmorTrimPlugin plugin;
    private final List<TrimPattern> patterns = new ArrayList<>();
    private final List<TrimMaterial> materials = new ArrayList<>();

    public TrimManager(ArmorTrimPlugin plugin) {
        this.plugin = plugin;
        Registry.TRIM_PATTERN.forEach(patterns::add);
        Registry.TRIM_MATERIAL.forEach(materials::add);
    }

    // ── Pattern ──────────────────────────────────────────────

    public List<TrimPattern> getAllPatterns()  { return Collections.unmodifiableList(patterns); }
    public List<TrimMaterial> getAllMaterials(){ return Collections.unmodifiableList(materials);}

    public String requiredRankForPattern(TrimPattern p) {
        return plugin.getConfig().getString(
                "trim-permissions." + p.key().getKey().toUpperCase(), "default").toLowerCase();
    }

    public String requiredRankForMaterial(TrimMaterial m) {
        return plugin.getConfig().getString(
                "material-permissions." + m.key().getKey().toUpperCase(), "default").toLowerCase();
    }

    // ── Display helpers ───────────────────────────────────────

    public String patternName(TrimPattern p) {
        return prettify(p.key().getKey());
    }

    public String materialName(TrimMaterial m) {
        return prettify(m.key().getKey());
    }

    private String prettify(String key) {
        String[] parts = key.replace("_", " ").split(" ");
        StringBuilder sb = new StringBuilder();
        for (String part : parts)
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase()).append(" ");
        return sb.toString().trim();
    }

    /** Liefert das passende Inventar-Item für ein Trim-Material. */
    public Material materialItem(TrimMaterial m) {
        return switch (m.key().getKey().toUpperCase()) {
            case "AMETHYST"  -> Material.AMETHYST_SHARD;
            case "COPPER"    -> Material.COPPER_INGOT;
            case "DIAMOND"   -> Material.DIAMOND;
            case "EMERALD"   -> Material.EMERALD;
            case "GOLD"      -> Material.GOLD_INGOT;
            case "IRON"      -> Material.IRON_INGOT;
            case "LAPIS"     -> Material.LAPIS_LAZULI;
            case "QUARTZ"    -> Material.QUARTZ;
            case "REDSTONE"  -> Material.REDSTONE;
            case "NETHERITE" -> Material.NETHERITE_INGOT;
            case "RESIN"     -> Material.RESIN_BRICK;
            default          -> Material.NETHER_STAR;
        };
    }

    /** Versucht das Smithing-Template-Item zu finden. */
    public Material patternItem(TrimPattern p) {
        try {
            return Material.valueOf(p.key().getKey().toUpperCase() + "_ARMOR_TRIM_SMITHING_TEMPLATE");
        } catch (IllegalArgumentException e) {
            return Material.PAPER;
        }
    }
}
