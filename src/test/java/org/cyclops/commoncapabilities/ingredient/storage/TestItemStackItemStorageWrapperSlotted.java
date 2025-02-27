package org.cyclops.commoncapabilities.ingredient.storage;

import net.minecraft.DetectedVersion;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.cyclops.commoncapabilities.IngredientComponents;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TestItemStackItemStorageWrapperSlotted {

    private static ItemStack APPLE_1;
    private static ItemStack APPLE_10;

    private static ItemStack APPLE_64;
    private static ItemStack APPLE_8;
    private static ItemStack APPLE_9;
    private static ItemStack APPLE_11;

    private IItemHandler innerStorage;
    private IngredientComponentStorageWrapperHandlerItemStack.ComponentStorageWrapper storage;
    private IngredientComponentStorageWrapperHandlerItemStack.ItemStorageWrapperSlotted wrapper;

    @BeforeClass
    public static void init() {
        // We need the Minecraft registries to be filled
        SharedConstants.setVersion(DetectedVersion.BUILT_IN);
        Bootstrap.bootStrap();
    }

    public static boolean eq(ItemStack a, ItemStack b) {
        return IngredientComponents.ITEMSTACK.getMatcher().matchesExactly(a, b);
    }

    @Before
    public void beforeEach() {
        APPLE_1 = new ItemStack(Items.APPLE, 1);
        APPLE_10 = new ItemStack(Items.APPLE, 10);

        APPLE_64 = new ItemStack(Items.APPLE, 64);
        APPLE_8 = new ItemStack(Items.APPLE, 8);
        APPLE_9 = new ItemStack(Items.APPLE, 9);
        APPLE_11 = new ItemStack(Items.APPLE, 11);

        innerStorage = new ItemStackHandler(10);
        ((ItemStackHandler) innerStorage).setStackInSlot(2, APPLE_1.copy());
        ((ItemStackHandler) innerStorage).setStackInSlot(4, APPLE_1.copy());
        ((ItemStackHandler) innerStorage).setStackInSlot(6, APPLE_10.copy());
        ((ItemStackHandler) innerStorage).setStackInSlot(8, APPLE_10.copy());
        storage = new IngredientComponentStorageWrapperHandlerItemStack.ComponentStorageWrapper(IngredientComponents.ITEMSTACK, innerStorage);
        wrapper = new IngredientComponentStorageWrapperHandlerItemStack.ItemStorageWrapperSlotted(IngredientComponents.ITEMSTACK, storage);
    }

    @Test
    public void testGetSlots() {
        assertThat(wrapper.getSlots(), is(10));
    }

    @Test
    public void testGetStackInSlot() {
        assertThat(eq(wrapper.getStackInSlot(0), ItemStack.EMPTY), is(true));
        assertThat(eq(wrapper.getStackInSlot(1), ItemStack.EMPTY), is(true));
        assertThat(eq(wrapper.getStackInSlot(2), APPLE_1), is(true));
        assertThat(eq(wrapper.getStackInSlot(4), APPLE_1), is(true));
        assertThat(eq(wrapper.getStackInSlot(6), APPLE_10), is(true));
        assertThat(eq(wrapper.getStackInSlot(8), APPLE_10), is(true));
    }

    @Test(expected = RuntimeException.class)
    public void testGetStackInSlotTooSmall() {
        wrapper.getStackInSlot(-1);
    }

    @Test(expected = RuntimeException.class)
    public void testGetStackInSlotTooBig() {
        wrapper.getStackInSlot(10);
    }

    @Test
    public void testInsertItem() {
        assertThat(eq(wrapper.insertItem(0, APPLE_1, true), ItemStack.EMPTY), is(true));
        assertThat(eq(wrapper.insertItem(0, APPLE_1, false), ItemStack.EMPTY), is(true));
    }

    @Test
    public void testExtractItem() {
        assertThat(eq(wrapper.extractItem(2, 10, true), APPLE_1), is(true));
        assertThat(eq(wrapper.extractItem(4, 10, true), APPLE_1), is(true));
        assertThat(eq(wrapper.extractItem(6, 10, true), APPLE_10), is(true));

        assertThat(eq(wrapper.extractItem(2, 10, false), APPLE_1), is(true));
        assertThat(eq(wrapper.extractItem(2, 10, false), ItemStack.EMPTY), is(true));
        assertThat(eq(wrapper.extractItem(4, 10, false), APPLE_1), is(true));
        assertThat(eq(wrapper.extractItem(6, 10, false), APPLE_10), is(true));
        assertThat(eq(wrapper.extractItem(8, 10, false), APPLE_10), is(true));
        assertThat(eq(wrapper.extractItem(8, 10, false), ItemStack.EMPTY), is(true));
    }

    @Test
    public void testGetSlotLimit() {
        assertThat(wrapper.getSlotLimit(0), is(64));
        assertThat(wrapper.getSlotLimit(1), is(64));
        assertThat(wrapper.getSlotLimit(2), is(64));
    }

}
