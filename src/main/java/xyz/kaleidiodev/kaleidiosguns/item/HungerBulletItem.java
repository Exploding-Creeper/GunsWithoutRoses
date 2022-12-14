package xyz.kaleidiodev.kaleidiosguns.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import xyz.kaleidiodev.kaleidiosguns.config.KGConfig;
import xyz.kaleidiodev.kaleidiosguns.entity.BulletEntity;

import javax.annotation.Nullable;
import java.util.List;

public class HungerBulletItem extends BulletItem {

	public HungerBulletItem(Properties properties, double damage) {
		super(properties, damage);
	}

	@Override
	public void consume(ItemStack stack, PlayerEntity player, ItemStack gunItem) {
		player.causeFoodExhaustion(costToUse(gunItem) * 3);
		stack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(player.getUsedItemHand()));
	}

	//this bullet type allows for suicide kills
	@Override
	public boolean hasAmmo(ItemStack stack, PlayerEntity player, ItemStack gunItem) {
		return player.getFoodData().getFoodLevel() >= costToUse(gunItem) * 3;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
		super.appendHoverText(stack, worldIn, tooltip, flagIn);
		tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.hunger_bullet").withStyle(TextFormatting.GRAY));
	}

	public static boolean isShot(ItemStack stack) {
		return !stack.isEmpty() && stack.getOrCreateTag().contains("shot");
	}
}
