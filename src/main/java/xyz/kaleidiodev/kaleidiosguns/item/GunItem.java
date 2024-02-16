package xyz.kaleidiodev.kaleidiosguns.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.*;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.*;
import net.minecraft.stats.Stats;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import xyz.kaleidiodev.kaleidiosguns.config.KGConfig;
import xyz.kaleidiodev.kaleidiosguns.enchantment.GunAccuracyEnchantment;
import xyz.kaleidiodev.kaleidiosguns.enchantment.GunDamageEnchantment;
import xyz.kaleidiodev.kaleidiosguns.entity.BulletEntity;
import xyz.kaleidiodev.kaleidiosguns.registry.ModEnchantments;
import xyz.kaleidiodev.kaleidiosguns.registry.ModItems;
import xyz.kaleidiodev.kaleidiosguns.registry.ModSounds;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import static xyz.kaleidiodev.kaleidiosguns.KaleidiosGuns.VivecraftForgeExtensionPresent;

public class GunItem extends Item {

	protected int bonusDamage;
	public double damageMultiplier;
	public int hadLava;
	protected int fireDelay;
	protected double inaccuracy;
	protected double projectileSpeed = 3;
	private final int enchantability;
	protected boolean ignoreInvulnerability = true;
	protected double chanceFreeShot = 0;
	protected boolean hasBlockMineAbility = false;
	protected int revolutions = 1;
	protected boolean shouldCollateral = false;
	protected int barrelSwitchSpeed = -1;
	protected double myKnockback = 0.1D;
	protected int stabilityTime;
	protected double instabilitySpreadAdditional; //additional spread to add every time a shot recharges stabilizer timer.
	protected boolean twoHandBonus;
	public boolean isExplosive;
	protected boolean isWither;
	protected boolean shouldRevenge;
	protected boolean isShadow;
	protected boolean isRedstone;
	protected boolean isCorruption;
	protected boolean hasVoltage;
	protected boolean isDefender;
	protected boolean isOneHanded;
	protected boolean isSensitive;
	protected boolean canFlinch;
	protected boolean isJuggler;
	protected boolean isLava;
	protected boolean isMeleeBonus;
	protected boolean breachDoors;
	protected double baseSpeed;
	protected double baseDamage;
	protected double currentSpeed;
	protected double currentDamage;
	protected int meleeBonusCounter;
	protected double mineChance;
	protected int ammoCost = 1;
	protected int burstSpeed;
	protected int burstAmount;
	protected boolean isVex;
	protected boolean isHero;
	protected boolean isGravity;
	protected boolean isPotion;
	protected boolean interactsWithBlocks;
	protected float sniperReplacementAim = 0.0f;
	protected float sniperMovementAim = 0.0f;

	protected SoundEvent fireSound = ModSounds.gun;
	protected SoundEvent reloadSound = ModSounds.double_shotgunReload;
	//Hey guess what if I just put the repair material it crashes... so well let's do like vanilla and just use a supplier
	protected Supplier<Ingredient> repairMaterial;
	private Multimap<Attribute, AttributeModifier> defaultModifiers;

	public GunItem(Properties properties, int bonusDamage, double damageMultiplier, int fireDelay, double inaccuracy, int enchantability, double attackSpeed, double attackDamage) {
		super(properties);
		this.bonusDamage = bonusDamage;
		this.damageMultiplier = damageMultiplier;
		this.enchantability = enchantability;
		this.fireDelay = fireDelay;
		this.inaccuracy = inaccuracy;
		this.baseSpeed = attackSpeed;
		this.baseDamage = attackDamage;

		setAttributes(attackDamage, attackSpeed);
	}

