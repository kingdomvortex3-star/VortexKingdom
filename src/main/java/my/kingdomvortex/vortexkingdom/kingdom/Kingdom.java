package my.kingdomvortex.vortexkingdom.kingdom;

public final class Kingdom {

    private final int id;
    private final String name;
    private final String ownerUuid;
    private final int level;
    private final long experience;
    private final long totalExperience;
    private final long totalKills;
    private final String description;
    private final boolean pvpEnabled;

    public Kingdom(
            int id,
            String name,
            String ownerUuid,
            int level,
            long experience,
            long totalExperience,
            long totalKills,
            String description,
            boolean pvpEnabled
    ) {
        this.id = id;
        this.name = name;
        this.ownerUuid = ownerUuid;
        this.level = level;
        this.experience = experience;
        this.totalExperience = totalExperience;
        this.totalKills = totalKills;
        this.description = description;
        this.pvpEnabled = pvpEnabled;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getOwnerUuid() {
        return ownerUuid;
    }

    public int getLevel() {
        return level;
    }

    public long getExperience() {
        return experience;
    }

    public long getTotalExperience() {
        return totalExperience;
    }

    public long getTotalKills() {
        return totalKills;
    }

    public String getDescription() {
        return description;
    }

    public boolean isPvpEnabled() {
        return pvpEnabled;
    }
}
