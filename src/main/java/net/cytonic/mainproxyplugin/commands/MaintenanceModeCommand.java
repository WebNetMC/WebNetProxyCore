package net.cytonic.mainproxyplugin.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.foxikle.foxrankvelocity.FoxRankAPI;
import net.cytonic.mainproxyplugin.MainProxy;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class MaintenanceModeCommand implements SimpleCommand {

    private final ProxyServer server;
    private final MainProxy plugin;

    public MaintenanceModeCommand(ProxyServer server, MainProxy plugin) {
        this.server = server;
        this.plugin = plugin;
    }

    @Override
    public void execute(final Invocation invocation) {
        if(invocation.source() instanceof Player player) {
            if(FoxRankAPI.getPlayerRank(player.getUniqueId()).getPermissionNodes().contains("webnet.trigger_maintenance_mode")) {
                if(MainProxy.MAINTENANCE_MODE) {
                    MainProxy.MAINTENANCE_MODE = false;
                    invocation.source().sendMessage(Component.text("WebNet is now no longer in maintenace mode.").color(NamedTextColor.GREEN));
                } else {
                    MainProxy.MAINTENANCE_MODE = true;
                    server.getAllPlayers().forEach(p -> {
                        if (FoxRankAPI.getPlayerRank(p.getUniqueId()).getPermissionNodes().contains("webnet.maintenance_mode_bypass")) {
                            p.sendMessage(Component.text("WebNet is now in MAINTENANCE MODE.").color(NamedTextColor.GREEN));
                        } else {
                            p.disconnect(Component.text("WebNet is now in Maintenance Mode.", NamedTextColor.RED));
                        }
                    });
                }
            } else {
                player.sendMessage(Component.text("You cannot do this!", NamedTextColor.RED));
            }
        } else {
            if(MainProxy.MAINTENANCE_MODE) {
                MainProxy.MAINTENANCE_MODE = false;
                invocation.source().sendMessage(Component.text("WebNet is now no longer in maintenace mode.").color(NamedTextColor.GREEN));
            } else {
                MainProxy.MAINTENANCE_MODE = true;
                server.getAllPlayers().forEach(p -> {
                    if (FoxRankAPI.getPlayerRank(p.getUniqueId()).getPermissionNodes().contains("webnet.maintenance_mode_bypass")) {
                        p.sendMessage(Component.text("WebNet is now in MAINTENANCE MODE.").color(NamedTextColor.GREEN));
                    } else {
                        p.disconnect(Component.text("WebNet is now in Maintenance Mode.", NamedTextColor.RED));
                    }
                });
            }
        }
    }


    // This method allows you to control who can execute the command.
    // If the executor does not have the required permission,
    // the execution of the command and the control of its autocompletion
    // will be sent directly to the server on which the sender is located
    @Override
    public boolean hasPermission(final Invocation invocation) {
        return true;
    }

    // With this method you can control the suggestions to send
    // to the CommandSource according to the arguments
    // it has already written or other requirements you need
    @Override
    public List<String> suggest(final Invocation invocation) {
        return List.of();
    }

    // Here you can offer argument suggestions in the same way as the previous method,
    // but asynchronously. It is recommended to use this method instead of the previous one
    // especially in cases where you make a more extensive logic to provide the suggestions
    @Override
    public CompletableFuture<List<String>> suggestAsync(final Invocation invocation) {
        return CompletableFuture.completedFuture(List.of());
    }
}