// COMPILE-TIME STUB ONLY — excluded from the mod jar (build.gradle: exclude 'com/tiviacz/**').
// PLAY_RECORD is a compile-time constant: javac INLINES the value into TravelersBackpackCompat,
// so this value MUST stay identical to the real TB source (verified: ServerActions.java:268,
// public static final int PLAY_RECORD = 3).
// modifyUpgradeTab(BackpackWrapper, int, boolean, int) is the real mixin target; its descriptor
// (Lcom/tiviacz/travelersbackpack/inventory/BackpackWrapper;IZI)V was verified against TB source.
package com.tiviacz.travelersbackpack.common;

import com.tiviacz.travelersbackpack.inventory.BackpackWrapper;

public class ServerActions {
        public static final int PLAY_RECORD = 3;

        public static void modifyUpgradeTab(BackpackWrapper wrapper, int slot, boolean open, int packetType) {
                throw new RuntimeException("VinURL compile-time stub");
        }
}
