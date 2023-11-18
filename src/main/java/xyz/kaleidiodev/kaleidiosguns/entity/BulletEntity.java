package xyz.kaleidiodev.kaleidiosguns.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.block.*;
import net.minecraft.entity.*;
import net.minecraft.entity.monster.EndermanEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.AbstractFireballEntity;
import net.minecraft.entity.projectile.ProjectileHelper;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.FakePlayer;
import xyz.kaleidiodev.kaleidiosguns.config.KGConfig;
import xyz.kaleidiodev.kaleidiosguns.item.GatlingItem;
import xyz.kaleidiodev.kaleidiosguns.item.GunItem;
import xyz.kaleidiodev.kaleidiosguns.item.IBullet;
import xyz.kaleidiodev.kaleidiosguns.network.NetworkUtils;
import xyz.kaleidiodev.kaleidiosguns.registry.ModEntities;
import xyz.kaleidiodev.kaleidiosguns.registry.ModItems;
import xyz.kaleidiodev.kaleidiosguns.registry.ModSounds;

import java.util.*;

public class BulletEntity extends AbstractFireballEntity {
	public enum PotionApplyMode {
		NONE,
		INJECT,
		SPLASH,
		LINGER
	}

	protected double damage = 1;
	protected double inaccuracy = 0.0;
	protected boolean ignoreInvulnerability = false;
	protected double knockbackStrength = 0.0;
	public long ticksOnFire;
	protected double healthRewardChance = 0.0f;
	protected float healthOfVictim;
	protected boolean shouldBreakBlock;
	protected boolean shouldCollateral;
	protected double bulletSpeed;
	public boolean isTorpedo;
	protected boolean shouldGlow;
	protected GunItem shootingGun;
	protected Vector3d origin;
	public boolean isExplosive;
	public boolean isPlasma;
	public boolean isWither;
	public boolean wasRevenge;
	public boolean wasDark;
	public boolean shootsLights;
	public boolean isClean;
	public boolean isCorrupted;
	public boolean shouldBreakDoors;
	public boolean shouldFlinch;
	public boolean healsFriendlies;
	public boolean juggle;
	public byte lavaMode; //bit 0 is player is on fire, bit 1 is enemy is on fire, bit 2 is is active, bit 3 is lava absorb
	public boolean isMeleeBonus;
	public boolean interactsWithBlocks;
	public int redstoneLevel;
	public double mineChance;
	public boolean clip;
	public boolean hero;
	public long actualTick;
	public List<EffectInstance> potionInstance = new ArrayList<EffectInstance>();
	public PotionApplyMode applyMode;
	public int lingeringTime;
	public double heroStep;
	public Vector3d lastPos = new Vector3d(0, 0, 0);
	public boolean hitBlock;
	public boolean hitEntity;
	public Set<Entity> entitiesThisTick = new HashSet<>();
	public boolean pollRemove;

	protected Set<Entity> entityHitHistory = new HashSet<>();
	public Set<Entity> headshotHistory = new HashSet<>();

	public BulletEntity(EntityType<? extends BulletEntity> entityType, World worldIn) {
		super(entityType, worldIn);
	}

	public BulletEntity(World worldIn, LivingEntity shooter) {
		this(worldIn, shooter, 0, 0, 0);
		ticksOnFire = level.getGameTime();
		setPos(shooter.getX(), shooter.getEyeY() - 0.1, shooter.getZ());
	}

	public BulletEntity(World worldIn, LivingEntity shooter, double accelX, double accelY, double accelZ) {
		super(ModEntities.BULLET, shooter, accelX, accelY, accelZ, worldIn);
	}

	//change the particle type the projectile is going to emit
	@Override
	protected IParticleData getTrailParticle() {
		if (isExplosive) return ParticleTypes.POOF;
		if (isPlasma) return ParticleTypes.INSTANT_EFFECT;
		if (wasRevenge || isMeleeBonus || hero) return ParticleTypes.HAPPY_VILLAGER;
		if (wasDark) return ParticleTypes.SMOKE;
		if (((lavaMode & 0x04) != 0) && (lavaMode > 0x04)) return ParticleTypes.LANDING_LAVA; //if was a lava bullet in any mode
		return ParticleTypes.CRIT;
	}

