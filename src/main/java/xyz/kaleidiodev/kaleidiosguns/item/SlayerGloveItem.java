package xyz.kaleidiodev.kaleidiosguns.item;

import com.sun.org.apache.xpath.internal.res.XPATHErrorResources_fr;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public class SlayerGloveItem extends Item {
    public double percentSpeedUp;

    public SlayerGloveItem(Properties pProperties, double newPercentSpeedUp) {
        super(pProperties);

        percentSpeedUp = newPercentSpeedUp;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        if (Screen.hasShiftDown()) {
            tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.glove.speed_change", ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(percentSpeedUp * 100.0f)));
        }
        else tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.shift"));
    }
}
