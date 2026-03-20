package de.armortrim.plugin.gui;

import de.armortrim.plugin.ArmorTrimPlugin;
import de.armortrim.plugin.manager.PlayerDataManager;
import de.armortrim.plugin.manager.RankManager;
import de.armortrim.plugin.manager.TrimManager;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.util.*;

public class GuiManager implements Listener {

    private final ArmorTrimPlugin  plugin;
    private final RankManager      rm;
    private final TrimManager      tm;
    private final GuiBuilder       builder;
    private final PlayerDataManager pdm;

    private final Map<UUID, PlayerSession> sessions = new HashMap<>();

    public GuiManager(ArmorTrimPlugin plugin) {
        this.plugin   = plugin;
        this.rm       = plugin.getRankManager();
        this.tm       = plugin.getTrimManager();
        this.builder  = new GuiBuilder(plugin);
        this.pdm      = plugin.getPlayerDataManager();
    }

    // ── Einstieg ─────────────────────────────────────────────
    public void open(Player player) {
        PlayerSession session = new PlayerSession();

        // Letzte Leder-Farbe laden
        Color saved = pdm.loadLeatherColor(player.getUniqueId());
        if (saved != null) session.setLeatherColor(saved);

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
        int slot = event.getSlot();

        switch (session.getPage()) {
            case SLOT_SELECT   -> handleSlotSelect(player, session, slot);
            case ARMOR_SELECT  -> handleArmorSelect(player, session, slot, clicked);
            case TRIM_PATTERN  -> handlePatternSelect(player, session, slot);
            case TRIM_MATERIAL -> handleMaterialSelect(player, session, slot);
            case COLOR_SELECT  -> handleColorSelect(player, session, slot);
        }
    }

    // ══════════════════════════════════════════════════════════
    //  SLOT SELECT
    // ══════════════════════════════════════════════════════════
    private void handleSlotSelect(Player player, PlayerSession session, int slot) {
        switch (slot) {
            case 10 -> goToArmorPage(player, session, PlayerSession.Slot.HELMET);
            case 12 -> goToArmorPage(player, session, PlayerSession.Slot.CHESTPLATE);
            case 13 -> goToElytra(player, session);
            case 14 -> goToArmorPage(player, session, PlayerSession.Slot.LEGGINGS);
            case 16 -> goToArmorPage(player, session, PlayerSession.Slot.BOOTS);
            case 26 -> closeGui(player);
        }
    }

    private void goToArmorPage(Player player, PlayerSession session, PlayerSession.Slot armorSlot) {
        session.setSlot(armorSlot);
        session.setElytra(false);
        session.setPage(PlayerSession.Page.ARMOR_SELECT);
        click(player);
        player.openInventory(builder.armorPage(armorSlot));
    }

    private void goToElytra(Player player, PlayerSession session) {
        session.setSlot(PlayerSession.Slot.ELYTRA);
        session.setElytra(true);
        session.setChosenArmor(Material.ELYTRA);
        session.setLeather(false);
        // Elytra → direkt zu Trim Pattern
        session.setPage(PlayerSession.Page.TRIM_PATTERN);
        session.setPatternPage(0);
        click(player);
        player.openInventory(builder.patternPage(player, session));
    }

    // ══════════════════════════════════════════════════════════
    //  ARMOR SELECT
    // ══════════════════════════════════════════════════════════
    private void handleArmorSelect(Player player, PlayerSession session, int slot, ItemStack clicked) {
        if (slot == 18) {
            session.setPage(PlayerSession.Page.SLOT_SELECT);
            click(player);
            player.openInventory(builder.slotPage(player));
            return;
        }
        if (slot == 26) { closeGui(player); return; }

        Material mat = clicked.getType();
        GuiBuilder.ArmorType type = GuiBuilder.ArmorType.fromMaterial(mat);
        if (type == null || mat == Material.BARRIER) return;

        session.setChosenArmor(type.getMaterial(session.getSlot()));
        session.setLeather(type.isLeather());
        click(player);

        if (type.isLeather()) {
            session.setPage(PlayerSession.Page.COLOR_SELECT);
            session.setColorPage(0);
            player.openInventory(builder.colorPage(player, session));
        } else {
            session.setPage(PlayerSession.Page.TRIM_PATTERN);
            session.setPatternPage(0);
            player.openInventory(builder.patternPage(player, session));
        }
    }