	@Override
	public void tick() {
		//check timestamp of fire versus current world.  if longer than 5 seconds, or world time is sooner, assume the bullet was created last session rather than current
		//force remove
		actualTick++;
		if (!this.level.isClientSide) {
			long passage = this.level.getGameTime() - ticksOnFire;
			if (passage != actualTick) this.remove();
		}
		if (actualTick > 100) this.remove();

		//skip all processing if we were removed this or last tick
		Entity entity = this.getOwner();
		if (this.removed) return;
		if (entity != null) if (entity.removed) {
			this.remove();
			return;
		}

		if (pollRemove) {
			this.remove();
			return;
		}

		if (shouldGlow || clip) {
			this.setGlowing(true);
		}

		this.setSharedFlag(6, this.isGlowing());
		//note that "is away from owner" is absolutely useless anyway, so it was not included
		this.baseTick();

		//why rotate toward movement?  that makes no sense
		//this needs rewritten so that:
		//1. the rotation is correct on spawn before first tick
		//2. the rotation is always correct at any rotation speed, like an arrow
		this.checkInsideBlocks();
		ProjectileHelper.rotateTowardsMovement(this, 20.0F);

		//add support for torpedo enchantment
		float f = this.getInertia();
		Vector3d vector3d = this.getDeltaMovement();

		if (this.isInWater()) {
			//don't decrease inertia if the torpedo enchantment was on the gun
			if (!this.isTorpedo) vector3d.multiply(0.5, 0.5, 0.5);
		}

		if (!this.isNoGravity()) vector3d.subtract(0, vector3d.y - 0.0025, 0);

		this.setDeltaMovement(vector3d);

		this.setPos(this.getX() + vector3d.x, this.getY() + vector3d.y, this.getZ() + vector3d.z);

		//because the sniper cannot have a projectile ignore invulnerability anyway, this is safe to do.
		if (shouldCollateral) {
			if (!this.level.isClientSide) {
				for (Entity currentEntity : entitiesThisTick) {
					if (currentEntity instanceof LivingEntity) {
						entityHitProcess(currentEntity);
					}
				}
			}
		} else if (hitEntity) {
			//only process the last entity in the list, which is the closest to the previous position.
			if (!this.level.isClientSide) {
				Entity next = entityHitHistory.iterator().next();
				entityHitProcess(next);
			}
			this.setPos(lastPos.x, lastPos.y, lastPos.z);
			pollRemove = true;
		}

		//should not be set if we hit an entity!
		if (hitBlock) {
			this.setPos(lastPos.x, lastPos.y, lastPos.z);
			if (this.level.isClientSide) {
				Vector3d motionDiv = this.getDeltaMovement().normalize().multiply(new Vector3d(0.25, 0.25, 0.25)).reverse();

				level.addParticle(ParticleTypes.POOF, true, lastPos.x, lastPos.y, lastPos.z, motionDiv.x, motionDiv.y, motionDiv.z);
			}
			pollRemove = true;
		}

		if (this.level.isClientSide && (actualTick > 1) && !pollRemove) {
			//summon the particles in the center of the projectile instead of above it.
			//disable emitters when underwater, as otherwise it looks messy to have two emitters (bubble emitter happens elsewhere)

			int divisor = KGConfig.particlesPerTick.get();

			//if the particle count per tick is zero, disable trail particles entirely.
			if (divisor == 0) return;

			//interpolation is always ahead, we want to draw particles for the last tick travelled not the current.
			Vector3d position = this.getBoundingBox().getCenter().subtract(this.getDeltaMovement());

			Vector3d motionDiv = this.getDeltaMovement().multiply(new Vector3d(1D / (double)divisor, 1D / (double)divisor, 1D / (double)divisor));

			if (this.isUnderWater()) {
				for (int i = 0; i < KGConfig.particlesPerTick.get(); i++) {
					this.level.addParticle(ParticleTypes.BUBBLE, true, position.x - (motionDiv.x * i), position.y - (motionDiv.x * i), position.z - (motionDiv.x * i), 0, 0, 0);
				}
			}
			else {
				for (int i = 0; i < KGConfig.particlesPerTick.get(); i++) {
					this.level.addParticle(this.getTrailParticle(), true, position.x - (motionDiv.x * i), position.y - (motionDiv.x * i), position.z - (motionDiv.x * i), 0, 0, 0);
				}
			}
		}

		if (!pollRemove) this.traceHits();
	}

