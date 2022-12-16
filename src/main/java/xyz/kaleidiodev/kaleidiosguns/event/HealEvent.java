package xyz.kaleidiodev.kaleidiosguns.event;

import net.minecraft.particles.ParticleTypes;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.kaleidiodev.kaleidiosguns.KaleidiosGuns;

@Mod.EventBusSubscriber(modid = KaleidiosGuns.MODID)
public class HealEvent {
    @SubscribeEvent
    public static void livingHealEvent(LivingHealEvent event)
    {
        //if the damage is negative, such as gun mod provided damage
        if (event.getEntity().level.isClientSide) {
            event.getEntity().level.addParticle(ParticleTypes.HAPPY_VILLAGER, event.getEntity().getX(), event.getEntity().getY() + 1, event.getEntity().getZ(), Math.random() * 0.2, Math.random() * 0.3, Math.random() * 0.2);
        }
    }
}