    // ══════════════════════════════════════════════════════════
    //  TRIM PATTERN
    // ══════════════════════════════════════════════════════════
    private void handlePatternSelect(Player player, PlayerSession session, int slot) {
        if (slot == GuiBuilder.BTN_BACK) {
            if (session.isElytra()) {
                session.setPage(PlayerSession.Page.SLOT_SELECT);
                player.openInventory(builder.slotPage(player));
            } else {
                session.setPage(PlayerSession.Page.ARMOR_SELECT);
                player.openInventory(builder.armorPage(session.getSlot()));
            }
            click(player); return;
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

        int gridIdx = gridIndex(slot);
        if (gridIdx < 0) return;
        int patternIdx = session.getPatternPage() * GuiBuilder.GRID_SIZE + gridIdx;
        List<TrimPattern> patterns = tm.getAllPatterns();
        if (patternIdx >= patterns.size()) return;

        TrimPattern p = patterns.get(patternIdx);
        if (!rm.hasRank(player, tm.requiredRankForPattern(p))) {
            player.sendMessage("§cDu benötigst " + rm.displayRank(tm.requiredRankForPattern(p)) + "§c!");
            return;
        }

        if (session.getPattern() != null && session.getPattern().key().equals(p.key())) {
            session.setPattern(null);
        } else {
            session.setPattern(p);
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
    private void handleMaterialSelect(Player player, PlayerSession session, int slot) {
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
        if (slot == 53 && session.getPattern() != null && session.getMaterial() != null) {
            applyTrim(player, session); return;
        }

        int gridIdx = gridIndex(slot);
        if (gridIdx < 0) return;
        int matIdx = session.getMaterialPage() * GuiBuilder.GRID_SIZE + gridIdx;
        List<TrimMaterial> materials = tm.getAllMaterials();
        if (matIdx >= materials.size()) return;

        TrimMaterial m = materials.get(matIdx);
        if (!rm.hasRank(player, tm.requiredRankForMaterial(m))) {
            player.sendMessage("§cDu benötigst " + rm.displayRank(tm.requiredRankForMaterial(m)) + "§c!");
            return;
        }

        if (session.getMaterial() != null && session.getMaterial().key().equals(m.key())) {
            session.setMaterial(null);
        } else {
            session.setMaterial(m);
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
    private void handleColorSelect(Player player, PlayerSession session, int slot) {
        if (slot == GuiBuilder.BTN_BACK) {
            session.setPage(PlayerSession.Page.ARMOR_SELECT);
            click(player);
            player.openInventory(builder.armorPage(session.getSlot())); return;
        }
        if (slot == GuiBuilder.BTN_CLOSE) { closeGui(player); return; }
        if (slot == GuiBuilder.BTN_PREV) {
            session.setColorPage(Math.max(0, session.getColorPage() - 1));
            player.openInventory(builder.colorPage(player, session)); return;
        }
        if (slot == GuiBuilder.BTN_NEXT) {
            List<GuiBuilder.LeatherColor> colors = builder.getConfiguredColors();
            int max = (int) Math.ceil((double) colors.size() / GuiBuilder.GRID_SIZE);
            session.setColorPage(Math.min(max-1, session.getColorPage() + 1));
            player.openInventory(builder.colorPage(player, session)); return;
        }
        if (slot == 53) {
            applyLeatherColor(player, session); return;
        }

        // Grid-Klick → Farbe auswählen
        int gridIdx = gridIndex(slot);
        if (gridIdx < 0) return;

        List<GuiBuilder.LeatherColor> colors = builder.getConfiguredColors();
        int colorIdx = session.getColorPage() * GuiBuilder.GRID_SIZE + gridIdx;
        if (colorIdx >= colors.size()) return;

        GuiBuilder.LeatherColor lc = colors.get(colorIdx);
        if (!rm.hasRank(player, lc.rank)) {
            player.sendMessage("§cDu benötigst " + rm.displayRank(lc.rank) + " §cfür diese Farbe!");
            return;
        }

        session.setLeatherColor(lc.color);
        player.sendMessage("§aFarbe ausgewählt: §f" + lc.name);
        click(player);
        player.openInventory(builder.colorPage(player, session));
    }

    // ══════════════════════════════════════════════════════════
    //  APPLY – Trim (auch für Elytra)
    // ══════════════════════════════════════════════════════════
    private void applyTrim(Player player, PlayerSession session) {
        if (session.getPattern() == null || session.getMaterial() == null) {
            player.sendMessage("§cBitte wähle Pattern und Material!"); return;
        }
        if (!rm.hasRank(player, tm.requiredRankForPattern(session.getPattern()))) {
            player.sendMessage("§cKein Rang für diesen Pattern!"); return;
        }
        if (!rm.hasRank(player, tm.requiredRankForMaterial(session.getMaterial()))) {
            player.sendMessage("§cKein Rang für dieses Material!"); return;
        }

        ItemStack piece;
        Material targetMat = session.getChosenArmor();

        if (session.isElytra()) {
            // Elytra liegt im Chestplate-Slot
            piece = player.getInventory().getChestplate();
            if (piece == null || piece.getType() != Material.ELYTRA) {
                piece = new ItemStack(Material.ELYTRA);
            }
        } else {
            ItemStack[] armor = player.getInventory().getArmorContents();
            int idx = session.getSlot().armorIndex;
            piece = armor[idx];
            if (piece == null || piece.getType() == Material.AIR || !isSameSlot(piece.getType(), session.getSlot())) {
                piece = new ItemStack(targetMat);
            }
        }

        if (!(piece.getItemMeta() instanceof ArmorMeta armorMeta)) {
            player.sendMessage("§cDieses Item unterstützt keine Trims!"); return;
        }

        armorMeta.setTrim(new ArmorTrim(session.getMaterial(), session.getPattern()));
        piece.setItemMeta(armorMeta);

        if (session.isElytra()) {
            player.getInventory().setChestplate(piece);
        } else {
            ItemStack[] armor = player.getInventory().getArmorContents();
            armor[session.getSlot().armorIndex] = piece;
            player.getInventory().setArmorContents(armor);
        }

        // Letzte Wahl speichern
        pdm.saveLastPattern(player.getUniqueId(), session.getPattern().key().value());
        pdm.saveLastMaterial(player.getUniqueId(), session.getMaterial().key().value());

        success(player);
        player.sendMessage("§8§m──────────────────────────────────");
        player.sendMessage("§6§l✦ Trim angewendet! ✦");
        player.sendMessage("§7Slot:     §e" + session.getSlot().display);
        player.sendMessage("§7Rüstung:  §f" + targetMat.name().replace("_"," ").toLowerCase());
        player.sendMessage("§7Pattern:  §b" + tm.patternName(session.getPattern()));
        player.sendMessage("§7Material: §d" + tm.materialName(session.getMaterial()));
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

        Material targetMat = session.getChosenArmor();
        ItemStack[] armor  = player.getInventory().getArmorContents();
        int idx = session.getSlot().armorIndex;
        ItemStack piece = armor[idx];

        if (piece == null || piece.getType() == Material.AIR || !piece.getType().equals(targetMat)) {
            piece = new ItemStack(targetMat);
        }

        if (!(piece.getItemMeta() instanceof LeatherArmorMeta meta)) {
            player.sendMessage("§cDieses Item ist kein Leder-Rüstungsteil!"); return;
        }

        Color color = session.getLeatherColor();
        meta.setColor(color);
        piece.setItemMeta(meta);
        armor[idx] = piece;
        player.getInventory().setArmorContents(armor);

        // Farbe dauerhaft speichern
        pdm.saveLeatherColor(player.getUniqueId(), color.getRed(), color.getGreen(), color.getBlue());

        success(player);
        player.sendMessage("§8§m──────────────────────────────────");
        player.sendMessage("§6§l✦ Leder-Farbe angewendet & gespeichert! ✦");
        player.sendMessage("§7Slot:  §e" + session.getSlot().display);
        player.sendMessage("§7Farbe: §f" + String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue()));
        player.sendMessage("§7RGB:   §f(" + color.getRed() + ", " + color.getGreen() + ", " + color.getBlue() + ")");
        player.sendMessage("§7Diese Farbe wird beim nächsten /trims vorgeladen!");
        player.sendMessage("§8§m──────────────────────────────────");

        sessions.remove(player.getUniqueId());
        player.closeInventory();
    }

    // ── Util ─────────────────────────────────────────────────
    private boolean isSameSlot(Material mat, PlayerSession.Slot slot) {
        String name = mat.name();
        return switch (slot) {
            case HELMET     -> name.endsWith("_HELMET") || name.equals("TURTLE_HELMET");
            case CHESTPLATE -> name.endsWith("_CHESTPLATE");
            case LEGGINGS   -> name.endsWith("_LEGGINGS");
            case BOOTS      -> name.endsWith("_BOOTS");
            case ELYTRA     -> mat == Material.ELYTRA;
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

    private void click(Player p)   { p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f); }
    private void success(Player p) {
        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1.2f);
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
    }
}
