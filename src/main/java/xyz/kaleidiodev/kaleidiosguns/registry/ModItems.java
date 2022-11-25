package xyz.kaleidiodev.kaleidiosguns.registry;

import net.minecraft.item.Item;
import net.minecraft.item.crafting.Ingredient;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.IForgeRegistry;
import xyz.kaleidiodev.kaleidiosguns.KaleidiosGuns;
import xyz.kaleidiodev.kaleidiosguns.config.KGConfig;
import xyz.kaleidiodev.kaleidiosguns.item.*;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = KaleidiosGuns.MODID)
public class ModItems {

	public static GunItem
			ironGun,
			revolver,
			shadowRevolver,
			skillShotPistol,
			blessedPistol,
			diamondShotgun,
			vampireShotgun,
			doubleBarrelShotgun,
			carbineSniper,
			diamondSniper,
			diamondLauncher,
			witherLauncher,
			minegunGatling,
			corruptionGatling,
	        voltgun,
			plasmaGatling,
			assaultGatling,
	        defenderRifle;

	public static BulletItem flintBullet, ironBullet, blazeBullet, hungerBullet, xpBullet;

	@SubscribeEvent
	public static void registerItems(final RegistryEvent.Register<Item> event) {
		IForgeRegistry<Item> reg = event.getRegistry();

		// Pistols
		ironGun = initItem(reg, new GunItem(defP().durability(KGConfig.ironPistolDurability.get()), 0, KGConfig.ironPistolDamageMultiplier.get(), KGConfig.ironPistolFireDelay.get(), KGConfig.ironPistolInaccuracy.get(), KGConfig.ironPistolEnchantability.get(), KGConfig.ironPistolSwitchSpeed.get() - 4, KGConfig.ironPistolMeleeDamage.get() - 1).repair(() -> Ingredient.of(Tags.Items.INGOTS_IRON)), "pistol").projectileSpeed(KGConfig.ironPistolProjectileSpeed.get()).fireSound(ModSounds.pistol).setIsOneHanded(true).setIsQuiet(true);
		revolver = initItem(reg, new GunItem(defP().durability(KGConfig.diamondRevolverDurability.get()), 0, KGConfig.diamondRevolverDamageMultiplier.get(), KGConfig.diamondRevolverFireDelay.get(), KGConfig.diamondRevolverInaccuracy.get(), KGConfig.diamondRevolverEnchantability.get(), KGConfig.diamondRevolverSwitchSpeed.get() - 4, KGConfig.diamondRevolverMeleeDamage.get() - 1).repair(() -> Ingredient.of(Tags.Items.GEMS_DIAMOND)).chambers(6).setBarrelSwitchSpeed(KGConfig.diamondRevolverChamberSwitchSpeed.get()).instabilityAdditionalSpread(KGConfig.diamondRevolverSpreadoutStrength.get()).setStabilityTime(KGConfig.diamondRevolverStabilityTime.get()), "revolver").projectileSpeed(KGConfig.diamondRevolverProjectileSpeed.get()).fireSound(ModSounds.revolver).reloadSound(ModSounds.revolverReload).setIsOneHanded(true);
		skillShotPistol = initItem(reg, new GunItem(defP().durability(KGConfig.goldSkillshotDurability.get()), 0, KGConfig.goldSkillshotDamageMultiplier.get(), KGConfig.goldSkillshotFireDelay.get(), KGConfig.goldSkillshotInaccuracy.get(), KGConfig.goldSkillshotEnchantability.get(), KGConfig.goldSkillshotSwitchSpeed.get() - 4, KGConfig.goldSkillshotMeleeDamage.get() - 1).repair(() -> Ingredient.of(Tags.Items.INGOTS_GOLD)).setShouldCombo(true), "skill_shot").projectileSpeed(KGConfig.goldSkillshotProjectileSpeed.get()).fireSound(ModSounds.skillShot).setIsOneHanded(true);
		shadowRevolver = initItem(reg, new GunItem(defP().durability(KGConfig.shadowRevolverDurability.get()), 0, KGConfig.shadowRevolverDamageMultiplier.get(), KGConfig.shadowRevolverFireDelay.get(), KGConfig.shadowRevolverInaccuracy.get(), KGConfig.shadowRevolverEnchantability.get(), KGConfig.shadowRevolverSwitchSpeed.get() - 4, KGConfig.shadowRevolverMeleeDamage.get() - 1).repair(() -> Ingredient.of(Tags.Items.INGOTS_NETHERITE)).chambers(7).setBarrelSwitchSpeed(KGConfig.shadowRevolverChamberSwitchSpeed.get()).instabilityAdditionalSpread(KGConfig.shadowRevolverSpreadoutStrength.get()).setStabilityTime(KGConfig.shadowRevolverStabilityTime.get()), "shadow_magnum").projectileSpeed(KGConfig.shadowRevolverProjectileSpeed.get()).fireSound(ModSounds.shadowMagnum).reloadSound(ModSounds.revolverReload).setIsShadow(true).setIsOneHanded(true);
		blessedPistol = initItem(reg, new GunItem(defP().durability(KGConfig.emeraldBlessedDurability.get()), 0, KGConfig.emeraldBlessedDamageMultiplier.get(), KGConfig.emeraldBlessedFireDelay.get(), KGConfig.emeraldBlessedInaccuracy.get(), KGConfig.emeraldBlessedEnchantability.get(), KGConfig.emeraldBlessedSwitchSpeed.get() - 4, KGConfig.emeraldBlessedMeleeDamage.get() - 1).repair(() -> Ingredient.of(Tags.Items.GEMS_EMERALD)), "blessed_pistol").projectileSpeed(KGConfig.emeraldBlessedProjectileSpeed.get()).fireSound(ModSounds.blessedPistol).setShouldRevenge(true).setIsOneHanded(true);

		// Shotguns
		doubleBarrelShotgun = initItem(reg, new ShotgunItem(defP().durability(KGConfig.goldDoubleShotgunDurability.get()), 0, KGConfig.goldDoubleShotgunDamageMultiplier.get(), KGConfig.goldDoubleShotgunFireDelay.get(), KGConfig.goldDoubleShotgunInaccuracy.get(), KGConfig.goldDoubleShotgunEnchantability.get(), KGConfig.goldDoubleShotgunBulletCount.get(), KGConfig.goldDoubleShotgunSwitchSpeed.get() - 4, KGConfig.goldDoubleShotgunMeleeDamage.get() - 1).fireSound(ModSounds.double_shotgun).reloadSound(ModSounds.double_shotgunReload).repair(() -> Ingredient.of(Tags.Items.INGOTS_GOLD)).chambers(2).setBarrelSwitchSpeed(KGConfig.goldDoubleShotgunChamberSwitchSpeed.get()).setKnockbackStrength(KGConfig.goldDoubleShotgunKnockback.get()), "double_barrel_shotgun").projectileSpeed(KGConfig.goldDoubleShotgunProjectileSpeed.get());
		diamondShotgun = initItem(reg, new ShotgunItem(defP().durability(KGConfig.diamondShotgunDurability.get()), 0, KGConfig.diamondShotgunDamageMultiplier.get(), KGConfig.diamondShotgunFireDelay.get(), KGConfig.diamondShotgunInaccuracy.get(), KGConfig.diamondShotgunEnchantability.get(), KGConfig.diamondShotgunBulletCount.get(), KGConfig.diamondShotgunSwitchSpeed.get() - 4, KGConfig.diamondShotgunMeleeDamage.get() - 1).fireSound(ModSounds.shotgun).repair(() -> Ingredient.of(Tags.Items.GEMS_DIAMOND)), "shotgun").projectileSpeed(KGConfig.diamondShotgunProjectileSpeed.get());
		vampireShotgun = initItem(reg, new ShotgunItem(defP().durability(KGConfig.netheriteShotgunDurability.get()), 0, KGConfig.netheriteShotgunDamageMultiplier.get(), KGConfig.netheriteShotgunFireDelay.get(), KGConfig.netheriteShotgunInaccuracy.get(), KGConfig.netheriteShotgunEnchantability.get(), KGConfig.netheriteShotgunBulletCount.get(), KGConfig.netheriteShotgunSwitchSpeed.get() - 4, KGConfig.netheriteShotgunMeleeDamage.get() - 1).setIsVampire(true).fireSound(ModSounds.vampireShotgun).repair(() -> Ingredient.of(Tags.Items.INGOTS_NETHERITE)), "vampire_shotgun").projectileSpeed(KGConfig.netheriteShotgunProjectileSpeed.get());

		// Snipers
		carbineSniper = initItem(reg, new GunItem(defP().durability(KGConfig.ironCarbineDurability.get()), 0, KGConfig.ironCarbineDamageMultiplier.get(), KGConfig.ironCarbineFireDelay.get(), KGConfig.ironCarbineInaccuracy.get(), KGConfig.ironCarbineEnchantability.get(), KGConfig.ironCarbineSwitchSpeed.get() - 4, KGConfig.ironCarbineMeleeDamage.get() - 1).projectileSpeed(KGConfig.ironCarbineProjectileSpeed.get()).fireSound(ModSounds.carbine).repair(() -> Ingredient.of(Tags.Items.INGOTS_IRON)), "carbine").setCanBreakGlass(true);
		diamondSniper = initItem(reg, new GunItem(defP().durability(KGConfig.diamondSniperDurability.get()), 0, KGConfig.diamondSniperDamageMultiplier.get(), KGConfig.diamondSniperFireDelay.get(), KGConfig.diamondSniperInaccuracy.get(), KGConfig.diamondSniperEnchantability.get(), KGConfig.diamondSniperSwitchSpeed.get() - 4, KGConfig.diamondSniperMeleeDamage.get() - 1).projectileSpeed(KGConfig.diamondSniperProjectileSpeed.get()).fireSound(ModSounds.sniper).repair(() -> Ingredient.of(Tags.Items.GEMS_DIAMOND)), "sniper").ignoreInvulnerability(false).collateral(true);

		// Gatlings
		assaultGatling = initItem(reg, new GatlingItem(defP().durability(KGConfig.ironAssaultDurability.get()), 0, KGConfig.ironAssaultDamageMultiplier.get(), KGConfig.ironAssaultFireDelay.get(), KGConfig.ironAssaultInaccuracy.get(), KGConfig.ironAssaultEnchantability.get(), KGConfig.ironAssaultSwitchSpeed.get() - 4, KGConfig.ironAssaultMeleeDamage.get() - 1).repair(() -> Ingredient.of(Tags.Items.INGOTS_IRON)), "assault_rifle").projectileSpeed(KGConfig.ironAssaultProjectileSpeed.get()).fireSound(ModSounds.smg).setIsSensitive(true);
		plasmaGatling = initItem(reg, new GatlingItem(defP().durability(KGConfig.goldPlasmaDurability.get()), 0, KGConfig.goldPlasmaDamageMultiplier.get(), KGConfig.goldPlasmaFireDelay.get(), KGConfig.goldPlasmaInaccuracy.get(), KGConfig.goldPlasmaEnchantability.get(), KGConfig.goldPlasmaSwitchSpeed.get() - 4, KGConfig.goldPlasmaMeleeDamage.get() - 1).repair(() -> Ingredient.of(Tags.Items.INGOTS_GOLD)), "plasma_rifle").projectileSpeed(KGConfig.goldPlasmaProjectileSpeed.get()).fireSound(ModSounds.plasma_rifle);
		defenderRifle = initItem(reg, new GatlingItem(defP().durability(KGConfig.defenderRifleDurability.get()), 0, KGConfig.defenderRifleDamageMultiplier.get(), KGConfig.defenderRifleFireDelay.get(), KGConfig.defenderRifleInaccuracy.get(), KGConfig.defenderRifleEnchantability.get(), KGConfig.defenderRifleSwitchSpeed.get() - 4, KGConfig.defenderRifleMeleeDamage.get() - 1).repair(() -> Ingredient.of(Tags.Items.GEMS_EMERALD)), "defender_rifle").projectileSpeed(KGConfig.defenderRifleProjectileSpeed.get()).fireSound(ModSounds.defender_rifle).setIsDefender(true);

		// Launchers
		diamondLauncher = initItem(reg, new GunItem(defP().durability(KGConfig.diamondLauncherDurability.get()), 0, KGConfig.diamondLauncherDamageMultiplier.get(), KGConfig.diamondLauncherFireDelay.get(), KGConfig.diamondLauncherInaccuracy.get(), KGConfig.diamondLauncherEnchantability.get(), KGConfig.diamondLauncherSwitchSpeed.get() - 4, KGConfig.diamondLauncherMeleeDamage.get() - 1).projectileSpeed(KGConfig.diamondLauncherProjectileSpeed.get()).fireSound(ModSounds.rocketLauncher).repair(() -> Ingredient.of(Tags.Items.GEMS_DIAMOND)), "rocket_launcher").setIsExplosive(true);
		witherLauncher = initItem(reg, new GunItem(defP().durability(KGConfig.witherLauncherDurability.get()), 0, KGConfig.witherLauncherDamageMultiplier.get(), KGConfig.witherLauncherFireDelay.get(), KGConfig.witherLauncherInaccuracy.get(), KGConfig.witherLauncherEnchantability.get(), KGConfig.witherLauncherSwitchSpeed.get() - 4, KGConfig.witherLauncherMeleeDamage.get() - 1).projectileSpeed(KGConfig.witherLauncherProjectileSpeed.get()).fireSound(ModSounds.witherLauncher).repair(() -> Ingredient.of(Tags.Items.INGOTS_NETHERITE)), "wither_launcher").setIsExplosive(true).setIsWither(true);

		// Redstones
		voltgun = initItem(reg, new GunItem(defP().durability(KGConfig.ironVoltgunDurability.get()), 0, KGConfig.ironVoltgunDamageMultiplier.get(), KGConfig.ironVoltgunFireDelay.get(), KGConfig.ironVoltgunInaccuracy.get(), KGConfig.ironVoltgunEnchantability.get(), KGConfig.ironVoltgunSwitchSpeed.get() - 4, KGConfig.ironVoltgunMeleeDamage.get() - 1).repair(() -> Ingredient.of(Tags.Items.INGOTS_IRON)), "voltgun").projectileSpeed(KGConfig.ironVoltgunProjectileSpeed.get()).setIsRedstone(true).setHasVoltage(true).fireSound(ModSounds.voltgun);
		minegunGatling = initItem(reg, new GatlingItem(defP().durability(KGConfig.diamondMinegunDurability.get()), 0, KGConfig.diamondMinegunDamageMultiplier.get(), KGConfig.diamondMinegunFireDelay.get(), KGConfig.diamondMinegunInaccuracy.get(), KGConfig.diamondMinegunEnchantability.get(), KGConfig.diamondMinegunSwitchSpeed.get() - 4, KGConfig.diamondMinegunMeleeDamage.get() - 1).repair(() -> Ingredient.of(Tags.Items.GEMS_DIAMOND)), "minegun").projectileSpeed(KGConfig.diamondMinegunProjectileSpeed.get()).canMineBlocks(true).setIsRedstone(true);
		corruptionGatling = initItem(reg, new GatlingItem(defP().durability(KGConfig.netheriteMinegunDurability.get()), 0, KGConfig.netheriteMinegunDamageMultiplier.get(), KGConfig.netheriteMinegunFireDelay.get(), KGConfig.netheriteMinegunInaccuracy.get(), KGConfig.netheriteMinegunEnchantability.get(), KGConfig.netheriteMinegunSwitchSpeed.get() - 4, KGConfig.netheriteMinegunMeleeDamage.get() - 1).repair(() -> Ingredient.of(Tags.Items.INGOTS_NETHERITE)), "corruption_gun").projectileSpeed(KGConfig.netheriteMinegunProjectileSpeed.get()).canMineBlocks(true).setIsRedstone(true).setIsCorruption(true).fireSound(ModSounds.corruptionGun);

		// Bullets
		flintBullet = initItem(reg, new BulletItem(defP(), KGConfig.flintBulletDamage.get()), "flint_bullet");
		ironBullet = initItem(reg, new BulletItem(defP(), KGConfig.ironBulletDamage.get()), "iron_bullet");
		blazeBullet = initItem(reg, new BlazeBulletItem(defP(), KGConfig.blazeBulletDamage.get()), "blaze_bullet");
		hungerBullet = initItem(reg, new HungerBulletItem(defP().stacksTo(1), KGConfig.hungerBulletDamage.get()), "hunger_bullet");
		xpBullet = initItem(reg, new XPBulletItem(defP().stacksTo(1), KGConfig.xpBulletDamage.get()), "xp_bullet");

	}

	public static Item.Properties defP() {
		return new Item.Properties().tab(ItemGroupGuns.INSTANCE);
	}

	public static <I extends Item> I initItem(IForgeRegistry<Item> reg, I item, String name) {
		item.setRegistryName(KaleidiosGuns.MODID, name);
		reg.register(item);
		return item;
	}
}
