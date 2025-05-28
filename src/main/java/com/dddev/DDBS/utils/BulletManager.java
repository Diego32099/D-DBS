package com.dddev.DDBS.utils;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

import java.util.ArrayDeque;
import java.util.List;

import static com.dddev.DDBS.Dbug.TNTSpawner.spawnActiveTNT;
import static com.dddev.DDBS.Dbug.TNTSpawner.spawnActiveTNTXYZ;


public class BulletManager {

    public static final BulletPool POOL = new BulletPool(20000);
    private static List<LivingEntity> currentTickEntities = List.of();


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

            int lastIdx = activeIndices.size() - 1;
            int lastBulletIdx = activeIndices.getInt(lastIdx);
            activeIndices.set(listIndex, lastBulletIdx);
            activeIndices.removeInt(lastIdx);

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
    private static final float AIR_RESISTANCE = 0.99f;
    private static final int MAX_TICKS = 1000;




    private static void SimulateBullet(BulletData bullet, int idx) {
        double startX = bullet.posX;
        double startY = bullet.posY;
        double startZ = bullet.posZ;

        double dx = bullet.velX * TICK_DURATION;
        double dy = bullet.velY * TICK_DURATION;
        double dz = bullet.velZ * TICK_DURATION;

        bullet.posX += dx;
        bullet.posY += dy;
        bullet.posZ += dz;

        bullet.velX *= AIR_RESISTANCE;
        bullet.velY *= AIR_RESISTANCE;
        bullet.velY -= GRAVITY_PER_TICK;
        bullet.velZ *= AIR_RESISTANCE;

        double endX = bullet.posX;
        double endY = bullet.posY;
        double endZ = bullet.posZ;


        Vec3 startVec = new Vec3(startX, startY, startZ);
        Vec3 endVec = new Vec3(endX, endY, endZ);
        Entity[] hitEntity = new Entity[1];
        double[] hitPos = new double[3];

        BlockHitResult blockHit = bullet.level.clip(new ClipContext(
                startVec, endVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, bullet.shooter
        ));


        boolean hasEntityHit = findEntityAlongPath(
                bullet.level,
                startX, startY, startZ,
                endX, endY, endZ,
                bullet.shooter,
                hitEntity, hitPos
        );
        double blockDist = blockHit.getType() == HitResult.Type.BLOCK
                ? startVec.distanceToSqr(blockHit.getLocation()) : Double.MAX_VALUE;

        double entityDist = hasEntityHit
                ? (hitPos[0] - startX) * (hitPos[0] - startX)
                + (hitPos[1] - startY) * (hitPos[1] - startY)
                + (hitPos[2] - startZ) * (hitPos[2] - startZ)
                : Double.MAX_VALUE;


        if (hasEntityHit && entityDist < blockDist) {
            handleEntityImpact(hitEntity[0], hitPos[0], hitPos[1], hitPos[2]);
            POOL.destroy(idx);
        } else if (blockHit.getType() == HitResult.Type.BLOCK) {
            handleBlockImpact(bullet.level, blockHit.getBlockPos(), blockHit.getLocation());
            POOL.destroy(idx);
        }
    }


    private static boolean findEntityAlongPath(Level level,
                                               double startX, double startY, double startZ,
                                               double endX, double endY, double endZ,
                                               Entity shooter,
                                               Entity[] outEntity, double[] outPos) {

        double dx = endX - startX;
        double dy = endY - startY;
        double dz = endZ - startZ;

        double invX = dx != 0.0 ? 1.0 / dx : Double.POSITIVE_INFINITY;
        double invY = dy != 0.0 ? 1.0 / dy : Double.POSITIVE_INFINITY;
        double invZ = dz != 0.0 ? 1.0 / dz : Double.POSITIVE_INFINITY;

        AABB rayBox = new AABB(startX, startY, startZ, endX, endY, endZ).inflate(0.01);
        List<Entity> entities = level.getEntities(shooter, rayBox);

        Entity closestEntity = null;
        double closestDistSq = Double.MAX_VALUE;
        double hitX = 0, hitY = 0, hitZ = 0;

        for (Entity entity : entities) {
            if (!(entity instanceof LivingEntity) || entity.isSpectator()) continue;

            AABB box = entity.getBoundingBox();
            double tmin = 0.0, tmax = Double.MAX_VALUE;

            double t1 = (box.minX - startX) * invX;
            double t2 = (box.maxX - startX) * invX;
            double tNear = Math.min(t1, t2);
            double tFar = Math.max(t1, t2);
            tmin = Math.max(tmin, tNear);
            tmax = Math.min(tmax, tFar);

            t1 = (box.minY - startY) * invY;
            t2 = (box.maxY - startY) * invY;
            tNear = Math.min(t1, t2);
            tFar = Math.max(t1, t2);
            tmin = Math.max(tmin, tNear);
            tmax = Math.min(tmax, tFar);

            t1 = (box.minZ - startZ) * invZ;
            t2 = (box.maxZ - startZ) * invZ;
            tNear = Math.min(t1, t2);
            tFar = Math.max(t1, t2);
            tmin = Math.max(tmin, tNear);
            tmax = Math.min(tmax, tFar);

            if (tmin > tmax || tmax < 0.0) continue;

            double hx = startX + dx * tmin;
            double hy = startY + dy * tmin;
            double hz = startZ + dz * tmin;

            double distSq = (hx - startX) * (hx - startX)
                    + (hy - startY) * (hy - startY)
                    + (hz - startZ) * (hz - startZ);

            if (distSq < closestDistSq) {
                closestDistSq = distSq;
                closestEntity = entity;
                hitX = hx;
                hitY = hy;
                hitZ = hz;
            }
        }

        if (closestEntity != null) {
            outEntity[0] = closestEntity;
            outPos[0] = hitX;
            outPos[1] = hitY;
            outPos[2] = hitZ;
            return true;
        }

        return false;
    }


    private static void handleEntityImpact(Entity entity, double x, double y, double z) {
        entity.hurt(entity.level().damageSources().generic(), 10);
    }

    private static void handleBlockImpact(Level level,BlockPos pos, Vec3 hitVec) {
    }


    public static void serverTick() {
        BulletData[] bullets = POOL.getAllBullets();
        IntArrayList active = POOL.getActiveIndices();

        for (int i = 0; i < active.size();) {
            int idx = active.getInt(i);
            BulletData bullet = bullets[idx];

            SimulateBullet(bullet, idx);

            if (++bullet.age > MAX_TICKS) {
                POOL.destroy(idx);
            } else {
                i++;
            }
        }
    }
}
