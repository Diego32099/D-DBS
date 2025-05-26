package com.dddev.DDBS.Dbug;

import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.EntityType;

public class TNTSpawner {
    public static void spawnActiveTNT(Level level, Vec3 pos) {
        if (level.isClientSide) return;

        PrimedTnt tnt = new PrimedTnt(
                EntityType.TNT,
                level
        );
        tnt.setPos(pos.x, pos.y, pos.z);
        level.addFreshEntity(tnt);
    }
}