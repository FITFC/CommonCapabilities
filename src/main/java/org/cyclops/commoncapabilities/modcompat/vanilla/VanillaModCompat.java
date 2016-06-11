package org.cyclops.commoncapabilities.modcompat.vanilla;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.world.IWorldNameable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.cyclops.commoncapabilities.CommonCapabilities;
import org.cyclops.commoncapabilities.Reference;
import org.cyclops.commoncapabilities.api.capability.work.IWorker;
import org.cyclops.commoncapabilities.capability.Capabilities;
import org.cyclops.commoncapabilities.capability.worldnameable.EntityLivingWorldNameable;
import org.cyclops.commoncapabilities.capability.worldnameable.ItemStackWorldNameable;
import org.cyclops.commoncapabilities.modcompat.vanilla.capability.work.VanillaBrewingStandWorker;
import org.cyclops.commoncapabilities.modcompat.vanilla.capability.work.VanillaFurnaceWorker;
import org.cyclops.cyclopscore.modcompat.IModCompat;
import org.cyclops.cyclopscore.modcompat.capabilities.CapabilityConstructorRegistry;
import org.cyclops.cyclopscore.modcompat.capabilities.DefaultCapabilityProvider;
import org.cyclops.cyclopscore.modcompat.capabilities.ICapabilityConstructor;

import javax.annotation.Nullable;

/**
 * Capabilities for Vanilla.
 * @author rubensworks
 */
public class VanillaModCompat implements IModCompat {
    @Override
    public String getModID() {
        return Reference.MOD_VANILLA;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getComment() {
        return "Furnace and Brewing stand capabilities.";
    }

    @Override
    public void onInit(Step initStep) {
        if(initStep == Step.INIT) {
            CapabilityConstructorRegistry registry = CommonCapabilities._instance.getCapabilityConstructorRegistry();
            // Worker
            registry.registerTile(TileEntityFurnace.class,
                    new ICapabilityConstructor<IWorker, TileEntityFurnace>() {
                        @Override
                        public Capability<IWorker> getCapability() {
                            return Capabilities.WORKER;
                        }

                        @Nullable
                        @Override
                        public ICapabilityProvider createProvider(TileEntityFurnace host) {
                            return new DefaultCapabilityProvider<>(Capabilities.WORKER, new VanillaFurnaceWorker(host));
                        }
                    });
            registry.registerTile(TileEntityBrewingStand.class,
                    new ICapabilityConstructor<IWorker, TileEntityBrewingStand>() {
                        @Override
                        public Capability<IWorker> getCapability() {
                            return Capabilities.WORKER;
                        }

                        @Override
                        public ICapabilityProvider createProvider(TileEntityBrewingStand host) {
                            return new DefaultCapabilityProvider<>(Capabilities.WORKER, new VanillaBrewingStandWorker(host));
                        }
                    });

            // WorldNameable
            registry.registerInheritableTile(IWorldNameable.class,
                    new ICapabilityConstructor<IWorldNameable, TileEntity>() {
                        @Override
                        public Capability<IWorldNameable> getCapability() {
                            return Capabilities.WORLDNAMEABLE;
                        }

                        @Override
                        public ICapabilityProvider createProvider(TileEntity host) {
                            return new DefaultCapabilityProvider<>(Capabilities.WORLDNAMEABLE, (IWorldNameable) host);
                        }
                    });
            registry.registerInheritableEntity(IWorldNameable.class,
                    new ICapabilityConstructor<IWorldNameable, Entity>() {
                        @Override
                        public Capability<IWorldNameable> getCapability() {
                            return Capabilities.WORLDNAMEABLE;
                        }

                        @Override
                        public ICapabilityProvider createProvider(Entity host) {
                            return new DefaultCapabilityProvider<>(Capabilities.WORLDNAMEABLE, (IWorldNameable) host);
                        }
                    });
            registry.registerInheritableEntity(EntityLiving.class,
                    new ICapabilityConstructor<IWorldNameable, EntityLiving>() {
                        @Override
                        public Capability<IWorldNameable> getCapability() {
                            return Capabilities.WORLDNAMEABLE;
                        }

                        @Override
                        public ICapabilityProvider createProvider(EntityLiving host) {
                            return new DefaultCapabilityProvider<>(Capabilities.WORLDNAMEABLE, new EntityLivingWorldNameable(host));
                        }
                    });
            registry.registerInheritableItem(Item.class,
                    new ICapabilityConstructor<IWorldNameable, ItemStack>() {
                        @Override
                        public Capability<IWorldNameable> getCapability() {
                            return Capabilities.WORLDNAMEABLE;
                        }

                        @Override
                        public ICapabilityProvider createProvider(ItemStack host) {
                            if(host.hasDisplayName()) {
                                return new DefaultCapabilityProvider<>(Capabilities.WORLDNAMEABLE, new ItemStackWorldNameable(host));
                            }
                            return null;
                        }
                    });
        }
    }
}
