package net.henkdude.sophisticatedbackpackspickblockupgrade.registry;

import net.henkdude.sophisticatedbackpackspickblockupgrade.SophisticatedBackpacksPickBlockUpgrade;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SophisticatedBackpacksPickBlockUpgrade.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN =
            TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + SophisticatedBackpacksPickBlockUpgrade.MODID))
                    .icon(() -> new ItemStack(ModItems.PICK_BLOCK_UPGRADE.get()))
                    .displayItems((params, out) -> out.accept(ModItems.PICK_BLOCK_UPGRADE.get()))
                    .build()
            );

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}
