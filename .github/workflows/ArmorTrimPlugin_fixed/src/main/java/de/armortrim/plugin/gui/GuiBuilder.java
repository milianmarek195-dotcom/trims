package de.armortrim.plugin.gui;

import de.armortrim.plugin.ArmorTrimPlugin;
import de.armortrim.plugin.manager.RankManager;
import de.armortrim.plugin.manager.TrimManager;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.util.Arrays;
import java.util.List;

/**
 * Baut alle GUI-Inventare.
 * Öffentliche Methoden geben fertige Inventory-Objekte zurück.
 */
public class GuiBuilder {

    // ── Titel ─────────────────────────────────────────────────
    public static final String T_SLOT    = "§8✦ §6Armor Customizer §8│ §eSlot";
    public static final String T_ARMOR   = "§8✦ §6Armor Customizer §8│ §eRüstung";
    public static final String T_PATTERN = "§8✦ §6Armor Customizer §8│ §bPattern";
    public static final String T_MATERIAL= "§8✦ §6Armor Customizer §8│ §dMaterial";
    public static final String T_COLOR   = "§8✦ §6Armor Customizer §8│ §aFarbe";

    // Grid-Slots: 4 Zeilen × 7 Felder (Spalten 1–7 der inneren Rows 1–4)
    static final int[] GRID = {
            10,11,12,13,14,15,16,
            19,20,21,22,23,24,25,
            28,29,30,31,32,33,34,
            37,38,39,40,41,42,43
    };
    static final int GRID_SIZE = GRID.length; // 28

    // Feste Navigation-Slots
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
    //  SEITE 1 – Slot auswählen
    // ══════════════════════════════════════════════════════════
    public Inventory slotPage(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, T_SLOT);
        border(inv, Material.GRAY_STAINED_GLASS_PANE);

        ItemStack[] worn = player.getInventory().getArmorContents();
        // worn[0]=boots [1]=leggings [2]=chestplate [3]=helmet

        // Slots anzeigen: Helm(10), Brust(12), Hose(14), Stiefel(16)
        addSlotButton(inv, 10, PlayerSession.Slot.HELMET,     worn[3]);
        addSlotButton(inv, 12, PlayerSession.Slot.CHESTPLATE, worn[2]);
        addSlotButton(inv, 14, PlayerSession.Slot.LEGGINGS,   worn[1]);
        addSlotButton(inv, 16, PlayerSession.Slot.BOOTS,      worn[0]);

