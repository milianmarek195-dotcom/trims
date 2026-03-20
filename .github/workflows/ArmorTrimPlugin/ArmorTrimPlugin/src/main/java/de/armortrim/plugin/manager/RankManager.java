package de.armortrim.plugin.manager;

import de.armortrim.plugin.ArmorTrimPlugin;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.List;

public class RankManager {

    private static final List<String> HIERARCHY = List.of("default", "vip", "mod", "admin", "owner");

    private final ArmorTrimPlugin plugin;
    private LuckPerms luckPerms;
    private final boolean lpEnabled;

    public RankManager(ArmorTrimPlugin plugin) {
        this.plugin = plugin;
        this.luckPerms = trySetupLP();
        this.lpEnabled = luckPerms != null;
        plugin.getLogger().info(lpEnabled ? "[RankManager] LuckPerms verbunden." : "[RankManager] Kein LuckPerms – nutze Bukkit-Permissions.");
    }

    private LuckPerms trySetupLP() {
        if (plugin.getServer().getPluginManager().getPlugin("LuckPerms") == null) return null;
        RegisteredServiceProvider<LuckPerms> rsp = plugin.getServer().getServicesManager().getRegistration(LuckPerms.class);
        return rsp == null ? null : rsp.getProvider();
    }

    /** Gibt den effektiven Rang des Spielers zurück. */
    public String getPlayerRank(Player player) {
        if (player.hasPermission("armortrim.bypass") || player.isOp()) return "owner";
        return lpEnabled ? getRankLP(player) : getRankBukkit(player);
    }

    private String getRankLP(Player player) {
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) return "default";

        String best = "default";
        // Alle geerbten Gruppen durchsuchen
        for (var group : user.getInheritedGroups(QueryOptions.nonContextual())) {
            String mapped = mapGroup(group.getName().toLowerCase());
            if (mapped != null && level(mapped) > level(best)) best = mapped;
        }
        // Primary group auch prüfen
        String prim = mapGroup(user.getPrimaryGroup().toLowerCase());
        if (prim != null && level(prim) > level(best)) best = prim;
        return best;
    }

    private String mapGroup(String g) {
        return switch (g) {
            case "owner", "inhaber", "besitzer", "co-owner", "coowner" -> "owner";
            case "admin", "administrator", "srmod", "sr-mod", "senior-mod" -> "admin";
            case "mod", "moderator", "supporter", "helper" -> "mod";
            case "vip", "vip+", "mvp", "premium", "donator" -> "vip";
            case "default", "member", "spieler", "user", "player" -> "default";
            default -> null;
        };
    }

    private String getRankBukkit(Player player) {
        if (player.hasPermission("armortrim.rank.owner")) return "owner";
        if (player.hasPermission("armortrim.rank.admin")) return "admin";
        if (player.hasPermission("armortrim.rank.mod"))   return "mod";
        if (player.hasPermission("armortrim.rank.vip"))   return "vip";
        return "default";
    }

    public int level(String rank) {
        int i = HIERARCHY.indexOf(rank.toLowerCase());
        return i < 0 ? 0 : i;
    }

    public boolean hasRank(Player player, String required) {
        return level(getPlayerRank(player)) >= level(required);
    }

    /** Farbiges Display-Prefix für einen Rang. */
    public String displayRank(String rank) {
        return switch (rank.toLowerCase()) {
            case "owner" -> "§4§lOwner";
            case "admin" -> "§c§lAdmin";
            case "mod"   -> "§a§lMod";
            case "vip"   -> "§6§lVIP";
            default      -> "§7Default";
        };
    }

    public boolean isLpEnabled() { return lpEnabled; }
}
