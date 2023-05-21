package xyz.kaleidiodev.kaleidiosguns.event;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.kaleidiodev.kaleidiosguns.KaleidiosGuns;
import xyz.kaleidiodev.kaleidiosguns.item.GunItem;

@Mod.EventBusSubscriber(modid = KaleidiosGuns.MODID)
public class ClientGunUseEvent {
    //don't allow the weapon to fire if the gun is still in attack cooldown.
    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onClickEvent(InputEvent.ClickInputEvent event) {
        PlayerEntity player = Minecraft.getInstance().player;
        if (event.isUseItem() && (player != null)) {
            ItemStack gun = player.getItemInHand(event.getHand());
            if (gun != ItemStack.EMPTY) {
                if (gun.getItem() instanceof GunItem) {
                    GunItem gunItem = (GunItem)gun.getItem();

                    if (1 > player.getAttackStrengthScale(0)) {
                        event.setCanceled(true);
                    }
                }
            }
        }
    }
}
