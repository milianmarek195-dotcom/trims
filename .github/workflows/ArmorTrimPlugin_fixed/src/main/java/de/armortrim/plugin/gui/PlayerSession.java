package de.armortrim.plugin.gui;

import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

/**
 * Speichert den aktuellen GUI-Zustand eines Spielers.
 *
 * GUI-Flow:
 *  SLOT_SELECT  →  ARMOR_SELECT  →  (Leder) COLOR_SELECT
 *                              →  (Nicht-Leder) TRIM_PATTERN  →  TRIM_MATERIAL
 */
public class PlayerSession {

    public enum Page {
        SLOT_SELECT,      // Welchen Rüstungs-Slot?
        ARMOR_SELECT,     // Welche Rüstung (Typ)?
        TRIM_PATTERN,     // Welches Trim-Pattern?
        TRIM_MATERIAL,    // Welches Trim-Material?
        COLOR_SELECT      // Leder-Farb-Auswahl
    }

    public enum Slot {
        HELMET   ("Helm",        3, org.bukkit.Material.IRON_HELMET),
        CHESTPLATE("Brustpanzer",2, org.bukkit.Material.IRON_CHESTPLATE),
        LEGGINGS ("Hosen",       1, org.bukkit.Material.IRON_LEGGINGS),
        BOOTS    ("Stiefel",     0, org.bukkit.Material.IRON_BOOTS);

        public final String display;
        public final int armorIndex;       // Index in getArmorContents(): 0=boots…3=helmet
        public final org.bukkit.Material icon;

        Slot(String display, int armorIndex, org.bukkit.Material icon) {
            this.display    = display;
            this.armorIndex = armorIndex;
            this.icon       = icon;
        }
    }

    // ── State ─────────────────────────────────────────────────

    private Page         page      = Page.SLOT_SELECT;
    private Slot         slot      = null;
    private org.bukkit.Material chosenArmor = null; // welcher Rüstungstyp wurde gewählt
    private boolean      isLeather = false;

    private TrimPattern  pattern   = null;
    private TrimMaterial material  = null;

    // Pagination
    private int patternPage  = 0;
    private int materialPage = 0;

    // Chosen leather color (RGB packed as int)
    private int leatherRed   = 160;
    private int leatherGreen = 101;
    private int leatherBlue  = 64;  // Vanilla-Default

    // ── Getters / Setters ─────────────────────────────────────

    public Page getPage()                   { return page; }
    public void setPage(Page p)             { this.page = p; }

    public Slot getSlot()                   { return slot; }
    public void setSlot(Slot s)             { this.slot = s; }

    public org.bukkit.Material getChosenArmor()         { return chosenArmor; }
    public void setChosenArmor(org.bukkit.Material m)   { this.chosenArmor = m; }

    public boolean isLeather()              { return isLeather; }
    public void setLeather(boolean l)       { this.isLeather = l; }

    public TrimPattern  getPattern()        { return pattern; }
    public void setPattern(TrimPattern p)   { this.pattern = p; }

    public TrimMaterial getMaterial()       { return material; }
    public void setMaterial(TrimMaterial m) { this.material = m; }

    public int getPatternPage()             { return patternPage; }
    public void setPatternPage(int p)       { this.patternPage = p; }

    public int getMaterialPage()            { return materialPage; }
    public void setMaterialPage(int p)      { this.materialPage = p; }

    public int getLeatherRed()              { return leatherRed; }
    public int getLeatherGreen()            { return leatherGreen; }
    public int getLeatherBlue()             { return leatherBlue; }

    public void setLeatherColor(int r, int g, int b) {
        this.leatherRed   = Math.max(0, Math.min(255, r));
        this.leatherGreen = Math.max(0, Math.min(255, g));
        this.leatherBlue  = Math.max(0, Math.min(255, b));
    }

    public org.bukkit.Color getLeatherColor() {
        return org.bukkit.Color.fromRGB(leatherRed, leatherGreen, leatherBlue);
    }
}
