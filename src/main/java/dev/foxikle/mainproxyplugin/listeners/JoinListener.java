package dev.foxikle.mainproxyplugin.listeners;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import dev.foxikle.foxrankvelocity.FoxRankAPI;
import dev.foxikle.foxrankvelocity.Rank;
import dev.foxikle.mainproxyplugin.MainProxy;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.checkerframework.checker.units.qual.C;

import java.util.List;
import java.util.UUID;

public class JoinListener {
    private final MainProxy plugin;

    public JoinListener(MainProxy plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onPlayerLogin(LoginEvent event) {
        Player player = event.getPlayer();
        Rank rank = FoxRankAPI.getPlayerRank(player.getUniqueId());
        plugin.getDatabase().addName(player.getUniqueId(), player.getUsername());
        if(MainProxy.MAINTENANCE_MODE) {
            if(rank.getPermissionNodes().contains("webnet.maintenance_mode_bypass")) {
                event.setResult(ResultedEvent.ComponentResult.allowed());
            } else {
                event.setResult(ResultedEvent.ComponentResult.denied(Component.text("WebNet is currently in Maintenance Mode.", NamedTextColor.RED)));
                return;
            }
        }

        // allowed in
        List<UUID> friends = plugin.getDatabase().getFriends(player.getUniqueId());
        plugin.getFriendManager().setFriends(player.getUniqueId(), friends);
        friends.forEach(uuid -> {
            if(plugin.getProxy().getPlayer(uuid).isPresent()){
                Player p = plugin.getProxy().getPlayer(uuid).get();
                p.sendMessage(Component.text("Friend > ", NamedTextColor.AQUA)
                        .append(rank.getPrefix())
                        .append(Component.text(player.getUsername(), rank.getColor()))
                        .append(Component.text(" joined.", NamedTextColor.YELLOW)));
            }
        });
    }
}