	public void setAttributes(double newDamage, double newSpeed) {
		this.currentDamage = newDamage;
		this.currentSpeed = newSpeed;

		ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
		builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Tool modifier", newDamage - 1, AttributeModifier.Operation.ADDITION));
		builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Tool modifier", newSpeed - 4, AttributeModifier.Operation.ADDITION));
		this.defaultModifiers = builder.build();
	}
	@Override
	public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlotType pEquipmentSlot) {
		return pEquipmentSlot == EquipmentSlotType.MAINHAND ? this.defaultModifiers : super.getDefaultAttributeModifiers(pEquipmentSlot);
	}

	@Override
	public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
		ItemStack gun = player.getItemInHand(hand);
		ItemStack ammo;
		//"Oh yeah I will use the vanilla method so that quivers can do their thing"
		//guess what the quivers suck
		ammo = mergeStacks(player, gun);
		//don't use if trying to use flint bullets on launcher
		if (this.isExplosive && (ammo.getItem() == ModItems.flintBullet)) return ActionResult.fail(gun);
		if (!caliburCheck(EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.calibur, gun), ammo.getItem())) return ActionResult.fail(gun);

		//don't fire if redstone block is not nearby
		if (this.isRedstone) {
			if (checkRedstoneLevel(world, player, gun) != -1) return handleWeapon(world, player, gun, hand, ammo);
			else return ActionResult.fail(gun);
		}
		else return handleWeapon(world, player, gun, hand, ammo);
	}

	protected boolean caliburCheck(int level, Item item) {
		if (level == 0) return true;
		if ((item == ModItems.flintBullet) && (level == 1)) return true;
		if ((item == ModItems.ironBullet) && (level == 2)) return true;
		if ((item == ModItems.blazeBullet) && (level == 3)) return true;
		if ((item == ModItems.hungerBullet) && (level == 4)) return true;
		if ((item == ModItems.xpBullet) && (level == 5)) return true;
		return false;
	}

	public ItemStack mergeStacks(PlayerEntity player, ItemStack gun) {
		ItemStack newAmmo = ItemStack.EMPTY;
		if (!player.level.isClientSide) {
			for (int i = 0; i < player.inventory.getContainerSize(); i++) {
				ItemStack itemstack1 = player.inventory.getItem(i);
				if (itemstack1.getItem() instanceof BulletItem) {
					//first, automerge the stack, before judging the count
					//automerge stacks to prevent a bunch of slots with unusable ammo
					if (!itemstack1.isEmpty()) {
						//only look at indexes ahead of the current, forcing permutation without duplicate checks
						for (int j = i; j < player.inventory.getContainerSize(); j++) {
							if ((j != i) && (player.inventory.getItem(j).getItem() == itemstack1.getItem())) {
								ItemStack itemstack2 = player.inventory.getItem(j);

								int needed = 64 - itemstack1.getCount();
								int has = itemstack2.getCount();

								//if the entirety of the second stack can merge, destroy it and add all count to the other
								if (has < needed) {
									player.inventory.removeItem(itemstack2);
									itemstack1.grow(has);
								}
								//otherwise only bring over as many as needed.
								else {
									itemstack2.shrink(needed);
									itemstack1.grow(needed);
								}
							}
						}
					}

					//skip if we just merged, and check the next stack
					if (itemstack1.isEmpty()) {
						player.inventory.removeItem(itemstack1);
					}
				}
			}
		}

		for(int i = 0; i < player.inventory.getContainerSize(); i++) {
			//now read in order, prioritizing leftmost, fixes issue with three stacks at a time
			ItemStack itemstack1 = player.inventory.getItem(i);
			if (itemstack1.getItem() instanceof BulletItem) {
				BulletItem item = (BulletItem)itemstack1.getItem();
				if (newAmmo.getItem() instanceof BulletItem) {
					BulletItem otherItem = (BulletItem)newAmmo.getItem();
					if (item.hasAmmo(itemstack1, player, gun) && (item.damage > otherItem.damage)) {
						newAmmo = itemstack1;
					}
				}
				else
				{
					//assume null, pick immediately
					if (item.hasAmmo(itemstack1, player, gun)) {
						newAmmo = itemstack1;
					}
				}
			}
		}

		if (isExplosive && player.abilities.instabuild && (newAmmo.getItem() == ModItems.flintBullet)) newAmmo = new ItemStack(ModItems.ironBullet);

		return newAmmo;
	}

	protected int checkRedstoneLevel(World world, PlayerEntity player, ItemStack gun) {
		//check for redstone block in radius
		int redstone = -1; //is -1 if there is no redstone block nearby
		if (((GunItem)gun.getItem()).isRedstone) {
			BlockPos closestPos = null;
			BlockPos checkPos;

			int checkRadius = (int)(KGConfig.redstoneRadius.get() * (1 + (EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.signalBoost, gun) * KGConfig.signalMultiplier.get())));

			//code provided and modified via forum post courtesy of @jabelar https://forums.minecraftforge.net/topic/63221-trying-to-get-closest-block/
			for (int x = player.blockPosition().getX() - checkRadius; x < player.blockPosition().getX() + checkRadius; x++) {
				for (int y = player.blockPosition().getY() - checkRadius; y < player.blockPosition().getY() + checkRadius; y++) {
					for (int z = player.blockPosition().getZ() - checkRadius; z < player.blockPosition().getZ() + checkRadius; z++) {
						checkPos = new BlockPos(x, y, z);
						if (world.getBlockState(checkPos).getBlock() == Blocks.REDSTONE_BLOCK) {
							// check if it is closer than any previously found position
							if (closestPos == null ||
									player.blockPosition().distManhattan(checkPos) < player.blockPosition().distManhattan(closestPos)) {
								closestPos = checkPos;
							}
						}
					}
				}
			}

			if (closestPos != null) redstone = closestPos.distManhattan(player.blockPosition());

			//only allow a circular radius
			if (redstone > checkRadius) redstone = -1;

			//summon a strand of redstone particles in the air between the player and the redstone block targeted.
		}

		return redstone;
	}

	protected ActionResult<ItemStack> handleWeapon(World world, PlayerEntity player, ItemStack gun, Hand hand, ItemStack ammo) {
		if (!ammo.isEmpty() || player.abilities.instabuild) {
			if (ammo.isEmpty()) ammo = new ItemStack(ModItems.flintBullet);
			if (ammo.getItem() == Items.ARROW) ammo = new ItemStack(ModItems.flintBullet); //sigh

			IBullet bulletItem = (IBullet) (ammo.getItem() instanceof IBullet ? ammo.getItem() : ModItems.flintBullet);

			player.startUsingItem(hand);

			if (!world.isClientSide) {
				boolean bulletFree = player.abilities.instabuild || !shouldConsumeAmmo(gun, player);

				//Workaround for quivers not respecting getAmmoPredicate()
				ItemStack shotAmmo = ammo.getItem() instanceof IBullet ? ammo : new ItemStack(ModItems.flintBullet);
				fireWeapon(world, player, gun, shotAmmo, bulletItem, bulletFree);

				gun.hurtAndBreak(((BulletItem)ammo.getItem()).durabilityDamage, player, (p) -> p.broadcastBreakEvent(player.getUsedItemHand()));
				if (!bulletFree) bulletItem.consume(ammo, player, gun);

				float volume = (EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.silenced, gun) > 0 ? 2.0F : 10.0F);

				world.playSound(null, player.getX(), player.getY(), player.getZ(), fireSound, SoundCategory.PLAYERS, volume, 1.0F);
				player.awardStat(Stats.ITEM_USED.get(this));

				CompoundNBT nbt = gun.getOrCreateTag();

				if (this.revolutions > 1) {
					if (!nbt.contains("chambers")) nbt.putInt("chambers", revolutions);
					int chambers = nbt.getInt("chambers"); //note, happens first, so the tag will get created, even if tooltip wasn't inspected first.
					chambers--;
					if (chambers <= 0) {
						world.playSound(null, player.getX(), player.getY(), player.getZ(), reloadSound, SoundCategory.HOSTILE, 1.0F, 1.0F);
						chambers = this.revolutions;
					}
					nbt.putInt("chambers",chambers);
					gun.setTag(nbt);
				}

				if (EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.tracer, gun) > 0) {
					player.addEffect(new EffectInstance(Effects.GLOWING, KGConfig.playerGlowTicks.get(), 0));
				}

				player.getCooldowns().addCooldown(this, getActualFireDelay(gun, player));

				//force both hands to cooldown.
				if (player.getMainHandItem().getItem() == this) {
					if (player.getOffhandItem().getItem() instanceof GunItem) {
						player.getCooldowns().addCooldown(player.getOffhandItem().getItem(), getActualFireDelay(gun, player) + 1);
					}
				}
				//don't bother if two of the same weapon.
				else if (player.getOffhandItem().getItem() != this) {
					if (player.getOffhandItem().getItem() instanceof GunItem) {
						player.getCooldowns().addCooldown(player.getMainHandItem().getItem(), getActualFireDelay(gun, player) + 1);
					}
				}

				mergeStacks(player, gun);
			}

			return ActionResult.consume(gun);
		}
		return ActionResult.fail(gun);
	}

	/**
	 * Fires one shot after all the checks have passed. You can actually fire whatever you want here.<br>
	 * Ammo is consumed afterwards, we're only shooting the bullet(s) here.
	 * @param world world
	 * @param player Player that shoots
	 * @param gun Gun shooting
	 * @param ammo Ammo being shot
	 * @param bulletItem IBullet used for the shot, may not match the ammo
	 * @param bulletFree true if no ammo was actually consumed (creative or Preserving enchant for example)
	 */
	protected void fireWeapon(World world, PlayerEntity player, ItemStack gun, ItemStack ammo, IBullet bulletItem, boolean bulletFree) {
		boolean isPlasma = gun.getItem() == ModItems.plasmaGatling;
		double nextInaccuracy = getInaccuracy(gun, player);
		BulletEntity shot = bulletItem.createProjectile(world, ammo, player, isPlasma);

		shootShot(shot, player, gun, nextInaccuracy);

		//subtract player velocity to make the bullet independent
		Vector3d projectileMotion = player.getDeltaMovement();
		shot.setDeltaMovement(shot.getDeltaMovement().subtract(projectileMotion.x, player.isOnGround() ? 0.0D : projectileMotion.y, projectileMotion.z));

		shot.setShootingGun(this);
		shot.setInaccuracy(nextInaccuracy);
		shot.setIgnoreInvulnerability(ignoreInvulnerability);
		shot.setHealthRewardChance(EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.passionForBlood, gun) * 0.1);
		shot.setShouldBreakBlock(hasBlockMineAbility);
		shot.setShouldCollateral(shouldCollateral);
		shot.setBulletSpeed(getProjectileSpeed(gun, player));
		shot.setKnockbackStrength(myKnockback);
		shot.setExplosive(isExplosive);
		shot.setShouldGlow(EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.tracer, gun) > 0);
		shot.setOrigin(player.position());
		shot.setNoGravity(!isGravity);

		double someDamage = (shot.getDamage() + getBonusDamage(gun, player)) * getDamageMultiplier(gun);
		if (gun.getItem() instanceof ShotgunItem) {
			ShotgunItem thisGun = (ShotgunItem)gun.getItem();
			shot.setDamage(someDamage * ((double)thisGun.getBaseBulletCount() / (double)thisGun.getBulletCount(gun, player)));
		}
		else if ((player.getMainHandItem().isEmpty() ^ player.getOffhandItem().isEmpty()) && twoHandBonus) shot.setDamage(someDamage * KGConfig.goldShowmanTwoHandedUse.get());
		else shot.setDamage(someDamage);

		shot.wasRevenge = (this.shouldRevenge && (player.getHealth() < (player.getMaxHealth() * KGConfig.emeraldBlessedHealthMinimumRatio.get())));
		//use sky light or block light unless it's night, then use purely blocklight
		shot.wasDark = this.isShadow && (((!world.isNight() ? Math.max(world.getBrightness(LightType.SKY, player.blockPosition().above(1)), world.getBrightness(LightType.BLOCK, player.blockPosition().above(1))) : world.getBrightness(LightType.BLOCK, player.blockPosition().above(1))) <= KGConfig.shadowRevolverLightLevelRequired.get())); //.above(1) so that it's getting an actual blocklight value.

		if (hasVoltage) shot.redstoneLevel = checkRedstoneLevel(world, player, gun);

		boolean noPhysics = this.shouldCollateral;
		boolean vex = false;

		if (this.isVex) {
			ItemStack membrane = getOtherHand(player);

			if (membrane.getItem() == Items.PHANTOM_MEMBRANE) {
				noPhysics = true;
				vex = true;

				if (!player.abilities.instabuild) {
					membrane.shrink(1);
					if (membrane.isEmpty()) player.inventory.removeItem(membrane);
				}
			}
		}

		shot.noPhysics = noPhysics;
		shot.clip = vex;
		shot.isPlasma = (this.getItem() == ModItems.plasmaGatling);
		shot.isClean = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.cleanShot, gun) > 0;
		shot.isWither = this.isWither;
		shot.isCorrupted = this.isCorruption;
		shot.isTorpedo = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.torpedo, gun) == 1;
		shot.shouldBreakDoors = this.breachDoors;
		shot.shouldFlinch = this.canFlinch;
		//shot.healsFriendlies = this.isDefender;
		shot.isMeleeBonus = this.meleeBonusCounter > 0;
		shot.shootsLights = this.isShadow;
		shot.juggle = this.isJuggler;
		shot.interactsWithBlocks = this.interactsWithBlocks;
		shot.silenced = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.silenced, gun) == 1;

		shot.hero = false;
		if (isHero) {
			//find if any harmful effects are on the player.  break on the first found
			Collection<EffectInstance> effectsOnPlayer = player.getActiveEffects();
			for (EffectInstance effect : effectsOnPlayer) {
				if (effect.getEffect().getCategory() == EffectType.HARMFUL) {
					shot.hero = true;
					break;
				}
			}
		}

		if (player.getEffect(Effects.DIG_SPEED) != null) {
			shot.mineChance = this.mineChance + (KGConfig.hasteBonusMineChance.get() * player.getEffect(Effects.DIG_SPEED).getAmplifier());
		}
		else shot.mineChance = this.mineChance;
		if (this.isLava) {
			shot.lavaMode = 0x04;

			ItemStack bucket = getOtherHand(player);
			if (bucket.getItem() == Items.LAVA_BUCKET) {
				this.hadLava = KGConfig.lavaSmgLavaBonusCount.get();
				player.inventory.removeItem(bucket);
				player.inventory.add(new ItemStack(Items.BUCKET));
			}

			if (player.isOnFire()) shot.lavaMode += 0x01;
			if (this.hadLava > 0) {
				shot.lavaMode += 0x08;
				hadLava--;
			}
		}

		if (this.isPotion) {
			ItemStack potion = getOtherHand(player);
			shot.applyMode = BulletEntity.PotionApplyMode.NONE;

			if (potion.getItem() == Items.POTION) shot.applyMode = BulletEntity.PotionApplyMode.INJECT;
			if (potion.getItem() == Items.LINGERING_POTION) {
				shot.applyMode = BulletEntity.PotionApplyMode.LINGER;
				shot.lingeringTime = KGConfig.potionCannonLingeringTime.get();
			}
			if (potion.getItem() == Items.SPLASH_POTION) shot.applyMode = BulletEntity.PotionApplyMode.SPLASH;

			if (shot.applyMode != BulletEntity.PotionApplyMode.NONE) {
				Potion actualPotion = PotionUtils.getPotion(potion);
				shot.potionInstance = actualPotion.getEffects();

				//shrink by one to support mods that stack potions
				potion.shrink(1);
				if (potion.isEmpty()) player.inventory.removeItem(potion);
			}
		}

		if (this.meleeBonusCounter > 0) this.meleeBonusCounter--;

		changeBullet(world, player, gun, shot, bulletFree);

		//reset timer for stability
		CompoundNBT nbt = gun.getOrCreateTag();

		int base = Math.max(1, stabilityTime - (int)(stabilityTime * EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.sleightOfHand, gun) * KGConfig.sleightOfHandFireRateDecrease.get()));
		nbt.putInt("stabilizerTimer", base);
		nbt.putInt("shotsBeforeStability", nbt.getInt("shotsBeforeStability") + 1);

		int burstTimer = nbt.getInt("burstTimer");
		if (burstTimer == 0) burstTimer = burstSpeed * burstAmount;
		nbt.putInt("burstTimer", burstTimer);

		gun.setTag(nbt);

		shot.traceHits();

		world.addFreshEntity(shot);
	}

	public ItemStack getOtherHand(PlayerEntity player) {
		ItemStack otherItem;

		if (player.getUsedItemHand() == Hand.OFF_HAND) {
			otherItem = player.getItemInHand(Hand.MAIN_HAND);
		} else {
			otherItem = player.getItemInHand(Hand.OFF_HAND);
		}

		return otherItem;
	}

	//called during the firing sequence, override if the spray pattern needs to be specified
	public void shootShot(BulletEntity shot, PlayerEntity player, ItemStack gun, double nextInaccuracy) {
		shot.shootFromRotation(player, player.xRot, player.yRot, 0, (float)getProjectileSpeed(gun, player), VivecraftForgeExtensionPresent ? 0.0F : (float)nextInaccuracy);
	}

	//used to tick the stability timer on guns that use it.
	@Override
	public void inventoryTick(ItemStack pStack, World pLevel, Entity pEntity, int pItemSlot, boolean pIsSelected) {
		//turns out this method gets called for every chunk generator thread.  let's avoid that, with a tick elapsed counter and client filter
		CompoundNBT nbt = pStack.getOrCreateTag();
		long currentTime = pLevel.getGameTime();
		if (!nbt.contains("ticksPassed")) nbt.putLong("ticksPassed", currentTime);

		long ticksPassed = nbt.getLong("ticksPassed");
		if (currentTime > ticksPassed) {
			nbt.putLong("ticksPassed", currentTime);
			pStack.setTag(nbt);

			this.onActualInventoryTick(pLevel, pEntity, pStack);
		}

		super.inventoryTick(pStack, pLevel, pEntity, pItemSlot, pIsSelected);
	}

	@Override
	public boolean hurtEnemy(ItemStack pStack, LivingEntity pTarget, LivingEntity pAttacker) {
		if (this.isMeleeBonus) this.meleeBonusCounter = KGConfig.emeraldMusketPostMeleeCount.get();
		return super.hurtEnemy(pStack, pTarget, pAttacker);
	}

	protected void onActualInventoryTick(World world, Entity entity, ItemStack stack) {
		CompoundNBT nbt = stack.getOrCreateTag();

		//get previous player velocity
		Vector3d previousPos = new Vector3d(nbt.getDouble("previousPosX"), nbt.getDouble("previousPosY"), nbt.getDouble("previousPosZ"));
		int stabilizerTimer = nbt.getInt("stabilizerTimer");
		int burstTimer = nbt.getInt("burstTimer");

		Vector3d playerVelocity = entity.position().subtract(previousPos);
		nbt.putDouble("playerVelocityX", playerVelocity.x);
		nbt.putDouble("playerVelocityY", playerVelocity.y);
		nbt.putDouble("playerVelocityZ", playerVelocity.z);
		nbt.putDouble("previousPosX", entity.position().x);
		nbt.putDouble("previousPosY", entity.position().y);
		nbt.putDouble("previousPosZ", entity.position().z);

		if (stabilizerTimer > 0) stabilizerTimer--;
		if (stabilizerTimer == 0) nbt.putInt("shotsBeforeStability", 0);
		nbt.putInt("stabilizerTimer", stabilizerTimer);

		if (burstTimer > 0) burstTimer--;
		if (burstSpeed != 0) {
			if ((burstTimer % burstSpeed == 0) && (entity instanceof PlayerEntity) && (burstTimer > 0)) {
				PlayerEntity player = (PlayerEntity) entity;
				if (this == player.getMainHandItem().getItem()) this.use(world, (PlayerEntity) entity, Hand.MAIN_HAND);
				if (this == player.getOffhandItem().getItem()) this.use(world, (PlayerEntity) entity, Hand.OFF_HAND);
			}
		}
		nbt.putInt("burstTimer", burstTimer);

		stack.setTag(nbt);
	}

	/**
	 * Lets the gun do custom stuff to default bullets without redoing all the base stuff from shoot.
	 */
	protected void changeBullet(World world, PlayerEntity player, ItemStack gun, BulletEntity bullet, boolean bulletFree) {

	}

	/**
	 * Rolls chance to know if ammo should be consumed for the shot. Applies both the baseline chance and Preserving enchantment.<br>
	 * If you change this don't forget to tweak getInverseChanceFreeShot accordingly for the tooltip (and call super).
	 */
	public boolean shouldConsumeAmmo(ItemStack stack, PlayerEntity player) {
		if (chanceFreeShot > 0 && random.nextDouble() < chanceFreeShot) return false;

		int preserving = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.preserving, stack);
		return !((preserving * KGConfig.preservingRateIncrease.get()) - random.nextDouble() > 0);
	}

	/**
	 * Gets the flat bonus damage (applied BEFORE the multiplier). This takes into account Impact enchantment.
	 */
	public double getBonusDamage(ItemStack stack, @Nullable PlayerEntity player) {
		int impact = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.impact, stack);
		return bonusDamage + (impact >= 1 ? (impact * KGConfig.impactDamageIncrease.get()) : 0);
	}

	public double getDamageMultiplier(ItemStack stack) {
		return damageMultiplier * ((double)getFireDelayRaw(stack) / (double)fireDelay);
	}

	/**
	 * Gets the min time in ticks between 2 shots. This takes into account Sleight of Hand enchantment.
	 */
	public int getFireDelayRaw(ItemStack stack) {
		return Math.max(1, fireDelay - (int)(fireDelay * EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.sleightOfHand, stack) * KGConfig.sleightOfHandFireRateDecrease.get()));
	}

	public int getFireDelay(ItemStack stack, @Nullable PlayerEntity player) {
		int base = getFireDelayRaw(stack);

		CompoundNBT nbt = stack.getOrCreateTag();

		//have instant tick time if barrel side has been switched
		if (!nbt.contains("chambers")) return base;
		if (nbt.getInt("chambers") == revolutions) {
			return base;
		}
		else
		{
			return Math.max(1, base / barrelSwitchSpeed);
		}
	}

	public int getActualFireDelay(ItemStack stack, @Nullable PlayerEntity player) {
		int base = getFireDelay(stack, player);

		if (player != null) {
			if (player.hasEffect(Effects.DIG_SLOWDOWN) && !(stack.getItem() instanceof GatlingItem)) {
				base = (int)((double)base * (1 + ((double)player.getEffect(Effects.DIG_SLOWDOWN).getAmplifier() * 0.2)));
			}
			if (player.hasEffect(Effects.DIG_SPEED) && !(stack.getItem() instanceof GatlingItem)) {
				base = (int)((double)base * (1 - (0.1 * (double)player.getEffect(Effects.DIG_SPEED).getAmplifier())));
			}
		}

		//increase time spend if two hands on a shotgun class
		if ((player != null) && (EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.cowboy, stack) == 0) && (stack.getItem() instanceof ShotgunItem)) {
			//if both hands are full, because one is the gun and one is something else
			if (!player.getMainHandItem().isEmpty() && !player.getOffhandItem().isEmpty()) {
				base *= KGConfig.oneHandShotgunRateMultiplier.get();
			}
		}

		return base;
	}

	/**
	 * Checks if the gun has baseline perfect accuracy.<br>
	 * Used for tooltip and for Bullseye (which can't be applied since it would do nothing).
	 */
	public boolean hasPerfectAccuracy() {
		return inaccuracy <= 0;
	}

	/**
	 * Gets the inaccuracy, taking into account Bullseye enchantment.<br>
	 * Accuracy is actually inaccuracy internally, because it's easier to math.<br>
	 * The formula is just accuracy = 1 / inaccuracy.
	 */
	public double getInaccuracy(ItemStack stack, @Nullable PlayerEntity player) {
		double nextInaccuracy = baseInaccuracy(stack, player);

		CompoundNBT nbt = stack.getOrCreateTag();

		nextInaccuracy += nbt.getInt("shotsBeforeStability") * Math.max(0, instabilitySpreadAdditional / ((EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.bullseye, stack) * KGConfig.bullseyeAccuracyIncrease.get()) + 1.0D));

		if (player != null) {
			Vector3d playerVelocity = new Vector3d(nbt.getDouble("playerVelocityX"), nbt.getDouble("playerVelocityY"), nbt.getDouble("playerVelocityZ"));

			//check sniper class
			if ((inaccuracy == 0) &&
					playerVelocity.length() > 0.03 &&
					!(this instanceof GatlingItem) &&
					!(this instanceof ShotgunItem)) {
				nextInaccuracy += sniperMovementAim;
			}

			//check player hands
			if ((EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.cowboy, stack) == 0) && !(stack.getItem() instanceof ShotgunItem) && !isOneHanded) {
				//if both hands are full, because one is the gun and one is something else
				if (!player.getMainHandItem().isEmpty() && !player.getOffhandItem().isEmpty()) {
					//if sniper class, give a new inaccuracy
					if (inaccuracy == 0) nextInaccuracy += sniperReplacementAim;
						//else multiply
					else nextInaccuracy *= KGConfig.oneHandInaccuracyMultiplier.get();
				}
			}

			//check assault rifle
			if (!player.isOnGround() && (EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.cowboy, stack) == 0) && this.isSensitive) {
				nextInaccuracy *= KGConfig.sensitivityMultiplier.get() * 1.5;
			}
			else if (this.isSensitive && !player.isCrouching() && (KGConfig.sensitivityMultiplier.get() != 0)) {
				nextInaccuracy *= KGConfig.sensitivityMultiplier.get();
			}

			//check weakness effect
			if ((player.getEffect(Effects.WEAKNESS) != null) && !(stack.getItem() instanceof ShotgunItem)) {
				if (nextInaccuracy == 0) nextInaccuracy += sniperReplacementAim;
				else nextInaccuracy *= KGConfig.weaknessEffectInaccuracyMultiplier.get() * (player.getEffect(Effects.WEAKNESS).getAmplifier() + 1);
			}

			if ((player.getEffect(Effects.LUCK) != null) && !(stack.getItem() instanceof ShotgunItem)) {
				nextInaccuracy /= KGConfig.luckEffectInaccuracyDivider.get() * (player.getEffect(Effects.LUCK).getAmplifier() + 1);
			}
		}

		return nextInaccuracy;
	}

	public double baseInaccuracy(ItemStack stack, @Nullable PlayerEntity player) {
		return Math.max(0, inaccuracy / ((EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.bullseye, stack) * KGConfig.bullseyeAccuracyIncrease.get()) + 1.0));
	}

	/**
	 *
	 * Gets the projectile speed, taking into account Accelerator enchantment.
	 */
	public double getProjectileSpeed(ItemStack stack, @Nullable PlayerEntity player) {
		return baseSpeed(stack, player);
	}

	public double baseSpeed(ItemStack stack, @Nullable PlayerEntity player) {
		return Math.max(0, projectileSpeed + ((EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.accelerator, stack) * KGConfig.acceleratorSpeedIncrease.get() * projectileSpeed)));
	}

	/**
	 * Chance to actually CONSUME ammo, to properly multiply probabilities together.<br>
	 * Tooltip then does the math to display it nicely.
	 */
	public double getInverseChanceFreeShot(ItemStack stack, @Nullable PlayerEntity player) {
		double chance = 1 - chanceFreeShot;
		int preserving = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.preserving, stack);
		if (preserving >= 1) chance *= preserving * KGConfig.preservingRateIncrease.get();
		return chance;
	}

	/**
	 * Says if the damage is changed from base value. Used for tooltip.
	 */
	protected boolean isDamageModified(ItemStack stack) {
		return EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.impact, stack) >= 1;
	}

	/**
	 * Says if the fire delay is changed from base value. Used for tooltip.
	 */
	protected boolean isFireDelayModified(ItemStack stack) {
		return EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.sleightOfHand, stack) >= 1;
	}

	/**
	 * Says if the (in)accuracy is changed from base value. Used for tooltip.
	 */
	protected boolean isInaccuracyModified(ItemStack stack) {
		return !hasPerfectAccuracy() && EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.bullseye, stack) >= 1;
	}

	/**
	 * Says if the chance for free shots is changed from base value. Used for tooltip.
	 */
	protected boolean isChanceFreeShotModified(ItemStack stack) {
		return EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.preserving, stack) >= 1;
	}

	protected boolean isProjectileSpeedModified(ItemStack stack) {
		return EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.accelerator, stack) >= 1;
	}

	/**
	 * Sets the firing sound, used when making the item for registering.
	 */
	public GunItem fireSound(SoundEvent fireSound) {
		this.fireSound = fireSound;
		return this;
	}

	/**
	 * Sets the reload sound, used when making the item for registering.
	 */
	public GunItem reloadSound(SoundEvent newReloadSound) {
		this.reloadSound = newReloadSound;
		return this;
	}

	/**
	 * Sets a projectile speed, used when making the item for registering.<br>
	 * Base value is 3.
	 */
	public GunItem projectileSpeed(double projectileSpeed) {
		this.projectileSpeed = projectileSpeed;
		return this;
	}

	/**
	 * Gives the gun the ability to break blocks.
	 */
	public GunItem canMineBlocks(boolean mineBlocks){
		this.hasBlockMineAbility = mineBlocks;
		return this;
	}

	/**
	 * Sets the repair material, used when making the item for registering.
	 */
	public GunItem repair(Supplier<Ingredient> repairMaterial) {
		this.repairMaterial = repairMaterial;
		return this;
	}

	public GunItem chambers(int revolverCount) {
		this.revolutions = revolverCount;
		return this;
	}

	public GunItem collateral(boolean collateralSetting) {
		this.shouldCollateral = collateralSetting;
		return this;
	}

	public GunItem setKnockbackStrength(double newKnockback) {
		this.myKnockback = newKnockback;
		return this;
	}

	public GunItem instabilityAdditionalSpread(double additionalSpread) {
		this.instabilitySpreadAdditional = additionalSpread;
		return this;
	}

	public GunItem setStabilityTime(int stability) {
		this.stabilityTime = stability;
		return this;
	}

	public GunItem setTwoHandBonus(boolean twoHand) {
		this.twoHandBonus = twoHand;
		return this;
	}

	public GunItem setIsExplosive(boolean explosive) {
		this.isExplosive = explosive;
		return this;
	}

	public GunItem setIsWither(boolean wither) {
		this.isWither = wither;
		return this;
	}

	public GunItem setShouldRevenge(boolean revenge) {
		this.shouldRevenge = revenge;
		return this;
	}

	public GunItem setIsShadow(boolean shadow) {
		this.isShadow = shadow;
		return this;
	}

	public GunItem setIsRedstone(boolean redstone) {
		this.isRedstone = redstone;
		return this;
	}

	public GunItem setIsCorruption(boolean corruption) {
		this.isCorruption = corruption;
		return this;
	}

	public GunItem setHasVoltage(boolean voltage) {
		this.hasVoltage = voltage;
		return this;
	}

	public GunItem setIsDefender(boolean defender) {
		this.isDefender = defender;
		return this;
	}

	public GunItem setIsOneHanded(boolean oneHanded) {
		this.isOneHanded = oneHanded;
		return this;
	}

	public GunItem setIsSensitive(boolean sensitive) {
		this.isSensitive = sensitive;
		return this;
	}

	public GunItem setIsJuggler(boolean juggle) {
		this.isJuggler = juggle;
		return this;
	}

	public GunItem setCanFlinch(boolean flinch) {
		this.canFlinch = flinch;
		return this;
	}

	public GunItem setBreachDoors(boolean breach) {
		this.breachDoors = breach;
		return this;
	}

	public GunItem setIsLava(boolean slag) {
		this.isLava = slag;
		return this;
	}

	public GunItem setMeleeBonus(boolean meleeBonus) {
		this.isMeleeBonus = meleeBonus;
		return this;
	}

	public GunItem setMineChance(double newMineChance) {
		this.mineChance = newMineChance;
		return this;
	}

	public GunItem setCost(int cost) {
		this.ammoCost = cost;
		return this;
	}

	public GunItem setVex(boolean vex) {
		this.isVex = vex;
		return this;
	}

	public GunItem setBurst(int burstRate, int burstCount) {
		this.burstSpeed = burstRate;
		this.burstAmount = burstCount;
		return this;
	}

	public GunItem setHero(boolean hero) {
		this.isHero = hero;
		return this;
	}

	public GunItem setSniperAim(double sniperAim) {
		this.sniperMovementAim = (float)sniperAim;
		return this;
	}

	public GunItem setSniperReplacementAim(double sniperReplacement) {
		this.sniperReplacementAim = (float)sniperReplacement;
		return this;
	}

	public GunItem setIsGravity(boolean gravity) {
		this.isGravity = gravity;
		return this;
	}

	public GunItem setIsPotion(boolean potion) {
		this.isPotion = potion;
		return this;
	}

	public GunItem setInteractBlocks(boolean interact) {
		this.interactsWithBlocks = interact;
		return this;
	}

	public int getCost(ItemStack stack) {
		return Math.max(1, ammoCost - (int)(ammoCost * EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.sleightOfHand, stack) * KGConfig.sleightOfHandFireRateDecrease.get()));
	}

	/**
	 *
	 * @param barrelSwitch set the divider that divides the fire rate to denote how many ticks it takes to switch barrels
	 *
	 */
	public GunItem setBarrelSwitchSpeed(int barrelSwitch) {
		this.barrelSwitchSpeed = barrelSwitch;
		return this;
	}

	@Override
	public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
		return super.isBookEnchantable(stack, book);
	}

	@Override
	public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
		//Disallow these for specific gun types
		GunItem me = (GunItem) stack.getItem();
		if ((enchantment instanceof GunAccuracyEnchantment) && hasPerfectAccuracy()) return false; //not for sniper
		if ((enchantment instanceof GunDamageEnchantment) && isExplosive) return false; //not for launcher
		if ((enchantment == ModEnchantments.cowboy) && isOneHanded) return false; //not for pistol
		if (((enchantment == ModEnchantments.sleightOfHand)) && ((me instanceof GatlingItem) || (me.isExplosive))) return false; //not for gatling nor launcher

		//only let these apply to certain gun types
		if ((enchantment == ModEnchantments.division) && !(me instanceof ShotgunItem)) return false; //shotgun only
		if ((enchantment == ModEnchantments.cleanShot) && ((me instanceof ShotgunItem) || (me instanceof GatlingItem) || (me.isExplosive) || (me.getInaccuracy(stack, null) != 0))) return false; //sniper only
		if ((enchantment == ModEnchantments.signalBoost) && !isRedstone) return false; //redstone only
		if ((enchantment == ModEnchantments.tracer) && ((me instanceof ShotgunItem) || (me instanceof GatlingItem) || (me.isExplosive) || (me.getInaccuracy(stack, null) == 0))) return false; //pistol only

		return super.canApplyAtEnchantingTable(stack, enchantment);
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
		if (Screen.hasShiftDown()) {
			CompoundNBT nbt = stack.getOrCreateTag();

			if (!nbt.contains("chambers")) nbt.putInt("chambers", revolutions);
			stack.setTag(nbt);

			//Damage
			double damageMultiplier = getDamageMultiplier(stack);
			double damageBonus = getBonusDamage(stack, null) * damageMultiplier;

			if (this instanceof ShotgunItem) {
				if (isExplosive)
					tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.gun.shotgun.damage.explosion", ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(damageMultiplier)));
				else if (damageMultiplier != 1) {
					if (damageBonus != 0)
						tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.gun.shotgun.damage.both" + (isDamageModified(stack) ? ".modified" : ""), ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(damageMultiplier), ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(damageBonus)));
					else
						tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.gun.shotgun.damage.mult" + (isDamageModified(stack) ? ".modified" : ""), ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(damageMultiplier)));
				} else if (damageBonus != 0)
					tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.gun.shotgun.damage.flat" + (isDamageModified(stack) ? ".modified" : ""), ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(damageBonus)));
			}
			else {
				if (isExplosive)
					tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.gun.damage.explosion", ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(damageMultiplier)));
				else if (damageMultiplier != 1) {
					if (damageBonus != 0)
						tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.gun.damage.both" + (isDamageModified(stack) ? ".modified" : ""), ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(damageMultiplier), ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(damageBonus)));
					else
						tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.gun.damage.mult" + (isDamageModified(stack) ? ".modified" : ""), ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(damageMultiplier)));
				} else if (damageBonus != 0)
					tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.gun.damage.flat" + (isDamageModified(stack) ? ".modified" : ""), ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(damageBonus)));
			}

			//Fire rate
			int fireRate = getFireDelayRaw(stack);
			if (revolutions > 1) {
				tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.gun.reloadspeed" + (isFireDelayModified(stack) ? ".modified" : ""), ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(fireRate / 20.0)));
				tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.gun.barrels_left", nbt.getInt("chambers"), revolutions));
				fireRate = Math.max(1, fireRate / barrelSwitchSpeed);
			}
			tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.gun.firerate" + (isFireDelayModified(stack) ? ".modified" : ""), 1200 / fireRate));


			//Accuracy
			double inaccuracy = baseInaccuracy(stack, null);
			double projectileSpeed = baseSpeed(stack, null) * 16;
			//0.0075 is the deviation set by the inaccuracy formula before it does a bunch of trigonometry to calculate angles.  it's the same formula used in the balance calculator excel document as well
			//2.5 is the maximum expected deviation due to a Gaussian random weighted of 1.
			//so let's see how deviated it can be after a whole second.
			inaccuracy = inaccuracy * 0.0075 * 2.5 * projectileSpeed;

			//now we get how many seconds it takes for the radius to become less than 100% likely to hit a humanoid which makes the "perfect accuracy limit" in terms of time...
			//0.425 meters is half the width of a zombie hitbox plus the width of the bullet hitbox
			double timeToInaccurate = 0.425 / inaccuracy;

			//now we judge how many meters it is.
			inaccuracy = projectileSpeed * timeToInaccurate;

			if (inaccuracy == Double.POSITIVE_INFINITY) {
				if (this instanceof GatlingItem || this instanceof ShotgunItem || this.isExplosive || this.isRedstone) tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.gun.accuracy.perfect" + (isInaccuracyModified(stack) ? ".modified" : "")));
				else tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.gun.accuracy.perfect.sniper"));
			}
			else tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.gun.accuracy" + (isInaccuracyModified(stack) ? ".modified" : ""), ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(inaccuracy)));

			//Projectile Speed

			tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.gun.speed" + (isProjectileSpeedModified(stack) ? ".modified" : ""), ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(projectileSpeed)));

			int cost = getCost(stack);
			tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.gun.cost" + (ammoCost > cost ? ".modified" : ""), cost));

			//Chance to not consume ammo
			double inverseChanceFree = getInverseChanceFreeShot(stack, null);
			if (inverseChanceFree < 1) tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.gun.chance_free" + (isChanceFreeShotModified(stack) ? ".modified" : ""), (int)((1 - inverseChanceFree) * 100)));

			int calibur = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.calibur, stack);
			if (calibur > 0) {
				String result = "tooltip.kaleidiosguns.calibur";
				if (calibur == 1) result += ".flint";
				if (calibur == 2) result += ".iron";
				if (calibur == 3) result += ".blaze";
				if (calibur == 4) result += ".hunger";
				if (calibur == 5) result += ".xp";

				tooltip.add(new TranslationTextComponent(result));
			}


			if (KGConfig.showWeaponDetails.get()) {
				if (shouldCollateral) tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.collateral"));
				if (twoHandBonus) tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.showman_glock"));
				if (shouldRevenge) tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.blessed"));
				if (isShadow) tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.shadow"));
				if (isDefender) tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.defender"));
				if (canFlinch) tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.shatter"));
				if (isJuggler) tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.juggle"));
				if (hasVoltage) tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.voltage"));
				if (isMeleeBonus) tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.melee"));
				if (isLava) tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.lava"));
				if (isVex) tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.vex"));
				//if (this == ModItems.heroShotgun) tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.wave"));
				if (isHero) tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.hero"));
				if (isPotion) tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.potion"));
				if (interactsWithBlocks) tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.interact"));
				if (breachDoors) tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.doors"));
				if (isCorruption) tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.corruption"));
				if (isWither) tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.wither"));
				if (this.getItem() == ModItems.doubleBarrelShotgun) tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.knockback"));
				if (this.getItem() == ModItems.plasmaGatling) tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.plasma"));
			}

			if (KGConfig.showWeaponSecrets.get()) {
				if (hasBlockMineAbility) tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.minegun"));
				if (isShadow) tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.shadow_block"));
			}

			if (KGConfig.showClassDetails.get()) {
				if (isRedstone) tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.redstone"));
				if (revolutions > 1) tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.double_barrel"));
				if (stack.getItem() == ModItems.revolver)
					tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.revolver"));
				if (isSensitive) tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.sensitive"));
				if (isExplosive) tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.explosive"));
				if (!isOneHanded && (EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.cowboy, stack) == 0) && (this instanceof ShotgunItem))
					tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.twohands_shotgun"));
				if (!isOneHanded && (EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.cowboy, stack) == 0) && !(this instanceof ShotgunItem))
					tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.twohands"));
				if ((inaccuracy == 0) && !(this instanceof ShotgunItem) && !(this instanceof GatlingItem))
					tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.sniper"));
				if (isGravity) tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.gravity"));
			}

			addExtraStatsTooltip(stack, world, tooltip);
		}
		else tooltip.add(new TranslationTextComponent("tooltip.kaleidiosguns.shift"));
	}

	/**
	 * Add more tooltips that will be displayed below the base stats.
	 */
	protected void addExtraStatsTooltip(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip) {

	}

	@Override
	public UseAction getUseAnimation(ItemStack stack) {
		return UseAction.BOW;
	}

	@Override
	public int getEnchantmentValue() {
		return enchantability;
	}

	@Override
	public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
		return (repairMaterial != null && repairMaterial.get().test(repair)) || super.isValidRepairItem(toRepair, repair);
	}

	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return !ItemStack.isSameIgnoreDurability(oldStack, newStack);
	}

	@Override
	public int getUseDuration(ItemStack pStack) {
		return Math.max(getFireDelay(pStack, null) / 20, 10);
	}
}
