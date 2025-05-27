package com.dddev.DDBS.utils;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;


public class BulletManager {

    public static final BulletPool POOL = new BulletPool(20000);

    public static class BulletData {
        public double posX, posY, posZ;
        public double velX, velY, velZ;
        public Entity shooter;
        public float dmg, ap;
        public Level level;
        public int age;

        public void reset(double x, double y, double z, double vx, double vy, double vz, Entity shooter, float damage, float ap, Level level) {
            this.posX = x;
            this.posY = y;
            this.posZ = z;
            this.velX = vx;
            this.velY = vy;
            this.velZ = vz;
            this.shooter = shooter;
            this.dmg = damage;
            this.ap = ap;
            this.level = level;
            this.age = 0;
        }
    }

    public static class BulletPool {
        private final BulletData[] bullets;
        private final ArrayDeque<Integer> freeStack = new ArrayDeque<>();
        private final IntArrayList activeIndices = new IntArrayList();
        private final Int2IntMap indexMap = new Int2IntOpenHashMap();

        public BulletPool(int capacity) {
            bullets = new BulletData[capacity];
            for (int i = 0; i < capacity; i++) {
                bullets[i] = new BulletData();
                freeStack.push(i);
            }
        }

        public BulletData shoot(double x, double y, double z, double vx, double vy, double vz, Entity shooter, float dmg, float ap, Level level) {
            if (freeStack.isEmpty()) return null;

            int index = freeStack.pop();
            BulletData bullet = bullets[index];

            bullet.reset(x, y, z, vx, vy, vz, shooter, dmg, ap, level);

            activeIndices.add(index);
            indexMap.put(index, activeIndices.size() - 1);
            return bullet;
        }

        public void destroy(int bulletIndex) {
            int listIndex = indexMap.getOrDefault(bulletIndex, -1);
            if (listIndex == -1) return;

            // Swap con último elemento
            int lastIdx = activeIndices.size() - 1;
            int lastBulletIdx = activeIndices.getInt(lastIdx);
            activeIndices.set(listIndex, lastBulletIdx);
            activeIndices.removeInt(lastIdx);

            // Actualizar mapa solo si hubo swap
            if (listIndex != lastIdx) {
                indexMap.put(lastBulletIdx, listIndex);
            }
            indexMap.remove(bulletIndex);
            freeStack.push(bulletIndex);
        }

        public BulletData[] getAllBullets() {
            return bullets;
        }

        public IntArrayList getActiveIndices() {
            return activeIndices;
        }
    }




    private static final float TICK_DURATION = 0.05f;
    private static final float GRAVITY_PER_TICK = 0.49f;
    private static final float AIR_RESISTANCE = 0.98f;
    private static final int MAX_TICKS = 1000;



    private static void updatePhysics(BulletData bullet) {
        bullet.velX *= AIR_RESISTANCE;
        bullet.velY = (bullet.velY * AIR_RESISTANCE) - GRAVITY_PER_TICK;
        bullet.velZ *= AIR_RESISTANCE;

        bullet.posX += bullet.velX * TICK_DURATION;
        bullet.posY += bullet.velY * TICK_DURATION;
        bullet.posZ += bullet.velZ * TICK_DURATION;
    }


