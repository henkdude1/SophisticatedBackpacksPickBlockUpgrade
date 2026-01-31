package net.henkdude.sophisticatedbackpackspickblockupgrade.client;

import net.henkdude.sophisticatedbackpackspickblockupgrade.SophisticatedBackpacksPickBlockUpgrade;
import net.henkdude.sophisticatedbackpackspickblockupgrade.network.C2SRequestPickBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(
        modid = SophisticatedBackpacksPickBlockUpgrade.MODID,
        bus = EventBusSubscriber.Bus.GAME,
        value = Dist.CLIENT
)
public final class ClientPickBlockHandler {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.screen != null) return;

        // Fires once per press
        if (!mc.options.keyPickItem.consumeClick()) return;

        HitResult hit = mc.hitResult;
        if (!(hit instanceof BlockHitResult bhr) || hit.getType() != HitResult.Type.BLOCK) return;

        Block block = mc.level.getBlockState(bhr.getBlockPos()).getBlock();
        Item wanted = block.asItem();
        if (wanted == null || wanted == Items.AIR) return;

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(wanted);
        if (id == null) return;

        PacketDistributor.sendToServer(new C2SRequestPickBlock(id));
    }
}
