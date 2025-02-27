package org.cyclops.commoncapabilities.ingredient;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.cyclops.commoncapabilities.api.capability.fluidhandler.FluidMatch;
import org.cyclops.commoncapabilities.api.ingredient.IIngredientMatcher;
import org.cyclops.cyclopscore.helper.Helpers;

/**
 * Matcher for FluidStacks.
 * @author rubensworks
 */
public class IngredientMatcherFluidStack implements IIngredientMatcher<FluidStack, Integer> {

    @Override
    public boolean isInstance(Object object) {
        return object instanceof FluidStack;
    }

    @Override
    public Integer getAnyMatchCondition() {
        return FluidMatch.ANY;
    }

    @Override
    public Integer getExactMatchCondition() {
        return FluidMatch.EXACT;
    }

    @Override
    public Integer getExactMatchNoQuantityCondition() {
        return FluidMatch.FLUID | FluidMatch.TAG;
    }

    @Override
    public Integer withCondition(Integer matchCondition, Integer with) {
        return matchCondition | with;
    }

    @Override
    public Integer withoutCondition(Integer matchCondition, Integer without) {
        return matchCondition & ~without;
    }

    @Override
    public boolean hasCondition(Integer matchCondition, Integer searchCondition) {
        return (matchCondition & searchCondition) > 0;
    }

    @Override
    public boolean matches(FluidStack a, FluidStack b, Integer matchCondition) {
        return FluidMatch.areFluidStacksEqual(a, b, matchCondition);
    }

    @Override
    public FluidStack getEmptyInstance() {
        return FluidStack.EMPTY;
    }

    @Override
    public int hash(FluidStack instance) {
        if (instance.isEmpty()) {
            return 0;
        }

        int code = 1;
        code = 31 * code + instance.getFluid().hashCode();
        code = 31 * code + instance.getAmount();
        if (instance.getTag() != null)
            code = 31 * code + instance.getTag().hashCode();
        return code;
    }

    @Override
    public FluidStack copy(FluidStack instance) {
        if (instance.isEmpty()) {
            return getEmptyInstance();
        }
        return instance.copy();
    }

    @Override
    public long getQuantity(FluidStack instance) {
        return instance.getAmount();
    }

    @Override
    public FluidStack withQuantity(FluidStack instance, long quantity) {
        if (quantity == 0) {
            return getEmptyInstance();
        }
        if (instance.isEmpty()) {
            return new FluidStack(Fluids.WATER, Helpers.castSafe(quantity));
        }
        if (instance.getAmount() == quantity) {
            return instance;
        }
        FluidStack copy = instance.copy();
        copy.setAmount(Helpers.castSafe(quantity));
        return copy;
    }

    @Override
    public long getMaximumQuantity() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int conditionCompare(Integer a, Integer b) {
        return Integer.compare(a, b);
    }

    @Override
    public String localize(FluidStack instance) {
        return instance.getDisplayName().getString();
    }

    @Override
    public MutableComponent getDisplayName(FluidStack instance) {
        return (MutableComponent) instance.getDisplayName();
    }

    @Override
    public String toString(FluidStack instance) {
        return String.format("%s %s %s", ForgeRegistries.FLUIDS.getKey(instance.getFluid()), instance.getAmount(), instance.getTag());
    }

    @Override
    public int compare(FluidStack o1, FluidStack o2) {
        if (o1.isEmpty()) {
            if (o2.isEmpty()) {
                return 0;
            } else {
                return -1;
            }
        } else if (o2.isEmpty()) {
            return 1;
        } else if (o1.getFluid() == o2.getFluid()) {
            if (o1.getAmount() == o2.getAmount()) {
                return IngredientHelpers.compareTags(o1.getTag(), o2.getTag());
            }
            return o1.getAmount() - o2.getAmount();
        }
        return ForgeRegistries.FLUIDS.getKey(o1.getFluid()).compareTo(ForgeRegistries.FLUIDS.getKey(o2.getFluid()));
    }

}
