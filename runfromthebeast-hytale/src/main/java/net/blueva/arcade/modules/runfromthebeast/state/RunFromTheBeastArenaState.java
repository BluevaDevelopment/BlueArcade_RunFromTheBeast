package net.blueva.arcade.modules.runfromthebeast.state;

import com.hypixel.hytale.math.vector.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RunFromTheBeastArenaState {

    private UUID beastId;
    private boolean ended;
    private int timeLeftSeconds;
    private int releaseTimeSeconds;
    private List<String> cageBlocks = new ArrayList<>();
    private final Map<UUID, Location> checkpoints = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> checkpointUses = new ConcurrentHashMap<>();

    public UUID getBeastId() {
        return beastId;
    }

    public void setBeastId(UUID beastId) {
        this.beastId = beastId;
    }

    public boolean isEnded() {
        return ended;
    }

    public void setEnded(boolean ended) {
        this.ended = ended;
    }

    public int getTimeLeftSeconds() {
        return timeLeftSeconds;
    }

    public void setTimeLeftSeconds(int timeLeftSeconds) {
        this.timeLeftSeconds = timeLeftSeconds;
    }

    public int getReleaseTimeSeconds() {
        return releaseTimeSeconds;
    }

    public void setReleaseTimeSeconds(int releaseTimeSeconds) {
        this.releaseTimeSeconds = releaseTimeSeconds;
    }

    public List<String> getCageBlocks() {
        return cageBlocks;
    }

    public void setCageBlocks(List<String> cageBlocks) {
        this.cageBlocks = cageBlocks != null ? cageBlocks : new ArrayList<>();
    }

    public Map<UUID, Location> getCheckpoints() {
        return checkpoints;
    }

    public Map<UUID, Integer> getCheckpointUses() {
        return checkpointUses;
    }
}
