package com.bill.carpetbilladdition;

import carpet.api.settings.CarpetRule;
import carpet.api.settings.Rule;
import carpet.api.settings.RuleCategory;
import carpet.api.settings.Validator;
import carpet.api.settings.Validators;
import net.minecraft.commands.CommandSourceStack;

public final class CbaSettings {
    public static final String BILL = "bill";

    private CbaSettings() {
    }

    @Rule(
            categories = {BILL, RuleCategory.EXPERIMENTAL, RuleCategory.SURVIVAL},
            conditions = IllegalBlockEntitiesRemoved.class
    )
    public static boolean reintroduceIllegalBlockEntity = false;

    @Rule(
            categories = {BILL, RuleCategory.EXPERIMENTAL, RuleCategory.SURVIVAL},
            options = {"0", "16", "32", "64"},
            strict = false,
            validators = SoundSuppressionRangeValidator.class
    )
    public static int soundSuppressionRange = 0;

    @Rule(
            categories = {BILL, RuleCategory.COMMAND},
            options = {"true", "false", "ops", "0", "1", "2", "3", "4"},
            validators = Validators.CommandLevel.class
    )
    public static String commandPlayerPrefix = "false";

    public static final class SoundSuppressionRangeValidator extends Validator<Integer> {
        public static final int MAX = 64;

        @Override
        public Integer validate(CommandSourceStack source, CarpetRule<Integer> rule, Integer value, String userInput) {
            return value >= 0 && value <= MAX ? value : null;
        }

        @Override
        public String description() {
            return "Must be between 0 (vanilla) and " + MAX;
        }
    }

    /**
     * Vanilla 1.21 still has illegal block entities, so reintroduceIllegalBlockEntity only exists from 1.21.1 on.
     */
    public static final class IllegalBlockEntitiesRemoved implements Rule.Condition {
        @Override
        public boolean shouldRegister() {
            return MinecraftVersions.atLeast("1.21.1");
        }
    }
}
