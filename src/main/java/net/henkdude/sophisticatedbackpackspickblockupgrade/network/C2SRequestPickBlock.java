package net.henkdude.sophisticatedbackpackspickblockupgrade.network;

import net.henkdude.sophisticatedbackpackspickblockupgrade.SophisticatedBackpacksPickBlockUpgrade;
import net.henkdude.sophisticatedbackpackspickblockupgrade.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;

public record C2SRequestPickBlock(ResourceLocation itemId) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final Type<C2SRequestPickBlock> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(SophisticatedBackpacksPickBlockUpgrade.MODID, "pick_block"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SRequestPickBlock> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC, C2SRequestPickBlock::itemId,
                    C2SRequestPickBlock::new
            );

    @Override
    public Type<C2SRequestPickBlock> type() {
        return TYPE;
    }

    public static void handle(C2SRequestPickBlock msg, ServerPlayer player) {
        if (player == null) return;

        Item wantedItem = BuiltInRegistries.ITEM.get(msg.itemId());
        if (wantedItem == null) return;

        // Find a backpack in player's inventory that has our upgrade installed
        ItemStack backpackStack = findFirstUpgradedBackpack(player);
        if (backpackStack.isEmpty()) return;

        BackpackWrapper wrapper = (BackpackWrapper) BackpackWrapper.fromStack(backpackStack);
        InventoryHandler backpackInv = wrapper.getInventoryHandler();

        int request = Math.min(64, wantedItem.getDefaultInstance().getMaxStackSize());
        if (request <= 0) return;

        int slot = findBestSlotFor(backpackInv, wantedItem, request);
        if (slot == -1) return;

        ItemStack extracted = backpackInv.extractItem(slot, request, false);
        if (extracted.isEmpty()) return;

        // Try to place into main hand / hotbar in a sane way
        placeIntoHandOrHotbar(player, extracted);
    }

    private static ItemStack findFirstUpgradedBackpack(ServerPlayer player) {
        // main inventory
        for (ItemStack stack : player.getInventory().items) {
            if (isUpgradedBackpack(stack)) return stack;
        }
        // hotbar/offhand
        for (ItemStack stack : player.getInventory().offhand) {
            if (isUpgradedBackpack(stack)) return stack;
        }
        // armor slots (just in case)
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!slot.isArmor()) continue;
            ItemStack stack = player.getItemBySlot(slot);
            if (isUpgradedBackpack(stack)) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static boolean isUpgradedBackpack(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof BackpackItem)) return false;

        BackpackWrapper wrapper = (BackpackWrapper) BackpackWrapper.fromStack(stack);
        var upgrades = wrapper.getUpgradeHandler();
        for (int i = 0; i < upgrades.getSlots(); i++) {
            ItemStack up = upgrades.getStackInSlot(i);
            if (!up.isEmpty() && up.is(ModItems.PICK_BLOCK_UPGRADE.get())) {
                return true;
            }
        }
        return false;
    }

    // Priority:
    // 1) Slot that can provide a full request (>= request), prefer largest count
    // 2) Otherwise slot with the largest count
    private static int findBestSlotFor(IItemHandler inv, Item wanted, int request) {
        int bestSlot = -1;
        int bestCount = -1;

        int bestFullSlot = -1;
        int bestFullCount = -1;

        ItemStack wantedProbe = wanted.getDefaultInstance();

        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack s = inv.getStackInSlot(i);
            if (s.isEmpty()) continue;
            if (!ItemStack.isSameItemSameComponents(s, wantedProbe)) continue;

            int count = s.getCount();

            if (count >= request) {
                if (count > bestFullCount) {
                    bestFullCount = count;
                    bestFullSlot = i;
                }
            }

            if (count > bestCount) {
                bestCount = count;
                bestSlot = i;
            }
        }

        return bestFullSlot != -1 ? bestFullSlot : bestSlot;
    }

    private static void placeIntoHandOrHotbar(ServerPlayer player, ItemStack extracted) {
        var inv = player.getInventory();
        int max = extracted.getMaxStackSize();

        // 1) If main hand same item, top it up
        ItemStack hand = player.getMainHandItem();
        if (!hand.isEmpty() && ItemStack.isSameItemSameComponents(hand, extracted) && hand.getCount() < max) {
            int space = max - hand.getCount();
            int move = Math.min(space, extracted.getCount());
            hand.grow(move);
            extracted.shrink(move);
            if (extracted.isEmpty()) return;
        }

        // 2) If hotbar has same item stack with space, top that up
        for (int i = 0; i < 9 && !extracted.isEmpty(); i++) {
            ItemStack hot = inv.getItem(i);
            if (!hot.isEmpty() && ItemStack.isSameItemSameComponents(hot, extracted) && hot.getCount() < max) {
                int space = max - hot.getCount();
                int move = Math.min(space, extracted.getCount());
                hot.grow(move);
                extracted.shrink(move);
            }
        }
        if (extracted.isEmpty()) return;

        // 3) If selected slot empty, put it there
        int sel = inv.selected;
        ItemStack selStack = inv.getItem(sel);
        if (selStack.isEmpty()) {
            inv.setItem(sel, extracted);
            return;
        }

        // 4) Otherwise, try empty hotbar slot
        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i).isEmpty()) {
                inv.setItem(i, extracted);
                return;
            }
        }

        // 5) Otherwise, add to inventory; if it doesn't fit, drop remainder
        ItemStack remaining = inv.add(extracted) ? ItemStack.EMPTY : extracted;
        if (!remaining.isEmpty()) {
            player.drop(remaining, false);
        }
    }
}
