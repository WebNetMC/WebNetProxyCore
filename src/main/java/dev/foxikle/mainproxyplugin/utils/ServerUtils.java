package dev.foxikle.mainproxyplugin.utils;

import com.google.common.collect.Iterables;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ServerUtils {

    public static CompletableFuture<Void> connectToServer(String name, ProxyServer proxyServer, Player player) {
        List<RegisteredServer> lobbies = new ArrayList<>();
        List<RegisteredServer> allServers = new ArrayList<>(proxyServer.getAllServers());
        List<CompletableFuture<ServerPing>> futures = new ArrayList<>();
        allServers.forEach(s -> {
            if (s.getServerInfo().getName().contains(name)) {
                futures.add(
                        s.ping()
                                .whenComplete((serverPing, throwable1) -> {
                                    if (serverPing != null) {
                                        // server is online...
                                        if (player.getCurrentServer().get().getServer() != s) {
                                            lobbies.add(s);
                                        }
                                    }
                                })
                );
            }
        });
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).whenComplete((unused, throwable) -> {
            Component network = Component.text("[NETWORK]", NamedTextColor.YELLOW, TextDecoration.BOLD).appendSpace();
            if (lobbies.isEmpty()) {
                player.sendMessage(network.append(Component.text("Could not find a server to connect you to!", NamedTextColor.RED).decoration(TextDecoration.BOLD, TextDecoration.State.FALSE)));
            } else {
                RegisteredServer target = Iterables.getFirst(lobbies, null);
                player.sendMessage(network.append(Component.text("Connecting you to server " + target.getServerInfo().getName(), NamedTextColor.GRAY, TextDecoration.ITALIC).decoration(TextDecoration.BOLD, TextDecoration.State.FALSE)));
                player.createConnectionRequest(target).connect().thenAccept(result -> {
                    if (!result.isSuccessful()) {
                        player.sendMessage(network.append(Component.text("Failed to connect you to " + target.getServerInfo().getName(), NamedTextColor.RED).decoration(TextDecoration.BOLD, TextDecoration.State.FALSE)));
                    }
                });
            }
        });

    }
}
