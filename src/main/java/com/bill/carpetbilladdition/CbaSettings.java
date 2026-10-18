package com.bill.carpetbilladdition;

import carpet.api.settings.CarpetRule;
import carpet.api.settings.Rule;
import carpet.api.settings.RuleCategory;
import carpet.api.settings.Validator;
import net.minecraft.commands.CommandSourceStack;

public final class CbaSettings {
    public static final String BILL = "bill";

    private CbaSettings() {
    }

    @Rule(categories = {BILL, RuleCategory.EXPERIMENTAL, RuleCategory.SURVIVAL})
    public static boolean reintroduceIllegalBlockEntity = false;

    @Rule(
            categories = {BILL, RuleCategory.EXPERIMENTAL, RuleCategory.SURVIVAL},
            options = {"0", "16", "32", "64"},
            strict = false,
            validators = SoundSuppressionRangeValidator.class
    )
    public static int soundSuppressionRange = 0;

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
}
