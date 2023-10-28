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
import dev.foxikle.mainproxyplugin.data.enums.DisbandReason;
import dev.foxikle.mainproxyplugin.data.enums.TransferReason;
import dev.foxikle.mainproxyplugin.data.objects.Party;
import dev.foxikle.mainproxyplugin.managers.PartyManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class PartyCommand {
    public static BrigadierCommand createBrigadierCommand(final ProxyServer proxy, final MainProxy plugin) {


        // BrigadierCommand implements Command
        return new BrigadierCommand(getNode(proxy, plugin));
    }

    private static LiteralCommandNode<CommandSource> getNode(final ProxyServer proxy, final MainProxy plugin) {
        return LiteralArgumentBuilder.<CommandSource>literal("party")
                .requires(source -> source instanceof Player)
                .executes(context -> {
                    //todo: Help menu or something
                    return Command.SINGLE_SUCCESS;
                })
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("subcommand", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            builder.suggest("invite", VelocityBrigadierMessage.tooltip(Component.text("Sends a party invite.")));
                            builder.suggest("kick", VelocityBrigadierMessage.tooltip(Component.text("Removes a member from your party.")));
                            builder.suggest("mute", VelocityBrigadierMessage.tooltip(Component.text("Mutes your party, preventing members from chatting.")));
                            builder.suggest("list", VelocityBrigadierMessage.tooltip(Component.text("Lists your party members.")));
                            builder.suggest("join", VelocityBrigadierMessage.tooltip(Component.text("Joins a party.")));
                            builder.suggest("warp", VelocityBrigadierMessage.tooltip(Component.text("Summons all members of your party to your current server.")));
                            builder.suggest("disband", VelocityBrigadierMessage.tooltip(Component.text("Disbands your party.")));
                            builder.suggest("transfer", VelocityBrigadierMessage.tooltip(Component.text("Transfers leadership of the party.")));
                            builder.suggest("leave", VelocityBrigadierMessage.tooltip(Component.text("Leaves your party. If you are the leader, the party is transferred to a random member.")));
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            String argumentProvided = context.getArgument("subcommand", String.class);
                            switch (argumentProvided.toUpperCase(Locale.ROOT)) {
                                case "LIST" -> {
                                    Player player = (Player) context.getSource();
                                    Party party = plugin.getPartyManager().getParty(player.getUniqueId());
                                    if(party == null){
                                        player.sendMessage(Component.text("You are not in a party!", NamedTextColor.RED));
                                        return Command.SINGLE_SUCCESS;
                                    }
                                    boolean leaderOnline = proxy.getPlayer(party.getLeader()).isPresent();
                                    Player leader = null;
                                    if(leaderOnline)
                                        leader = proxy.getPlayer(party.getLeader()).get();

                                    Component base = Component.text("").decoration(TextDecoration.STRIKETHROUGH, TextDecoration.State.FALSE)
                                            .append(PartyManager.LINE)
                                            .append(Component.text("                  Your Party Members: (" + (party.getMembers().size() + 1) + "):", NamedTextColor.GOLD)).appendNewline().appendNewline()
                                            .append(Component.text("Party Leader: ", NamedTextColor.YELLOW))
                                            .append(FoxRankAPI.getPlayerRank(party.getLeader()).getPrefix())
                                            .append(Component.text(plugin.getDatabase().getName(party.getLeader()), FoxRankAPI.getPlayerRank(party.getLeader()).getColor()))
                                            .append(Component.text(" \u25A0", leader == null ? NamedTextColor.RED : NamedTextColor.GREEN))
                                            .appendNewline()
                                            .appendNewline()
                                            .append(Component.text("Party Members: ", NamedTextColor.YELLOW)).appendNewline();
                                    for (UUID uuid : party.getMembers()) {
                                        Rank memberRank = FoxRankAPI.getPlayerRank(uuid);
                                        boolean online = proxy.getPlayer(uuid).isPresent();
                                        base = base.append(memberRank.getPrefix())
                                                .append(Component.text(plugin.getDatabase().getName(uuid), memberRank.getColor()))
                                                .append(Component.text(" \u25A0", online ? NamedTextColor.GREEN : NamedTextColor.RED))
                                                .appendNewline();
                                    }

                                    base = base.append(PartyManager.LINE);

                                    player.sendMessage(base);
                                }
                                case "LEAVE" -> {
                                    Player player = (Player) context.getSource();
                                    Party party = plugin.getPartyManager().getParty(player.getUniqueId());
                                    if(party == null){
                                        player.sendMessage(Component.text("You are not in a party!", NamedTextColor.RED));
                                        return Command.SINGLE_SUCCESS;
                                    }
                                    plugin.getPartyManager().leaveParty(player.getUniqueId(), party);
                                }
                                case "KICK", "JOIN", "INVITE" -> {
                                    context.getSource().sendMessage(Component.text("Insufficient arguments provided!", NamedTextColor.RED));
                                    return Command.SINGLE_SUCCESS;
                                }
                                case "DISBAND" -> {
                                    Player player = (Player) context.getSource();
                                    Party party = plugin.getPartyManager().getParty(player.getUniqueId());
                                    if(party == null) {
                                        player.sendMessage(Component.text("You are not in a party!", NamedTextColor.RED));
                                        return Command.SINGLE_SUCCESS;
                                    }
                                    if(player.getUniqueId() == party.getLeader()){
                                        plugin.getPartyManager().disband(party, DisbandReason.COMMAND);
                                    } else {
                                        player.sendMessage(Component.text("You are not this party's leader!", NamedTextColor.RED));
                                        return Command.SINGLE_SUCCESS;
                                    }
                                }
                                case "WARP" -> {
                                    Player player = (Player) context.getSource();
                                    Party party = plugin.getPartyManager().getParty(player.getUniqueId());
                                    if(party == null){
                                        player.sendMessage(Component.text("You are not in a party!", NamedTextColor.RED));
                                        return Command.SINGLE_SUCCESS;
                                    }
                                    if(player.getUniqueId() != party.getLeader()){
                                        player.sendMessage(Component.text("You are not this party's leader!", NamedTextColor.RED));
                                        return Command.SINGLE_SUCCESS;
                                    }
                                    CompletableFuture<Void> master = new CompletableFuture<>();
                                    AtomicInteger atomicInteger = new AtomicInteger(0);
                                    for (UUID uuid : party.getMembers()) {
                                        if(proxy.getPlayer(uuid).isPresent()){
                                         Player warpee = proxy.getPlayer(uuid).get();
                                         master = CompletableFuture.allOf(
                                         warpee.createConnectionRequest(player.getCurrentServer().get().getServer()).connect()
                                                 .whenComplete((result, throwable) -> {
                                             if(result.isSuccessful()){
                                                 atomicInteger.getAndIncrement();
                                                 warpee.sendMessage(PartyManager.LINE
                                                         .append(FoxRankAPI.getPlayerRank(player.getUniqueId()).getPrefix())
                                                         .append(Component.text(player.getUsername(), FoxRankAPI.getPlayerRank(player.getUniqueId()).getColor()))
                                                         .append(Component.text(" warped you to their server.", NamedTextColor.YELLOW))
                                                         .appendNewline().append(PartyManager.LINE));
                                             }
                                         }));
                                        }
                                    }
                                    master.whenComplete((unused, throwable) -> player.sendMessage(Component.text("Successfully warped " + atomicInteger.get() +  (atomicInteger.get() == 1 ? " player " : " players ") + "to your server.", NamedTextColor.GREEN)));
                                }
                                default -> {
                                    // send a party invite to this player

                                    if(plugin.getDatabase().getUUID(argumentProvided) != null){
                                        if (plugin.getProxy().getPlayer(argumentProvided).isPresent()) {
                                            Player sender = (Player) context.getSource();
                                            Player target = plugin.getProxy().getPlayer(argumentProvided).get();
                                            plugin.getPartyManager().dispatchInvite(sender.getUniqueId(), target.getUniqueId());
                                        } else {
                                            context.getSource().sendMessage(Component.text(argumentProvided + " is not online!", NamedTextColor.RED));
                                        }
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
                                    Party party = plugin.getPartyManager().getParty(sender.getUniqueId());
                                    String argumentProvided = context.getArgument("specifier", String.class);
                                    String sub = context.getArgument("subcommand", String.class);
                                    Player target;
                                    try {
                                        UUID uuid = UUID.fromString(argumentProvided);
                                        if (proxy.getPlayer(uuid).isEmpty())
                                            sender.sendMessage(Component.text("This player doesn't exist!", NamedTextColor.RED));
                                        target = proxy.getPlayer(uuid).get();
                                    } catch (IllegalArgumentException ignored) {
                                        if (proxy.getPlayer(argumentProvided).isEmpty())
                                            sender.sendMessage(Component.text("This player doesn't exist!", NamedTextColor.RED));
                                        target = proxy.getPlayer(argumentProvided).get();
                                    }

                                    switch (sub.toUpperCase(Locale.ROOT)) {
                                        case "INVITE" -> {
                                            if(party == null) {
                                                party = new Party(sender.getUniqueId(), new ArrayList<>());
                                                plugin.getPartyManager().registerParty(party);
                                            }
                                            if (target.getUniqueId() == sender.getUniqueId()) {
                                                sender.sendMessage(Component.text().decoration(TextDecoration.STRIKETHROUGH, TextDecoration.State.FALSE)
                                                        .append(PartyManager.LINE)
                                                        .append(Component.text("You cannot invite yourself to your party!", NamedTextColor.YELLOW))
                                                        .appendNewline().append(PartyManager.LINE));
                                                return Command.SINGLE_SUCCESS;
                                            }
                                            if(sender.getUniqueId() != party.getLeader()){
                                                sender.sendMessage(Component.text("You are not this party's leader!", NamedTextColor.RED));
                                                return Command.SINGLE_SUCCESS;
                                            }
                                            plugin.getPartyManager().dispatchInvite(sender.getUniqueId(), target.getUniqueId());
                                        }
                                        case "KICK", "K" -> {
                                            if(party == null){
                                                sender.sendMessage(Component.text("You are not in a party!", NamedTextColor.RED));
                                                return Command.SINGLE_SUCCESS;
                                            }
                                            if(sender.getUniqueId() != party.getLeader()){
                                                sender.sendMessage(Component.text("You are not this party's leader!", NamedTextColor.RED));
                                                return Command.SINGLE_SUCCESS;
                                            }
                                            if (target.getUniqueId() == sender.getUniqueId()) {
                                                sender.sendMessage(Component.text().decoration(TextDecoration.STRIKETHROUGH, TextDecoration.State.FALSE)
                                                        .append(PartyManager.LINE)
                                                        .append(Component.text("You cannot kick yourself from your party!", NamedTextColor.YELLOW))
                                                        .appendNewline().append(PartyManager.LINE));
                                                return Command.SINGLE_SUCCESS;
                                            }
                                            if (party.getMembers().contains(target.getUniqueId())) {
                                                plugin.getPartyManager().kickPlayer(party, target.getUniqueId());
                                            } else {
                                                sender.sendMessage(
                                                        FoxRankAPI.getPlayerRank(target.getUniqueId()).getPrefix()
                                                                .append(Component.text(target.getUsername(), FoxRankAPI.getPlayerRank(target.getUniqueId()).getColor()))
                                                                .append(Component.text(" isn't in your party!", NamedTextColor.YELLOW))
                                                );
                                            }
                                            return Command.SINGLE_SUCCESS;
                                        }
                                        case "TRANSFER", "T" -> {
                                            if(party == null){
                                                sender.sendMessage(Component.text("You are not in a party!", NamedTextColor.RED));
                                                return Command.SINGLE_SUCCESS;
                                            }
                                            if(sender.getUniqueId() != party.getLeader()){
                                                sender.sendMessage(Component.text("You are not this party's leader!", NamedTextColor.RED));
                                                return Command.SINGLE_SUCCESS;
                                            }
                                            if (target.getUniqueId() == sender.getUniqueId()) {
                                                sender.sendMessage(Component.text().decoration(TextDecoration.STRIKETHROUGH, TextDecoration.State.FALSE)
                                                        .append(PartyManager.LINE)
                                                        .append(Component.text("You cannot transfer your party to yourself!", NamedTextColor.YELLOW))
                                                        .appendNewline().append(PartyManager.LINE));
                                                return Command.SINGLE_SUCCESS;
                                            }
                                            if (party.getMembers().contains(target.getUniqueId())) {
                                                plugin.getPartyManager().transfer(target.getUniqueId(), party, TransferReason.COMMAND);
                                            } else {
                                                sender.sendMessage(
                                                        FoxRankAPI.getPlayerRank(target.getUniqueId()).getPrefix()
                                                                .append(Component.text(target.getUsername(), FoxRankAPI.getPlayerRank(target.getUniqueId()).getColor()))
                                                                .append(Component.text(" isn't in your party!", NamedTextColor.YELLOW))
                                                );
                                            }
                                            return Command.SINGLE_SUCCESS;
                                        }
                                        case "JOIN" -> {
                                            if (plugin.getPartyManager().getInvite(sender.getUniqueId(), target.getUniqueId()) != null) {
                                                plugin.getPartyManager().acceptInvite(sender.getUniqueId(), target.getUniqueId());
                                            } else {
                                                sender.sendMessage(
                                                        FoxRankAPI.getPlayerRank(target.getUniqueId()).getPrefix()
                                                                .append(Component.text(target.getUsername(), FoxRankAPI.getPlayerRank(target.getUniqueId()).getColor()))
                                                                .append(Component.text(" has not sent you a party invite recently!", NamedTextColor.YELLOW))
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