	//for any hit types that aren't vanilla, such as vex carbine's through blocks check since it can be unpredictable, or a sniper's collateral
	public void traceHits() {
		//put some code here for the manual raytrace
		//the raytrace needs to be from current position to delta from last known position
		entitiesThisTick.clear();
		AxisAlignedBB bb = this.getBoundingBox();

		//start at previous position
		//we cannot rely on the speed being the same for the bullet over its lifetime, so let's recalculate per tick
		double actualSpeed = Vector3d.ZERO.distanceTo(this.getDeltaMovement());
		Vector3d delta = this.getDeltaMovement();
		Vector3d incPosition = new Vector3d(delta.x / (actualSpeed * 8), delta.y / (actualSpeed * 8), delta.z / (actualSpeed * 8));

		//the raytrace is really just a bunch of steps for boundary boxes.  this means accelerator makes sniper collateral further
		for (int i = 0; i < (actualSpeed * 8); i ++) {
			bb = bb.move(incPosition);

			//don't bother adding entities to the list that are already there.
			Set<Entity> thisEntities = new HashSet<>(this.level.getEntities(this, bb));
			thisEntities.removeIf(entity -> checkIsSameTeam(getOwner(), entity));
			thisEntities.remove(getOwner());
			thisEntities.removeIf(entity -> !entity.isAlive());

			//don't process anything we've previously hit on this hit as well
			thisEntities.removeAll(entityHitHistory);

			entitiesThisTick.addAll(thisEntities);
			entityHitHistory.addAll(entitiesThisTick); //entity hit history is shared between ticks.  entities is for the current line trace.

			//calculate headshot for the first trace an entity is found.  this prevents future trace steps from trying to count as headshots.
			//a shot to the leg and up through the body will not count as a headshot!
			if (thisEntities.size() > 0) {
				//subtract by position so explosion is outside of block, not inside.  fixes physics
				if (isExplosive) this.explode(bb.getCenter());

				double bulletBBFloor = bb.getCenter().y - (bb.getYsize() / 2);

				for (Entity victim : thisEntities) {
					if ((victim.getBoundingBox().getYsize() / 2 < victim.getBoundingBox().getXsize()) || (victim.getBoundingBox().getYsize() / 2 < victim.getBoundingBox().getZsize()))
						continue;

					AxisAlignedBB tempBB = victim.getBoundingBox();
					double enemyBoxHeight = (tempBB.getYsize() / 2);
					double enemyTop = tempBB.getCenter().y + enemyBoxHeight;
					double enemyChin = enemyTop - ((enemyBoxHeight * 2) / 4);

					//if the raytrace is at or above the enemy's chin, it's a headshot
					//the chin is a third of the height from the top of the entity
					if ((bulletBBFloor > enemyChin) && (bulletBBFloor < enemyTop)) {
						if ((getOwner() != null) && (!this.level.isClientSide()) && !(victim instanceof EndermanEntity))
							getOwner().level.playSound(null, bb.getCenter().x, bb.getCenter().y, bb.getCenter().z, SoundEvents.PLAYER_ATTACK_CRIT, SoundCategory.VOICE, 5.0f, 1.0f);
						headshotHistory.add(victim);
					}
				}

				if (!shouldCollateral) {
					lastPos = bb.getCenter(); //may need to subtract
					hitEntity = true;
					break;
				}
			}

			if (!clip) {
				//kill trace early if we hit a tile doing this, so it doesn't trace through walls.
				BlockPos someBlockPos = new BlockPos(bb.getCenter());
				BlockState someBlockState = this.level.getBlockState(someBlockPos);

				if (((this.lavaMode & 0x04) != 0) && (this.shootingGun != null)) {
					if (someBlockState.getBlock() == Blocks.LAVA) {
						this.shootingGun.hadLava = KGConfig.lavaSmgLavaBonusCount.get();
						level.setBlock(someBlockPos, Blocks.AIR.defaultBlockState(), Constants.BlockFlags.BLOCK_UPDATE);

						hitBlock = true;
					}
					if ((someBlockState.getBlock() == Blocks.WATER) && (this.lavaMode > 0x04)) {
						level.setBlock(someBlockPos, Blocks.STONE.defaultBlockState(), Constants.BlockFlags.BLOCK_UPDATE);

						hitBlock = true;
					}
				}

				if (shootsLights) {
					Block blockToChange = someBlockState.getBlock();
					if (blockToChange.getLightValue(someBlockState, level, someBlockPos) > 0) {
						level.destroyBlock(someBlockPos, false);

						hitBlock = true;
					}
				}

				if (interactsWithBlocks && this.level.getBlockCollisions(this, bb).findAny().isPresent()) {
					FakePlayer fakePlayer = new FakePlayer((ServerWorld) level, new GameProfile(null, "[KaleidiosGunsFakePlayer]"));
					someBlockState.use(this.level, fakePlayer, Hand.MAIN_HAND, new BlockRayTraceResult(bb.getCenter(), Direction.getNearest(this.getDeltaMovement().x, this.getDeltaMovement().y, this.getDeltaMovement().z), someBlockPos, true));

					hitBlock = true;
				}

				//solid blocks are handled different
				//the getBlockCollisions check makes sure only to fire this if the collision box overlaps the block
				if (someBlockState.getMaterial().blocksMotion() && this.level.getBlockCollisions(this, bb).findAny().isPresent()) {
					if (isExplosive) explode(bb.getCenter().subtract(incPosition));
					else onHitBlock(bb.getCenter());

					hitBlock = true;
				}

				if (hitBlock) {
					lastPos = bb.getCenter(); //may need to subtract
					break;
				}
			}
		}
	}

