package lykrast.gunswithoutroses.registry;

import lykrast.gunswithoutroses.GunsWithoutRoses;
import lykrast.gunswithoutroses.item.*;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.Ingredient;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.IForgeRegistry;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = GunsWithoutRoses.MODID)
public class ModItems {

	public static GunItem ironGun, goldGun, diamondShotgun, diamondSniper, diamondGatling;
	public static BulletItem flintBullet, ironBullet, blazeBullet, hungerBullet;

	@SubscribeEvent
	public static void registerItems(final RegistryEvent.Register<Item> event) {
		IForgeRegistry<Item> reg = event.getRegistry();

		ironGun = initItem(reg, new GunItem(defP().durability(513), 0, 1, 16, 1.5, 14).repair(() -> Ingredient.of(Tags.Items.INGOTS_IRON)), "iron_gun");
		goldGun = initItem(reg, new GunItem(defP().durability(104), 0, 1.5, 28, 1.5, 22).repair(() -> Ingredient.of(Tags.Items.INGOTS_GOLD)), "gold_gun");
		diamondShotgun = initItem(reg, new ShotgunItem(defP().durability(2076), 0, 0.5, 24, 6, 10, 5).ignoreInvulnerability(true).fireSound(ModSounds.shotgun).repair(() -> Ingredient.of(Tags.Items.GEMS_DIAMOND)), "diamond_shotgun");
		diamondSniper = initItem(reg, new GunItem(defP().durability(2076), 0, 2.5, 48, 0, 10).projectileSpeed(4).fireSound(ModSounds.sniper).repair(() -> Ingredient.of(Tags.Items.GEMS_DIAMOND)), "diamond_sniper");
		diamondGatling = initItem(reg, new GatlingItem(defP().durability(2076), 0, 0.75, 4, 4, 10).ignoreInvulnerability(true).repair(() -> Ingredient.of(Tags.Items.GEMS_DIAMOND)), "diamond_gatling");

		flintBullet = initItem(reg, new BulletItem(defP(), 5), "flint_bullet");
		ironBullet = initItem(reg, new BulletItem(defP(), 7), "iron_bullet");
		blazeBullet = initItem(reg, new BlazeBulletItem(defP(), 10), "blaze_bullet");
		hungerBullet = initItem(reg, new HungerBulletItem(defP().stacksTo(1), 6), "hunger_bullet");
	}

	public static Item.Properties defP() {
		return new Item.Properties().tab(ItemGroupGunsWithoutRoses.INSTANCE);
	}

	public static Item.Properties compat(String modid) {
		//The Team Abnormals way, works for now cause not using classes from other mods
		return new Item.Properties().tab(ModList.get().isLoaded(modid) ? ItemGroupGunsWithoutRoses.INSTANCE : null);
	}

	public static <I extends Item> I initItem(IForgeRegistry<Item> reg, I item, String name) {
		item.setRegistryName(GunsWithoutRoses.MODID, name);
		reg.register(item);
		return item;
	}
}
