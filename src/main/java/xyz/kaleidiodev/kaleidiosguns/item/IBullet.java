package xyz.kaleidiodev.kaleidiosguns.item;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.world.World;
import xyz.kaleidiodev.kaleidiosguns.config.KGConfig;
import xyz.kaleidiodev.kaleidiosguns.entity.BulletEntity;
import xyz.kaleidiodev.kaleidiosguns.registry.ModEnchantments;

import javax.annotation.Nullable;

public interface IBullet {
	/**
	 * Creates a projectile and set its stats and stuff. The gun will give it velocity and spawn it in the world.
	 */
	BulletEntity createProjectile(World world, ItemStack stack, LivingEntity shooter, boolean isPlasma);

	/**
	 * Uses up 1 item worth of ammo. Can be used for RF or magic based bullet pouches or something.
	 */
	void consume(ItemStack stack, PlayerEntity player, ItemStack gunItem);

	default int costToUse(ItemStack stack) {
		if (stack.getItem() instanceof GunItem) return ((GunItem)stack.getItem()).getCost();
		else return 1;
	}

	/**
	 * Called on server only when a default projectile (or one that extends it) sucessfully damages a LivingEntity (so after damage).
	 * <br/>May change that later.
	 */
	default void onLivingEntityHit(BulletEntity projectile, LivingEntity target, @Nullable Entity shooter, World world) {
		//if the chance to heal rolls successfully
		if (projectile.rollRewardChance()) {
			//calculate the damage the enemy recieved.
			float damageDelta = projectile.getPreviousHealthOfVictim() - target.getHealth();

			//heal the shooter by a fraction of what damage the enemy recieved.
			LivingEntity shooterEntity = (LivingEntity)shooter;
			//cast to primitive first before casting to float.  thanks forge.
			if (shooterEntity != null) if (shooterEntity.isAlive()) shooterEntity.heal(damageDelta * (float)(double) KGConfig.passionForBloodHealIncrease.get());
		}
	}

	/**
	 * Called on server only as damage is being applied when a bullet carrying this item hits. The target may not be a LivingEntity.
	 * <br/>May change that later.
	 */
	default double modifyDamage(double damage, BulletEntity projectile, Entity target, @Nullable Entity shooter, World world) {
		//if puncturing enchantment is present.
		double newDamage = damage;

		//if the bullet is a plasma type, deal very high damage to a shield if one is in use.
		//this way we let the vanilla mechanic of a shield taking damage as durability into effect
		if ((projectile.isPlasma) && (target instanceof LivingEntity)) {
			LivingEntity livingTarget = (LivingEntity)target;

			if (target instanceof PlayerEntity) {
				PlayerEntity victim = (PlayerEntity) target;
				if (victim.getUseItem().isShield(victim) && (Math.random() < KGConfig.goldPlasmaShieldAdditional.get())) {
					victim.getCooldowns().addCooldown(victim.getUseItem().getItem(), 100);
					victim.stopUsingItem();
					world.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.SHIELD_BREAK, SoundCategory.PLAYERS, 1.0f, 1.0f);
				}
			}

			if (Math.random() < KGConfig.goldPlasmaSlowChance.get()) {
				livingTarget.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, KGConfig.goldPlasmaSlowTicks.get(), 2));
				livingTarget.addEffect(new EffectInstance(Effects.DIG_SLOWDOWN, KGConfig.goldPlasmaSlowTicks.get(), 2));
			}
		}

		//revenge shots should multiply their damage.
		if (projectile.wasRevenge) newDamage *= KGConfig.emeraldBlessedBlessingMultiplier.get();

		if (projectile.hero) newDamage *= KGConfig.heroShotgunEffectMultiplier.get();

		if (projectile.headshotHistory.contains(target)) {
			double actualHeadshot = KGConfig.headshotMultiplier.get();

			//use a logarithm to reduce headshot damage based on projectile protection level on the helmet
			if (target instanceof LivingEntity) {
				LivingEntity livingTarget = (LivingEntity) target;

				ItemStack helmet = livingTarget.getItemBySlot(EquipmentSlotType.HEAD);
				if (helmet != ItemStack.EMPTY) {
					//this works with a level of 0.  that's why the 1 + is in the logarithmic equation
					int projectileProtectionLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PROJECTILE_PROTECTION, helmet);

					double headshotTemp = KGConfig.headshotMultiplier.get() - 1.0;
					headshotTemp *= 1 / (1 + (projectileProtectionLevel * KGConfig.projectileProtectionHelmetHeadshotReduction.get()));

					actualHeadshot = 1 + headshotTemp;
				}
			}

			newDamage *= actualHeadshot;
		}

		//shooting shadow in the dark multiplies damage.
		if (projectile.wasDark) {
			newDamage /= projectile.getShootingGun().getDamageMultiplier(new ItemStack(projectile.getShootingGun().getItem()));
			newDamage *= (projectile.getShootingGun().getDamageMultiplier(new ItemStack(projectile.getShootingGun().getItem())) + KGConfig.shadowRevolverShadowAdditionalMultiplier.get());
		}

		//slag should give multipliers both if shooter or victim are on fire
		if ((projectile.lavaMode & 0x04) != 0) {
			if ((projectile.lavaMode & 0x08) != 0) newDamage *= KGConfig.lavaSmgLavaMultiplier.get();
			if ((projectile.lavaMode & 0x02) != 0) newDamage *= KGConfig.lavaSmgOnFireMultiplier.get();
			if ((projectile.lavaMode & 0x01) != 0) newDamage *= KGConfig.lavaSmgSelfOnFireMultiplier.get();
		}

		if (projectile.isMeleeBonus) newDamage *= KGConfig.emeraldBayonetPostMeleeMultiplier.get();

		//redstone distance multiplies damage.
		if (projectile.redstoneLevel > 0) {
			double multiplierDelta = KGConfig.ironVoltgunMaximumDamage.get() - KGConfig.ironVoltgunMinimumDamage.get();
			int maximumBlocks = (int)(((EnchantmentHelper.getItemEnchantmentLevel( ModEnchantments.signalBoost, new ItemStack(projectile.getShootingGun().getItem())) * KGConfig.signalMultiplier.get()) + 1) * KGConfig.redstoneRadius.get());
			double multiplierPerBlock = multiplierDelta / maximumBlocks;

			newDamage *= ((maximumBlocks - projectile.redstoneLevel) * (multiplierPerBlock)) + KGConfig.ironVoltgunMinimumDamage.get();
		}

		//deal no damage at all if it's for marking
		if (projectile.isGlowing() && !projectile.clip) newDamage /= KGConfig.glowDamageDivider.get();

		return newDamage;
	}

}
