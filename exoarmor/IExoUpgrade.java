package factorization.api;

import net.minecraft.util.DamageSource;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.ISpecialArmor.ArmorProperties;

public interface IExoUpgrade {
	/**
	 * 
	 * @param is - The upgrade's ItemStack
	 * @param armorIndex - Armor type. 0 = head, 1 = chest, 2 = legs, 3 = feet
	 * @return
	 */
	boolean canUpgradeArmor(ItemStack is, int armorIndex);
	
	/**
	 * 
	 * @param player
	 * @param armor
	 *            Armor that contains the upgrade
	 * @param is
	 *            The upgrade
	 * @param isEnabled
	 *            If the upgrade has been activated
	 * @return non-null to save changes to the item. Returning null causes no changes. Set stackSize to 0 to remove.
	 */
	ItemStack tickUpgrade(EntityPlayer player, ItemStack armor, ItemStack upgrade, boolean isEnabled);

	void addArmorProperties(ItemStack is, ArmorProperties armor);

	/**
	 * @param is
	 *            the upgrade
	 * @return how many halves of armor to draw on the HUD
	 */
	int getArmorDisplay(ItemStack is);

	/**
	 * @param entity
	 * @param stack
	 * @param source
	 * @param damage
	 * @param slot
	 * @return true if the stack's NBT needs to be updated
	 */
	boolean damageArmor(EntityLiving entity, ItemStack stack, DamageSource source, int damage, int slot);

	String getDescription();
}
