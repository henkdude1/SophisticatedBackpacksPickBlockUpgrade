package net.henkdude.sophisticatedbackpackspickblockupgrade.client;

import net.henkdude.sophisticatedbackpackspickblockupgrade.SophisticatedBackpacksPickBlockUpgrade;
import net.henkdude.sophisticatedbackpackspickblockupgrade.network.C2SRequestPickBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;
import top.theillusivec4.curios.api.CuriosApi;

@EventBusSubscriber(
        modid = SophisticatedBackpacksPickBlockUpgrade.MODID,
        bus = EventBusSubscriber.Bus.GAME,
        value = Dist.CLIENT
)
public final class ClientPickBlockHandler {

    private static boolean wasDown = false;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.screen != null) return;

        boolean down = mc.options.keyPickItem.isDown();
        if (!down || wasDown) {
            wasDown = down;
            return;
        }

        // Let vanilla pick-block work unless player has the upgrade somewhere
        if (!playerHasPickBlockUpgrade(mc.player)) {
            wasDown = down;
            return;
        }

        HitResult hit = mc.hitResult;
        if (!(hit instanceof BlockHitResult bhr) || hit.getType() != HitResult.Type.BLOCK) {
            wasDown = down;
            return;
        }

        Item wanted = mc.level.getBlockState(bhr.getBlockPos()).getBlock().asItem();
        if (wanted == null) {
            wasDown = down;
            return;
        }

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(wanted);
        if (id != null) {
            PacketDistributor.sendToServer(new C2SRequestPickBlock(id));
        }

        wasDown = down;
    }

    /* ========================= HELPERS ========================= */

    private static boolean playerHasPickBlockUpgrade(Player player) {
        // Inventory
        for (ItemStack stack : player.getInventory().items) {
            if (isBackpackWithUpgrade(stack)) return true;
        }

        // Armor + offhand
        for (ItemStack stack : player.getArmorSlots()) {
            if (isBackpackWithUpgrade(stack)) return true;
        }
        if (isBackpackWithUpgrade(player.getOffhandItem())) return true;

        // Curios
        return CuriosApi.getCuriosInventory(player)
                .map(curios ->
                        curios.getCurios().values().stream().anyMatch(handler -> {
                            for (int i = 0; i < handler.getSlots(); i++) {
                                if (isBackpackWithUpgrade(handler.getStacks().getStackInSlot(i))) {
                                    return true;
                                }
                            }
                            return false;
                        })
                )
                .orElse(false);
    }

    private static boolean isBackpackWithUpgrade(ItemStack stack) {
        if (!(stack.getItem() instanceof BackpackItem)) return false;

        // ✅ FIX: use the interface type, not BackpackWrapper
        IBackpackWrapper wrapper = BackpackWrapper.fromStack(stack);
        if (wrapper == null) return false;

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
}
