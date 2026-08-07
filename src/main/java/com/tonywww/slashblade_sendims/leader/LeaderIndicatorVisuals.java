package com.tonywww.slashblade_sendims.leader;

import com.tonywww.slashblade_sendims.SBSDValues;
import org.joml.Vector3f;

public final class LeaderIndicatorVisuals {
    public static final Vector3f DANGER_START_COLOR = new Vector3f(1.0F, 1.0F, 1.0F);
    public static final Vector3f DANGER_END_COLOR = new Vector3f(0.3F, 0.1F, 0.1F);
    public static final int STUN_COLOR = 0xFFFFC83D;
    public static final int MANAGED_WARNING_DURATION_TICKS = SBSDValues.PARRY_TICK + 2;

    private LeaderIndicatorVisuals() {
    }

    public static float dangerProgress(int remainingTicks) {
        int ticksBeforeAttack = Math.max(0, remainingTicks - 2);
        return 1.0F - Math.min(1.0F,
                ticksBeforeAttack / (float) SBSDValues.PARRY_TICK);
    }

    public static Vector3f dangerColor(int remainingTicks) {
        float progress = dangerProgress(remainingTicks);
        return new Vector3f(
                lerp(DANGER_START_COLOR.x(), DANGER_END_COLOR.x(), progress),
                lerp(DANGER_START_COLOR.y(), DANGER_END_COLOR.y(), progress),
                lerp(DANGER_START_COLOR.z(), DANGER_END_COLOR.z(), progress));
    }

    public static int dangerArgb(int remainingTicks) {
        Vector3f color = dangerColor(remainingTicks);
        return 0xFF000000
                | Math.round(color.x() * 255.0F) << 16
                | Math.round(color.y() * 255.0F) << 8
                | Math.round(color.z() * 255.0F);
    }

    private static float lerp(float start, float end, float progress) {
        return start + (end - start) * progress;
    }
}