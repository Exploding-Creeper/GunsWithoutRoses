package xyz.kaleidiodev.kaleidiosguns.event;

import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.kaleidiodev.kaleidiosguns.KaleidiosGuns;
import xyz.kaleidiodev.kaleidiosguns.item.GunItem;

@Mod.EventBusSubscriber(modid = KaleidiosGuns.MODID)
public class GunDropEvent {
    @SubscribeEvent
    public static void onPlayerDropItem(ItemTossEvent event) {
        ItemEntity itemEntity = event.getEntityItem();
        if (itemEntity != null) {
            ItemStack stack = itemEntity.getItem();
            Item item = stack.getItem();
            if (item instanceof GunItem) {
                CompoundNBT nbt = stack.getOrCreateTag();

                //reset all timers, fixes burst rifles still being bursted after being dropped, and revolvers still being unstable as well
                nbt.putDouble("playerVelocityX", 0);
                nbt.putDouble("playerVelocityY", 0);
                nbt.putDouble("playerVelocityZ", 0);
                nbt.putDouble("previousPosX", 0);
                nbt.putDouble("previousPosY", 0);
                nbt.putDouble("previousPosZ", 0);
                nbt.putInt("stabilizerTimer", 0);
                nbt.putInt("burstTimer", 0);
                nbt.putLong("ticksPassed", 0);

                stack.setTag(nbt);
            }
        }
    }
}