	//cannot guarantee this fires on the client side
	protected void onHitBlock(Vector3d pos) {
		//make a spherical poof and a sound
		if (!this.level.isClientSide) this.level.playSound(null, pos.x, pos.y, pos.z, ModSounds.impact, SoundCategory.VOICE, 0.25f, (random.nextFloat() * 0.5f) + 0.75f);

		BlockPos blockPositionToMine = new BlockPos(pos);

		if (KGConfig.griefEnabled.get() && !level.isClientSide) {
			if (shouldBreakBlock) {
				//test if the block is of the right tool type to mine with.
				//we could not guarantee the projectile ended up inside the block on this tick, so let's add some mathematics to work around that

				ItemStack newTool;

				if (this.getDamage() > KGConfig.mineGunFifthLevel.get()) {
					newTool = new ItemStack(Items.DIAMOND_PICKAXE);
					tryBreakBlock(blockPositionToMine, newTool);
					newTool = new ItemStack(Items.DIAMOND_AXE);
					tryBreakBlock(blockPositionToMine, newTool);
					newTool = new ItemStack(Items.DIAMOND_SHOVEL);
					tryBreakBlock(blockPositionToMine, newTool);
					newTool = new ItemStack(Items.SHEARS);
					tryBreakBlock(blockPositionToMine, newTool);
					breakWeakBlocks(blockPositionToMine);
				} else if (this.getDamage() > KGConfig.mineGunFourthLevel.get()) {
					newTool = new ItemStack(Items.IRON_PICKAXE);
					tryBreakBlock(blockPositionToMine, newTool);
					newTool = new ItemStack(Items.IRON_AXE);
					tryBreakBlock(blockPositionToMine, newTool);
					newTool = new ItemStack(Items.IRON_SHOVEL);
					tryBreakBlock(blockPositionToMine, newTool);
					newTool = new ItemStack(Items.SHEARS);
					tryBreakBlock(blockPositionToMine, newTool);
					breakWeakBlocks(blockPositionToMine);
				} else if (this.getDamage() > KGConfig.mineGunThirdLevel.get()) {
					newTool = new ItemStack(Items.STONE_PICKAXE);
					tryBreakBlock(blockPositionToMine, newTool);
					newTool = new ItemStack(Items.STONE_AXE);
					tryBreakBlock(blockPositionToMine, newTool);
					newTool = new ItemStack(Items.STONE_SHOVEL);
					tryBreakBlock(blockPositionToMine, newTool);
					breakWeakBlocks(blockPositionToMine);
				} else if (this.getDamage() > KGConfig.mineGunSecondLevel.get()) {
					newTool = new ItemStack(Items.WOODEN_PICKAXE);
					tryBreakBlock(blockPositionToMine, newTool);
					newTool = new ItemStack(Items.WOODEN_AXE);
					tryBreakBlock(blockPositionToMine, newTool);
					newTool = new ItemStack(Items.WOODEN_SHOVEL);
					tryBreakBlock(blockPositionToMine, newTool);
					breakWeakBlocks(blockPositionToMine);
				} else {
					breakWeakBlocks(blockPositionToMine);
				}

				//don't do corruption stuff if the block wasn't successfully mined
				if ((isCorrupted) && (level.getBlockState(blockPositionToMine).getBlock() == Blocks.AIR) && KGConfig.netheriteMinegunCorruptBlock.get()) {
					level.setBlock(blockPositionToMine, Blocks.NETHERRACK.defaultBlockState(), 1);
					//don't place fire if something is above current block
					if (isOnFire() &&
							(random.nextDouble() < KGConfig.netheriteMinegunIgnitionChance.get()) &&
							(level.getBlockState(blockPositionToMine.above(1)).getBlock() == Blocks.AIR)) {
						level.setBlock(blockPositionToMine.above(1), Blocks.FIRE.defaultBlockState(), 3);
					}
				}
			}

			if (shouldBreakDoors && actualTick <= 2) {
				Block blockToChange = level.getBlockState(blockPositionToMine).getBlock();
				//break wooden doors
				if (BlockTags.WOODEN_DOORS.getValues().contains(blockToChange) ||
						BlockTags.WOODEN_TRAPDOORS.getValues().contains(blockToChange) ||
						BlockTags.FENCE_GATES.getValues().contains(blockToChange) ||
						BlockTags.FENCES.getValues().contains(blockToChange) ||
						BlockTags.ICE.getValues().contains(blockToChange) ||
						blockToChange instanceof GlassBlock ||
						blockToChange instanceof StainedGlassBlock ||
						blockToChange instanceof PaneBlock ||
						blockToChange == Blocks.IRON_DOOR ||
						blockToChange == Blocks.IRON_TRAPDOOR ||
						blockToChange == Blocks.HAY_BLOCK ||
						blockToChange == Blocks.HONEY_BLOCK ||
						blockToChange == Blocks.SLIME_BLOCK) {
					level.destroyBlock(blockPositionToMine, false);
				}
			}
		}
	}

