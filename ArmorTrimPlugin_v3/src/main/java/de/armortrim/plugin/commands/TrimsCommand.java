package de.armortrim.plugin.commands;

import de.armortrim.plugin.ArmorTrimPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class TrimsCommand implements CommandExecutor, TabCompleter {

    private final ArmorTrimPlugin plugin;

    public TrimsCommand(ArmorTrimPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cNur Spieler können diesen Befehl benutzen!");
            return true;
        }
        if (!player.hasPermission("armortrim.use")) {
            player.sendMessage("§cKeine Berechtigung!");
            return true;
        }

        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "reload" -> {
                    if (!player.hasPermission("armortrim.admin")) {
                        player.sendMessage("§cKeine Berechtigung!"); return true;
                    }
                    plugin.reloadConfig();
                    player.sendMessage("§aKonfiguration neu geladen!");
                    return true;
                }
                case "rank" -> {
                    String rank = plugin.getRankManager().getPlayerRank(player);
                    player.sendMessage("§7Dein Rang: " + plugin.getRankManager().displayRank(rank));
                    player.sendMessage("§7LuckPerms: §a" + plugin.getRankManager().isLpEnabled());
                    return true;
                }
            }
        }

        plugin.getGuiManager().open(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            List<String> opts = new java.util.ArrayList<>(List.of("rank"));
            if (sender.hasPermission("armortrim.admin")) opts.add("reload");
            return opts.stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }
        return Collections.emptyList();
    }
}
