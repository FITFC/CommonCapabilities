package org.cyclops.commoncapabilities.ingredient.storage;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.apache.commons.lang3.tuple.Pair;
import org.cyclops.commoncapabilities.api.capability.itemhandler.ISlotlessItemHandler;
import org.cyclops.commoncapabilities.api.capability.itemhandler.ItemHandlerItemStackIterator;
import org.cyclops.commoncapabilities.api.capability.itemhandler.ItemMatch;
import org.cyclops.commoncapabilities.api.ingredient.IngredientComponent;
import org.cyclops.commoncapabilities.api.ingredient.storage.IIngredientComponentStorage;
import org.cyclops.commoncapabilities.api.ingredient.storage.IIngredientComponentStorageSlotted;
import org.cyclops.commoncapabilities.api.ingredient.storage.IIngredientComponentStorageWrapperHandler;
import org.cyclops.commoncapabilities.api.ingredient.storage.IngredientComponentStorageEmpty;
import org.cyclops.commoncapabilities.capability.itemhandler.SlotlessItemHandlerConfig;
import org.cyclops.cyclopscore.datastructure.Wrapper;
import org.cyclops.cyclopscore.helper.Helpers;
import org.cyclops.cyclopscore.ingredient.collection.FilteredIngredientCollectionIterator;
import org.cyclops.cyclopscore.ingredient.collection.IIngredientMapMutable;
import org.cyclops.cyclopscore.ingredient.collection.IngredientHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Item storage wrapper handler for {@link IItemHandler}.
 * @author rubensworks
 */
