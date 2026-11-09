// COMPILE-TIME STUB ONLY — excluded from the mod jar (build.gradle: exclude 'com/tiviacz/**').
// CRITICAL: the real field is declared as Guava BiMap (UpgradeManager.java:22,
// public BiMap<Integer, Optional<UpgradeBase<?>>> mappedUpgrades). Field descriptors are
// resolved exactly at runtime, so this stub must ALSO declare BiMap — declaring it as Map
// would cause NoSuchFieldError when TravelersBackpackCompat reads the field.
package com.tiviacz.travelersbackpack.inventory;

import com.google.common.collect.BiMap;
import com.tiviacz.travelersbackpack.inventory.upgrades.UpgradeBase;

import java.util.Optional;

public class UpgradeManager {
        public BiMap<Integer, Optional<UpgradeBase<?>>> mappedUpgrades;
}
