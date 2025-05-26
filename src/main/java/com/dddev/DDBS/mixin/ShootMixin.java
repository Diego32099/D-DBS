package com.dddev.DDBS.mixin;

import com.dddev.DDBS.data.AmmoConfig;
import com.dddev.DDBS.utils.BulletManager;
import com.vicmatskiv.pointblank.item.FireModeInstance;
import com.vicmatskiv.pointblank.item.GunItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(value = GunItem.class, remap = false, priority = 1300)
public abstract class ShootMixin {

    @Unique
    private static final Set<String> UNALLOWED_ITEM_IDS = new HashSet<>(Arrays.asList(
            "pointblank:javelin",
            "pointblank:smaw",
            "pointblank:m32mgl",
            "pointblank:at4",
            "pointblank:m134minigun"

    ));

    @Inject(
            method = "handleClientHitScanFireRequest",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onFire(ServerPlayer player, FireModeInstance fireModeInstance, UUID stateId,
                        int slotIndex, int correlationId, boolean isAiming, long requestSeed,
                        CallbackInfo ci) {

        ItemStack gunStack = player.getMainHandItem();
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(gunStack.getItem());


        if (itemId != null && !UNALLOWED_ITEM_IDS.contains(itemId.toString())) {
            CompoundTag gunTag = gunStack.getOrCreateTag();

            // Check if gun has ammo
            if (!gunTag.contains("Mag") || gunTag.getList("Mag", 8).isEmpty()) {
                ci.cancel();
                return;
            }

            // Get the last bullet type from LoadedBullets/Mag array
            ListTag loadedBullets = gunTag.getList("Mag", 8);
            String bulletType = loadedBullets.getString(loadedBullets.size() - 1);

            // Remove the fired bullet from the array
            loadedBullets.remove(loadedBullets.size() - 1);
            gunTag.put("Mag", loadedBullets);
            gunTag.putInt("ammo", loadedBullets.size());

            // Create dummy stack with bullet type
            // When firing:
            Vec3 startPos = player.getEyePosition();
            try {
                AmmoConfig.Caliber caliber = AmmoConfig.Caliber.valueOf(gunTag.getString("Cal"));
                AmmoConfig.AmmoType ammoType = AmmoConfig.AmmoType.valueOf(bulletType);

                AmmoConfig.AmmoData data = AmmoConfig.AMMUNITION
                        .getOrDefault(caliber, Map.of())
                        .get(ammoType);
                Vec3 Velocity = player.getLookAngle().scale(data.v());

                for (int i = 0; i < data.qty(); i++) {


                    BulletManager.POOL.shoot(
                            startPos.x, startPos.y, startPos.z,
                            Velocity.x, Velocity.y, Velocity.z,
                            player,
                            data.dmg(),
                            data.ap(),
                            player.level()
                    );
                }
            } catch (IllegalArgumentException ex) {
                System.err.println("Invalid ammunition config: " + ex.getMessage());
            }
            ci.cancel();
        }
    }
}