    private static void checkCollisions(BulletData bullet, int idx) {
        Vec3 start = new Vec3(
                bullet.posX - bullet.velX * TICK_DURATION,
                bullet.posY - bullet.velY * TICK_DURATION,
                bullet.posZ - bullet.velZ * TICK_DURATION
        );
        Vec3 end = new Vec3(bullet.posX, bullet.posY, bullet.posZ);

        BlockHitResult blockHit = bullet.level.clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, bullet.shooter
        ));

        EntityHitResult entityHit = findEntityAlongPath(bullet.level, start, end, bullet.shooter);
        HitResult finalHit = determineClosestHit(start, blockHit, entityHit);

        if (finalHit != null) {
            handleImpact(finalHit, bullet.shooter);
            POOL.destroy(idx);
        }
    }

    @Nullable
    private static EntityHitResult findEntityAlongPath(Level level, Vec3 start, Vec3 end, Entity shooter) {
        Vec3 rayDir = end.subtract(start);

        // Manejo de división por cero para rayDirInv
        double invX = rayDir.x != 0 ? 1.0 / rayDir.x : Double.POSITIVE_INFINITY;
        double invY = rayDir.y != 0 ? 1.0 / rayDir.y : Double.POSITIVE_INFINITY;
        double invZ = rayDir.z != 0 ? 1.0 / rayDir.z : Double.POSITIVE_INFINITY;

        AABB rayBox = new AABB(start, end); // No se infla

        List<Entity> entities = level.getEntities(shooter, rayBox);

        Entity closestEntity = null;
        Vec3 closestHit = null;
        double closestDistance = Double.MAX_VALUE;

        for (Entity entity : entities) {
            if (entity.isSpectator() || !entity.isPickable()) continue;

            AABB box = entity.getBoundingBox(); // sin inflate

            // === intersectRayAABB inline ===
            double tmin = 0.0;
            double tmax = Double.MAX_VALUE;

            for (int axis = 0; axis < 3; axis++) {
                double origin = axis == 0 ? start.x : (axis == 1 ? start.y : start.z);
                double invDir = axis == 0 ? invX : (axis == 1 ? invY : invZ);
                double bmin = axis == 0 ? box.minX : (axis == 1 ? box.minY : box.minZ);
                double bmax = axis == 0 ? box.maxX : (axis == 1 ? box.maxY : box.maxZ);

                double t1 = (bmin - origin) * invDir;
                double t2 = (bmax - origin) * invDir;

                double tNear = Math.min(t1, t2);
                double tFar = Math.max(t1, t2);

                tmin = Math.max(tmin, tNear);
                tmax = Math.min(tmax, tFar);
            }

            if (tmin > tmax) continue; // No intersección

            // Si hay intersección, intentamos clip
            Optional<Vec3> hitOpt = box.clip(start, end);
            if (hitOpt.isPresent()) {
                double dist = start.distanceToSqr(hitOpt.get());
                if (dist < closestDistance) {
                    closestEntity = entity;
                    closestHit = hitOpt.get();
                    closestDistance = dist;
                }
            }
        }

        return closestEntity != null ? new EntityHitResult(closestEntity, closestHit) : null;
    }


    @Nullable
    private static HitResult determineClosestHit(Vec3 start, BlockHitResult blockHit, EntityHitResult entityHit) {
        double blockDist = blockHit.getType() == HitResult.Type.BLOCK ?
                start.distanceToSqr(blockHit.getLocation()) : Double.MAX_VALUE;
        double entityDist = entityHit != null ?
                start.distanceToSqr(entityHit.getLocation()) : Double.MAX_VALUE;

        if (blockDist < entityDist) {
            return blockHit;
        } else if (entityHit != null) {
            return entityHit;
        }
        return null;
    }

    private static void handleImpact(HitResult result, Entity shooter) {
        if (result.getType() == HitResult.Type.ENTITY) {
            DamageSource source = shooter.level().damageSources().generic();
            Entity target = ((EntityHitResult) result).getEntity();
            target.hurt(source, 10);
            // Apply damage to target
        } else if (result.getType() == HitResult.Type.BLOCK) {
            BlockPos hitPos = ((BlockHitResult) result).getBlockPos();
            // Create impact effect
        }
    }

    public static void serverTick() {
        BulletData[] bullets = POOL.getAllBullets();
        IntArrayList active = POOL.getActiveIndices();

        for (int i = 0; i < active.size();) {
            int idx = active.getInt(i);
            BulletData bullet = bullets[idx];

            // 1. Actualizar física
            updatePhysics(bullet);

            // 2. Detección de colisiones
            checkCollisions(bullet, idx);

            // 3. Verificar expiración
            if (++bullet.age > MAX_TICKS) {
                POOL.destroy(idx);
            } else {
                i++;
            }
        }
    }
}