	protected void breakWeakBlocks(BlockPos blockPosToTest) {
		if (!level.getBlockState(blockPosToTest).requiresCorrectToolForDrops()) {
			breakBlock(blockPosToTest);
		}
	}

	protected void entityHitProcess(Entity entity) {
		Entity shooter = getOwner();
		IBullet bullet = (IBullet) getItemRaw().getItem();

		//get health of the victim before they get hit.
		if (entity instanceof LivingEntity) {
			LivingEntity victim = (LivingEntity) entity;
			healthOfVictim = victim.getHealth();

			if (shouldGlow) {
				victim.addEffect(new EffectInstance(Effects.GLOWING, KGConfig.enemyGlowTicks.get(), 0));
			}
		} else healthOfVictim = 0.0f;

		if (shooter != null) {
			if (healsFriendlies && (entity instanceof LivingEntity)) {
				if ((entity.getClassification(true) == EntityClassification.CREATURE) ||
						(entity.getClassification(true) == EntityClassification.WATER_CREATURE) ||
						checkIsSameTeam(shooter, entity)) {
					//heal by how much the bullet would end up as damage
					LivingEntity target = (LivingEntity) entity;
					target.heal((float)(bullet.modifyDamage(this.damage, this, target, shooter, this.level) * KGConfig.defenderRifleHealRatio.get()));
					//add some particle and sound effect here.
				}
				else giveDamage(shooter, entity, bullet);
			}
			else if (!checkIsSameTeam(shooter, entity)) giveDamage(shooter, entity, bullet);
		}
		else {
			giveDamage(null, entity, bullet);
		}
	}

