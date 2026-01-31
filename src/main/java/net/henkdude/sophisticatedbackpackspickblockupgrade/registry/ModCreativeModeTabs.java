package net.henkdude.sophisticatedbackpackspickblockupgrade.registry;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;

import static net.henkdude.sophisticatedbackpackspickblockupgrade.SophisticatedBackpacksPickBlockUpgrade.CREATIVE_MODE_TABS;


public class ModCreativeModeTabs {
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = CREATIVE_MODE_TABS.register("tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("creativetab.henkdude.sophisticated_backpacks_pick_block_upgrade"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> ModItems.PICK_BLOCK_UPGRADE.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(ModItems.PICK_BLOCK_UPGRADE.get());

            }).build());

    public static void register(IEventBus modEventBus) {

        CREATIVE_MODE_TABS.register(modEventBus);
    }
}