public class IngredientComponentStorageWrapperHandlerItemStack
        implements IIngredientComponentStorageWrapperHandler<ItemStack, Integer, IItemHandler> {

    private final IngredientComponent<ItemStack, Integer> ingredientComponent;

    public IngredientComponentStorageWrapperHandlerItemStack(IngredientComponent<ItemStack, Integer> ingredientComponent) {
        this.ingredientComponent = Objects.requireNonNull(ingredientComponent);
    }

    @Override
    public IIngredientComponentStorage<ItemStack, Integer> wrapComponentStorage(IItemHandler storage) {
        return new ComponentStorageWrapper(getComponent(), storage);
    }

    public IIngredientComponentStorage<ItemStack, Integer> wrapComponentStorage(IItemHandler storage,
                                                                                ISlotlessItemHandler slotlessStorage) {
        return new ComponentStorageWrapperCombined(getComponent(), storage, slotlessStorage);
    }

    @Override
    public IItemHandler wrapStorage(IIngredientComponentStorage<ItemStack, Integer> componentStorage) {
        if (componentStorage instanceof IIngredientComponentStorageSlotted) {
            return new ItemStorageWrapperSlotted(getComponent(),
                    (IIngredientComponentStorageSlotted<ItemStack, Integer>) componentStorage);
        }
        return new ItemStorageWrapper(getComponent(), componentStorage);
    }

    @Override
    public LazyOptional<IItemHandler> getStorage(ICapabilityProvider capabilityProvider, @Nullable Direction facing) {
        return capabilityProvider.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing);
    }

    @Override
    public IIngredientComponentStorage<ItemStack, Integer> getComponentStorage(ICapabilityProvider capabilityProvider,
                                                                               @Nullable Direction facing) {
        LazyOptional<IItemHandler> storageSlotted = getStorage(capabilityProvider, facing);
        LazyOptional<ISlotlessItemHandler> storageSlotless = capabilityProvider.getCapability(SlotlessItemHandlerConfig.CAPABILITY, facing);
        if (storageSlotted.isPresent()) {
            if (storageSlotless.isPresent()) {
                return wrapComponentStorage(storageSlotted.orElse(null), storageSlotless.orElse(null));
            } else {
                return wrapComponentStorage(storageSlotted.orElse(null));
            }
        }
        return new IngredientComponentStorageEmpty<>(getComponent());
    }

    @Override
    public IngredientComponent<ItemStack, Integer> getComponent() {
        return this.ingredientComponent;
    }

    public static class ComponentStorageWrapper implements IIngredientComponentStorageSlotted<ItemStack, Integer> {

        private final IngredientComponent<ItemStack, Integer> ingredientComponent;
        private final IItemHandler storage;

        public ComponentStorageWrapper(IngredientComponent<ItemStack, Integer> ingredientComponent,
                                       IItemHandler storage) {
            this.ingredientComponent = ingredientComponent;
            this.storage = storage;
        }

        @Override
        public IngredientComponent<ItemStack, Integer> getComponent() {
            return this.ingredientComponent;
        }

        @Override
        public Iterator<ItemStack> iterator() {
            return new ItemHandlerItemStackIterator(storage);
        }

        @Override
        public Iterator<ItemStack> iterator(@Nonnull ItemStack prototype, Integer matchFlags) {
            if (getComponent().getMatcher().getAnyMatchCondition().equals(matchFlags)) {
                return iterator();
            }
            return new FilteredIngredientCollectionIterator<>(iterator(), getComponent().getMatcher(),
                    prototype, matchFlags);
        }

        @Override
        public long getMaxQuantity() {
            long sum = 0;
            int slots = storage.getSlots();
            for (int slot = 0; slot < slots; slot++) {
                sum = Math.addExact(sum, storage.getSlotLimit(slot));
            }
            return sum;
        }

        @Override
        public ItemStack insert(@Nonnull ItemStack ingredient, boolean simulate) {
            return ItemHandlerHelper.insertItem(storage, ingredient, simulate);
        }

        @Override
        public ItemStack extract(@Nonnull ItemStack prototype, Integer matchFlags, boolean simulate) {
            int slots = storage.getSlots();
            boolean checkStackSize = (matchFlags & ItemMatch.STACKSIZE) > 0;
            int requiredStackSize = prototype.getCount();

            // Maintain a temporary mapping of prototype items to their total count over all slots,
            // plus the list of slots in which they are present.
            IIngredientMapMutable<ItemStack, Integer, Pair<Wrapper<Integer>, List<Integer>>> validInstancesCollapsed = new IngredientHashMap<>(getComponent());
            int subMatchFlags = matchFlags & ~ItemMatch.STACKSIZE;

            for (int slot = 0; slot < slots; slot++) {
                ItemStack extractedSimulated = storage.extractItem(slot, requiredStackSize, true);
                if (!extractedSimulated.isEmpty()
                        && getComponent().getMatcher().matches(prototype, extractedSimulated, subMatchFlags)) {
                    ItemStack storagePrototype = getComponent().getMatcher().withQuantity(extractedSimulated, 1);

                    // Get existing value from temporary mapping
                    Pair<Wrapper<Integer>, List<Integer>> existingValue = validInstancesCollapsed.get(storagePrototype);
                    if (existingValue == null) {
                        existingValue = Pair.of(new Wrapper<>(0), Lists.newLinkedList());
                        validInstancesCollapsed.put(storagePrototype, existingValue);
                    }

                    // Update the counter and slot-list for our prototype
                    int newCount = existingValue.getLeft().get() + extractedSimulated.getCount();
                    existingValue.getLeft().set(newCount);
                    existingValue.getRight().add(slot);

                    // If the count is sufficient for our query, return
                    if (newCount >= requiredStackSize) {
                        // Actually extract if we are not simulating the extraction
                        // We assume that the simulated extraction resulted in the same output
                        // as the non-simulated output, so we ignore its output
                        existingValue.getLeft().set(requiredStackSize);
                        return finalizeExtraction(storagePrototype, existingValue, requiredStackSize, simulate);
                    }
                }
            }

            // If we reach this point, then our effective count is below requiredStackSize

            // Fail if we required an exact quantity
            if (checkStackSize) {
                return ItemStack.EMPTY;
            }

            // Extract for the instance that had the most matches if we didn't require an exact quantity
            Pair<Wrapper<Integer>, List<Integer>> maxValue = Pair.of(new Wrapper<>(0), Lists.newArrayList());
            ItemStack maxInstance = ItemStack.EMPTY;
            for (Map.Entry<ItemStack, Pair<Wrapper<Integer>, List<Integer>>> entry : validInstancesCollapsed) {
                if (entry.getValue().getLeft().get() > maxValue.getLeft().get()) {
                    maxInstance = entry.getKey();
                    maxValue = entry.getValue();
                }
            }
            return finalizeExtraction(maxInstance, maxValue, requiredStackSize, simulate);
        }

        protected ItemStack finalizeExtraction(ItemStack instancePrototype, Pair<Wrapper<Integer>, List<Integer>> value,
                                               int requiredQuantity, boolean simulate) {
            long extractedCount = value.getLeft().get();
            if (!simulate && extractedCount > 0) {
                int toExtract = requiredQuantity;
                for (Integer finalSlot : value.getRight()) {
                    ItemStack extractedActual = storage.extractItem(finalSlot, toExtract, false);
                    toExtract -= extractedActual.getCount();
                }
                // Quick heuristic check to see if 'storage' did not lie during its simulation
                if (toExtract != requiredQuantity - extractedCount) {
                    throw new IllegalStateException("An item storage resulted in inconsistent simulated and non-simulated output.");
                }
            }
            return getComponent().getMatcher().withQuantity(instancePrototype, extractedCount);
        }

        @Override
        public ItemStack extract(long maxQuantity, boolean simulate) {
            int slots = storage.getSlots();
            int amount = Helpers.castSafe(maxQuantity);
            for (int slot = 0; slot < slots; slot++) {
                ItemStack extractedSimulated = storage.extractItem(slot, amount, true);
                if (!extractedSimulated.isEmpty()) {
                    return simulate ? extractedSimulated : storage.extractItem(slot, amount, false);
                }
            }
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlots() {
            return storage.getSlots();
        }

        @Override
        public ItemStack getSlotContents(int slot) {
            return storage.getStackInSlot(slot);
        }

        @Override
        public long getMaxQuantity(int slot) {
            return storage.getSlotLimit(slot);
        }

        @Override
        public ItemStack insert(int slot, @Nonnull ItemStack ingredient, boolean simulate) {
            return storage.insertItem(slot, ingredient, simulate);
        }

        @Override
        public ItemStack extract(int slot, long maxQuantity, boolean simulate) {
            return storage.extractItem(slot, Helpers.castSafe(maxQuantity), simulate);
        }
    }

    public static class ComponentStorageWrapperCombined extends ComponentStorageWrapper {

        private final ISlotlessItemHandler storageSlotless;

        public ComponentStorageWrapperCombined(IngredientComponent<ItemStack, Integer> ingredientComponent,
                                               IItemHandler storage, ISlotlessItemHandler storageSlotless) {
            super(ingredientComponent, storage);
            this.storageSlotless = storageSlotless;
        }

        @Override
        public Iterator<ItemStack> iterator() {
            return storageSlotless.getItems();
        }

        @Override
        public Iterator<ItemStack> iterator(@Nonnull ItemStack prototype, Integer matchFlags) {
            return storageSlotless.findItems(prototype, matchFlags);
        }

        @Override
        public long getMaxQuantity() {
            return storageSlotless.getLimit();
        }

        @Override
        public ItemStack insert(@Nonnull ItemStack ingredient, boolean simulate) {
            return storageSlotless.insertItem(ingredient, simulate);
        }

        @Override
        public ItemStack extract(long maxQuantity, boolean simulate) {
            return storageSlotless.extractItem(Helpers.castSafe(maxQuantity), simulate);
        }

        @Override
        public ItemStack extract(@Nonnull ItemStack prototype, Integer matchFlags, boolean simulate) {
            return storageSlotless.extractItem(prototype, matchFlags, simulate);
        }
    }

    public static class ItemStorageWrapper implements IItemHandler {

        private final IngredientComponent<ItemStack, Integer> ingredientComponent;
        private final IIngredientComponentStorage<ItemStack, Integer> storage;

        public ItemStorageWrapper(IngredientComponent<ItemStack, Integer> ingredientComponent,
                                  IIngredientComponentStorage<ItemStack, Integer> storage) {
            this.ingredientComponent = ingredientComponent;
            this.storage = storage;
        }

        @Override
        public int getSlots() {
            // +1 so that at least one slot appears empty, for when others want to insert
            return Iterators.size(storage.iterator()) + 1;
        }

        @Nonnull
        @Override
        public ItemStack getStackInSlot(int slot) {
            try {
                return Iterators.get(storage.iterator(), slot);
            } catch (IndexOutOfBoundsException e) {
                return ItemStack.EMPTY;
            }
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            return storage.insert(stack, simulate);
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack slotItem = Iterators.get(storage.iterator(), slot, ItemStack.EMPTY);
            if (slotItem.isEmpty()) {
                return slotItem;
            }
            return storage.extract(ingredientComponent.getMatcher().withQuantity(slotItem, amount),
                    ingredientComponent.getMatcher().getExactMatchNoQuantityCondition(), simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return Helpers.castSafe(ingredientComponent.getMatcher().getMaximumQuantity());
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return true;
        }
    }

    public static class ItemStorageWrapperSlotted implements IItemHandler {

        private final IngredientComponent<ItemStack, Integer> ingredientComponent;
        private final IIngredientComponentStorageSlotted<ItemStack, Integer> storage;

        public ItemStorageWrapperSlotted(IngredientComponent<ItemStack, Integer> ingredientComponent,
                                         IIngredientComponentStorageSlotted<ItemStack, Integer> storage) {
            this.ingredientComponent = ingredientComponent;
            this.storage = storage;
        }

        @Override
        public int getSlots() {
            return storage.getSlots();
        }

        @Nonnull
        @Override
        public ItemStack getStackInSlot(int slot) {
            return storage.getSlotContents(slot);
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            return storage.insert(slot, stack, simulate);
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return storage.extract(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return Helpers.castSafe(storage.getMaxQuantity(slot));
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return true;
        }
    }
}
