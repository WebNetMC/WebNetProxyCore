package net.cytonic.mainproxyplugin.commands;

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
import net.cytonic.mainproxyplugin.MainProxy;
import net.cytonic.mainproxyplugin.data.enums.ChatChannel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import java.util.Locale;

public class ChatChannelCommand {
    public static BrigadierCommand createBrigadierCommand(final ProxyServer proxy, final MainProxy plugin) {
        return new BrigadierCommand(getNode(proxy, plugin));
    }

    private static LiteralCommandNode<CommandSource> getNode(final ProxyServer proxy, final MainProxy plugin) {
        return LiteralArgumentBuilder.<CommandSource>literal("chat")
                .requires(source -> source instanceof Player)
                .executes(context -> {
                    //todo: Help menu or something
                    return Command.SINGLE_SUCCESS;
                })
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("subcommand", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            Player player = (Player) ctx.getSource();
                            Rank rank = FoxRankAPI.getPlayerRank(player.getUniqueId());
                            if(rank.getPermissionNodes().contains("webnet.chat.staff"))
                                builder.suggest("STAFF", VelocityBrigadierMessage.tooltip(Component.text("Sets your default chat channel to STAFF chat.")));
                            if(rank.getPermissionNodes().contains("webnet.chat.admin"))
                                builder.suggest("ADMIN", VelocityBrigadierMessage.tooltip(Component.text("Sets your default chat channel to ADMIN chat.")));
                            if(rank.getPermissionNodes().contains("webnet.chat.mod"))
                                builder.suggest("MOD", VelocityBrigadierMessage.tooltip(Component.text("Sets your default chat channel to MOD chat.")));
                            if(plugin.getPartyManager().getParty(player.getUniqueId()) != null)
                                builder.suggest("PARTY", VelocityBrigadierMessage.tooltip(Component.text("Sets your defualt chat channel to PARTY chat.")));
                            builder.suggest("ALL", VelocityBrigadierMessage.tooltip(Component.text("Sets your default chat channel to regular server chat.")));
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            Player player = (Player) context.getSource();
                            Rank rank = FoxRankAPI.getPlayerRank(player.getUniqueId());
                            String argumentProvided = context.getArgument("subcommand", String.class);
                            switch (argumentProvided.toUpperCase(Locale.ROOT)) {
                                case "ALL", "A" -> {
                                    plugin.getChatManager().setChannel(player.getUniqueId(), ChatChannel.ALL);
                                    player.sendMessage(
                                            Component.text("You are now in the ", NamedTextColor.GREEN)
                                                    .append(Component.text("ALL", NamedTextColor.GOLD))
                                                    .append(Component.text(" channel.", NamedTextColor.GREEN))
                                    );
                                }
                                case "ADMIN" -> {
                                    if (rank.getPermissionNodes().contains("webnet.chat.admin")){
                                        plugin.getChatManager().setChannel(player.getUniqueId(), ChatChannel.ADMIN);
                                        player.sendMessage(
                                                Component.text("You are now in the ", NamedTextColor.GREEN)
                                                        .append(Component.text("ADMIN", NamedTextColor.GOLD))
                                                        .append(Component.text(" channel.", NamedTextColor.GREEN))
                                        );
                                    } else {
                                        player.sendMessage(Component.text("You do not have access to this channel.", NamedTextColor.RED));
                                    }
                                }
                                case "MOD", "M" -> {
                                    if (rank.getPermissionNodes().contains("webnet.chat.mod")){
                                        plugin.getChatManager().setChannel(player.getUniqueId(), ChatChannel.MOD);
                                        player.sendMessage(
                                                Component.text("You are now in the ", NamedTextColor.GREEN)
                                                        .append(Component.text("MOD", NamedTextColor.GOLD))
                                                        .append(Component.text(" channel.", NamedTextColor.GREEN))
                                        );
                                    } else {
                                        player.sendMessage(Component.text("You do not have access to this channel.", NamedTextColor.RED));
                                    }
                                }
                                case "STAFF", "S" -> {
                                    if (rank.getPermissionNodes().contains("webnet.chat.staff")){
                                        plugin.getChatManager().setChannel(player.getUniqueId(), ChatChannel.STAFF);
                                        player.sendMessage(
                                                Component.text("You are now in the ", NamedTextColor.GREEN)
                                                        .append(Component.text("STAFF", NamedTextColor.GOLD))
                                                        .append(Component.text(" channel.", NamedTextColor.GREEN))
                                        );
                                    } else {
                                        player.sendMessage(Component.text("You do not have access to this channel.", NamedTextColor.RED));
                                    }
                                }
                                case "PARTY", "P" -> {
                                    if(plugin.getPartyManager().getParty(player.getUniqueId()) != null){
                                        plugin.getChatManager().setChannel(player.getUniqueId(), ChatChannel.PARTY);
                                        player.sendMessage(
                                                Component.text("You are now in the ", NamedTextColor.GREEN)
                                                        .append(Component.text("PARTY", NamedTextColor.GOLD))
                                                        .append(Component.text(" channel.", NamedTextColor.GREEN))
                                        );
                                    } else {
                                        player.sendMessage(Component.text("You are not in a party!", NamedTextColor.RED));
                                    }
                                }
                            }
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .build();
    }
}