	protected void giveDamage(Entity shooter, Entity victim, IBullet bullet) {
		if (victim.isOnFire()) lavaMode += 0x02;
		if (isOnFire()) victim.setSecondsOnFire(5);
		int lastHurtResistant = victim.invulnerableTime;
		if (ignoreInvulnerability) victim.invulnerableTime = 0;

		Vector3d previousDelta = victim.getDeltaMovement();
		if (victim instanceof LivingEntity) {
			healthOfVictim = ((LivingEntity)victim).getHealth();
		}

		boolean damaged = victim.hurt((new IndirectEntityDamageSource("arrow", this, shooter)).setProjectile(), (float) bullet.modifyDamage(damage, this, victim, shooter, level));

		if (isClean) victim.setDeltaMovement(previousDelta);
		else if (damaged && victim instanceof LivingEntity) {
			LivingEntity livingTarget = (LivingEntity) victim;

			double actualKnockback;
			if (this.shootingGun != null) {
				if (this.shootingGun.getItem() == ModItems.doubleBarrelShotgun) {
					actualKnockback = knockbackStrength / actualTick;

					Vector3d vec = getDeltaMovement().multiply(1, 0.25, 1).normalize().scale(actualKnockback);
					livingTarget.push(vec.x, vec.y, vec.z);
				}

				if (this.shootingGun.getItem() instanceof GatlingItem) {
					Vector3d vec = getDeltaMovement().multiply(1, 0, 1).normalize().scale(0.02);
					livingTarget.push(vec.x, vec.y, vec.z);

					Vector3d newMovement = livingTarget.getDeltaMovement();
					//this should cancel the vertical knockback done by vanilla's damage event by default
					if (livingTarget.isOnGround()) livingTarget.setDeltaMovement(newMovement.x, 0, newMovement.z);
				}
			}

			//force an overwrite of vertical velocity if the gun can juggle
			if (juggle) {
				Vector3d vel = livingTarget.getDeltaMovement();
				livingTarget.setDeltaMovement(new Vector3d(vel.x, 0.4, vel.z));
			}

			if (applyMode == PotionApplyMode.INJECT) {
				for (EffectInstance effect : potionInstance) {
					livingTarget.addEffect(effect);
				}
			}

			bullet.onLivingEntityHit(this, livingTarget, shooter, level);
		} else if (!damaged && ignoreInvulnerability) victim.invulnerableTime = lastHurtResistant;
	}

	protected boolean checkIsSameTeam(Entity player, Entity victim) {
		//check pet role first before team, as null team means they don't belong to any team in the first place
		if (player != null && victim != null) {
			if (victim instanceof BulletEntity) return true;
			if (victim instanceof TameableEntity) {
				return ((TameableEntity) victim).getOwner() == player;
			}
			if ((player.getTeam() == null) || (victim.getTeam() == null)) return false;
			return player.getTeam() == victim.getTeam();
		}
		return true; //assume same team if anything is null
	}

	public void explode(Vector3d position) {
		if (this.getShootingGun() == null) return;
		double newRadius = this.getShootingGun().damageMultiplier;

		level.explode(this, position.x, position.y, position.z, (float)newRadius, isOnFire(), KGConfig.explosionsEnabled.get() ? Explosion.Mode.DESTROY : Explosion.Mode.NONE);
		if (isWither) {
			newRadius *= KGConfig.witherLauncherEffectRadiusMultiplier.get();

			List<LivingEntity> entities = getExplosionAffected(position, newRadius);

			for (LivingEntity mob : entities) {
				if (getOwner() != null) if (!checkIsSameTeam(getOwner(), mob)) mob.addEffect(new EffectInstance(Effects.WITHER, 200, 1));
			}
		}

		if (applyMode == PotionApplyMode.SPLASH) {
			for (EffectInstance effect : potionInstance) {
				List<LivingEntity> entities = getExplosionAffected(position, newRadius * KGConfig.potionCannonSplashMultiplier.get());

				for (LivingEntity mob : entities) {
					//we must construct a new one because java's for loop will destroy this instance
					mob.addEffect(new EffectInstance(effect));
				}
			}

		}

		if (applyMode == PotionApplyMode.LINGER) {
			for (EffectInstance effect : potionInstance) {
				AreaEffectCloudEntity areaEffectCloud = new AreaEffectCloudEntity(level, position.x, position.y, position.z);

				areaEffectCloud.setPotion(new Potion(effect));
				areaEffectCloud.setDuration(lingeringTime);
				areaEffectCloud.setRadius((float)newRadius);
				areaEffectCloud.setRadiusPerTick(-((float)newRadius / lingeringTime));

				level.addFreshEntity(areaEffectCloud);
			}
		}

		remove();
	}

