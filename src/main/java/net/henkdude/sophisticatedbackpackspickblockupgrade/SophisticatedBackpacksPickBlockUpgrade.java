package net.henkdude.sophisticatedbackpackspickblockupgrade;

import net.henkdude.sophisticatedbackpackspickblockupgrade.registry.ModCreativeModeTabs;
import net.henkdude.sophisticatedbackpackspickblockupgrade.registry.ModItems;
import net.henkdude.sophisticatedbackpackspickblockupgrade.registry.ModNetworking;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(SophisticatedBackpacksPickBlockUpgrade.MODID)
public class SophisticatedBackpacksPickBlockUpgrade {
    public static final String MODID = "sophisticated_backpacks_pick_block_upgrade";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SophisticatedBackpacksPickBlockUpgrade(IEventBus modEventBus, ModContainer modContainer) {
        ModItems.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);

        modEventBus.addListener(this::commonSetup);

    }


    private void commonSetup(final FMLCommonSetupEvent event) {
        // nothing yet
    }
}
