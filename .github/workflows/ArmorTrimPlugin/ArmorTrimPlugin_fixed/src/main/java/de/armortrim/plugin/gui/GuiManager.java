package de.armortrim.plugin.gui;

import de.armortrim.plugin.ArmorTrimPlugin;
import de.armortrim.plugin.manager.RankManager;
import de.armortrim.plugin.manager.TrimManager;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.util.*;

public class GuiManager implements Listener {

    private final ArmorTrimPlugin plugin;
    private final RankManager rm;
    private final TrimManager tm;
    private final GuiBuilder  builder;

    private final Map<UUID, PlayerSession> sessions = new HashMap<>();

    public GuiManager(ArmorTrimPlugin plugin) {
        this.plugin  = plugin;
        this.rm      = plugin.getRankManager();
        this.tm      = plugin.getTrimManager();
        this.builder = new GuiBuilder(plugin);
    }

    // ── Einstieg ─────────────────────────────────────────────
    public void open(Player player) {
        PlayerSession session = new PlayerSession();
        sessions.put(player.getUniqueId(), session);
        player.openInventory(builder.slotPage(player));
    }

    // ══════════════════════════════════════════════════════════
    //  Click Handler
    // ══════════════════════════════════════════════════════════
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        if (!sessions.containsKey(uuid)) return;
        if (event.getClickedInventory() == null) return;
        if (!event.getClickedInventory().equals(event.getView().getTopInventory())) return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        PlayerSession session = sessions.get(uuid);
        String title = event.getView().getTitle();
        int slot = event.getSlot();

