package com.dddev.DDBS.utils;

import net.minecraft.world.phys.Vec3;
import java.util.concurrent.ThreadLocalRandom;

public class BulletDispersion {
    private static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

    public static class TangentPlane {
        public final Vec3 u;
        public final Vec3 v;

        public TangentPlane(Vec3 direction) {
            Vec3 dirNormalized = direction.normalize();

            // Improved arbitrary vector selection
            Vec3 arbitrary = Math.abs(dirNormalized.y) > 0.999 ?
                    new Vec3(1, 0, 0) : new Vec3(0, 1, 0);

            this.u = dirNormalized.cross(arbitrary).normalize();
            this.v = dirNormalized.cross(u).normalize();
        }
    }

    public static Vec3 applyUniformDispersion(Vec3 velocity, TangentPlane plane, double radius) {
        double speed = velocity.length();
        Vec3 normalizedDir = velocity.normalize();

        double theta = RANDOM.nextDouble(Math.PI * 2);
        double r = radius * Math.sqrt(RANDOM.nextDouble());

        Vec3 dispersionOffset = plane.u.scale(r * Math.cos(theta))
                .add(plane.v.scale(r * Math.sin(theta)));

        return normalizedDir.add(dispersionOffset)
                .normalize()
                .scale(speed); // Preserve original speed
    }
}
