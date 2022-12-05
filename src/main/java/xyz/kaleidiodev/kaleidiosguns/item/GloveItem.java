package xyz.kaleidiodev.kaleidiosguns.item;

import com.sun.org.apache.xpath.internal.res.XPATHErrorResources_fr;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public class GloveItem extends Item {
    public double percentSpeedUp;
    protected boolean hastey;

    public GloveItem(Properties pProperties, double newPercentSpeedUp) {
        super(pProperties);

        percentSpeedUp = newPercentSpeedUp;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        if (Screen.hasShiftDown()) {
            if (this.hastey) tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.glove.speed_change"));
        }
        else tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.shift"));

        tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.glove"));
    }

    @Override
    public void inventoryTick(ItemStack pStack, World pLevel, Entity pEntity, int pItemSlot, boolean pIsSelected) {
        if ((pEntity instanceof LivingEntity) && (hastey)) {
            LivingEntity entity = (LivingEntity)pEntity;
            if (entity.getOffhandItem().getItem() == this)
            entity.addEffect(new EffectInstance(Effects.DIG_SPEED, 1, 2));
        }
    }

    public GloveItem setGivesHaste(boolean givesHaste) {
        this.hastey = givesHaste;
        return this;
    }
}
