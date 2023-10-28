package dev.foxikle.mainproxyplugin.managers;

import dev.foxikle.foxrankvelocity.FoxRankAPI;
import dev.foxikle.mainproxyplugin.MainProxy;
import dev.foxikle.mainproxyplugin.data.enums.ChatChannel;
import dev.foxikle.mainproxyplugin.data.objects.Party;
import net.kyori.adventure.text.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChatManager {
    private final MainProxy plugin;
    private final Map<UUID, ChatChannel> channels = new HashMap<>();

    public ChatManager(MainProxy plugin) {
        this.plugin = plugin;
    }

    public void removeChannel(UUID uuid) {
        channels.remove(uuid);
    }

    public void setChannel(UUID uuid, ChatChannel channel) {
        channels.put(uuid, channel);
    }

    public ChatChannel getChannel(UUID uuid){
        return channels.getOrDefault(uuid, ChatChannel.ALL);
    }

    public void sendMessageToChannel(Component component, ChatChannel chatChannel, Object object){
        switch (chatChannel){
            case ADMIN -> // send a message to all players with webnet.chat.admin permission
                    plugin.getProxy().getAllPlayers().forEach(player -> {
                        if(FoxRankAPI.getPlayerRank(player.getUniqueId()).getPermissionNodes().contains("webnet.chat.admin")){
                            player.sendMessage(component);
                        }
                    });
            case PARTY -> {
                if(object instanceof Party party){
                    if (plugin.getProxy().getPlayer(party.getLeader()).isPresent())
                        plugin.getProxy().getPlayer(party.getLeader()).get().sendMessage(component);
                    party.getMembers().forEach(uuid -> {
                        if (plugin.getProxy().getPlayer(uuid).isPresent())
                            plugin.getProxy().getPlayer(uuid).get().sendMessage(component);
                    });
                } else {
                    throw new IllegalArgumentException("object must be an instance of Party!");
                }
            }
            case MOD -> plugin.getProxy().getAllPlayers().forEach(player -> {
                if(FoxRankAPI.getPlayerRank(player.getUniqueId()).getPermissionNodes().contains("webnet.chat.mod")){
                    player.sendMessage(component);
                }
            });
            case STAFF -> plugin.getProxy().getAllPlayers().forEach(player -> {
                if(FoxRankAPI.getPlayerRank(player.getUniqueId()).getPermissionNodes().contains("webnet.chat.staff")){
                    player.sendMessage(component);
                }
            });
            case LEAGUE -> {
                // leagues..
            }
            case PRIVATE_MESSAGE -> {
                // priveate messages
            }
        }
    }
}
