package net.henkdude.sophisticatedbackpackspickblockupgrade.registry;

import net.henkdude.sophisticatedbackpackspickblockupgrade.SophisticatedBackpacksPickBlockUpgrade;
import net.henkdude.sophisticatedbackpackspickblockupgrade.network.C2SRequestPickBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = SophisticatedBackpacksPickBlockUpgrade.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class ModNetworking {
    @SubscribeEvent
    public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(
                C2SRequestPickBlock.TYPE,
                C2SRequestPickBlock.STREAM_CODEC,
                (msg, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof net.minecraft.server.level.ServerPlayer sp) {
                        C2SRequestPickBlock.handle(msg, sp);
                    }
                })
        );
    }
}

