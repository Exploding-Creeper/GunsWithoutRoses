package xyz.kaleidiodev.kaleidiosguns.event;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.kaleidiodev.kaleidiosguns.KaleidiosGuns;
import xyz.kaleidiodev.kaleidiosguns.item.GloveItem;
import xyz.kaleidiodev.kaleidiosguns.item.GunItem;

@Mod.EventBusSubscriber(modid = KaleidiosGuns.MODID)
public class WeaponSwitchEvent {
    @SubscribeEvent
    public static void onWeaponSwitch(LivingEquipmentChangeEvent event) {
        //if switching to gun, consume glove
        if ((event.getTo().getItem() instanceof GunItem) && (event.getEntity() instanceof PlayerEntity)) {
            PlayerEntity livingEntity = (PlayerEntity)event.getEntity();

            if (livingEntity.getOffhandItem().getItem() instanceof GloveItem) {
                GloveItem glove = (GloveItem)livingEntity.getOffhandItem().getItem();
                glove.consume(livingEntity.getOffhandItem(), livingEntity);
            }
        }
    }
}
