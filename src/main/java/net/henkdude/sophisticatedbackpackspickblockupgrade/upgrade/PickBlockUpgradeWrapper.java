package net.henkdude.sophisticatedbackpackspickblockupgrade.upgrade;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;

import java.util.function.Consumer;

public class PickBlockUpgradeWrapper extends UpgradeWrapperBase<PickBlockUpgradeWrapper, PickBlockUpgradeItem>
        implements IPickBlockUpgrade {

    public PickBlockUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
        super(storageWrapper, upgrade, upgradeSaveHandler);
    }

    @Override
    public boolean tryPickFromThisBackpack(Player player, Item wanted) {
        Level level = player.level();
        if (!isEnabled() || isInCooldown(level)) return false;

        // If already in hotbar, just select it
        int hotbarSlot = findInHotbar(player, wanted);
        if (hotbarSlot >= 0) {
            player.getInventory().selected = hotbarSlot;
            setCooldown(level, 2);
            return true;
        }

        // Extract from THIS backpack
        ITrackedContentsItemHandler inv = storageWrapper.getInventoryForUpgradeProcessing();
        int slot = findFirstSlot(inv, wanted);
        if (slot < 0) return false;

        ItemStack extracted = inv.extractItem(slot, 1, false);
        if (extracted.isEmpty()) return false;

        if (!placeIntoSelectedHotbar(player, extracted)) {
            // Best-effort revert
            ItemStack remainder = inv.insertItem(slot, extracted, false);
            if (!remainder.isEmpty()) player.drop(remainder, false);
            return false;
        }

        setCooldown(level, 2);
        return true;
    }

    private static int findInHotbar(Player player, Item wanted) {
        for (int i = 0; i < 9; i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (!s.isEmpty() && s.getItem() == wanted) return i;
        }
        return -1;
    }

    private static int findFirstSlot(ITrackedContentsItemHandler inv, Item wanted) {
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack s = inv.getStackInSlot(i);
            if (!s.isEmpty() && s.getItem() == wanted) return i;
        }
        return -1;
    }

    private static boolean placeIntoSelectedHotbar(Player player, ItemStack stack) {
        int selected = player.getInventory().selected;
        ItemStack current = player.getInventory().getItem(selected);

        // Empty -> place
        if (current.isEmpty()) {
            player.getInventory().setItem(selected, stack);
            return true;
        }

        // Merge if same
        if (ItemStack.isSameItemSameComponents(current, stack)) {
            int canMove = Math.min(stack.getCount(), current.getMaxStackSize() - current.getCount());
            if (canMove > 0) {
                current.grow(canMove);
                stack.shrink(canMove);
                if (stack.isEmpty()) return true;
            }
        }

        // Otherwise stash current
        if (!player.getInventory().add(current.copy())) return false;
        player.getInventory().setItem(selected, stack);
        return true;
    }
}
