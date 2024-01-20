package dev.foxikle.mainproxyplugin.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import dev.foxikle.mainproxyplugin.MainProxy;
import dev.foxikle.mainproxyplugin.utils.ServerUtils;
import net.kyori.adventure.text.Component;

public class PluginChannelListener {
    private final MainProxy plugin;

    public PluginChannelListener(MainProxy plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (event.getIdentifier() == MainProxy.MAIN_CHANNEL) {
            // main channel things
        } else if (event.getIdentifier() == MainProxy.LOBBY_REQUEST) {
           if(event.getSource() instanceof Player player) {
               player.sendMessage(Component.text("Yo ur gettin transfered!"));
               plugin.getProxy().getScheduler().buildTask(plugin, () -> ServerUtils.connectToServer("lobby", plugin.getProxy(), player)).schedule();
           }
        }
    }
}