        switch (session.getPage()) {
            case SLOT_SELECT   -> handleSlotSelect(player, session, slot);
            case ARMOR_SELECT  -> handleArmorSelect(player, session, slot, clicked);
            case TRIM_PATTERN  -> handlePatternSelect(player, session, slot, clicked);
            case TRIM_MATERIAL -> handleMaterialSelect(player, session, slot, clicked);
            case COLOR_SELECT  -> handleColorSelect(player, session, slot, clicked);
        }
    }

    // ══════════════════════════════════════════════════════════
    //  SLOT SELECT
    // ══════════════════════════════════════════════════════════
    private void handleSlotSelect(Player player, PlayerSession session, int slot) {
        switch (slot) {
            case 10 -> goToArmorPage(player, session, PlayerSession.Slot.HELMET);
            case 12 -> goToArmorPage(player, session, PlayerSession.Slot.CHESTPLATE);
            case 14 -> goToArmorPage(player, session, PlayerSession.Slot.LEGGINGS);
            case 16 -> goToArmorPage(player, session, PlayerSession.Slot.BOOTS);
            case 26 -> closeGui(player);
        }
    }

    private void goToArmorPage(Player player, PlayerSession session, PlayerSession.Slot armorSlot) {
        session.setSlot(armorSlot);
        session.setPage(PlayerSession.Page.ARMOR_SELECT);
        click(player);
        player.openInventory(builder.armorPage(armorSlot));
    }

    // ══════════════════════════════════════════════════════════
    //  ARMOR SELECT
    // ══════════════════════════════════════════════════════════
    private void handleArmorSelect(Player player, PlayerSession session, int slot, ItemStack clicked) {
        if (slot == 18) { // Zurück
            session.setPage(PlayerSession.Page.SLOT_SELECT);
            click(player);
            player.openInventory(builder.slotPage(player));
            return;
        }
        if (slot == 26) { closeGui(player); return; }

        // Welcher ArmorType wurde geklickt?
        Material mat = clicked.getType();
        GuiBuilder.ArmorType type = GuiBuilder.ArmorType.fromMaterial(mat);
        if (type == null || mat == Material.BARRIER) {
            player.sendMessage("§cDieses Rüstungsteil existiert nicht für diesen Slot.");
            return;
        }

        Material armorMat = type.getMaterial(session.getSlot());
        session.setChosenArmor(armorMat);
        session.setLeather(type.isLeather());

        click(player);

        if (type.isLeather()) {
            // → Farb-GUI
            session.setPage(PlayerSession.Page.COLOR_SELECT);
            player.openInventory(builder.colorPage(player, session));
        } else {
            // → Trim Pattern GUI
            session.setPage(PlayerSession.Page.TRIM_PATTERN);
            session.setPatternPage(0);
            player.openInventory(builder.patternPage(player, session));
        }
    }

    // ══════════════════════════════════════════════════════════
    //  TRIM PATTERN
    // ══════════════════════════════════════════════════════════
    private void handlePatternSelect(Player player, PlayerSession session, int slot, ItemStack clicked) {
        if (slot == GuiBuilder.BTN_BACK) {
            session.setPage(PlayerSession.Page.ARMOR_SELECT);
            click(player);
            player.openInventory(builder.armorPage(session.getSlot()));
            return;
        }
        if (slot == GuiBuilder.BTN_CLOSE) { closeGui(player); return; }
        if (slot == GuiBuilder.BTN_PREV) {
            session.setPatternPage(Math.max(0, session.getPatternPage() - 1));
            player.openInventory(builder.patternPage(player, session)); return;
        }
        if (slot == GuiBuilder.BTN_NEXT) {
            int max = (int) Math.ceil((double) tm.getAllPatterns().size() / GuiBuilder.GRID_SIZE);
            session.setPatternPage(Math.min(max-1, session.getPatternPage() + 1));
            player.openInventory(builder.patternPage(player, session)); return;
        }

        // Grid-Klick?
        int gridIdx = gridIndex(slot);
        if (gridIdx < 0) return;
        int patternIdx = session.getPatternPage() * GuiBuilder.GRID_SIZE + gridIdx;
        List<TrimPattern> patterns = tm.getAllPatterns();
        if (patternIdx >= patterns.size()) return;

        TrimPattern p = patterns.get(patternIdx);
        String required = tm.requiredRankForPattern(p);
        if (!rm.hasRank(player, required)) {
            player.sendMessage("§cDu benötigst " + rm.displayRank(required) + " §cfür diesen Pattern!");
            return;
        }

        // Toggle
        if (session.getPattern() != null && session.getPattern().key().equals(p.key())) {
            session.setPattern(null);
        } else {
            session.setPattern(p);
            player.sendMessage("§aPattern: §b" + tm.patternName(p) + " §agewählt.");
            // Weiter zu Material
            session.setPage(PlayerSession.Page.TRIM_MATERIAL);
            session.setMaterialPage(0);
            click(player);
            player.openInventory(builder.materialPage(player, session));
            return;
        }
        click(player);
        player.openInventory(builder.patternPage(player, session));
    }

    // ══════════════════════════════════════════════════════════
    //  TRIM MATERIAL
    // ══════════════════════════════════════════════════════════
    private void handleMaterialSelect(Player player, PlayerSession session, int slot, ItemStack clicked) {
        if (slot == GuiBuilder.BTN_BACK) {
            session.setPage(PlayerSession.Page.TRIM_PATTERN);
            click(player);
            player.openInventory(builder.patternPage(player, session)); return;
        }
        if (slot == GuiBuilder.BTN_CLOSE) { closeGui(player); return; }
        if (slot == GuiBuilder.BTN_PREV) {
            session.setMaterialPage(Math.max(0, session.getMaterialPage() - 1));
            player.openInventory(builder.materialPage(player, session)); return;
        }
        if (slot == GuiBuilder.BTN_NEXT) {
            int max = (int) Math.ceil((double) tm.getAllMaterials().size() / GuiBuilder.GRID_SIZE);
            session.setMaterialPage(Math.min(max-1, session.getMaterialPage() + 1));
            player.openInventory(builder.materialPage(player, session)); return;
        }
        // Trim anwenden Button
        if (slot == 53 && session.getPattern() != null && session.getMaterial() != null) {
            applyTrim(player, session);
            return;
        }

        int gridIdx = gridIndex(slot);
        if (gridIdx < 0) return;
        int matIdx = session.getMaterialPage() * GuiBuilder.GRID_SIZE + gridIdx;
        List<TrimMaterial> materials = tm.getAllMaterials();
        if (matIdx >= materials.size()) return;

        TrimMaterial m = materials.get(matIdx);
        String required = tm.requiredRankForMaterial(m);
        if (!rm.hasRank(player, required)) {
            player.sendMessage("§cDu benötigst " + rm.displayRank(required) + " §cfür dieses Material!");
            return;
        }

        if (session.getMaterial() != null && session.getMaterial().key().equals(m.key())) {
            session.setMaterial(null);
        } else {
            session.setMaterial(m);
            player.sendMessage("§aMaterial: §d" + tm.materialName(m) + " §agewählt.");
            // Wenn beide gewählt → direkt anwenden
            if (session.getPattern() != null) {
                applyTrim(player, session);
                return;
            }
        }
        click(player);
        player.openInventory(builder.materialPage(player, session));
    }

    // ══════════════════════════════════════════════════════════
    //  COLOR SELECT
    // ══════════════════════════════════════════════════════════
    private void handleColorSelect(Player player, PlayerSession session, int slot, ItemStack clicked) {
        String name = clicked.getItemMeta() != null ? clicked.getItemMeta().getDisplayName() : "";

        if (slot == GuiBuilder.BTN_BACK) {
            session.setPage(PlayerSession.Page.ARMOR_SELECT);
            click(player);
            player.openInventory(builder.armorPage(session.getSlot())); return;
        }
        if (slot == 53) { // Anwenden
            applyLeatherColor(player, session); return;
        }

        // Preset-Farben (slots 45-52)
        if (slot >= 45 && slot <= 52) {
            Color[] presets = {
                Color.RED, Color.ORANGE, Color.YELLOW, Color.LIME,
                Color.AQUA, Color.BLUE, Color.PURPLE, Color.WHITE
            };
            Color c = presets[slot - 45];
            session.setLeatherColor(c.getRed(), c.getGreen(), c.getBlue());
            click(player);
            player.openInventory(builder.colorPage(player, session)); return;
        }

        // Slider-Zeilen: Rot(18-26), Grün(27-35), Blau(36-44)
        int delta = parseDelta(name);
        if (delta == 0) return;

        int rowStart = (slot / 9) * 9;
        int r = session.getLeatherRed();
        int g = session.getLeatherGreen();
        int b = session.getLeatherBlue();

        if      (rowStart == 18) session.setLeatherColor(r + delta, g,         b);
        else if (rowStart == 27) session.setLeatherColor(r,         g + delta, b);
        else if (rowStart == 36) session.setLeatherColor(r,         g,         b + delta);

        click(player);
        player.openInventory(builder.colorPage(player, session));
    }

    private int parseDelta(String name) {
        if (name.contains("-20")) return -20;
        if (name.contains("-5"))  return -5;
        if (name.contains("-1"))  return -1;
        if (name.contains("+1"))  return +1;
        if (name.contains("+5"))  return +5;
        if (name.contains("+20")) return +20;
        return 0;
    }

    // ══════════════════════════════════════════════════════════
    //  APPLY – Trim
    // ══════════════════════════════════════════════════════════
    private void applyTrim(Player player, PlayerSession session) {
        if (session.getPattern() == null || session.getMaterial() == null || session.getSlot() == null) {
            player.sendMessage("§cBitte wähle Pattern, Material und Slot!"); return;
        }

        // Doppelt-Check Rang
        if (!rm.hasRank(player, tm.requiredRankForPattern(session.getPattern()))) {
            player.sendMessage("§cKein Rang für diesen Pattern!"); return;
        }
        if (!rm.hasRank(player, tm.requiredRankForMaterial(session.getMaterial()))) {
            player.sendMessage("§cKein Rang für dieses Material!"); return;
        }

        // Slot holen und Rüstungsteil erstellen / anpassen
        ItemStack[] armor = player.getInventory().getArmorContents();
        int idx = session.getSlot().armorIndex;
        ItemStack piece = armor[idx];

        // Wenn kein Item oder falsches Item → neues erstellen
        Material targetMat = session.getChosenArmor();
        if (piece == null || piece.getType() == Material.AIR || !isSameSlot(piece.getType(), session.getSlot())) {
            piece = new ItemStack(targetMat);
        }

        if (!(piece.getItemMeta() instanceof ArmorMeta armorMeta)) {
            player.sendMessage("§cDieses Item unterstützt keine Trims!"); return;
        }

        ArmorTrim trim = new ArmorTrim(session.getMaterial(), session.getPattern());
        armorMeta.setTrim(trim);
        piece.setItemMeta(armorMeta);
        armor[idx] = piece;
        player.getInventory().setArmorContents(armor);

        success(player);
        player.sendMessage("§8§m──────────────────────────────────");
        player.sendMessage("§6§l✦ Trim angewendet! ✦");
        player.sendMessage("§7Slot:    §e" + session.getSlot().display);
        player.sendMessage("§7Rüstung: §f" + targetMat.name().replace("_"," ").toLowerCase());
        player.sendMessage("§7Pattern: §b" + tm.patternName(session.getPattern()));
        player.sendMessage("§7Material:§d " + tm.materialName(session.getMaterial()));
        player.sendMessage("§8§m──────────────────────────────────");

        sessions.remove(player.getUniqueId());
        player.closeInventory();
    }

    // ══════════════════════════════════════════════════════════
    //  APPLY – Leather Color
    // ══════════════════════════════════════════════════════════
    private void applyLeatherColor(Player player, PlayerSession session) {
        if (session.getSlot() == null || session.getChosenArmor() == null) {
            player.sendMessage("§cFehler: Kein Slot gewählt!"); return;
        }

        ItemStack[] armor = player.getInventory().getArmorContents();
        int idx = session.getSlot().armorIndex;
        ItemStack piece = armor[idx];

        Material targetMat = session.getChosenArmor();

        // Neues Leder-Item wenn nötig
        if (piece == null || piece.getType() == Material.AIR || !piece.getType().equals(targetMat)) {
            piece = new ItemStack(targetMat);
        }

        if (!(piece.getItemMeta() instanceof LeatherArmorMeta meta)) {
            player.sendMessage("§cDieses Item ist kein Leder-Rüstungsteil!"); return;
        }

        meta.setColor(session.getLeatherColor());
        piece.setItemMeta(meta);
        armor[idx] = piece;
        player.getInventory().setArmorContents(armor);

        int r = session.getLeatherRed(), g = session.getLeatherGreen(), b = session.getLeatherBlue();
        success(player);
        player.sendMessage("§8§m──────────────────────────────────");
        player.sendMessage("§6§l✦ Leder-Farbe angewendet! ✦");
        player.sendMessage("§7Slot:    §e" + session.getSlot().display);
        player.sendMessage("§7Farbe:   §fRGB(" + r + ", " + g + ", " + b + ")");
        player.sendMessage("§7Hex:     §f#" + String.format("%02X%02X%02X", r, g, b));
        player.sendMessage("§8§m──────────────────────────────────");

        sessions.remove(player.getUniqueId());
        player.closeInventory();
    }

    // ══════════════════════════════════════════════════════════
    //  Util
    // ══════════════════════════════════════════════════════════

    private boolean isSameSlot(Material mat, PlayerSession.Slot slot) {
        String name = mat.name();
        return switch (slot) {
            case HELMET      -> name.endsWith("_HELMET")      || name.equals("TURTLE_HELMET");
            case CHESTPLATE  -> name.endsWith("_CHESTPLATE");
            case LEGGINGS    -> name.endsWith("_LEGGINGS");
            case BOOTS       -> name.endsWith("_BOOTS");
        };
    }

    private int gridIndex(int slot) {
        for (int i = 0; i < GuiBuilder.GRID.length; i++)
            if (GuiBuilder.GRID[i] == slot) return i;
        return -1;
    }

    private void closeGui(Player player) {
        sessions.remove(player.getUniqueId());
        player.closeInventory();
    }

    private void click(Player p) {
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
    }

    private void success(Player p) {
        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_USE,          1f, 1.2f);
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
    }

    public Map<UUID, PlayerSession> getSessions() { return sessions; }
}
