package xyz.kaleidiodev.kaleidiosguns.item;

import net.minecraft.client.ClientGameSession;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.stats.Stats;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import xyz.kaleidiodev.kaleidiosguns.config.KGConfig;
import xyz.kaleidiodev.kaleidiosguns.registry.ModEnchantments;
import xyz.kaleidiodev.kaleidiosguns.registry.ModItems;

import javax.annotation.Nullable;
import java.util.List;

public class GatlingItem extends GunItem {
	protected boolean isFirstShot = true;

	public GatlingItem(Properties properties, int bonusDamage, double damageMultiplier, int fireDelay, double inaccuracy, int enchantability) {
		super(properties, bonusDamage, damageMultiplier, fireDelay, inaccuracy, enchantability);
	}

	@Override
	public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
		ItemStack itemstack = player.getItemInHand(hand);

		//don't fire if redstone block is not nearby
		if (this.isRedstone) {
			if (checkRedstoneLevel(world, player, itemstack) != -1) return handleGatling(world, player, itemstack, hand);
			else return ActionResult.fail(itemstack);
		}
		else return handleGatling(world, player, itemstack, hand);
	}

	protected ActionResult<ItemStack> handleGatling(World world, PlayerEntity player, ItemStack gun, Hand hand) {
		if (!player.abilities.instabuild && mergeStacks(player, gun).isEmpty()) {
			return ActionResult.fail(gun);
		}
		else {
			player.startUsingItem(hand);
			if (this.isFirstShot && !world.isClientSide()){
				fireGatling(world, player, gun, 1, mergeStacks(player, gun));
			}
			return ActionResult.consume(gun);
		}
	}

	@Override
	public void releaseUsing(ItemStack itemstack, World level, LivingEntity living, int timeLeft) {
		if (!level.isClientSide()) this.isFirstShot = true;
		//prevent first shot from being taken again for delay
		if (living instanceof PlayerEntity) {
			PlayerEntity player = (PlayerEntity) living;
			player.getCooldowns().addCooldown(this, getFireDelay(itemstack, player));
		}
	}

	@Override
	public void onUseTick(World world, LivingEntity user, ItemStack gun, int ticks) {
		if (user instanceof PlayerEntity) {
			PlayerEntity player = (PlayerEntity) user;
			ItemStack ammo = mergeStacks(player, gun);
			//stop immediately if player is dead.
			if (player.isDeadOrDying()) player.stopUsingItem();
			fireGatling(world, user, gun, ticks, ammo);
		}
	}

	public void fireGatling(World world, LivingEntity user, ItemStack gun, int ticks, ItemStack ammo) {
		if (user instanceof PlayerEntity) {
			PlayerEntity player = (PlayerEntity) user;
			//stop using immediately if out of range.
			if ((this.isRedstone) && (checkRedstoneLevel(world, player, gun) == -1)) player.stopUsingItem();

			//give player speed effect if maneuvering is instated.
			if ((EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.maneuvering, gun) != 0) && player.isOnGround() && world.isClientSide) {
				Vector3d newSpeed = player.getDeltaMovement().multiply(1.67, 1, 1.67);
				if (newSpeed.x > 1) newSpeed = new Vector3d(1, newSpeed.y, newSpeed.z);
				if (newSpeed.x < -1) newSpeed = new Vector3d(-1, newSpeed.y, newSpeed.z);
				if (newSpeed.z > 1) newSpeed = new Vector3d(newSpeed.x, newSpeed.y, 1);
				if (newSpeed.z < -1) newSpeed = new Vector3d(newSpeed.x, newSpeed.y, -1);
				System.out.println(newSpeed);
				player.setDeltaMovement(newSpeed); //apply speed for every tick so that the slow speed is nullified
			}

			int used = getUseDuration(gun) - ticks;
			int rateChange = (getFireDelay(gun, player) - ((isDefender && checkTileEntities(world, player)) ? KGConfig.defenderRifleDelayDelta.get() : 0));
			if (((used > 0 && used % rateChange == 0) || this.isFirstShot) && !world.isClientSide()) {
				//"Oh yeah I will use the vanilla method so that quivers can do their thing"
				//guess what the quivers suck
				this.isFirstShot = false;

				//stop immediately if out of ammo
				if (ammo.isEmpty() && !player.abilities.instabuild) player.stopUsingItem();

				if (!ammo.isEmpty() || player.abilities.instabuild) {
					if (ammo.isEmpty()) ammo = new ItemStack(ModItems.flintBullet);
					if (ammo.getItem() == Items.ARROW) ammo = new ItemStack(ModItems.flintBullet); //sigh

					IBullet bulletItem = (IBullet) (ammo.getItem() instanceof IBullet ? ammo.getItem() : ModItems.flintBullet);

					if (!world.isClientSide) {
						boolean bulletFree = player.abilities.instabuild || !shouldConsumeAmmo(gun, player);

						//Workaround for quivers not respecting getAmmoPredicate()
						ItemStack shotAmmo = ammo.getItem() instanceof IBullet ? ammo : new ItemStack(ModItems.flintBullet);
						fireWeapon(world, player, gun, shotAmmo, bulletItem, bulletFree);

						int durabilityDamage = 1;
						if (((BulletItem)ammo.getItem()).damage >= KGConfig.blazeBulletDamage.get()) {
							durabilityDamage += 1;
						}
						if (((BulletItem)ammo.getItem()).damage >= KGConfig.xpBulletDamage.get()) {
							durabilityDamage += 1;
						}

						gun.hurtAndBreak(durabilityDamage, player, (p) -> p.broadcastBreakEvent(player.getUsedItemHand()));
						if (!bulletFree) bulletItem.consume(ammo, player, gun);
					}

					float volume = (EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.silenced, gun) > 0 ? 2.0F : 10.0F);
					if (this.isQuiet) volume /= 2;

					world.playSound(null, player.getX(), player.getY(), player.getZ(), fireSound, SoundCategory.PLAYERS, volume, 1.0F);
					player.awardStat(Stats.ITEM_USED.get(this));
				}
			}
		}
	}

	@Override
	public int getUseDuration(ItemStack stack) {
		return 72000;
	}

	@Override
	protected void addExtraStatsTooltip(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip) {
		tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.gatling.hold"));
	}

	protected boolean checkTileEntities(World world, PlayerEntity player) {
		BlockPos checkPos;
		BlockPos closestPos = null;
		int checkRadius = KGConfig.defenderRifleRange.get();
		int distance = -1;

		for (int x = player.blockPosition().getX() - checkRadius; x < player.blockPosition().getX() + checkRadius; x++) {
			for (int y = player.blockPosition().getY() - checkRadius; y < player.blockPosition().getY() + checkRadius; y++) {
				for (int z = player.blockPosition().getZ() - checkRadius; z < player.blockPosition().getZ() + checkRadius; z++) {
					checkPos = new BlockPos(x, y, z);
					if (world.getBlockEntity(checkPos) != null) {
						// check if it is closer than any previously found position
						if (closestPos == null ||
								player.distanceToSqr(player.getX() - checkPos.getX(),
										player.getY() - checkPos.getY(),
										player.getZ() - checkPos.getZ())
										< player.distanceToSqr(player.getX() - closestPos.getX(),
										player.getY() - closestPos.getY(),
										player.getZ() - closestPos.getZ())) {
							closestPos = checkPos;
						}
					}
				}
			}
		}

		if (closestPos != null) distance = closestPos.distManhattan(player.blockPosition());

		//only allow a circular radius
		if (distance > checkRadius) distance = -1;

		return distance != -1;
	}
}
