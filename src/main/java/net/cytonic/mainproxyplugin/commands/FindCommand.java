package net.cytonic.mainproxyplugin.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.cytonic.mainproxyplugin.MainProxy;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import java.util.UUID;

public class FindCommand {
    public static BrigadierCommand createBrigadierCommand(final ProxyServer proxy, final MainProxy plugin) {

        // BrigadierCommand implements Command
        return new BrigadierCommand(getNode(proxy, plugin));
    }

    private static LiteralCommandNode<CommandSource> getNode(final ProxyServer proxy, final MainProxy plugin) {
        return LiteralArgumentBuilder.<CommandSource>literal("find")
                .requires(source -> source instanceof Player)
                .executes(context -> {
                    context.getSource().sendMessage(Component.text("Invalid arguments!"));
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
                                            sender.sendMessage(Component.text("This player isn't online!", NamedTextColor.RED));
                                        target = proxy.getPlayer(uuid).get();
                                    } catch (IllegalArgumentException ignored) {
                                        if (proxy.getPlayer(argumentProvided).isEmpty())
                                            sender.sendMessage(Component.text("This player isn't online!", NamedTextColor.RED));
                                        target = proxy.getPlayer(argumentProvided).get();
                                    }

                                    String servername = target.getCurrentServer().get().getServerInfo().getName();

                                    sender.sendMessage(Component.text(target.getUsername() + " is on server " + servername, NamedTextColor.YELLOW)
                                            .append(Component.text("[GO THERE]", NamedTextColor.GREEN, TextDecoration.BOLD)
                                                    .hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, Component.text("Click to travel to server: '" + servername + "'"))))
                                            .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/server " + servername))
                                    );

                                    sender.createConnectionRequest(target.getCurrentServer().get().getServer());

                                    return Command.SINGLE_SUCCESS;
                                })

                )
                .build();
    }
}
