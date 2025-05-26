package com.dddev.DDBS.mixin;

import com.corrinedev.vpb_magazines.json.JsonMagazineItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.item.AirItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;


import java.util.*;


@Mixin(JsonMagazineItem.class)
public abstract class MagazineItemMixin {
    // Helper Methods for managing the NBT String list
    @Unique
    private static List<String> getStringList(ItemStack stack, String key) {
        List<String> values = new ArrayList<>();
        if (stack.hasTag()) {
            CompoundTag tag = stack.getTag();
            if (tag.contains(key, Tag.TAG_LIST)) {
                ListTag list = tag.getList(key, Tag.TAG_STRING);
                for (int i = 0; i < list.size(); i++) {
                    values.add(list.getString(i));
                }
            }
        }
        return values;
    }

    @Unique
    private static void addStringToList(ItemStack stack, String key, String newValue) {
        List<String> values = getStringList(stack, key);
        values.add(newValue);
        setStringList(stack, key, values);
    }

    @Unique
    private static void setStringList(ItemStack stack, String key, List<String> values) {
        CompoundTag tag = stack.getOrCreateTag();
        ListTag list = new ListTag();
        for (String value : values) {
            list.add(StringTag.valueOf(value));
        }
        tag.put(key, list);
        stack.setTag(tag);
    }

    @Unique
    private static void removeLastStringFromList(ItemStack stack, String key) {
        List<String> values = getStringList(stack, key);
        if (!values.isEmpty()) {
            values.remove(values.size() - 1);
            setStringList(stack, key, values);
        }
    }

    @Unique
    private static String getLastStringFromList(ItemStack stack, String key) {
        List<String> values = getStringList(stack, key);
        if (!values.isEmpty()) {
            return values.get(values.size() - 1);
        }
        return null;
    }

    /**
     * @author dddev
     * @reason reloading with bulletTypes and order
     */
    @Overwrite
    public boolean overrideOtherStackedOnMe(@NotNull ItemStack itemStack, ItemStack stack, Slot slot,
                                            ClickAction click, Player player, SlotAccess slotAccess) {
        boolean deny = false;
        CompoundTag tagAmmo = stack.getOrCreateTag();


        for(String itemId : ((JsonMagazineItem)(Object)this).ammoIds) {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
            if (stack.is(item) && itemStack.getDamageValue() != 0) {
                deny = true;
                player.playSound(SoundEvents.METAL_STEP, 1.0F, 1.0F);

                // Check that the ammo has a Bullet "Type" string.
                if (stack.getTag() != null && stack.getTag().contains("Type", Tag.TAG_STRING)) {
                    // Retrieve the bullet type from the ammo's tag.
                    String loadBulletType = tagAmmo.getString("Type");
                    // If the bullet type is null or empty exit
                    if (loadBulletType == null || loadBulletType.isEmpty()) {
                        return false;
                    }


                    if (click == ClickAction.PRIMARY) {
                        while(itemStack.getDamageValue() != 0 && stack.getCount() != 0) {
                            addStringToList(itemStack, "Mag", loadBulletType);
                            itemStack.getOrCreateTag().putString("Cal", stack.getItem().getDescriptionId().substring(stack.getItem().getDescriptionId().lastIndexOf('.')+1));
                            itemStack.setDamageValue(itemStack.getDamageValue() - 1);
                            stack.setCount(stack.getCount() - 1);
                        }
                    } else {
                        addStringToList(itemStack, "Mag", loadBulletType);
                        itemStack.getOrCreateTag().putString("Cal", stack.getItem().getDescriptionId().substring(stack.getItem().getDescriptionId().lastIndexOf('.')+1));
                        itemStack.setDamageValue(itemStack.getDamageValue() - 1);
                        stack.setCount(stack.getCount() - 1);
                    }
                }
            }
        }

        return deny;
    }

    /**
     * @author dddev
     * @reason loading with bulletTypes and order
     */
    @Overwrite
    public boolean overrideStackedOnOther(ItemStack magazinestack, Slot slot, ClickAction click, Player player) {
        boolean deny = false;

        for(String itemId : ((JsonMagazineItem)(Object)this).ammoIds) {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
            if (slot.getItem().is(item) && click == ClickAction.SECONDARY) {
                deny = true;
            }
            // Get the last bullet from the magazine's "LoadedBullets"/"Mag" list.
            String lastBullet = getLastStringFromList(magazinestack, "Mag");
            // If there is no bullet loaded, do nothing.
            if (lastBullet == null || lastBullet.isEmpty()) {
                continue;
            }
            // Create a new ammo ItemStack with the same item.
            ItemStack unloadedBullet = new ItemStack(item);
            unloadedBullet.getOrCreateTag().putString("Type", lastBullet);

            if (slot.getItem().getCount() != slot.getItem().getMaxStackSize() &&
                    magazinestack.getDamageValue() != magazinestack.getMaxDamage() &&
                    ((click == ClickAction.SECONDARY && slot.getItem().getItem() instanceof AirItem) || slot.getItem().is(item))) {
                deny = true;
                if (slot.getItem().getItem() instanceof AirItem) {
                    slot.set(unloadedBullet);
                } else {
                    String bulletTag = unloadedBullet.getOrCreateTag().getString("Type");
                    String slotAmmo = slot.getItem().getOrCreateTag().getString("Type");
                    if (Objects.equals(bulletTag, slotAmmo)) {
                        slot.getItem().setCount(slot.getItem().getCount() + 1);
                    } else {
                        player.getInventory().placeItemBackInInventory(unloadedBullet);
                    }
                }
                removeLastStringFromList(magazinestack, "Mag");
                magazinestack.setDamageValue(magazinestack.getDamageValue() + 1);
                return true;
            }
        }
        return deny;
    }
    }
