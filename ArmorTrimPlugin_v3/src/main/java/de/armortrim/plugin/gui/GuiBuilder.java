package de.armortrim.plugin.gui;

import de.armortrim.plugin.ArmorTrimPlugin;
import de.armortrim.plugin.manager.RankManager;
import de.armortrim.plugin.manager.TrimManager;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.util.*;

public class GuiBuilder {

    // Titel
    public static final String T_SLOT     = "§8✦ §6Armor Customizer §8│ §eSlot";
    public static final String T_ARMOR    = "§8✦ §6Armor Customizer §8│ §eRüstung";
    public static final String T_PATTERN  = "§8✦ §6Armor Customizer §8│ §bPattern";
    public static final String T_MATERIAL = "§8✦ §6Armor Customizer §8│ §dMaterial";
    public static final String T_COLOR    = "§8✦ §6Armor Customizer §8│ §aFarbe";

    // Grid: 4×7 innere Felder
    static final int[] GRID = {
        10,11,12,13,14,15,16,
        19,20,21,22,23,24,25,
        28,29,30,31,32,33,34,
        37,38,39,40,41,42,43
    };
    static final int GRID_SIZE = GRID.length;

    static final int BTN_BACK  = 45;
    static final int BTN_PREV  = 47;
    static final int BTN_PAGE  = 49;
    static final int BTN_NEXT  = 51;
    static final int BTN_CLOSE = 53;

    private final ArmorTrimPlugin plugin;
    private final RankManager rm;
    private final TrimManager tm;

    public GuiBuilder(ArmorTrimPlugin plugin) {
        this.plugin = plugin;
        this.rm = plugin.getRankManager();
        this.tm = plugin.getTrimManager();
    }

    // ══════════════════════════════════════════════════════════
    //  SEITE 1 – Slot auswählen (inkl. Elytra)
    // ══════════════════════════════════════════════════════════
    public Inventory slotPage(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, T_SLOT);
        border(inv, Material.GRAY_STAINED_GLASS_PANE);

        ItemStack[] worn = player.getInventory().getArmorContents();
        // worn[0]=boots [1]=leggings [2]=chestplate [3]=helmet

        addSlotButton(inv, 10, PlayerSession.Slot.HELMET,      worn[3]);
        addSlotButton(inv, 12, PlayerSession.Slot.CHESTPLATE,  worn[2]);
        addSlotButton(inv, 14, PlayerSession.Slot.LEGGINGS,    worn[1]);
        addSlotButton(inv, 16, PlayerSession.Slot.BOOTS,       worn[0]);

        // Elytra-Slot (Chestplate-Slot im Inventar)
        ItemStack elytraItem = player.getInventory().getChestplate();
        boolean hasElytra = elytraItem != null && elytraItem.getType() == Material.ELYTRA;
        inv.setItem(13, item(Material.ELYTRA,
                "§d§lElytra",
                hasElytra ? "§a✔ Elytra ausgerüstet" : "§7Kein Elytra ausgerüstet",
                "",
                "§eKlicken zum Auswählen"));

