package net.henkdude.sophisticatedbackpackspickblockupgrade.registry;

import net.henkdude.sophisticatedbackpackspickblockupgrade.SophisticatedBackpacksPickBlockUpgrade;
import net.henkdude.sophisticatedbackpackspickblockupgrade.upgrade.PickBlockUpgradeItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.p3pp3rf1y.sophisticatedbackpacks.Config;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(BuiltInRegistries.ITEM, SophisticatedBackpacksPickBlockUpgrade.MODID);

    public static final DeferredHolder<Item, PickBlockUpgradeItem> PICK_BLOCK_UPGRADE =
            ITEMS.register("pick_block_upgrade",
                    () -> new PickBlockUpgradeItem(Config.SERVER.maxUpgradesPerStorage)
            );

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
