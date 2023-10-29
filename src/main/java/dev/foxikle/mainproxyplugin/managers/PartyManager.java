package dev.foxikle.mainproxyplugin.managers;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.foxikle.foxrankvelocity.FoxRankAPI;
import dev.foxikle.foxrankvelocity.Rank;
import dev.foxikle.mainproxyplugin.MainProxy;
import dev.foxikle.mainproxyplugin.data.enums.DisbandReason;
import dev.foxikle.mainproxyplugin.data.enums.TransferReason;
import dev.foxikle.mainproxyplugin.data.objects.Party;
import dev.foxikle.mainproxyplugin.data.objects.Request;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PartyManager {
    private final MainProxy plugin;
    private final ProxyServer proxy;

    public static final Component LINE = Component.text("").decoration(TextDecoration.STRIKETHROUGH, TextDecoration.State.FALSE)
            .append(Component.text("                                                                                 ", NamedTextColor.GOLD, TextDecoration.STRIKETHROUGH));
    private final Map<UUID, Map<UUID, Request>> invites = new HashMap<>();
    private final Map<UUID, Party> parties = new HashMap<>();

    public PartyManager(MainProxy plugin) {
        this.plugin = plugin;
        proxy = plugin.getProxy();
    }

    public void registerParty(Party party) {
        parties.put(party.getLeader(), party);
    }

    public void transfer(UUID newLeader, Party party, TransferReason reason) {
        Rank newLeaderRank = FoxRankAPI.getPlayerRank(newLeader);
        String newLeaderName = plugin.getDatabase().getName(newLeader);

        Rank oldLeaderRank = FoxRankAPI.getPlayerRank(party.getLeader());
        String oldLeaderName = plugin.getDatabase().getName(party.getLeader());
        parties.remove(party.getLeader());


        if(reason == TransferReason.LEADER_LEFT){
            if(proxy.getPlayer(newLeader).isPresent())
                proxy.getPlayer(newLeader).get().sendMessage(
                        LINE.append(Component.text("The party was transferred you because ", NamedTextColor.YELLOW))
                                .append(oldLeaderRank.getPrefix())
                                .append(Component.text(oldLeaderName, oldLeaderRank.getColor()))
                                .append(Component.text(" left.", NamedTextColor.YELLOW))
                                .appendNewline().append(LINE)
                );

            party.getMembers().forEach(uuid -> {
                if(proxy.getPlayer(uuid).isPresent())
                    proxy.getPlayer(uuid).get().sendMessage(
                            LINE.append(Component.text("The party was transferred to ", NamedTextColor.YELLOW))
                                    .append(newLeaderRank.getPrefix())
                                    .append(Component.text(newLeaderName, newLeaderRank.getColor()))
                                    .append(Component.text(" because ", NamedTextColor.YELLOW))
                                    .append(oldLeaderRank.getPrefix())
                                    .append(Component.text(oldLeaderName, oldLeaderRank.getColor()))
                                    .append(Component.text(" left.", NamedTextColor.YELLOW))
                                    .appendNewline().append(LINE)
                    );
            });
        } else if(reason == TransferReason.COMMAND) {
            party.addPlayer(party.getLeader());
            if(proxy.getPlayer(newLeader).isPresent())
                proxy.getPlayer(newLeader).get().sendMessage(
                        LINE.append(oldLeaderRank.getPrefix())
                                .append(Component.text(oldLeaderName, oldLeaderRank.getColor()))
                                .append(Component.text(" transferred the party to you.", NamedTextColor.YELLOW))
                                .appendNewline().append(LINE)
                );

            party.getMembers().forEach(uuid -> {
                if(proxy.getPlayer(uuid).isPresent())
                    proxy.getPlayer(uuid).get().sendMessage(
                            LINE.append(Component.text("The party was transferred to ", NamedTextColor.YELLOW))
                                    .append(newLeaderRank.getPrefix())
                                    .append(Component.text(newLeaderName, newLeaderRank.getColor()))
                                    .append(Component.text(".", NamedTextColor.YELLOW))
                                    .appendNewline().append(LINE)
                    );
            });
        }
        party.transferLeader(newLeader);
        parties.put(newLeader, party);
    }

    public void disband(Party party, DisbandReason reason) {
        if(reason == DisbandReason.COMMAND){
            parties.remove(party.getLeader());
            if(proxy.getPlayer(party.getLeader()).isPresent()) {
                proxy.getPlayer(party.getLeader()).get().sendMessage(
                        LINE
                                .append(Component.text("You disbanded your party.", NamedTextColor.YELLOW))
                                .appendNewline().append(LINE)
                );
            }

            Rank playerRank = FoxRankAPI.getPlayerRank(party.getLeader());
            String username = plugin.getDatabase().getName(party.getLeader());
            party.getMembers().forEach(uuid -> {
                if(proxy.getPlayer(uuid).isPresent()){
                    proxy.getPlayer(uuid).get().sendMessage(
                            LINE
                                    .append(playerRank.getPrefix())
                                    .append(Component.text(username, playerRank.getColor()))
                                    .append(Component.text(" disbanded the party.", NamedTextColor.YELLOW))
                                    .appendNewline().append(LINE)
                    );
                }
            });
        } else if (reason == DisbandReason.EMPTY) {
            parties.remove(party.getLeader());
            if(proxy.getPlayer(party.getLeader()).isPresent()) {
                proxy.getPlayer(party.getLeader()).get().sendMessage(
                        LINE
                                .append(Component.text("Your party was disbanded since you are the only one.", NamedTextColor.YELLOW))
                                .appendNewline().append(LINE)
                );
            }

            Rank playerRank = FoxRankAPI.getPlayerRank(party.getLeader());
            String username = plugin.getDatabase().getName(party.getLeader());
            party.getMembers().forEach(uuid -> {
                if(proxy.getPlayer(uuid).isPresent()){
                    proxy.getPlayer(uuid).get().sendMessage(
                            LINE
                                    .append(playerRank.getPrefix())
                                    .append(Component.text(username, playerRank.getColor()))
                                    .append(Component.text(" The party was disbanded.", NamedTextColor.YELLOW))
                                    .appendNewline().append(LINE)
                    );
                }
            });
        }

    }

    public void kickPlayer(Party party, UUID player) {
        Rank playerRank = FoxRankAPI.getPlayerRank(player);
        String username = plugin.getDatabase().getName(player);
        if (proxy.getPlayer(player).isPresent()) {
            Player p = proxy.getPlayer(player).get();
            p.sendMessage(
                    LINE
                            .append(Component.text("You were kicked from the party.", NamedTextColor.YELLOW))
                            .appendNewline().append(LINE)
            );
        }
        party.removePlayer(player);
        party.getMembers().forEach(uuid -> {
            if (proxy.getPlayer(uuid).isPresent()) {
                proxy.getPlayer(uuid).get().sendMessage(
                        LINE
                                .append(playerRank.getPrefix())
                                .append(Component.text(username, playerRank.getColor()))
                                .append(Component.text(" was kicked from the party.", NamedTextColor.YELLOW))
                                .appendNewline().append(LINE)
                );
            }
        });


        if (proxy.getPlayer(party.getLeader()).isPresent()) {
            proxy.getPlayer(party.getLeader()).get().sendMessage(
                    LINE
                            .append(playerRank.getPrefix())
                            .append(Component.text(username, playerRank.getColor()))
                            .append(Component.text(" was kicked from the party.", NamedTextColor.YELLOW))
                            .appendNewline().append(LINE)
            );
        }
        checkForEmpty(party);
    }

    @Nullable
    public Request getInvite(UUID target, UUID sender) {
        if (invites.get(target) != null)
            return invites.get(target).get(sender);
        return null;
    }

    public void acceptInvite(UUID target, UUID sender) {
        if (invites.containsKey(target) && invites.get(target).containsKey(sender)) {
            Request request = invites.get(target).get(sender);
            request.setAccepted(true);
            invites.remove(target);

            Player author = plugin.getProxy().getPlayer(target).get();
            Player senderPlayer = plugin.getProxy().getPlayer(sender).get();

            Party party = getParty(request.getAuthor());
            if (party == null) {
                author.sendMessage(LINE
                        .append(Component.text("An error occurred whilst joining ", NamedTextColor.RED))
                        .append(FoxRankAPI.getPlayerRank(sender).getPrefix())
                        .append(Component.text(senderPlayer.getUsername(), FoxRankAPI.getPlayerRank(sender).getColor()))
                        .append(Component.text("'s party!", NamedTextColor.RED))

                        .append(LINE)
                );
                return;
            }

            joinParty(party, request.getTarget());
        }
    }

    public void joinParty(Party party, UUID toJoin) {
        Player author = plugin.getProxy().getPlayer(toJoin).get();
        Player senderPlayer = plugin.getProxy().getPlayer(party.getLeader()).get();

        party.getMembers().forEach(uuid -> {
            if (proxy.getPlayer(uuid).isPresent()) {
                Player p = proxy.getPlayer(uuid).get();
                if (p.getUniqueId() != toJoin)
                    p.sendMessage(LINE
                            .append(FoxRankAPI.getPlayerRank(toJoin).getPrefix())
                            .append(Component.text(author.getUsername(), FoxRankAPI.getPlayerRank(toJoin).getColor()))
                            .append(Component.text(" joined the party!", NamedTextColor.YELLOW))
                            .appendNewline().append(LINE)
                    );
            }
        });

        senderPlayer.sendMessage(LINE
                .append(FoxRankAPI.getPlayerRank(toJoin).getPrefix())
                .append(Component.text(author.getUsername(), FoxRankAPI.getPlayerRank(toJoin).getColor()))
                .append(Component.text(" joined the party!", NamedTextColor.YELLOW))
                .appendNewline().append(LINE)
        );

        author.sendMessage(LINE
                .append(Component.text("You joined ", NamedTextColor.YELLOW))
                .append(FoxRankAPI.getPlayerRank(party.getLeader()).getPrefix())
                .append(Component.text(senderPlayer.getUsername(), FoxRankAPI.getPlayerRank(party.getLeader()).getColor()))
                .append(Component.text("'s party!", NamedTextColor.YELLOW))
                .appendNewline().append(LINE)
        );
        party.addPlayer(toJoin);
    }

    @Nullable
    public Party getParty(UUID player) {
        if (parties.get(player) == null) {
            for (Party party : parties.values()) {
                if (party.getMembers().contains(player))
                    return party;
            }
        }
        return parties.get(player);
    }

    public void dispatchInvite(UUID sender, UUID target) {
        Player senderPlayer = plugin.getProxy().getPlayer(sender).get();

        if (getParty(sender) == null) {
            registerParty(new Party(sender, new ArrayList<>()));
        }
        if (plugin.getProxy().getPlayer(target).isEmpty()) {
            senderPlayer.sendMessage(
                            LINE.append(Component.text("That player is not online!", NamedTextColor.RED))
                            .appendNewline().append(LINE)
            );
            return;
        }
        Player targetPlayer = plugin.getProxy().getPlayer(target).get();
        if (targetPlayer.getUniqueId() == senderPlayer.getUniqueId()) {
            senderPlayer.sendMessage(Component.text().decoration(TextDecoration.STRIKETHROUGH, TextDecoration.State.FALSE)
                    .append(LINE)
                    .append(Component.text("You cannot invite yourself to your party!", NamedTextColor.YELLOW))
                    .appendNewline().append(LINE));
            return;
        }
        if (invites.get(target) != null) {
            if (invites.get(target).containsKey(sender)) {
                senderPlayer.sendMessage(Component.text().decoration(TextDecoration.STRIKETHROUGH, TextDecoration.State.FALSE)
                        .append(Component.text("You already have a pending invite with ", NamedTextColor.YELLOW))
                        .append(FoxRankAPI.getPlayerRank(target).getPrefix())
                        .append(Component.text(targetPlayer.getUsername(), FoxRankAPI.getPlayerRank(target).getColor()))
                        .append(Component.text(".", NamedTextColor.YELLOW))
                );
                return;
            }
        }
        if (invites.get(sender) != null) {
            if (invites.get(sender).containsKey(target)) {
                senderPlayer.sendMessage(Component.text().decoration(TextDecoration.STRIKETHROUGH, TextDecoration.State.FALSE)
                        .append(LINE)
                        .append(FoxRankAPI.getPlayerRank(target).getPrefix())
                        .append(Component.text(targetPlayer.getUsername(), FoxRankAPI.getPlayerRank(target).getColor()))
                        .append(Component.text(" has already sent you an invite. To accept it, type '/party join " + targetPlayer.getUsername() + "'.", NamedTextColor.YELLOW))
                        .appendNewline().append(LINE)
                );
                return;
            }
        }

        if (!getParty(sender).getMembers().contains(target)) {
            Request request = new Request(
                    Instant.now().plusSeconds(60L),
                    sender, target, Request.RequestType.PARTY);
            Map<UUID, Request> requestMap = invites.get(target);
            if (requestMap == null)
                requestMap = new HashMap<>();
            requestMap.put(sender, request);
            invites.put(target, requestMap);

            // create an expiry runnable
            plugin.getProxy().getScheduler().buildTask(plugin, () -> {
                if (!request.isAccepted() && !request.isDeclined()) {
                    // not accepted, so it times out
                    Map<UUID, Request> foo = invites.get(target);
                    if (foo == null)
                        foo = new HashMap<>();
                    foo.remove(sender);
                    invites.put(target, foo);
                    // send messages

                    targetPlayer.sendMessage(LINE.append(
                                    Component.text("Your party invite from ", NamedTextColor.YELLOW)
                                            .append(FoxRankAPI.getPlayerRank(sender).getPrefix())
                                            .append(Component.text(senderPlayer.getUsername(), FoxRankAPI.getPlayerRank(sender).getColor()))
                                            .append(Component.text(" has expired.", NamedTextColor.YELLOW))
                                            .appendNewline().append(LINE)
                            )
                    );

                    senderPlayer.sendMessage(LINE.append(
                                    Component.text("Your party invite to ", NamedTextColor.YELLOW)
                                            .append(FoxRankAPI.getPlayerRank(target).getPrefix())
                                            .append(Component.text(targetPlayer.getUsername(), FoxRankAPI.getPlayerRank(target).getColor()))
                                            .append(Component.text(" has expired.", NamedTextColor.YELLOW))
                                            .appendNewline().append(LINE)
                            )
                    );
                }
            }).delay(Duration.of(60L, ChronoUnit.SECONDS)).schedule();
            // handle textual interface

            targetPlayer.sendMessage(LINE.append(
                            FoxRankAPI.getPlayerRank(sender).getPrefix()
                                    .append(Component.text(senderPlayer.getUsername(), FoxRankAPI.getPlayerRank(sender).getColor()))
                                    .append(Component.text(" invited you to their party! You have 60 seconds to accept it.", NamedTextColor.YELLOW))
                                    .append(Component.text(" [JOIN]", NamedTextColor.GREEN).clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/party join " + sender.toString())))
                                    .appendNewline().append(LINE)
                    )
            );

            senderPlayer.sendMessage(LINE.append(
                            Component.text("You invited ", NamedTextColor.YELLOW)
                                    .append(FoxRankAPI.getPlayerRank(target).getPrefix())
                                    .append(Component.text(targetPlayer.getUsername(), FoxRankAPI.getPlayerRank(target).getColor()))
                                    .append(Component.text(" to your party! They have 60 seconds to accept.", NamedTextColor.YELLOW))
                                    .appendNewline().append(LINE)
                    )
            );
        } else {
            senderPlayer.sendMessage(Component.empty().decoration(TextDecoration.STRIKETHROUGH, TextDecoration.State.FALSE)
                    .append(LINE)
                    .append(FoxRankAPI.getPlayerRank(target).getPrefix())
                    .append(Component.text(targetPlayer.getUsername(), FoxRankAPI.getPlayerRank(target).getColor()))
                    .append(Component.text(" is already in your party!", NamedTextColor.YELLOW))
                    .appendNewline().append(LINE));
        }
    }

    public void leaveParty(UUID member, Party party){
        if(member == party.getLeader()){
            UUID newLeader = null;
            for (UUID uuid : party.getMembers()) {
                if(proxy.getPlayer(uuid).isPresent()){
                    newLeader = uuid;
                    break;
                }
            }
            if(newLeader == null) {
                disband(party, DisbandReason.EMPTY);
                return;
            }
            transfer(newLeader, party, TransferReason.LEADER_LEFT);

        } else {
            party.removePlayer(member);
            Player leaving = proxy.getPlayer(member).get();
            Rank leavingRank = FoxRankAPI.getPlayerRank(member);
            party.getMembers().forEach(uuid -> {
                if(proxy.getPlayer(uuid).isPresent()){
                    proxy.getPlayer(uuid).get().sendMessage(LINE
                            .append(leavingRank.getPrefix())
                            .append(Component.text(leaving.getUsername(), leavingRank.getColor()))
                            .append(Component.text(" left the party.", NamedTextColor.YELLOW))
                            .appendNewline().append(LINE)
                    );
                }
            });

            if(proxy.getPlayer(party.getLeader()).isPresent()){
                proxy.getPlayer(party.getLeader()).get().sendMessage(LINE
                        .append(leavingRank.getPrefix())
                        .append(Component.text(leaving.getUsername(), leavingRank.getColor()))
                        .append(Component.text(" left the party.", NamedTextColor.YELLOW))
                        .appendNewline().append(LINE)
                );
            }
        }
        if(proxy.getPlayer(member).isPresent())
            proxy.getPlayer(member).get().sendMessage(
                    LINE.append(Component.text("You left the party.", NamedTextColor.YELLOW))
                            .appendNewline().append(LINE)
            );
        checkForEmpty(party);
    }

    public void checkForEmpty(Party party){
        if(party.getMembers().isEmpty()){
            disband(party, DisbandReason.EMPTY);
        }
    }

    public void yoinkParty(UUID uuid, Party party) {

        parties.remove(party.getLeader());
        party.addPlayer(party.getLeader());
        party.transferLeader(uuid);
        parties.put(uuid, party);

        Player newLeader = proxy.getPlayer(uuid).get();
        Rank leaderRank = FoxRankAPI.getPlayerRank(uuid);


        party.getMembers().forEach(uuid1 -> {
            if(proxy.getPlayer(uuid1).isPresent())
                proxy.getPlayer(uuid1).get().sendMessage(
                        LINE.append(leaderRank.getPrefix())
                                .append(Component.text(newLeader.getUsername(), leaderRank.getColor()))
                                .append(Component.text(" yoinked the party!", NamedTextColor.YELLOW))
                                .appendNewline().append(LINE)
                );
        });

        newLeader.sendMessage(LINE.append(
                Component.text("You yoinked the party!", NamedTextColor.YELLOW))
                .appendNewline().append(LINE)
        );
    }
}
