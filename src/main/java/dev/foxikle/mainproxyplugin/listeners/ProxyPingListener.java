package dev.foxikle.mainproxyplugin.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.server.ServerPing;
import dev.foxikle.mainproxyplugin.MainProxy;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class ProxyPingListener {

    @Subscribe
    public void onProxyPing(ProxyPingEvent event) {
        final ServerPing.Builder ping = event.getPing().asBuilder();
        if(MainProxy.MAINTENANCE_MODE) {
            ping.description(Component.text("Webnet is currently in maintenance mode!", NamedTextColor.RED));
        } else {
            ping.description(MiniMessage.miniMessage().deserialize("<green>                Webnet Network </green><red>[1.20.4]</red>\n                  <gold><bold>BEDWARS BETA TEST"));
        }
        event.setPing(ping.build());
    }
}
