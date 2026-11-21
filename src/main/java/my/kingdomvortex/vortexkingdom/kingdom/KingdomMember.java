package my.kingdomvortex.vortexkingdom.kingdom;

import java.util.Locale;
import java.util.UUID;

public final class KingdomMember {

    public enum Role {
        OWNER,
        LEADER,
        OFFICER,
        MEMBER;

        public static Role fromString(String value) {
            if (value == null) {
                return MEMBER;
            }

            try {
                return Role.valueOf(value.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                return MEMBER;
            }
        }
    }

    private final int kingdomId;
    private final String playerUuid;
    private final String playerName;
    private final Role role;
    private final long joinedAt;

    public KingdomMember(
            int kingdomId,
            String playerUuid,
            String playerName,
            Role role,
            long joinedAt
    ) {
        this.kingdomId = kingdomId;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.role = role == null ? Role.MEMBER : role;
        this.joinedAt = joinedAt;
    }

    public int getKingdomId() {
        return kingdomId;
    }

    public String getPlayerUuid() {
        return playerUuid;
    }

    public UUID getPlayerUniqueId() {
        try {
            return UUID.fromString(playerUuid);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    public String getPlayerName() {
        return playerName;
    }

    public Role getRole() {
        return role;
    }

    public long getJoinedAt() {
        return joinedAt;
    }

    public boolean isOwner() {
        return role == Role.OWNER;
    }
}
