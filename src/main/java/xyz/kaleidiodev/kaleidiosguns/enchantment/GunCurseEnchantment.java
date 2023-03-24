package xyz.kaleidiodev.kaleidiosguns.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentType;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.util.DamageSource;

public class GunCurseEnchantment extends GunEnchantment {

	public GunCurseEnchantment(Rarity rarityIn, int maxLevel, int minCost, int levelCost, int levelCostSpan, EnchantmentType enchantmentType) {
		super(rarityIn, maxLevel, minCost, levelCost, levelCostSpan, enchantmentType);
	}

	@Override
	protected boolean checkCompatibility(Enchantment pEnchant) {
		if (pEnchant instanceof GunCurseEnchantment) return false;
		else return true;
	}

	@Override
	public boolean isCurse() {
		return true;
	}
}
