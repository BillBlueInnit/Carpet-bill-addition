package com.bill.carpetbilladdition;

import carpet.api.settings.Rule;
import carpet.api.settings.RuleCategory;

public final class CbaSettings {
    public static final String BILL = "bill";

    private CbaSettings() {
    }

    @Rule(categories = {BILL, RuleCategory.EXPERIMENTAL, RuleCategory.SURVIVAL})
    public static boolean reintroduceIllegalBlockEntity = false;

    @Rule(categories = {BILL, RuleCategory.FEATURE, RuleCategory.SURVIVAL})
    public static LegacyThrowableMovementMode legacyThrowableMovement = LegacyThrowableMovementMode.OFF;

    public enum LegacyThrowableMovementMode {
        OFF,
        PEARL,
        ALL
    }
}
