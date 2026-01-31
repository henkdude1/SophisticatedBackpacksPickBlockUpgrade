package net.henkdude.sophisticatedbackpackspickblockupgrade.client;

import net.henkdude.sophisticatedbackpackspickblockupgrade.SophisticatedBackpacksPickBlockUpgrade;
import net.henkdude.sophisticatedbackpackspickblockupgrade.network.C2SRequestPickBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.world.item.Items;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;


@EventBusSubscriber(modid = SophisticatedBackpacksPickBlockUpgrade.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class ClientPickBlockHandler {

    private static boolean wasDown = false;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.screen != null) return;

        // hitResult is not guaranteed to be non-null every tick
        if (mc.hitResult == null) return;

        boolean down = mc.options.keyPickItem.isDown();
        if (down && !wasDown) {
            HitResult hit = mc.hitResult;

            if (hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult bhr) {

                // Don't do anything if player has no Sophisticated Backpacks at all
                boolean hasAnyBackpack = false;
                for (var s : mc.player.getInventory().items) {
                    if (!s.isEmpty() && s.getItem() instanceof BackpackItem) {
                        hasAnyBackpack = true;
                        break;
                    }
                }
                if (!hasAnyBackpack) {
                    wasDown = down;
                    return;
                }

                Block block = mc.level.getBlockState(bhr.getBlockPos()).getBlock();
                Item wanted = block.asItem();

                // asItem() can be AIR for blocks without an item form
                if (wanted == Items.AIR) {
                    wasDown = down;
                    return;
                }

                ResourceLocation id = BuiltInRegistries.ITEM.getKey(wanted);
                PacketDistributor.sendToServer(new C2SRequestPickBlock(id));
            }
        }

        wasDown = down;
    }


}