        inv.setItem(26, item(Material.BARRIER, "§cSchließen"));
        return inv;
    }

    private void addSlotButton(Inventory inv, int guiSlot, PlayerSession.Slot slot, ItemStack worn) {
        boolean hasItem = worn != null && worn.getType() != Material.AIR;
        Material icon   = hasItem ? worn.getType() : slot.icon;
        String   status = hasItem ? "§a✔ Ausgerüstet: §f" + worn.getType().name().replace("_"," ").toLowerCase()
                                  : "§c✘ Kein Item ausgerüstet";
        inv.setItem(guiSlot, item(icon, "§e§l" + slot.display, status, "", "§7Klicken zum Auswählen"));
    }

    // ══════════════════════════════════════════════════════════
    //  SEITE 2 – Rüstungstyp auswählen
    // ══════════════════════════════════════════════════════════
    public Inventory armorPage(PlayerSession.Slot slot) {
        Inventory inv = Bukkit.createInventory(null, 27, T_ARMOR);
        border(inv, Material.ORANGE_STAINED_GLASS_PANE);

        // Die verschiedenen Materialien für den gewählten Slot
        ArmorType[] types = ArmorType.values();
        int[] positions = {10, 11, 12, 13, 14, 15, 16};
        for (int i = 0; i < types.length && i < positions.length; i++) {
            ArmorType type = types[i];
            Material mat = type.getMaterial(slot);
            inv.setItem(positions[i], item(mat,
                    type.color + "§l" + type.displayName,
                    "§7Material: §f" + type.displayName,
                    "",
                    type == ArmorType.LEATHER ? "§a✦ Farbe anpassbar!" : "§b✦ Trims anpassbar!",
                    "",
                    "§eKlicken zum Auswählen"));
        }

        inv.setItem(18, item(Material.ARROW,   "§c§l← Zurück",   "§7Slot-Auswahl"));
        inv.setItem(26, item(Material.BARRIER, "§cSchließen"));
        return inv;
    }

    // ══════════════════════════════════════════════════════════
    //  SEITE 3A – Trim Pattern
    // ══════════════════════════════════════════════════════════
    public Inventory patternPage(Player player, PlayerSession session) {
        Inventory inv = Bukkit.createInventory(null, 54, T_PATTERN);
        border(inv, Material.CYAN_STAINED_GLASS_PANE);

        List<TrimPattern> all   = tm.getAllPatterns();
        int playerLevel         = rm.level(rm.getPlayerRank(player));
        int start               = session.getPatternPage() * GRID_SIZE;
        int totalPages          = (int) Math.ceil((double) all.size() / GRID_SIZE);

        for (int i = 0; i < GRID_SIZE; i++) {
            int idx = start + i;
            if (idx >= all.size()) break;
            TrimPattern p  = all.get(idx);
            String required = tm.requiredRankForPattern(p);
            boolean access  = playerLevel >= rm.level(required);
            boolean selected = session.getPattern() != null && session.getPattern().key().equals(p.key());

            if (selected) {
                inv.setItem(GRID[i], item(Material.LIME_STAINED_GLASS_PANE,
                        "§a§l✔ " + tm.patternName(p) + " §7(Aktiv)", "§7Klicken zum Abwählen"));
            } else if (access) {
                inv.setItem(GRID[i], item(tm.patternItem(p),
                        "§b§l" + tm.patternName(p),
                        "§7Rang: " + rm.displayRank(required),
                        "", "§eKlicken zum Auswählen"));
            } else {
                inv.setItem(GRID[i], item(Material.GRAY_STAINED_GLASS_PANE,
                        "§8§l" + tm.patternName(p) + " §8[GESPERRT]",
                        "§7Benötigt: " + rm.displayRank(required),
                        "§cKein Zugang"));
            }
        }

        navButtons(inv, session.getPatternPage(), totalPages);
        inv.setItem(BTN_BACK,  item(Material.ARROW,   "§c§l← Zurück",  "§7Rüstungs-Auswahl"));
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
                        "§a§l✔ " + tm.materialName(m) + " §7(Aktiv)", "§7Klicken zum Abwählen"));
            } else if (access) {
                inv.setItem(GRID[i], item(tm.materialItem(m),
                        "§d§l" + tm.materialName(m),
                        "§7Rang: " + rm.displayRank(required),
                        "", "§eKlicken zum Auswählen"));
            } else {
                inv.setItem(GRID[i], item(Material.GRAY_STAINED_GLASS_PANE,
                        "§8§l" + tm.materialName(m) + " §8[GESPERRT]",
                        "§7Benötigt: " + rm.displayRank(required),
                        "§cKein Zugang"));
            }
        }

        navButtons(inv, session.getMaterialPage(), totalPages);
        inv.setItem(BTN_BACK,  item(Material.ARROW,   "§c§l← Zurück",  "§7Pattern-Auswahl"));
        inv.setItem(BTN_CLOSE, item(Material.BARRIER, "§cSchließen"));

        // Trim anwenden Button (nur wenn Pattern gewählt)
        if (session.getPattern() != null && session.getMaterial() != null) {
            inv.setItem(53, item(Material.EMERALD, "§a§l✔ Trim anwenden!",
                    "§7Pattern: §b" + tm.patternName(session.getPattern()),
                    "§7Material: §d" + tm.materialName(session.getMaterial()),
                    "", "§eKlicken zum Anwenden"));
        }

        return inv;
    }

    // ══════════════════════════════════════════════════════════
    //  SEITE 4 – Leder-Farbe
    // ══════════════════════════════════════════════════════════
    /**
     * 54-Slot GUI:
     *  Zeile 0 (0-8)   : Border
     *  Zeile 1 (9-17)  : Vorschau-Item (Slot 13) + Info
     *  Zeile 2 (18-26) : Rot-Slider   (–– ●–– ++)
     *  Zeile 3 (27-35) : Grün-Slider
     *  Zeile 4 (36-44) : Blau-Slider
     *  Zeile 5 (45-53) : Preset-Farben + Zurück + Anwenden
     */
    public Inventory colorPage(Player player, PlayerSession session) {
        Inventory inv = Bukkit.createInventory(null, 54, T_COLOR);
        border(inv, Material.WHITE_STAINED_GLASS_PANE);

        int r = session.getLeatherRed();
        int g = session.getLeatherGreen();
        int b = session.getLeatherBlue();

        // ── Vorschau ──────────────────────────────────────────
        ItemStack preview = leatherPreview(session.getChosenArmor(), Color.fromRGB(r, g, b));
        ItemMeta pm = preview.getItemMeta();
        pm.setDisplayName("§e§lVorschau");
        pm.setLore(Arrays.asList(
                "§7Rot:   §c" + r,
                "§7Grün:  §a" + g,
                "§7Blau:  §9" + b,
                "§7Hex:   §f#" + String.format("%02X%02X%02X", r, g, b)
        ));
        preview.setItemMeta(pm);
        inv.setItem(13, preview);

        // ── RGB-Slider ────────────────────────────────────────
        buildSlider(inv, 18, "§cRot",   r, 'R');
        buildSlider(inv, 27, "§aGrün",  g, 'G');
        buildSlider(inv, 36, "§9Blau",  b, 'B');

        // ── Preset-Farben (Zeile 5, Slots 45-52) ─────────────
        Color[] presets = {
                Color.RED, Color.ORANGE, Color.YELLOW, Color.LIME,
                Color.AQUA, Color.BLUE, Color.PURPLE, Color.WHITE
        };
        String[] presetNames = {"§cRot","§6Orange","§eGelb","§aGrün","§bTürkis","§9Blau","§5Lila","§fWeiß"};
        Material[] presetMats = {
                Material.RED_DYE, Material.ORANGE_DYE, Material.YELLOW_DYE, Material.LIME_DYE,
                Material.LIGHT_BLUE_DYE, Material.BLUE_DYE, Material.PURPLE_DYE, Material.WHITE_DYE
        };
        for (int i = 0; i < presets.length; i++) {
            inv.setItem(45 + i, item(presetMats[i], presetNames[i], "§7Preset-Farbe", "§eKlicken"));
        }

        // ── Buttons ───────────────────────────────────────────
        inv.setItem(BTN_BACK,  item(Material.ARROW,   "§c§l← Zurück", "§7Rüstungs-Auswahl"));
        inv.setItem(53, item(Material.EMERALD, "§a§l✔ Farbe anwenden!",
                "§7Klicken um die Farbe zu übernehmen"));

        return inv;
    }

    /**
     * Baut einen Slider für einen RGB-Kanal.
     * Layout pro Zeile (9 Items, offset = Slot der ersten Zelle):
     *  offset+0 = -20  offset+1 = -5  offset+2 = -1
     *  offset+3 = Wert-Anzeige (Mitte)
     *  offset+4 = Label (Kanal-Name)  [nicht nötig – Wert reicht]
     *  offset+5 = +1   offset+6 = +5  offset+7 = +20
     *  offset+8 = Border
     */
    private void buildSlider(Inventory inv, int rowStart, String label, int value, char channel) {
        // –– Buttons
        inv.setItem(rowStart + 1, item(Material.RED_TERRACOTTA,  "§c-20 " + label, "§7Channel: " + channel));
        inv.setItem(rowStart + 2, item(Material.TERRACOTTA,      "§c-5 "  + label, "§7Channel: " + channel));
        inv.setItem(rowStart + 3, item(Material.BRICK,           "§c-1 "  + label, "§7Channel: " + channel));
        // Wert-Anzeige
        inv.setItem(rowStart + 4, item(Material.COMPARATOR, label + " §f" + value,
                "§7Aktueller Wert: §f" + value,
                "§7Min: §f0 §7Max: §f255"));
        // ++ Buttons
        inv.setItem(rowStart + 5, item(Material.GREEN_TERRACOTTA, "§a+1 "  + label, "§7Channel: " + channel));
        inv.setItem(rowStart + 6, item(Material.TERRACOTTA,       "§a+5 "  + label, "§7Channel: " + channel));
        inv.setItem(rowStart + 7, item(Material.LIME_TERRACOTTA,  "§a+20 " + label, "§7Channel: " + channel));
    }

    // ══════════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════════

    /** Erstellt ein Leder-Rüstungsteil mit der gegebenen Farbe als Vorschau. */
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
        inv.setItem(BTN_PAGE, item(Material.PAPER, "§7Seite §f" + (currentPage+1) + " §7/ §f" + totalPages));
        if (currentPage < totalPages - 1)
            inv.setItem(BTN_NEXT, item(Material.ARROW, "§e§lNächste Seite →",
                    "§7Seite " + (currentPage+2) + " / " + totalPages));
    }

    static void border(Inventory inv, Material mat) {
        ItemStack glass = new ItemStack(mat);
        ItemMeta m = glass.getItemMeta();
        m.setDisplayName(" ");
        glass.setItemMeta(m);
        int size = inv.getSize();
        int rows = size / 9;
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, glass);
            inv.setItem(size - 9 + i, glass);
        }
        for (int row = 1; row < rows-1; row++) {
            inv.setItem(row * 9,     glass);
            inv.setItem(row * 9 + 8, glass);
        }
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
        LEATHER   ("Leder",       "§6", Material.LEATHER_HELMET,   Material.LEATHER_CHESTPLATE,   Material.LEATHER_LEGGINGS,   Material.LEATHER_BOOTS),
        CHAINMAIL ("Kettenhemd",  "§7", Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS),
        IRON      ("Eisen",       "§f", Material.IRON_HELMET,      Material.IRON_CHESTPLATE,      Material.IRON_LEGGINGS,      Material.IRON_BOOTS),
        GOLDEN    ("Gold",        "§e", Material.GOLDEN_HELMET,    Material.GOLDEN_CHESTPLATE,    Material.GOLDEN_LEGGINGS,    Material.GOLDEN_BOOTS),
        DIAMOND   ("Diamant",     "§b", Material.DIAMOND_HELMET,   Material.DIAMOND_CHESTPLATE,   Material.DIAMOND_LEGGINGS,   Material.DIAMOND_BOOTS),
        NETHERITE ("Netherite",   "§8", Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS),
        TURTLE    ("Schildkröte", "§a", Material.TURTLE_HELMET,    null,                          null,                        null);

        public final String displayName;
        public final String color;
        private final Material helmet, chest, legs, boots;

        ArmorType(String dn, String color, Material h, Material c, Material l, Material b) {
            this.displayName = dn; this.color = color;
            this.helmet = h; this.chest = c; this.legs = l; this.boots = b;
        }

        public Material getMaterial(PlayerSession.Slot slot) {
            return switch (slot) {
                case HELMET     -> helmet != null ? helmet : Material.BARRIER;
                case CHESTPLATE -> chest  != null ? chest  : Material.BARRIER;
                case LEGGINGS   -> legs   != null ? legs   : Material.BARRIER;
                case BOOTS      -> boots  != null ? boots  : Material.BARRIER;
            };
        }

        public boolean isLeather() { return this == LEATHER; }

        public static ArmorType fromMaterial(Material m) {
            for (ArmorType t : values())
                for (PlayerSession.Slot s : PlayerSession.Slot.values())
                    if (t.getMaterial(s) == m) return t;
            return null;
        }
    }
}
