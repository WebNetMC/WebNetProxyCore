package net.cytonic.mainproxyplugin.managers;

import com.velocitypowered.api.proxy.Player;
import dev.foxikle.foxrankvelocity.FoxRankAPI;
import net.cytonic.mainproxyplugin.MainProxy;
import net.cytonic.mainproxyplugin.data.objects.Request;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class FriendManager {
    private final MainProxy plugin;
    /**
     * The map of requests. KEYED BY TARGET!!, then keyed by sender
     */
    private Map<UUID, Map<UUID, Request>> requests = new HashMap<>();
    private Map<UUID, List<UUID>> friends = new HashMap<>();
    public static final Component LINE = Component.text("").decoration(TextDecoration.STRIKETHROUGH, TextDecoration.State.FALSE)
            .append(Component.text("                                                                  ", NamedTextColor.DARK_AQUA, TextDecoration.STRIKETHROUGH)
                    .append(Component.text("").decoration(TextDecoration.STRIKETHROUGH, TextDecoration.State.FALSE)));

    public FriendManager(MainProxy plugin) {
        this.plugin = plugin;
    }

    public void dispachRequest(UUID sender, UUID target) {
        Player senderPlayer = plugin.getProxy().getPlayer(sender).get();
        if (plugin.getProxy().getPlayer(target).isEmpty()) {
            senderPlayer.sendMessage(
                    Component.text().decoration(TextDecoration.STRIKETHROUGH, TextDecoration.State.FALSE)
                            .append(LINE).appendNewline()
                            .append(Component.text("That player is not online!", NamedTextColor.RED))
                            .appendNewline()
            );
            return;
        }
        Player targetPlayer = plugin.getProxy().getPlayer(target).get();
        if (targetPlayer.getUniqueId() == senderPlayer.getUniqueId()) {
            senderPlayer.sendMessage(Component.text().decoration(TextDecoration.STRIKETHROUGH, TextDecoration.State.FALSE)
                    .append(FriendManager.LINE).appendNewline()
                    .append(Component.text("You cannot add yourself as a friend!", NamedTextColor.YELLOW)).appendNewline()
                    .append(FriendManager.LINE));
            return;
        }
        if (requests.get(target) != null) {
            if (requests.get(target).containsKey(sender)) {
                senderPlayer.sendMessage(Component.text().decoration(TextDecoration.STRIKETHROUGH, TextDecoration.State.FALSE)
                        .append(Component.text("You already have a pending request with ", NamedTextColor.YELLOW))
                        .append(FoxRankAPI.getPlayerRank(target).getPrefix())
                        .append(Component.text(targetPlayer.getUsername(), FoxRankAPI.getPlayerRank(target).getColor()))
                        .append(Component.text(".", NamedTextColor.YELLOW))
                );
                return;
            }
        }
        if (requests.get(sender) != null) {
            if (requests.get(sender).containsKey(target)) {
                senderPlayer.sendMessage(Component.text().decoration(TextDecoration.STRIKETHROUGH, TextDecoration.State.FALSE)
                        .append(LINE).appendNewline()
                        .append(FoxRankAPI.getPlayerRank(target).getPrefix())
                        .append(Component.text(targetPlayer.getUsername(), FoxRankAPI.getPlayerRank(target).getColor()))
                        .append(Component.text(" has already sent you a friend request. To accept it, type '/friend accept " + targetPlayer.getUsername() + "'.", NamedTextColor.YELLOW))
                        .appendNewline().append(LINE)
                );
                return;
            }
        }

        if (!isFriend(sender, target)) {
            Request request = new Request(
                    Instant.now().plusSeconds(60L),
                    sender, target, Request.RequestType.FRIEND);
            Map<UUID, Request> requestMap = requests.get(target);
            if (requestMap == null)
                requestMap = new HashMap<>();
            requestMap.put(sender, request);
            requests.put(target, requestMap);

            // create an expiry runnable
            plugin.getProxy().getScheduler().buildTask(plugin, () -> {
                if (!request.isAccepted() && !request.isDeclined()) {
                    // not accepted, so it times out
                    Map<UUID, Request> foo = requests.get(target);
                    if (foo == null)
                        foo = new HashMap<>();
                    foo.remove(sender);
                    requests.put(target, foo);
                    // send messages

                    targetPlayer.sendMessage(LINE.appendNewline().append(
                                    Component.text("Your friend request from ", NamedTextColor.YELLOW)
                                            .append(FoxRankAPI.getPlayerRank(sender).getPrefix())
                                            .append(Component.text(senderPlayer.getUsername(), FoxRankAPI.getPlayerRank(sender).getColor()))
                                            .append(Component.text(" has expired.", NamedTextColor.YELLOW))
                                            .appendNewline()
                                            .append(LINE)
                            )
                    );

                    senderPlayer.sendMessage(LINE.appendNewline().append(
                                    Component.text("Your friend request to ", NamedTextColor.YELLOW)
                                            .append(FoxRankAPI.getPlayerRank(target).getPrefix())
                                            .append(Component.text(targetPlayer.getUsername(), FoxRankAPI.getPlayerRank(target).getColor()))
                                            .append(Component.text(" has expired.", NamedTextColor.YELLOW))
                                            .appendNewline()
                                            .append(LINE)
                            )
                    );
                }
            }).delay(Duration.of(60L, ChronoUnit.SECONDS)).schedule();
            // handle textual interface

            targetPlayer.sendMessage(LINE.appendNewline().append(
                            FoxRankAPI.getPlayerRank(sender).getPrefix()
                                    .append(Component.text(senderPlayer.getUsername(), FoxRankAPI.getPlayerRank(sender).getColor()))
                                    .append(Component.text(" sent you a friend request! You have 60 seconds to accept it.", NamedTextColor.YELLOW).appendNewline())
                                    .append(Component.text("[ACCEPT] ", NamedTextColor.GREEN).clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/friend accept " + sender.toString())))
                                    .append(Component.text("[DECLINE] ", NamedTextColor.RED).clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/friend deny " + sender)))
                                    .append(Component.text("[IGNORE]", NamedTextColor.GRAY).clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/ignore add " + sender)))
                                    .appendNewline()
                                    .append(LINE)
                    )
            );

            senderPlayer.sendMessage(LINE.appendNewline().append(
                            Component.text("You sent a friend request to ", NamedTextColor.YELLOW)
                                    .append(FoxRankAPI.getPlayerRank(target).getPrefix())
                                    .append(Component.text(targetPlayer.getUsername(), FoxRankAPI.getPlayerRank(target).getColor()))
                                    .appendNewline()
                                    .append(LINE)
                    )
            );
        } else {
            senderPlayer.sendMessage(Component.empty().decoration(TextDecoration.STRIKETHROUGH, TextDecoration.State.FALSE)
                    .append(LINE).appendNewline()
                    .append(Component.text("You are already friends with ", NamedTextColor.YELLOW))
                    .append(FoxRankAPI.getPlayerRank(target).getPrefix())
                    .append(Component.text(targetPlayer.getUsername(), FoxRankAPI.getPlayerRank(target).getColor()))
                    .append(Component.text("!", NamedTextColor.YELLOW))
                    .appendNewline().append(LINE));
        }
    }

    @Nullable
    public Request getRequest(UUID target, UUID sender) {
        if (requests.get(target) != null)
            return requests.get(target).get(sender);
        return null;
    }

    public void acceptRequest(UUID target, UUID sender) {
        if (requests.containsKey(target) && requests.get(target).containsKey(sender)) {
            Request request = requests.get(target).get(sender);
            request.setAccepted(true);
            requests.remove(target);

            addFriend(request.getAuthor(), request.getTarget());

            Player author = plugin.getProxy().getPlayer(target).get();
            Player senderPlayer = plugin.getProxy().getPlayer(sender).get();

            senderPlayer.sendMessage(LINE.appendNewline()
                    .append(FoxRankAPI.getPlayerRank(target).getPrefix())
                    .append(Component.text(author.getUsername(), FoxRankAPI.getPlayerRank(target).getColor()))
                    .append(Component.text(" accepted your friend request!", NamedTextColor.YELLOW))
                    .appendNewline()
                    .append(LINE)
            );

            author.sendMessage(LINE.appendNewline()
                    .append(Component.text("You accepted ", NamedTextColor.YELLOW))
                    .append(FoxRankAPI.getPlayerRank(sender).getPrefix())
                    .append(Component.text(senderPlayer.getUsername(), FoxRankAPI.getPlayerRank(sender).getColor()))
                    .append(Component.text("'s friend request!", NamedTextColor.YELLOW))
                    .appendNewline()
                    .append(LINE)
            );
        }
    }

    public void declineRequest(UUID target, UUID sender) {
        if (requests.containsKey(target) && requests.get(target).containsKey(sender)) {
            Request request = requests.get(target).get(sender);
            request.setDeclined(true);
            requests.remove(target);

            Player author = plugin.getProxy().getPlayer(target).get();
            Player senderPlayer = plugin.getProxy().getPlayer(sender).get();

            senderPlayer.sendMessage(LINE.appendNewline()
                    .append(FoxRankAPI.getPlayerRank(target).getPrefix())
                    .append(Component.text(author.getUsername(), FoxRankAPI.getPlayerRank(target).getColor()))
                    .append(Component.text(" declined your friend request.", NamedTextColor.YELLOW))
                    .appendNewline()
                    .append(LINE)
            );

            author.sendMessage(LINE.appendNewline()
                    .append(Component.text("You declined ", NamedTextColor.YELLOW))
                    .append(FoxRankAPI.getPlayerRank(sender).getPrefix())
                    .append(Component.text(senderPlayer.getUsername(), FoxRankAPI.getPlayerRank(sender).getColor()))
                    .append(Component.text("'s friend request.", NamedTextColor.YELLOW))
                    .appendNewline()
                    .append(LINE)
            );


        }
    }

    public void addFriend(UUID author, UUID target) {
        List<UUID> foo = friends.get(author);
        if (foo == null)
            foo = new ArrayList<>();
        foo.add(target);
        friends.put(author, foo);

        List<UUID> bar = friends.get(target);
        if (bar == null)
            bar = new ArrayList<>();
        bar.add(author);
        friends.put(target, bar);

        plugin.getDatabase().setFriends(author, friends.get(author));
        plugin.getDatabase().setFriends(target, friends.get(target));

    }

    public void removeFriend(UUID author, UUID target) {
        List<UUID> foo = friends.get(author);
        foo.remove(target);
        friends.put(author, foo);

        List<UUID> bar = friends.get(target);
        foo.remove(author);
        friends.put(target, bar);


        Player authorPlayer = plugin.getProxy().getPlayer(author).get();
        Player senderPlayer = plugin.getProxy().getPlayer(target).get();

        authorPlayer.sendMessage(LINE.appendNewline()
                .append(Component.text("You removed ", NamedTextColor.YELLOW))
                .append(FoxRankAPI.getPlayerRank(target).getPrefix())
                .append(Component.text(senderPlayer.getUsername(), FoxRankAPI.getPlayerRank(target).getColor()))
                .append(Component.text(" from your friends list.", NamedTextColor.YELLOW))
                .appendNewline()
                .append(LINE)
        );

        senderPlayer.sendMessage(LINE.appendNewline()
                .append(FoxRankAPI.getPlayerRank(author).getPrefix())
                .append(Component.text(authorPlayer.getUsername(), FoxRankAPI.getPlayerRank(author).getColor()))
                .append(Component.text(" removed you from their friends list.", NamedTextColor.YELLOW))
                .appendNewline()
                .append(LINE)
        );

        plugin.getDatabase().setFriends(author, friends.get(author));
        plugin.getDatabase().setFriends(target, friends.get(target));

    }

    public boolean isFriend(UUID uuid1, UUID uuid2) {
        return friends.get(uuid1) != null && friends.get(uuid2) != null && friends.get(uuid1).contains(uuid2) && friends.get(uuid2).contains(uuid1);
    }

    public List<UUID> getFriends(UUID uuid) {
        if (friends.get(uuid) != null)
            return friends.get(uuid);
        return null;
    }

    public void setFriends(UUID uuid, List<UUID> friends) {
        this.friends.put(uuid, friends);
    }
}
