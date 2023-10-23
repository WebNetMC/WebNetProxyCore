package dev.foxikle.mainproxyplugin.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.VelocityBrigadierMessage;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.foxikle.foxrankvelocity.FoxRankAPI;
import dev.foxikle.foxrankvelocity.Rank;
import dev.foxikle.mainproxyplugin.MainProxy;
import dev.foxikle.mainproxyplugin.managers.FriendManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.Locale;
import java.util.UUID;

public class FriendCommand {
    public static BrigadierCommand createBrigadierCommand(final ProxyServer proxy, final MainProxy plugin) {


        // BrigadierCommand implements Command
        return new BrigadierCommand(getNode(proxy, plugin));
    }

    private static LiteralCommandNode<CommandSource> getNode(final ProxyServer proxy, final MainProxy plugin) {
        return LiteralArgumentBuilder.<CommandSource>literal("friend")
                .requires(source -> source instanceof Player)
                .executes(context -> {
                    //todo: Help menu or something
                    return Command.SINGLE_SUCCESS;
                })
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("subcommand", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            builder.suggest("add", VelocityBrigadierMessage.tooltip(Component.text("Sends a friend request.")));
                            builder.suggest("accept", VelocityBrigadierMessage.tooltip(Component.text("Accepts a friend request.")));
                            builder.suggest("remove", VelocityBrigadierMessage.tooltip(Component.text("Removes a friend.")));
                            builder.suggest("list", VelocityBrigadierMessage.tooltip(Component.text("Lists your friends.")));
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            String argumentProvided = context.getArgument("subcommand", String.class);
                            switch (argumentProvided.toUpperCase(Locale.ROOT)) {
                                case "LIST" -> {
                                    Player player = (Player) context.getSource();
                                    Component base = Component.text("").decoration(TextDecoration.STRIKETHROUGH, TextDecoration.State.FALSE)
                                            .append(FriendManager.LINE).appendNewline()
                                            .append(Component.text("                         Your Friends:", NamedTextColor.GOLD)).appendNewline().appendNewline();
                                    for (UUID uuid : plugin.getFriendManager().getFriends(player.getUniqueId())) {
                                        Rank friendRank = FoxRankAPI.getPlayerRank(uuid);
                                        boolean online = proxy.getPlayer(uuid).isPresent();
                                        Player friend = null;
                                        if (online) friend = proxy.getPlayer(uuid).get();
                                        base = base.append(friendRank.getPrefix())
                                                .append(Component.text(plugin.getDatabase().getName(uuid), friendRank.getColor()))
                                                .append(online ?
                                                        Component.text(" on server " + friend.getCurrentServer().get().getServerInfo().getName(), NamedTextColor.YELLOW) :
                                                        Component.text(" currently offline.", NamedTextColor.RED, TextDecoration.ITALIC))
                                                .appendNewline();
                                    }
                                    if(plugin.getFriendManager().getFriends(player.getUniqueId()).isEmpty()){
                                        base = base.append(Component.text("                  You have no friends :(", NamedTextColor.RED)).appendNewline()
                                                .append(Component.text("     Type '/friend add <player>' to invite some!", NamedTextColor.YELLOW, TextDecoration.ITALIC));
                                    }
                                    base = base.appendNewline().append(FriendManager.LINE);

                                    player.sendMessage(base);
                                }
                                case "ADD", "REMOVE", "ACCEPT" -> {
                                    context.getSource().sendMessage(Component.text("Insufficient arguments provided!", NamedTextColor.RED));
                                    return Command.SINGLE_SUCCESS;
                                }
                                default -> {
                                    // send a friend request to this player

                                    if (plugin.getProxy().getPlayer(argumentProvided).isPresent()) {
                                        Player sender = (Player) context.getSource();
                                        Player target = plugin.getProxy().getPlayer(argumentProvided).get();
                                        plugin.getFriendManager().dispachRequest(sender.getUniqueId(), target.getUniqueId());
                                    } else {
                                        Player sender = (Player) context.getSource();
                                        sender.sendMessage(Component.text("'" + argumentProvided + "' is not a valid player or sub command!", NamedTextColor.RED));
                                    }

                                }
                            }
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("specifier", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    proxy.getAllPlayers().forEach(player -> builder.suggest(player.getUsername()));
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    Player sender = (Player) context.getSource();
                                    String argumentProvided = context.getArgument("specifier", String.class);
                                    Player target;
                                    try {
                                        UUID uuid = UUID.fromString(argumentProvided);
                                        if (proxy.getPlayer(uuid).isEmpty())
                                            context.getSource().sendMessage(Component.text("This player doesn't exist!", NamedTextColor.RED));
                                        target = proxy.getPlayer(uuid).get();
                                    } catch (IllegalArgumentException ignored) {
                                        if (proxy.getPlayer(argumentProvided).isEmpty())
                                            context.getSource().sendMessage(Component.text("This player doesn't exist!", NamedTextColor.RED));
                                        target = proxy.getPlayer(argumentProvided).get();
                                    }
                                    if (target.getUniqueId() == sender.getUniqueId()) {
                                        sender.sendMessage(Component.text().decoration(TextDecoration.STRIKETHROUGH, TextDecoration.State.FALSE)
                                                .append(FriendManager.LINE).appendNewline()
                                                .append(Component.text("You cannot add yourself as a friend!", NamedTextColor.YELLOW)).appendNewline()
                                                .append(FriendManager.LINE));
                                        return Command.SINGLE_SUCCESS;
                                    }
                                    String sub = context.getArgument("subcommand", String.class);

                                    switch (sub.toLowerCase()) {
                                        case "add" ->
                                                plugin.getFriendManager().dispachRequest(sender.getUniqueId(), target.getUniqueId());
                                        case "remove" -> {
                                            if (plugin.getFriendManager().isFriend(target.getUniqueId(), sender.getUniqueId())) {
                                                plugin.getFriendManager().removeFriend(sender.getUniqueId(), target.getUniqueId());
                                            } else {
                                                sender.sendMessage(
                                                        FoxRankAPI.getPlayerRank(target.getUniqueId()).getPrefix()
                                                                .append(Component.text(target.getUsername(), FoxRankAPI.getPlayerRank(target.getUniqueId()).getColor()))
                                                                .append(Component.text(" isn't on your friends list!", NamedTextColor.YELLOW))
                                                );
                                            }
                                        }
                                        case "deny" -> {
                                            if (plugin.getFriendManager().getRequest(sender.getUniqueId(), target.getUniqueId()) != null) {
                                                plugin.getFriendManager().declineRequest(sender.getUniqueId(), target.getUniqueId());
                                            } else {
                                                sender.sendMessage(
                                                        FoxRankAPI.getPlayerRank(target.getUniqueId()).getPrefix()
                                                                .append(Component.text(target.getUsername(), FoxRankAPI.getPlayerRank(target.getUniqueId()).getColor()))
                                                                .append(Component.text(" has not sent you a friend request recently!", NamedTextColor.YELLOW))
                                                );
                                            }
                                        }
                                        case "accept" -> {
                                            if (plugin.getFriendManager().getRequest(sender.getUniqueId(), target.getUniqueId()) != null) {
                                                plugin.getFriendManager().acceptRequest(sender.getUniqueId(), target.getUniqueId());
                                            } else {
                                                sender.sendMessage(
                                                        FoxRankAPI.getPlayerRank(target.getUniqueId()).getPrefix()
                                                                .append(Component.text(target.getUsername(), FoxRankAPI.getPlayerRank(target.getUniqueId()).getColor()))
                                                                .append(Component.text(" has not sent you a friend request recently!", NamedTextColor.YELLOW))
                                                );
                                            }
                                        }
                                    }

                                    return Command.SINGLE_SUCCESS;
                                }))

                )
                .build();
    }
}
