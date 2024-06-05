package xyz.kaleidiodev.kaleidiosguns.item;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import xyz.kaleidiodev.kaleidiosguns.config.KGConfig;
import xyz.kaleidiodev.kaleidiosguns.entity.BulletEntity;
import xyz.kaleidiodev.kaleidiosguns.registry.ModEnchantments;

import javax.annotation.Nullable;
import java.util.List;

public class ShotgunItem extends GunItem {

	private final int bulletCount;
	protected boolean isVampire;
	private int vampireCount;
	private int currentShot;
	private boolean isWave;
	private boolean isSpread;

	public ShotgunItem(Properties properties, int bonusDamage, double damageMultiplier, int fireDelay, double inaccuracy, int enchantability, int bulletCount, double attackSpeed, double attackDamage) {
		super(properties, bonusDamage, damageMultiplier, fireDelay, inaccuracy, enchantability, attackSpeed, attackDamage);
		this.bulletCount = bulletCount;
	}

	@Override
	protected void fireWeapon(World world, PlayerEntity player, ItemStack gun, ItemStack ammo, IBullet bulletItem, boolean bulletFree) {
		//always fire vampire check first
		vampireBulletCount(player);
		//then fire bullet count check.  this will ensure that division only applies to bullets AFTER vampire check has added its own so damage scales accordingly
		for (int i = 0; i < getBulletCount(gun, player); i++) {
			currentShot = i;
			super.fireWeapon(world, player, gun, ammo, bulletItem, bulletFree);
		}
	}

	@Override
	protected void addExtraStatsTooltip(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip) {
		tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.shotgun.shots" + (isProjectileCountModified(stack) ? ".modified" : ""), getBulletCount(stack, null)));
		if (isVampire) tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.vampire_shotgun"));
		if (isWave) tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.wave"));
		if (isSpread) tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.spread"));
	}

	@Override
	public void shootShot(BulletEntity shot, PlayerEntity player, ItemStack gun, double nextInaccuracy) {
		//if (this.isWave) {
			//shoot in a vertical wave by scrolling through xRots step by step, no inaccuracy
		//	float bounds = (float)((KGConfig.heroShotgunInaccuracy.get() * 2) / ((EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.bullseye, gun) * KGConfig.bullseyeAccuracyIncrease.get()) + 1.0));
		//	float shotHorizBound = -bounds / 2; //get only one side of the arc
		//	float shotHorizStep = (bounds / (getBulletCount(gun, player) - 1)); //subtract by one so we don't count the first shot, which is always on the lower bound.
		//	float currentStep = shotHorizBound + (shotHorizStep * currentShot);

		//	shot.heroStep = currentStep;
		//
		//	shot.shootFromRotation(player, player.xRot, player.yRot + currentStep, 0, (float)getProjectileSpeed(gun, player), 0.0F);
		//}
		//else
		super.shootShot(shot, player, gun, nextInaccuracy);
	}

	@Override
	public double getProjectileSpeed(ItemStack stack, @Nullable PlayerEntity player) {
		if (this.isSpread) {
			return super.baseSpeed(stack, null) * (KGConfig.blunderbussMinimumSpeed.get() + (Math.random() * (KGConfig.blunderbussMaximumSpeed.get() - KGConfig.blunderbussMinimumSpeed.get())));
		} else return super.baseSpeed(stack, null);
	}

	protected int getBulletCount(ItemStack stack, @Nullable PlayerEntity player) {
		int divisionFactor = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.division, stack) * KGConfig.divisionCountIncrease.get();

		return getBaseBulletCount() + divisionFactor;
	}

	@Override
	public double getInaccuracy(ItemStack stack, @Nullable PlayerEntity player) {
		if (this.isWave) return 0;  //trick inaccuracy into being 0 so the wave attack specifies angles on its own
		else return super.getInaccuracy(stack, player);
	}

	protected void vampireBulletCount(@Nullable PlayerEntity player) {
		int entityCount = 0;
		vampireCount = 0;
		if (isVampire && player != null) {
			List<Entity> victims = player.level.getEntitiesOfClass(Entity.class, AxisAlignedBB.ofSize(KGConfig.netheriteShotgunEntityRadius.get() * 2, KGConfig.netheriteShotgunEntityRadius.get() * 2, KGConfig.netheriteShotgunEntityRadius.get() * 2).move(player.position()));

			for (Entity mob : victims) {
				if (mob instanceof LivingEntity) {
					LivingEntity creature = (LivingEntity) mob;
					//every creature in this 10 block box gets a heart sacrificed for a new bullet in the shotgun
					//cap at a certain amount of entities
					if ((creature.getUUID() != player.getUUID()) && (entityCount < KGConfig.netheriteShotgunEntityCap.get())) {
						if (!checkIsSameTeam(player, mob)) {
							creature.hurt((new EntityDamageSource("magic", (Entity) player)), (float) (double) KGConfig.netheriteShotgunEntityHurt.get()); //set value for vampire via config later
							vampireCount += KGConfig.netheriteShotgunBulletsPerEntity.get();
							entityCount++;
						}
					}
				}
			}
		}
	}

	protected boolean checkIsSameTeam(Entity player, Entity victim) {
		//check pet role first before team, as null team means they don't belong to any team in the first place
		if (victim instanceof TameableEntity) {
			return ((TameableEntity) victim).getOwner() == player;
		}
		if ((player.getTeam() == null) && (victim.getTeam() == null)) return false;
		return (player.getTeam() == victim.getTeam()) && !player.getTeam().isAllowFriendlyFire();
	}

	protected boolean isProjectileCountModified(ItemStack stack) {
		return EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.division, stack) >= 1;
	}

	public int getBaseBulletCount() {
		return this.bulletCount + this.vampireCount;
	}

	public ShotgunItem setIsVampire(boolean vampire) {
		this.isVampire = vampire;
		return this;
	}

	public ShotgunItem setIsWave(boolean wave) {
		this.isWave = wave;
		return this;
	}

	public ShotgunItem setIsSpread(boolean spread) {
		this.isSpread = spread;
		return this;
	}
}
