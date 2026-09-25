package dev.aurelium.auraskills.paper.util;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;

/**
 * Paper-only pathfinding helpers. Callers must check {@link PaperUtil#IS_PAPER}
 * before use, since Mob#getPathfinder does not exist on Spigot.
 */
public class EntityPathUtil {

    public static void moveTo(Entity entity, Location location, double speed) {
        if (entity instanceof Mob mob) {
            mob.getPathfinder().moveTo(location, speed);
        }
    }

    public static void stopPath(Entity entity) {
        if (entity instanceof Mob mob) {
            mob.getPathfinder().stopPathfinding();
        }
    }
}