        inv.setItem(26, item(Material.BARRIER, "§cSchließen"));
        return inv;
    }

    private void addSlotButton(Inventory inv, int guiSlot, PlayerSession.Slot slot, ItemStack worn) {
        boolean hasItem = worn != null && worn.getType() != Material.AIR;
        Material icon   = hasItem ? worn.getType() : slot.icon;
        String   status = hasItem
                ? "§a✔ §f" + worn.getType().name().replace("_"," ").toLowerCase()
                : "§c✘ Kein Item ausgerüstet";
        inv.setItem(guiSlot, item(icon, "§e§l" + slot.display, status, "", "§7Klicken zum Auswählen"));
    }

    // ══════════════════════════════════════════════════════════
    //  SEITE 2 – Rüstungstyp auswählen
    // ══════════════════════════════════════════════════════════
    public Inventory armorPage(PlayerSession.Slot slot) {
        // Elytra hat keine Typ-Auswahl → direkt zu Trim
        Inventory inv = Bukkit.createInventory(null, 27, T_ARMOR);
        border(inv, Material.ORANGE_STAINED_GLASS_PANE);

        ArmorType[] types = ArmorType.values();
        int[] positions = {10, 11, 12, 13, 14, 15, 16};
        for (int i = 0; i < types.length && i < positions.length; i++) {
            ArmorType type = types[i];
            Material mat = type.getMaterial(slot);
            if (mat == Material.BARRIER) {
                inv.setItem(positions[i], item(Material.BARRIER, "§8Nicht verfügbar"));
                continue;
            }
            inv.setItem(positions[i], item(mat,
                    type.color + "§l" + type.displayName,
                    type == ArmorType.LEATHER ? "§a✦ Farbe anpassbar!" : "§b✦ Trims anpassbar!",
                    "", "§eKlicken zum Auswählen"));
        }

        inv.setItem(18, item(Material.ARROW,   "§c§l← Zurück", "§7Slot-Auswahl"));
        inv.setItem(26, item(Material.BARRIER, "§cSchließen"));
        return inv;
    }

    // ══════════════════════════════════════════════════════════
    //  SEITE 3A – Trim Pattern
    // ══════════════════════════════════════════════════════════
    public Inventory patternPage(Player player, PlayerSession session) {
        Inventory inv = Bukkit.createInventory(null, 54, T_PATTERN);
        border(inv, Material.CYAN_STAINED_GLASS_PANE);

        List<TrimPattern> all = tm.getAllPatterns();
        int playerLevel       = rm.level(rm.getPlayerRank(player));
        int start             = session.getPatternPage() * GRID_SIZE;
        int totalPages        = (int) Math.ceil((double) all.size() / GRID_SIZE);

        for (int i = 0; i < GRID_SIZE; i++) {
            int idx = start + i;
            if (idx >= all.size()) break;
            TrimPattern p   = all.get(idx);
            String required = tm.requiredRankForPattern(p);
            boolean access  = playerLevel >= rm.level(required);
            boolean selected = session.getPattern() != null && session.getPattern().key().equals(p.key());

            if (selected) {
                inv.setItem(GRID[i], item(Material.LIME_STAINED_GLASS_PANE,
                        "§a§l✔ " + tm.patternName(p) + " §7(Aktiv)", "§7Erneut klicken zum Abwählen"));
            } else if (access) {
                inv.setItem(GRID[i], item(tm.patternItem(p),
                        "§b§l" + tm.patternName(p),
                        "§7Rang: " + rm.displayRank(required), "", "§eKlicken"));
            } else {
                inv.setItem(GRID[i], item(Material.GRAY_STAINED_GLASS_PANE,
                        "§8§l" + tm.patternName(p) + " §8[GESPERRT]",
                        "§7Benötigt: " + rm.displayRank(required), "§cKein Zugang"));
            }
        }

        navButtons(inv, session.getPatternPage(), totalPages);
        inv.setItem(BTN_BACK,  item(Material.ARROW,   "§c§l← Zurück", "§7Rüstungs-Auswahl"));
        inv.setItem(BTN_CLOSE, item(Material.BARRIER, "§cSchließen"));
        return inv;
    }

    // ══════════════════════════════════════════════════════════
    //  SEITE 3B – Trim Material
    // ══════════════════════════════════════════════════════════
    public Inventory materialPage(Player player, PlayerSession session) {
        Inventory inv = Bukkit.createInventory(null, 54, T_MATERIAL);
        border(inv, Material.PURPLE_STAINED_GLASS_PANE);

        List<TrimMaterial> all = tm.getAllMaterials();
        int playerLevel        = rm.level(rm.getPlayerRank(player));
        int start              = session.getMaterialPage() * GRID_SIZE;
        int totalPages         = (int) Math.ceil((double) all.size() / GRID_SIZE);

        for (int i = 0; i < GRID_SIZE; i++) {
            int idx = start + i;
            if (idx >= all.size()) break;
            TrimMaterial m  = all.get(idx);
            String required = tm.requiredRankForMaterial(m);
            boolean access  = playerLevel >= rm.level(required);
            boolean selected = session.getMaterial() != null && session.getMaterial().key().equals(m.key());

            if (selected) {
                inv.setItem(GRID[i], item(Material.LIME_STAINED_GLASS_PANE,
                        "§a§l✔ " + tm.materialName(m) + " §7(Aktiv)", "§7Erneut klicken zum Abwählen"));
            } else if (access) {
                inv.setItem(GRID[i], item(tm.materialItem(m),
                        "§d§l" + tm.materialName(m),
                        "§7Rang: " + rm.displayRank(required), "", "§eKlicken"));
            } else {
                inv.setItem(GRID[i], item(Material.GRAY_STAINED_GLASS_PANE,
                        "§8§l" + tm.materialName(m) + " §8[GESPERRT]",
                        "§7Benötigt: " + rm.displayRank(required), "§cKein Zugang"));
            }
        }

        navButtons(inv, session.getMaterialPage(), totalPages);
        inv.setItem(BTN_BACK,  item(Material.ARROW,   "§c§l← Zurück", "§7Pattern-Auswahl"));
        inv.setItem(BTN_CLOSE, item(Material.BARRIER, "§cSchließen"));

        if (session.getPattern() != null && session.getMaterial() != null) {
            inv.setItem(53, item(Material.EMERALD, "§a§l✔ Trim anwenden!",
                    "§7Pattern: §b" + tm.patternName(session.getPattern()),
                    "§7Material: §d" + tm.materialName(session.getMaterial()),
                    "", "§eKlicken"));
        }
        return inv;
    }

    // ══════════════════════════════════════════════════════════
    //  SEITE 4 – Leder-Farbe (aus config.yml)
    // ══════════════════════════════════════════════════════════
    public Inventory colorPage(Player player, PlayerSession session) {
        // Alle konfigurierten Farben laden
        List<LeatherColor> colors = getConfiguredColors();
        int playerLevel = rm.level(rm.getPlayerRank(player));

        int totalPages = Math.max(1, (int) Math.ceil((double) colors.size() / GRID_SIZE));
        int colorPage  = session.getColorPage();
        int start      = colorPage * GRID_SIZE;

        Inventory inv = Bukkit.createInventory(null, 54, T_COLOR);
        border(inv, Material.WHITE_STAINED_GLASS_PANE);

        // ── Vorschau (Slot 4 - oben Mitte) ───────────────────
        Color current = session.getLeatherColor();
        ItemStack preview = leatherPreview(session.getChosenArmor(), current);
        ItemMeta pm = preview.getItemMeta();
        pm.setDisplayName("§e§lVorschau");
        pm.setLore(Arrays.asList(
                "§7Aktuelle Farbe:",
                "§7RGB: §f(" + current.getRed() + ", " + current.getGreen() + ", " + current.getBlue() + ")",
                "§7Hex: §f#" + String.format("%02X%02X%02X", current.getRed(), current.getGreen(), current.getBlue()),
                "",
                "§7Letzte gespeicherte Farbe wird",
                "§7beim nächsten /trims vorgeladen."
        ));
        preview.setItemMeta(pm);
        inv.setItem(4, preview);

        // ── Farb-Grid ────────────────────────────────────────
        for (int i = 0; i < GRID_SIZE; i++) {
            int colorIdx = start + i;
            if (colorIdx >= colors.size()) break;
            LeatherColor lc = colors.get(colorIdx);
            boolean access  = playerLevel >= rm.level(lc.rank);
            boolean active  = current.getRed()   == lc.color.getRed()
                           && current.getGreen() == lc.color.getGreen()
                           && current.getBlue()  == lc.color.getBlue();

            if (active) {
                ItemStack sel = leatherPreview(session.getChosenArmor(), lc.color);
                ItemMeta sm = sel.getItemMeta();
                sm.setDisplayName("§a§l✔ " + lc.name + " §7(Aktiv)");
                sm.setLore(Arrays.asList(
                        "§7RGB: §f(" + lc.color.getRed() + ", " + lc.color.getGreen() + ", " + lc.color.getBlue() + ")",
                        "§7Hex: §f#" + String.format("%02X%02X%02X", lc.color.getRed(), lc.color.getGreen(), lc.color.getBlue())
                ));
                sel.setItemMeta(sm);
                inv.setItem(GRID[i], sel);
            } else if (access) {
                ItemStack ci = item(lc.dyeIcon, "§f§l" + lc.name,
                        "§7Rang: " + rm.displayRank(lc.rank),
                        "§7RGB: §f(" + lc.color.getRed() + ", " + lc.color.getGreen() + ", " + lc.color.getBlue() + ")",
                        "§7Hex: §f#" + String.format("%02X%02X%02X", lc.color.getRed(), lc.color.getGreen(), lc.color.getBlue()),
                        "", "§eKlicken zum Auswählen");
                inv.setItem(GRID[i], ci);
            } else {
                inv.setItem(GRID[i], item(Material.GRAY_STAINED_GLASS_PANE,
                        "§8§l" + lc.name + " §8[GESPERRT]",
                        "§7Benötigt: " + rm.displayRank(lc.rank), "§cKein Zugang"));
            }
        }

        navButtons(inv, colorPage, totalPages);
        inv.setItem(BTN_BACK,  item(Material.ARROW,   "§c§l← Zurück", "§7Rüstungs-Auswahl"));
        inv.setItem(BTN_CLOSE, item(Material.BARRIER, "§cSchließen"));
        inv.setItem(53, item(Material.EMERALD, "§a§l✔ Farbe anwenden!",
                "§7Klicken um die ausgewählte Farbe zu übernehmen",
                "§7und dauerhaft zu speichern."));
        return inv;
    }

    // ── Konfigurierte Farben lesen ────────────────────────────
    public List<LeatherColor> getConfiguredColors() {
        List<LeatherColor> list = new ArrayList<>();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("leather-colors");
        if (section == null) return list;

        for (String key : section.getKeys(false)) {
            String rgb  = section.getString(key + ".rgb", "160,101,64");
            String rank = section.getString(key + ".rank", "default");
            String dye  = section.getString(key + ".dye", "WHITE_DYE");

            String[] parts = rgb.split(",");
            try {
                int r = Integer.parseInt(parts[0].trim());
                int g = Integer.parseInt(parts[1].trim());
                int b = Integer.parseInt(parts[2].trim());
                Color color = Color.fromRGB(r, g, b);

                Material dyeMat;
                try { dyeMat = Material.valueOf(dye); }
                catch (IllegalArgumentException e) { dyeMat = Material.WHITE_DYE; }

                list.add(new LeatherColor(key, color, rank, dyeMat));
            } catch (Exception e) {
                plugin.getLogger().warning("Ungültige Farbe in config.yml: " + key);
            }
        }
        return list;
    }

    // ── Datenklasse für Farben ────────────────────────────────
    public static class LeatherColor {
        public final String   name;
        public final Color    color;
        public final String   rank;
        public final Material dyeIcon;

        public LeatherColor(String name, Color color, String rank, Material dyeIcon) {
            this.name    = name;
            this.color   = color;
            this.rank    = rank;
            this.dyeIcon = dyeIcon;
        }
    }

    // ── Helpers ───────────────────────────────────────────────
    public ItemStack leatherPreview(Material armorMat, Color color) {
        if (armorMat == null) armorMat = Material.LEATHER_CHESTPLATE;
        ItemStack item = new ItemStack(armorMat);
        if (item.getItemMeta() instanceof LeatherArmorMeta meta) {
            meta.setColor(color);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void navButtons(Inventory inv, int currentPage, int totalPages) {
        if (currentPage > 0)
            inv.setItem(BTN_PREV, item(Material.ARROW, "§e§l← Vorherige Seite",
                    "§7Seite " + currentPage + " / " + totalPages));
        inv.setItem(BTN_PAGE, item(Material.PAPER,
                "§7Seite §f" + (currentPage+1) + " §7/ §f" + totalPages));
        if (currentPage < totalPages - 1)
            inv.setItem(BTN_NEXT, item(Material.ARROW, "§e§lNächste Seite →",
                    "§7Seite " + (currentPage+2) + " / " + totalPages));
    }

    static void border(Inventory inv, Material mat) {
        ItemStack glass = new ItemStack(mat);
        ItemMeta m = glass.getItemMeta();
        m.setDisplayName(" ");
        glass.setItemMeta(m);
        int size = inv.getSize(), rows = size / 9;
        for (int i = 0; i < 9; i++) { inv.setItem(i, glass); inv.setItem(size-9+i, glass); }
        for (int row = 1; row < rows-1; row++) { inv.setItem(row*9, glass); inv.setItem(row*9+8, glass); }
    }

    public static ItemStack item(Material mat, String name, String... lore) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta   = stack.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) meta.setLore(Arrays.asList(lore));
        stack.setItemMeta(meta);
        return stack;
    }

    // ══════════════════════════════════════════════════════════
    //  ArmorType Enum
    // ══════════════════════════════════════════════════════════
    public enum ArmorType {
        LEATHER   ("Leder",      "§6", Material.LEATHER_HELMET,   Material.LEATHER_CHESTPLATE,   Material.LEATHER_LEGGINGS,   Material.LEATHER_BOOTS),
        CHAINMAIL ("Kettenhemd", "§7", Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS),
        IRON      ("Eisen",      "§f", Material.IRON_HELMET,      Material.IRON_CHESTPLATE,      Material.IRON_LEGGINGS,      Material.IRON_BOOTS),
        GOLDEN    ("Gold",       "§e", Material.GOLDEN_HELMET,    Material.GOLDEN_CHESTPLATE,    Material.GOLDEN_LEGGINGS,    Material.GOLDEN_BOOTS),
        DIAMOND   ("Diamant",    "§b", Material.DIAMOND_HELMET,   Material.DIAMOND_CHESTPLATE,   Material.DIAMOND_LEGGINGS,   Material.DIAMOND_BOOTS),
        NETHERITE ("Netherite",  "§8", Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS),
        TURTLE    ("Schildkröte","§a", Material.TURTLE_HELMET,    null,                          null,                        null);

        public final String displayName, color;
        private final Material helmet, chest, legs, boots;

        ArmorType(String dn, String color, Material h, Material c, Material l, Material b) {
            this.displayName=dn; this.color=color;
            this.helmet=h; this.chest=c; this.legs=l; this.boots=b;
        }

        public Material getMaterial(PlayerSession.Slot slot) {
            return switch (slot) {
                case HELMET      -> helmet != null ? helmet : Material.BARRIER;
                case CHESTPLATE  -> chest  != null ? chest  : Material.BARRIER;
                case LEGGINGS    -> legs   != null ? legs   : Material.BARRIER;
                case BOOTS       -> boots  != null ? boots  : Material.BARRIER;
                case ELYTRA      -> Material.BARRIER; // Elytra hat keinen ArmorType
            };
        }

        public boolean isLeather() { return this == LEATHER; }

        public static ArmorType fromMaterial(Material m) {
            for (ArmorType t : values())
                for (PlayerSession.Slot s : PlayerSession.Slot.values())
                    if (s != PlayerSession.Slot.ELYTRA && t.getMaterial(s) == m) return t;
            return null;
        }
    }
}
