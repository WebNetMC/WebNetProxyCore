package dev.foxikle.mainproxyplugin.data.objects;

import java.util.List;
import java.util.UUID;

public class Party {
    private UUID leader;
    private final List<UUID> members;
    private boolean muted;

    public Party(UUID leader, List<UUID> members) {
        this.leader = leader;
        this.members = members;
        this.muted = false;
    }

    public void transferLeader(UUID uuid) {
        members.remove(uuid);
        leader = uuid;
    }

    public void addPlayer(UUID uuid) {
        members.add(uuid);
    }

    public void removePlayer(UUID uuid) {
        members.remove(uuid);
    }

    public List<UUID> getMembers() {
        return members;
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public UUID getLeader() {
        return leader;
    }
}
