package net.cytonic.mainproxyplugin.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import net.cytonic.mainproxyplugin.MainProxy;
import net.cytonic.mainproxyplugin.utils.ServerUtils;
import net.kyori.adventure.text.Component;

public class PluginChannelListener {
    private final MainProxy plugin;

    public PluginChannelListener(MainProxy plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (event.getIdentifier() == MainProxy.MAIN_CHANNEL) {
            if(event.getSource() instanceof Player player) {
                player.sendMessage(Component.text("Yo ur gettin messaged on main channel!"));
            }
        } else if (event.getIdentifier() == MainProxy.LOBBY_REQUEST) {
            plugin.getLogger().info("Message recieved on lobby reqeust channel from: " + event.getSource().toString());
           if(event.getSource() instanceof Player player) {
               player.sendMessage(Component.text("Why thank you for reqesting to go to the lobby."));
               plugin.getProxy().getScheduler().buildTask(plugin, () -> ServerUtils.connectToServer("lobby", plugin.getProxy(), player)).schedule();
           } else if (event.getSource() instanceof ServerConnection connection) {
               connection.getPlayer().sendMessage(Component.text("Why thank you for reqesting to go to the lobby."));
               plugin.getProxy().getScheduler().buildTask(plugin, () -> ServerUtils.connectToServer("lobby", plugin.getProxy(), connection.getPlayer())).schedule();
           }
        }
    }
}
