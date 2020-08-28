package lykrast.gunswithoutroses.item;

import java.util.List;

import javax.annotation.Nullable;

import lykrast.gunswithoutroses.entity.BulletEntity;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class BulletItem extends Item {
	private int damage;

	public BulletItem(Properties properties, int damage) {
		super(properties);
		this.damage = damage;
	}

	public BulletEntity createProjectile(World world, ItemStack stack, LivingEntity shooter) {
		BulletEntity entity = new BulletEntity(world, shooter);
		entity.setStack(stack);
		entity.setDamage(damage);
		return entity;
	}

	/**
	 * Uses up 1 item worth of ammo. Can be used for RF or magic based bullet
	 * pouches or something.
	 */
	public void consume(ItemStack stack, PlayerEntity player) {
		stack.shrink(1);
		if (stack.isEmpty()) {
			player.inventory.deleteStack(stack);
		}
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
		tooltip.add(new TranslationTextComponent("tooltip.gunswithoutroses.bullet.damage", damage).func_240699_a_(TextFormatting.DARK_GREEN));
	}

}
