package de.armortrim.plugin.gui;

import org.bukkit.Color;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

public class PlayerSession {

    public enum Page {
        SLOT_SELECT, ARMOR_SELECT, TRIM_PATTERN, TRIM_MATERIAL, COLOR_SELECT
    }

    public enum Slot {
        HELMET    ("Helm",        3, org.bukkit.Material.IRON_HELMET),
        CHESTPLATE("Brustpanzer", 2, org.bukkit.Material.IRON_CHESTPLATE),
        LEGGINGS  ("Hosen",       1, org.bukkit.Material.IRON_LEGGINGS),
        BOOTS     ("Stiefel",     0, org.bukkit.Material.IRON_BOOTS),
        ELYTRA    ("Elytra",     -1, org.bukkit.Material.ELYTRA);

        public final String display;
        public final int    armorIndex;
        public final org.bukkit.Material icon;

        Slot(String d, int a, org.bukkit.Material i) { display=d; armorIndex=a; icon=i; }
    }

    private Page   page        = Page.SLOT_SELECT;
    private Slot   slot        = null;
    private org.bukkit.Material chosenArmor = null;
    private boolean isLeather  = false;
    private boolean isElytra   = false;

    private TrimPattern  pattern  = null;
    private TrimMaterial material = null;

    private int patternPage  = 0;
    private int materialPage = 0;
    private int colorPage    = 0;

    private int leatherRed   = 160;
    private int leatherGreen = 101;
    private int leatherBlue  = 64;

    public Page getPage()                 { return page; }
    public void setPage(Page p)           { this.page = p; }

    public Slot getSlot()                 { return slot; }
    public void setSlot(Slot s)           { this.slot = s; }

    public org.bukkit.Material getChosenArmor()       { return chosenArmor; }
    public void setChosenArmor(org.bukkit.Material m) { this.chosenArmor = m; }

    public boolean isLeather()            { return isLeather; }
    public void setLeather(boolean l)     { this.isLeather = l; }

    public boolean isElytra()             { return isElytra; }
    public void setElytra(boolean e)      { this.isElytra = e; }

    public TrimPattern  getPattern()          { return pattern; }
    public void setPattern(TrimPattern p)     { this.pattern = p; }

    public TrimMaterial getMaterial()         { return material; }
    public void setMaterial(TrimMaterial m)   { this.material = m; }

    public int getPatternPage()           { return patternPage; }
    public void setPatternPage(int p)     { this.patternPage = p; }

    public int getMaterialPage()          { return materialPage; }
    public void setMaterialPage(int p)    { this.materialPage = p; }

    public int getColorPage()             { return colorPage; }
    public void setColorPage(int p)       { this.colorPage = p; }

    public int getLeatherRed()            { return leatherRed; }
    public int getLeatherGreen()          { return leatherGreen; }
    public int getLeatherBlue()           { return leatherBlue; }

    public void setLeatherColor(int r, int g, int b) {
        this.leatherRed   = Math.max(0, Math.min(255, r));
        this.leatherGreen = Math.max(0, Math.min(255, g));
        this.leatherBlue  = Math.max(0, Math.min(255, b));
    }

    public void setLeatherColor(Color c) {
        setLeatherColor(c.getRed(), c.getGreen(), c.getBlue());
    }

    public Color getLeatherColor() {
        return Color.fromRGB(leatherRed, leatherGreen, leatherBlue);
    }
}
