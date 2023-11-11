package xyz.kaleidiodev.kaleidiosguns.registry;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.IForgeRegistry;
import xyz.kaleidiodev.kaleidiosguns.KaleidiosGuns;
import xyz.kaleidiodev.kaleidiosguns.enchantment.GunAccuracyEnchantment;
import xyz.kaleidiodev.kaleidiosguns.enchantment.GunCurseEnchantment;
import xyz.kaleidiodev.kaleidiosguns.enchantment.GunDamageEnchantment;
import xyz.kaleidiodev.kaleidiosguns.enchantment.GunEnchantment;
import xyz.kaleidiodev.kaleidiosguns.item.GunItem;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = KaleidiosGuns.MODID)
public class ModEnchantments {

	public static Enchantment impact, bullseye, sleightOfHand, preserving, accelerator, division, passionForBlood, cowboy, silenced, cleanShot, signalBoost, torpedo, tracer, calibur;
	public static final EnchantmentType TYPE_GUN = EnchantmentType.create("GWR_GUN", (item) -> item instanceof GunItem);

	@SubscribeEvent
	public static void registerEnchantments(final RegistryEvent.Register<Enchantment> event) {
		IForgeRegistry<Enchantment> reg = event.getRegistry();

		//flat upgrade (unbreaking counts as drbl)
		impact = initEnchant(reg, new GunDamageEnchantment(Enchantment.Rarity.COMMON, 4, 4, 8, 20, TYPE_GUN), "impact"); //dmg
		bullseye = initEnchant(reg, new GunAccuracyEnchantment(Enchantment.Rarity.COMMON, 4, 5, 10, 30, TYPE_GUN), "bullseye"); //acc
		sleightOfHand = initEnchant(reg, new GunEnchantment(Enchantment.Rarity.UNCOMMON, 4, 6, 12, 30, TYPE_GUN), "sleight_of_hand"); //rt
		accelerator = initEnchant(reg, new GunEnchantment(Enchantment.Rarity.COMMON, 4, 3, 6, 20, TYPE_GUN), "accelerator"); //spd

		//class specialized enhancement (compounds but only for certain gun types.
		division = initEnchant(reg, new GunEnchantment(Enchantment.Rarity.COMMON, 5, 4, 8, 20, TYPE_GUN), "division"); //shotgun
		cleanShot = initEnchant(reg, new GunEnchantment(Enchantment.Rarity.UNCOMMON, 1, 6, 12, 25, TYPE_GUN), "clean_shot"); //sniper
		signalBoost = initEnchant(reg, new GunEnchantment(Enchantment.Rarity.COMMON, 4, 6, 12, 25, TYPE_GUN), "signal_boost"); //redstone

		//enhancement (compounds onto base, mending counts as drbl)
		passionForBlood = initEnchant(reg, new GunEnchantment(Enchantment.Rarity.RARE, 4, 8, 16, 25, TYPE_GUN), "passion_for_blood"); //dmg
		cowboy = initEnchant(reg, new GunEnchantment(Enchantment.Rarity.UNCOMMON, 1, 8, 16, 25, TYPE_GUN), "cowboy"); //acc
		preserving = initEnchant(reg, new GunEnchantment(Enchantment.Rarity.RARE, 4, 5, 10, 30, TYPE_GUN), "preserving"); //rt
		torpedo = initEnchant(reg, new GunEnchantment(Enchantment.Rarity.UNCOMMON, 1, 6, 12, 25, TYPE_GUN), "torpedo"); //spd
		silenced = initEnchant(reg, new GunEnchantment(Enchantment.Rarity.UNCOMMON, 1, 3, 6, 20, TYPE_GUN), "silenced"); //hndl

		//curses
		tracer = initEnchant(reg, new GunCurseEnchantment(Enchantment.Rarity.RARE, 1, 7, 14, 30, TYPE_GUN), "tracer_ammo");
		calibur = initEnchant(reg, new GunCurseEnchantment(Enchantment.Rarity.UNCOMMON, 5, 5, 10, 20, TYPE_GUN), "calibur");
	}

	public static Enchantment initEnchant(IForgeRegistry<Enchantment> reg, Enchantment enchantment, String name) {
		enchantment.setRegistryName(KaleidiosGuns.MODID, name);
		reg.register(enchantment);
		return enchantment;
	}
}
