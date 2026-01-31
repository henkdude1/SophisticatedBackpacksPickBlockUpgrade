package net.henkdude.sophisticatedbackpackspickblockupgrade.network;

import net.henkdude.sophisticatedbackpackspickblockupgrade.SophisticatedBackpacksPickBlockUpgrade;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.List;

public record C2SRequestPickBlock(ResourceLocation itemId) implements CustomPacketPayload {

    public static final Type<C2SRequestPickBlock> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    SophisticatedBackpacksPickBlockUpgrade.MODID, "pick_block"
            ));

    /** ResourceLocation codec for NeoForge 1.21.x */
    public static final StreamCodec<RegistryFriendlyByteBuf, ResourceLocation> RL_CODEC =
            StreamCodec.of(
                    (buf, rl) -> buf.writeResourceLocation(rl),   // encoder: (B, V) -> void
                    RegistryFriendlyByteBuf::readResourceLocation // decoder: (B) -> V
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SRequestPickBlock> STREAM_CODEC =
            StreamCodec.composite(
                    RL_CODEC,
                    C2SRequestPickBlock::itemId,
                    C2SRequestPickBlock::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /* ========================= SERVER HANDLER ========================= */

    public static void handle(C2SRequestPickBlock msg, ServerPlayer player) {
        if (player == null) return;

        Item wanted = BuiltInRegistries.ITEM.get(msg.itemId());
        if (wanted == null) return;

        List<ItemStack> backpacks = new ArrayList<>();

        /* ---------- inventory ---------- */
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.getItem() instanceof BackpackItem) backpacks.add(s);
        }

        /* ---------- armor / offhand ---------- */
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack s = player.getItemBySlot(slot);
            if (s.getItem() instanceof BackpackItem) backpacks.add(s);
        }

        /* ---------- curios ---------- */
        CuriosApi.getCuriosInventory(player).ifPresent(curios ->
                curios.getCurios().forEach((id, handler) -> {
                    for (int i = 0; i < handler.getSlots(); i++) {
                        ItemStack s = handler.getStacks().getStackInSlot(i);
                        if (s.getItem() instanceof BackpackItem) backpacks.add(s);
                    }
                })
        );

        /* ---------- try each backpack ---------- */
        for (ItemStack backpackStack : backpacks) {
            IBackpackWrapper wrapper = BackpackWrapper.fromStack(backpackStack); // <-- returns IBackpackWrapper
            if (!hasPickBlockUpgrade(wrapper)) continue;

            ItemStack extracted = extractBestStack(wrapper, wanted, 64);
            if (extracted.isEmpty()) continue;

            giveToPlayer(player, extracted);
            return;
        }
    }

    /* ========================= HELPERS ========================= */

    private static boolean hasPickBlockUpgrade(IBackpackWrapper wrapper) {
        var upgrades = wrapper.getUpgradeHandler();
        for (int i = 0; i < upgrades.getSlots(); i++) {
            ItemStack up = upgrades.getStackInSlot(i);
            if (up.isEmpty()) continue;

            ResourceLocation id = BuiltInRegistries.ITEM.getKey(up.getItem());
            if (id != null
                    && id.getNamespace().equals(SophisticatedBackpacksPickBlockUpgrade.MODID)
                    && id.getPath().equals("pick_block_upgrade")
                    && up.getItem() instanceof IUpgradeItem<?>) {
                return true;
            }
        }
        return false;
    }

    private static ItemStack extractBestStack(IBackpackWrapper wrapper, Item wanted, int target) {
        var inv = wrapper.getInventoryHandler();

        int bestFullSlot = -1;
        int bestFullCount = -1;
        int bestAnySlot = -1;
        int bestAnyCount = -1;

        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack s = inv.getStackInSlot(i);
            if (s.isEmpty() || s.getItem() != wanted) continue;

            int c = s.getCount();
            if (c >= target && c > bestFullCount) {
                bestFullCount = c;
                bestFullSlot = i;
            }
            if (c > bestAnyCount) {
                bestAnyCount = c;
                bestAnySlot = i;
            }
        }

        int slot = bestFullSlot != -1 ? bestFullSlot : bestAnySlot;
        if (slot == -1) return ItemStack.EMPTY;

        ItemStack src = inv.getStackInSlot(slot);
        int take = Math.min(target, src.getCount());

        ItemStack out = src.copy();
        out.setCount(take);

        src.shrink(take);
        inv.setStackInSlot(slot, src);

        return out;
    }

    private static void giveToPlayer(ServerPlayer player, ItemStack stack) {
        var inv = player.getInventory();
        int sel = inv.selected;

        ItemStack hand = inv.getItem(sel);
        if (!hand.isEmpty()
                && ItemStack.isSameItemSameComponents(hand, stack)
                && hand.getCount() < hand.getMaxStackSize()) {

            int move = Math.min(hand.getMaxStackSize() - hand.getCount(), stack.getCount());
            hand.grow(move);
            stack.shrink(move);
        }

        if (!stack.isEmpty() && inv.getItem(sel).isEmpty()) {
            inv.setItem(sel, stack);
            stack = ItemStack.EMPTY;
        }

        if (!stack.isEmpty() && !inv.add(stack)) {
            player.drop(stack, false);
        }

        player.inventoryMenu.broadcastChanges();
    }
}
