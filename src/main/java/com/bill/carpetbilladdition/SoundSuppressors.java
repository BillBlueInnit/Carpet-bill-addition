package com.bill.carpetbilladdition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.SculkSensorBlockEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.phys.Vec3;

/**
 * Keeps track of the sound suppressors in each level for soundSuppressionRange.
 * <p>
 * A sound suppressor is a (calibrated) sculk sensor block entity whose block state is not a sculk sensor: it throws
 * as soon as a vibration reaches it. Vanilla only offers a game event to listeners in the chunk sections within the
 * event's notification radius (16 blocks for most events), and then only if they are within their listener radius
 * (8 for sculk sensors, 16 for calibrated ones). With the rule set, a suppressor gets every game event within the
 * rule's range instead:
 * <ul>
 *     <li>in the sections vanilla looks at, its listener radius is replaced by the range;</li>
 *     <li>suppressors in other sections get the event right after vanilla's pass.</li>
 * </ul>
 * Normal sculk sensors are never affected. The extra work per game event is a loop over the suppressors of the level,
 * which is empty in most worlds.
 */
public final class SoundSuppressors {
    private static final Map<ServerLevel, Map<GameEventListener, SculkSensorBlockEntity>> SUPPRESSORS = new WeakHashMap<>();

    private SoundSuppressors() {
    }

    public static boolean isSuppressor(SculkSensorBlockEntity sensor) {
        return !sensor.getType().isValid(sensor.getBlockState());
    }

    /**
     * Called when a sculk sensor block entity is registered as a game event listener or gets a new block state.
     */
    public static void update(ServerLevel level, SculkSensorBlockEntity sensor) {
        if (isSuppressor(sensor)) {
            SUPPRESSORS.computeIfAbsent(level, key -> new LinkedHashMap<>()).put(sensor.getListener(), sensor);
        } else {
            remove(level, sensor);
        }
    }

    public static void remove(ServerLevel level, SculkSensorBlockEntity sensor) {
        Map<GameEventListener, SculkSensorBlockEntity> suppressors = SUPPRESSORS.get(level);
        if (suppressors != null) {
            suppressors.remove(sensor.getListener());
        }
    }

    /**
     * The listener radius vanilla checks in the sections it looks at.
     */
    public static int listenerRadius(ServerLevel level, GameEventListener listener, int vanillaRadius) {
        int range = CbaSettings.soundSuppressionRange;
        if (range <= 0) {
            return vanillaRadius;
        }
        Map<GameEventListener, SculkSensorBlockEntity> suppressors = SUPPRESSORS.get(level);
        if (suppressors == null) {
            return vanillaRadius;
        }
        SculkSensorBlockEntity sensor = suppressors.get(listener);
        return sensor != null && isTracked(sensor) ? range : vanillaRadius;
    }

    /**
     * Offers the game event to suppressors within range that are outside the sections vanilla looked at.
     */
    public static void postToSuppressorsOutsideScannedSections(ServerLevel level, Holder<GameEvent> event, Vec3 source, GameEvent.Context context) {
        int range = CbaSettings.soundSuppressionRange;
        Map<GameEventListener, SculkSensorBlockEntity> suppressors = SUPPRESSORS.get(level);
        if (range <= 0 || suppressors == null || suppressors.isEmpty()) {
            return;
        }
        // The same section box as GameEventDispatcher.post()
        int radius = event.value().notificationRadius();
        BlockPos center = BlockPos.containing(source);
        int minX = SectionPos.blockToSectionCoord(center.getX() - radius);
        int minY = SectionPos.blockToSectionCoord(center.getY() - radius);
        int minZ = SectionPos.blockToSectionCoord(center.getZ() - radius);
        int maxX = SectionPos.blockToSectionCoord(center.getX() + radius);
        int maxY = SectionPos.blockToSectionCoord(center.getY() + radius);
        int maxZ = SectionPos.blockToSectionCoord(center.getZ() + radius);
        long rangeSqr = (long) range * range;
        // A copy, because a vibration can change blocks and with them the suppressors
        for (SculkSensorBlockEntity sensor : new ArrayList<>(suppressors.values())) {
            if (!isTracked(sensor)) {
                suppressors.remove(sensor.getListener());
                continue;
            }
            BlockPos pos = sensor.getBlockPos();
            int x = SectionPos.blockToSectionCoord(pos.getX());
            int y = SectionPos.blockToSectionCoord(pos.getY());
            int z = SectionPos.blockToSectionCoord(pos.getZ());
            if (x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ) {
                continue; // vanilla offered it the event already, with the radius from listenerRadius()
            }
            GameEventListener listener = sensor.getListener();
            Optional<Vec3> listenerPosition = listener.getListenerSource().getPosition(level);
            // The same distance check as EuclideanGameEventListenerRegistry
            if (listenerPosition.isPresent() && BlockPos.containing(listenerPosition.get()).distSqr(center) <= rangeSqr) {
                listener.handleGameEvent(level, event, context, source);
            }
        }
    }

    private static boolean isTracked(SculkSensorBlockEntity sensor) {
        return !sensor.isRemoved() && isSuppressor(sensor);
    }
}
