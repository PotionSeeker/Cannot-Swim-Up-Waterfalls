package com.stebars.cantswimupwaterfalls.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.tags.TagKey;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Component;

@Mixin(Entity.class)
public abstract class EntityMixin extends net.minecraftforge.common.capabilities.CapabilityProvider<Entity> {

    @Shadow public Level level;
    @Shadow private AABB bb;
    @Shadow private Vec3 deltaMovement = Vec3.ZERO;
    @Shadow protected Object2DoubleMap<TagKey<Fluid>> fluidHeight = new Object2DoubleArrayMap<>(2);

    protected EntityMixin(Class<Entity> baseClass) {
        super(baseClass);
    }

    @Shadow
    public AABB getBoundingBox() {
        return this.bb;
    }

    @Shadow
    public boolean isPushedByFluid() {
        return true;
    }

    @Shadow
    public Vec3 getDeltaMovement() {
        return this.deltaMovement;
    }

    @Shadow
    public void setDeltaMovement(Vec3 deltaMovement) {
        this.deltaMovement = deltaMovement;
    }

    @Shadow
    public Component getName() {
        return null;
    }

    @Overwrite
    public boolean updateFluidHeightAndDoFluidPushing(TagKey<Fluid> fluidTag, double motionScale) {
        AABB aabb = this.getBoundingBox().deflate(0.001D);
        int minX = Mth.floor(aabb.minX);
        int maxX = Mth.ceil(aabb.maxX);
        int minY = Mth.floor(aabb.minY);
        int maxY = Mth.ceil(aabb.maxY);
        int minZ = Mth.floor(aabb.minZ);
        int maxZ = Mth.ceil(aabb.maxZ);
        if (!this.level.hasChunksAt(minX, minY, minZ, maxX, maxY, maxZ)) {
            return false;
        }

        double fluidHeight = 0.0D;
        boolean isPushed = this.isPushedByFluid();
        boolean hasFluid = false;
        Vec3 fluidPush = Vec3.ZERO;
        int fluidBlocks = 0;
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int x = minX; x < maxX; ++x) {
            for (int y = minY; y < maxY; ++y) {
                for (int z = minZ; z < maxZ; ++z) {
                    mutablePos.set(x, y, z);
                    FluidState fluidState = this.level.getFluidState(mutablePos);
                    if (fluidState.is(fluidTag)) {
                        double height = (double)((float)y + fluidState.getHeight(this.level, mutablePos));
                        if (height >= aabb.minY) {
                            hasFluid = true;
                            fluidHeight = Math.max(height - aabb.minY, fluidHeight);
                            if (isPushed) {
                                Vec3 flow = fluidState.getFlow(this.level, mutablePos);
                                if (fluidHeight < 0.4D) {
                                    flow = flow.scale(fluidHeight);
                                }
                                // BEGIN added by mixin
                                if (fluidState.hasProperty(BlockStateProperties.FALLING) &&
                                        fluidState.getValue(BlockStateProperties.FALLING)) {
                                    flow = flow.add(0, -1.51, 0);
                                }
                                // END added by mixin
                                fluidPush = fluidPush.add(flow);
                                ++fluidBlocks;
                            }
                        }
                    }
                }
            }
        }

        if (fluidPush.length() > 0.0D) {
            if (fluidBlocks > 0) {
                fluidPush = fluidPush.scale(1.0D / (double)fluidBlocks);
            }

            if (!(((Entity) (Object) this) instanceof Player)) {
                fluidPush = fluidPush.normalize();
            }

            Vec3 currentMotion = this.getDeltaMovement();
            fluidPush = fluidPush.scale(motionScale * 1.0D);
            double minMotion = 0.003D;
            if (Math.abs(currentMotion.x) < minMotion && Math.abs(currentMotion.z) < minMotion && fluidPush.length() < 0.0045D) {
                fluidPush = fluidPush.normalize().scale(0.0045D);
            }

            this.setDeltaMovement(this.getDeltaMovement().add(fluidPush));
        }

        this.fluidHeight.put(fluidTag, fluidHeight);
        return hasFluid;
    }
}