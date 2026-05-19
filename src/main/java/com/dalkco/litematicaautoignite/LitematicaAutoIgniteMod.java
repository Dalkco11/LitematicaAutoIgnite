package com.dalkco.litematicaautoignite;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.malilib.util.LayerRange;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Vec3i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;

public class LitematicaAutoIgniteMod implements ClientModInitializer {
    public static final String MOD_ID = "litematica-auto-ignite";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private int tickCounter = 0;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Litematica Auto Ignite initialized!");

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null || client.gameMode == null) return;

            tickCounter++;
            if (tickCounter < 10) return; // Run every 10 ticks (0.5s)
            tickCounter = 0;

            ItemStack mainHand = client.player.getMainHandItem();
            ItemStack offHand = client.player.getOffhandItem();
            boolean hasFlintAndSteel = mainHand.getItem() == Items.FLINT_AND_STEEL ||
                                     offHand.getItem() == Items.FLINT_AND_STEEL;

            if (!hasFlintAndSteel) return;

            SchematicPlacementManager placementManager = DataManager.getSchematicPlacementManager();
            Collection<SchematicPlacement> placements = placementManager.getAllSchematicsPlacements();
            LayerRange layerRange = DataManager.getRenderLayerRange();

            BlockHitResult bestHitResult = null;
            double minDistanceSq = Double.MAX_VALUE;

            for (SchematicPlacement placement : placements) {
                if (!placement.isEnabled()) continue;

                Map<String, SubRegionPlacement> subRegions = placement.getEnabledRelativeSubRegionPlacements();
                
                for (Map.Entry<String, SubRegionPlacement> entry : subRegions.entrySet()) {
                    String regionName = entry.getKey();
                    SubRegionPlacement subPlacement = entry.getValue();
                    LitematicaBlockStateContainer container = placement.getSchematic().getSubRegionContainer(regionName);
                    
                    if (container == null) continue;

                    Vec3i size = container.getSize();
                    BlockPos subRegionPos = subPlacement.getPos();

                    for (int x = 0; x < size.getX(); x++) {
                        for (int y = 0; y < size.getY(); y++) {
                            for (int z = 0; z < size.getZ(); z++) {
                                BlockState schematicState = container.get(x, y, z);
                                
                                if (schematicState.getBlock() == Blocks.NETHER_PORTAL) {
                                    BlockPos relPos = subRegionPos.offset(x, y, z);
                                    BlockPos transformedPos = PositionUtils.getTransformedBlockPos(relPos, placement.getMirror(), placement.getRotation());
                                    BlockPos worldPos = placement.getOrigin().offset(transformedPos);

                                    if (layerRange != null && !layerRange.isPositionWithinRange(worldPos)) {
                                        continue;
                                    }

                                    Vec3 targetCenter = new Vec3(worldPos.getX() + 0.5, worldPos.getY() + 0.5, worldPos.getZ() + 0.5);
                                    Vec3 eyePos = client.player.getEyePosition();
                                    double distanceSq = eyePos.distanceToSqr(targetCenter);
                                    
                                    double maxReachSq = 5.0 * 5.0; 

                                    if (distanceSq > maxReachSq) {
                                        continue;
                                    }

                                    BlockState worldState = client.level.getBlockState(worldPos);

                                    if (worldState.isAir()) {
                                        int scanY = y;
                                        while (scanY + 1 < size.getY()) {
                                            BlockState s = container.get(x, scanY + 1, z);
                                            if (s.getBlock() != Blocks.NETHER_PORTAL) break;
                                            scanY++;
                                        }
                                        BlockPos highRel = subRegionPos.offset(x, scanY, z);
                                        BlockPos highTrans = PositionUtils.getTransformedBlockPos(highRel, placement.getMirror(), placement.getRotation());
                                        BlockPos highWorld = placement.getOrigin().offset(highTrans);
                                        if (layerRange != null && !layerRange.isPositionWithinRange(highWorld)) continue;

                                        scanY = y;
                                        while (scanY - 1 >= 0) {
                                            BlockState s = container.get(x, scanY - 1, z);
                                            if (s.getBlock() != Blocks.NETHER_PORTAL) break;
                                            scanY--;
                                        }
                                        BlockPos lowRel = subRegionPos.offset(x, scanY, z);
                                        BlockPos lowTrans = PositionUtils.getTransformedBlockPos(lowRel, placement.getMirror(), placement.getRotation());
                                        BlockPos lowWorld = placement.getOrigin().offset(lowTrans);
                                        if (layerRange != null && !layerRange.isPositionWithinRange(lowWorld)) continue;

                                        for (Direction dir : Direction.values()) {
                                            BlockPos neighborPos = worldPos.relative(dir);
                                            BlockState neighborState = client.level.getBlockState(neighborPos);

                                            if (neighborState.is(Blocks.OBSIDIAN)) {
                                                if (layerRange != null && !layerRange.isPositionWithinRange(neighborPos)) continue;

                                                Direction face = dir.getOpposite();
                                                Vec3 hitPos = new Vec3(
                                                    neighborPos.getX() + 0.5 + face.getStepX() * 0.5,
                                                    neighborPos.getY() + 0.5 + face.getStepY() * 0.5,
                                                    neighborPos.getZ() + 0.5 + face.getStepZ() * 0.5
                                                );

                                                double distSq = eyePos.distanceToSqr(hitPos);

                                                if (distSq > maxReachSq) continue;

                                                if (distSq < minDistanceSq) {
                                                    minDistanceSq = distSq;
                                                    bestHitResult = new BlockHitResult(hitPos, face, neighborPos, false);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            if (bestHitResult != null) {
                InteractionHand hand = mainHand.getItem() == Items.FLINT_AND_STEEL ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
                client.gameMode.useItemOn(client.player, hand, bestHitResult);
                client.player.swing(hand);
            }
        });
    }
}