	public List<LivingEntity> getExplosionAffected(Vector3d position, double newRadius) {
		AxisAlignedBB explosionTrace = new AxisAlignedBB(position.x - newRadius, position.y - newRadius, position.z - newRadius, position.x + newRadius, position.y + newRadius, position.z + newRadius);
		return this.level.getEntitiesOfClass(LivingEntity.class, explosionTrace);
	}

	protected void tryBreakBlock(BlockPos blockPosToTest, ItemStack stack) {
		//test if the tool tier found works
		if (ForgeHooks.isToolEffective(this.level, blockPosToTest, stack)) {
			breakBlock(blockPosToTest);
		}
	}

	protected void breakBlock(BlockPos blockToBreak) {
		//drop the block in a fixed chance
		Random random = new Random();
		if (mineChance - random.nextDouble() > 0) this.level.destroyBlock(blockToBreak, true);
	}

	@Override
	public void addAdditionalSaveData(CompoundNBT compound) {
		super.addAdditionalSaveData(compound);
		compound.putLong("tickfired", ticksOnFire);
		compound.putDouble("damage", damage);
		compound.putDouble("tsf", actualTick);
		compound.putBoolean("explosive", isExplosive);
		compound.putBoolean("collateral", shouldCollateral);
		//compound.putBoolean("isPlasma", isPlasma);
		if (ignoreInvulnerability) compound.putBoolean("ignoreinv", ignoreInvulnerability);
		if (knockbackStrength != 0) compound.putDouble("knockback", knockbackStrength);
		compound.putBoolean("clip", clip);
		compound.putByte("lava", lavaMode);
	}

	@Override
	public void readAdditionalSaveData(CompoundNBT compound) {
		super.readAdditionalSaveData(compound);
		ticksOnFire = compound.getLong("tickfired");
		actualTick = compound.getLong("tsf");
		damage = compound.getDouble("damage");
		isExplosive = compound.getBoolean("explosive");
		shouldCollateral = compound.getBoolean("collateral");
		//isPlasma = compound.getBoolean("isPlasma");
		//The docs says if it's not here it's gonna be false/0 so it should be good
		ignoreInvulnerability = compound.getBoolean("ignoreinv");
		knockbackStrength = compound.getDouble("knockback");
		clip = compound.getBoolean("clip");
		lavaMode = compound.getByte("lava");
	}

	public void setDamage(double damage) {
		this.damage = damage;
	}

	public double getDamage() {
		return damage;
	}

	public double getInaccuracy() {
		return inaccuracy;
	}

	public void setHealthRewardChance(double rewardChance) {
		this.healthRewardChance = rewardChance;
	}

	public float getPreviousHealthOfVictim() {
		return healthOfVictim;
	}

	public void setShouldBreakBlock(boolean breakBlock) {
		this.shouldBreakBlock = breakBlock;
	}

	public boolean rollRewardChance() {
		Random random = new Random();
		return (healthRewardChance - random.nextDouble()) > 0.0D;
	}

	public void setInaccuracy(double inaccuracy) {
		this.inaccuracy = inaccuracy;
	}

	public void setBulletSpeed(double speed) {
		this.bulletSpeed = speed;
	}

	public void setIgnoreInvulnerability(boolean ignoreInvulnerability) {
		//quick workaround, always make it ignore invulnerability.
		this.ignoreInvulnerability = ignoreInvulnerability;
	}

	public void setShouldCollateral(boolean collateral) {
		this.shouldCollateral = collateral;
	}

	public void setShouldGlow(boolean glow) {
		this.shouldGlow = glow;
	}

	public void setShootingGun(GunItem gun) {
		this.shootingGun = gun;
	}

	public void setExplosive(boolean explosive) {
		this.isExplosive = explosive;
	}

	public GunItem getShootingGun() {
		return this.shootingGun;
	}

	public void setOrigin(Vector3d shooterOrigin) { this.origin = shooterOrigin; }

	public Vector3d getOrigin() { return this.origin; }

	/**
	 * Knockback on impact, 0.6 is equivalent to Punch I.
	 */
	public void setKnockbackStrength(double knockbackStrength) {
		this.knockbackStrength = knockbackStrength;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		return false;
	}

	@Override
	protected boolean shouldBurn() {
		return false;
	}

	@Override
	protected float getInertia() {
		return 1;
	}

	@Override
	public IPacket<?> getAddEntityPacket() {
		return NetworkUtils.getProjectileSpawnPacket(this);
	}
}
