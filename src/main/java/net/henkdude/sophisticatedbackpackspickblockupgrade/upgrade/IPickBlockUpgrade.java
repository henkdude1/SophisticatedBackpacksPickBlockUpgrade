package net.henkdude.sophisticatedbackpackspickblockupgrade.upgrade;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

public interface IPickBlockUpgrade {
    boolean tryPickFromThisBackpack(Player player, Item wanted);
}
