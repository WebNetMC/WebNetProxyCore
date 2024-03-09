package net.cytonic.mainproxyplugin.listeners;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import dev.foxikle.foxrankvelocity.FoxRankAPI;
import dev.foxikle.foxrankvelocity.Rank;
import net.cytonic.mainproxyplugin.MainProxy;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.UUID;

public class LeaveListener {
    private final MainProxy plugin;

    public LeaveListener(MainProxy plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onPlayerLogin(DisconnectEvent event) {
        Player player = event.getPlayer();
        Rank rank = FoxRankAPI.getPlayerRank(player.getUniqueId());

        List<UUID> friends = plugin.getDatabase().getFriends(player.getUniqueId());
        friends.forEach(uuid -> {
            if(plugin.getProxy().getPlayer(uuid).isPresent()){
                Player p = plugin.getProxy().getPlayer(uuid).get();
                p.sendMessage(Component.text("Friend > ", NamedTextColor.AQUA)
                        .append(rank.getPrefix())
                        .append(Component.text(player.getUsername(), rank.getColor()))
                        .append(Component.text(" left.", NamedTextColor.YELLOW)));
            }
        });
    }
}
