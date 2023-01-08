package xyz.kaleidiodev.kaleidiosguns.item;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public class GloveItem extends Item {
    public double percentSpeedUp = 0;

    public GloveItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        if (Screen.hasShiftDown()) {
            if (this.percentSpeedUp > 0) tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.glove.speed_change", percentSpeedUp * 100));
        }
        else tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.shift"));

        tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.glove"));
    }

    public GloveItem setSpeedUp(double speedUp) {
        this.percentSpeedUp = speedUp;
        return this;
    }

    public void consume(ItemStack stack, PlayerEntity player) {
        stack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(player.getUsedItemHand()));
    }
}
