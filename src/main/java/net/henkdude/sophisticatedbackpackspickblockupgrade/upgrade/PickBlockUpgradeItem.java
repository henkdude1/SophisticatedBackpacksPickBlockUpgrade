package net.henkdude.sophisticatedbackpackspickblockupgrade.upgrade;

import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeCountLimitConfig;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;

import java.util.List;

public class PickBlockUpgradeItem extends UpgradeItemBase<PickBlockUpgradeWrapper> {

    public static final UpgradeType<PickBlockUpgradeWrapper> TYPE =
            new UpgradeType<>(PickBlockUpgradeWrapper::new);

    public PickBlockUpgradeItem(IUpgradeCountLimitConfig upgradeTypeLimitConfig) {
        super(upgradeTypeLimitConfig);
    }

    @Override
    public UpgradeType<PickBlockUpgradeWrapper> getType() {
        return TYPE;
    }

    @Override
    public List<IUpgradeItem.UpgradeConflictDefinition> getUpgradeConflicts() {
        return List.of(); // no conflicts
    }
}
