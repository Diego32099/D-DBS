package com.dddev.DDBS.mixin;

import com.corrinedev.vpb_magazines.ItemUtil;
import com.corrinedev.vpb_magazines.json.JsonMagazineItem;
import com.vicmatskiv.pointblank.item.FireModeInstance;
import com.vicmatskiv.pointblank.item.GunItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;


@Mixin(value = GunItem.class, remap = false, priority = 1300)
public abstract class LoadMagazineInGun {

    @Unique
    private static List<String> getStringList(ItemStack stack, String key) {
        List<String> values = new ArrayList<>();
        if (stack.hasTag()) {
            CompoundTag tag = stack.getTag();
            if (tag.contains(key, Tag.TAG_LIST)) { // Check if key exists and is a list
                ListTag list = tag.getList(key, Tag.TAG_STRING); // Get list as String NBT
                for (int i = 0; i < list.size(); i++) {
                    values.add(list.getString(i));
                }
            }
        }
        return values;
    }

    @Unique
    private static void setStringList(ItemStack stack, String key, List<String> values) {
        CompoundTag tag = stack.getOrCreateTag();
        ListTag list = new ListTag();
        for (String value : values) {
            list.add(StringTag.valueOf(value)); // Convert string to NBT format
        }
        tag.put(key, list);
        stack.setTag(tag);
    }

    @Unique
    private static void copyStringList(ItemStack source, ItemStack target, String key) {
        List<String> values = getStringList(source, key);
        setStringList(target, key, values);
    }

    @Shadow
        public static void setAmmo(ItemStack itemStack, FireModeInstance fireModeInstance, int ammo) {}

        @Shadow
        public static int getAmmo(ItemStack itemStack, FireModeInstance fireModeInstance) {
            return 0;
        }

        @Shadow
        private static boolean isCompatibleBullet(Item item, ItemStack gunStack, FireModeInstance fireModeInstance) {
            return false;
        }

        @Shadow
        public abstract int getMaxAmmoCapacity(ItemStack itemStack, FireModeInstance fireModeInstance);

        /**
         * @author dddev
         * @reason Magazine reload compatibility
         */
        @Overwrite
        int reloadGun(ItemStack gunStack, Player player, FireModeInstance fireModeInstance) {
            int ammo = 0;
            if (!(gunStack.getItem() instanceof GunItem)) {
                return 0;
            }

            GunItem gunItem = (GunItem) gunStack.getItem();
            System.out.println("HAS MAGAZINE COMPAT = " + ItemUtil.magazineItems.contains(gunItem));
            System.out.println("GUN DETAILS = " + gunItem.builtInRegistryHolder().key());
            System.out.println("GUN ID = " + gunItem);

            if (!gunItem.isEnabled()) {
                return 0;
            }

            if (player.getAbilities().instabuild) {
                setAmmo(gunStack, fireModeInstance, getMaxAmmoCapacity(gunStack, fireModeInstance));
                return getMaxAmmoCapacity(gunStack, fireModeInstance);
            }

            if (!ItemUtil.magazineItems.contains(gunItem) || !fireModeInstance.isUsingDefaultAmmoPool()) {
                int maxCapacity = gunItem.getMaxAmmoCapacity(gunStack, fireModeInstance);
                int currentAmmo = getAmmo(gunStack, fireModeInstance);
                int neededAmmo = maxCapacity - currentAmmo;

                if (neededAmmo <= 0) {
                    return 0;
                }

                if (player.isCreative()) {
                    int newAmmo = currentAmmo + neededAmmo;
                    setAmmo(gunStack, fireModeInstance, newAmmo);
                    return newAmmo;
                }

                int foundAmmoCount = 0;

                for (int i = 0; i < player.getInventory().items.size(); ++i) {
                    ItemStack inventoryItem = player.getInventory().items.get(i);
                    if (isCompatibleBullet(inventoryItem.getItem(), gunStack, fireModeInstance)) {
                        int availableBullets = inventoryItem.getCount();
                        if (availableBullets <= neededAmmo) {
                            foundAmmoCount += availableBullets;
                            neededAmmo -= availableBullets;
                            player.getInventory().items.set(i, ItemStack.EMPTY);
                        } else {
                            inventoryItem.shrink(neededAmmo);
                            foundAmmoCount += neededAmmo;
                            neededAmmo = 0;
                        }

                        if (neededAmmo == 0) {
                            break;
                        }
                    }
                }

                int newAmmo = currentAmmo + foundAmmoCount;
                setAmmo(gunStack, fireModeInstance, newAmmo);
                return newAmmo;
            }

            for (int i = 0; i < 36; ++i) {
                ItemStack invStack = player.getInventory().items.get(i);
                Item neededAmmo = invStack.getItem();

                if (neededAmmo instanceof JsonMagazineItem) {
                    JsonMagazineItem magazineItem = (JsonMagazineItem) neededAmmo;
                    // Check if the magazine is not empty and is compatible with this gun.
                    if (invStack.getDamageValue() != invStack.getMaxDamage() &&
                            magazineItem.compatibleGunIds.contains("pointblank:" + gunItem.toString())) {

                        // Ensure the gun's NBT has a "mag_id" key
                        CompoundTag gunTag = gunStack.getOrCreateTag();
                        if (!gunTag.contains("mag_id")) {
                            gunTag.putString("mag_id", "empty");
                        }

                        // Get the magazine stack from the player's slot.
                        ItemStack magstack = player.getSlot(i).get();
                        // Create a new magazine item stack from the resource location stored in the gun's mag_id tag.
                        String magId = gunStack.getOrCreateTag().getString("mag_id");
                        ItemStack stackmag = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(magId)));

                        // Update the gun's mag_id to the magazine in this slot.
                        List<String> NewMagBullets = getStringList(gunStack, "Mag");
                        copyStringList(invStack, gunStack, "Mag");
                        gunTag.putString("Cal", invStack.getOrCreateTag().getString("Cal"));
                        gunTag.putString("mag_id", "pointblank:" + player.getSlot(i).get().getItem().toString());

                        // Remove the magazine from the slot and replace it with an updated one.
                        player.getSlot(i).set(new ItemStack(net.minecraft.world.item.Items.AIR));
                        // Set the magazine's damage to reflect the ammo loaded.
                        stackmag.setDamageValue(stackmag.getMaxDamage() - getAmmo(gunStack, fireModeInstance));
                        player.getSlot(i).set(stackmag);
                        setStringList(stackmag, "Mag", NewMagBullets);

                        // Update the gun's ammo based on the magazine's ammo count.
                        setAmmo(gunStack, fireModeInstance, magstack.getMaxDamage() - magstack.getDamageValue());
                        ammo = Math.abs(invStack.getDamageValue() - invStack.getMaxDamage());

                        // Copy the "LoadedBullets"/"Mag" string list from the magazine to the gun.
                        return ammo;
                    }
                }
                // If we reach the last slot and no valid magazine was found, return 0.
                if (i == 36) {
                    return 0;
                }
            }
            return ammo;
        }
    }
