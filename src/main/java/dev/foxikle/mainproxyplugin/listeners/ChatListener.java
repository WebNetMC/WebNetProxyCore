package dev.foxikle.mainproxyplugin.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.foxikle.foxrankvelocity.FoxRankAPI;
import dev.foxikle.mainproxyplugin.MainProxy;
import dev.foxikle.mainproxyplugin.data.enums.ChatChannel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ChatListener {
    private final MainProxy plugin;
    private final ProxyServer proxy;

    public ChatListener(MainProxy plugin) {
        this.plugin = plugin;
        this.proxy = plugin.getProxy();
    }

    @Subscribe
    public void onChatMessage(PlayerChatEvent event){
        Player player = event.getPlayer();
        if(plugin.getChatManager().getChannel(player.getUniqueId()) != ChatChannel.ALL) {
            ChatChannel channel = plugin.getChatManager().getChannel(player.getUniqueId());
            Object object = null;
            if(channel == ChatChannel.PARTY){
                if(plugin.getPartyManager().getParty(player.getUniqueId()) == null){
                    player.sendMessage(Component.text("You are not in a party, so you were transferred to the ALL channel.", NamedTextColor.RED));
                    plugin.getChatManager().setChannel(player.getUniqueId(), ChatChannel.ALL);
                    return;
                } else {
                    object = plugin.getPartyManager().getParty(player.getUniqueId());
                    if(plugin.getPartyManager().getParty(player.getUniqueId()).isMuted()){
                        player.sendMessage(Component.text("The party is muted.", NamedTextColor.RED));
                    }
                }
            }
            event.setResult(PlayerChatEvent.ChatResult.denied());
            String originalMessage = event.getMessage();
            Component message = Component.text("")
                    .append(channel.getPrefix())
                    .append(FoxRankAPI.getPlayerRank(player.getUniqueId()).getPrefix())
                    .append(Component.text(player.getUsername(), FoxRankAPI.getPlayerRank(player.getUniqueId()).getColor()))
                    .appendSpace()
                    .append(Component.text(originalMessage, NamedTextColor.WHITE));
            plugin.getChatManager().sendMessageToChannel(message, plugin.getChatManager().getChannel(player.getUniqueId()), object);

        } else {
            event.setResult(PlayerChatEvent.ChatResult.allowed());
        }
    }
}

