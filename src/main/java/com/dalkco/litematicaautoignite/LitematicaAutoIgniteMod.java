package com.example.litematicaautoignite;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.malilib.util.LayerRange;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
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
            if (client.player == null || client.world == null || client.interactionManager == null) return;

            tickCounter++;
            if (tickCounter < 10) return; // Run every 10 ticks (0.5s)
            tickCounter = 0;

            // Check if holding Flint and Steel
            ItemStack mainHand = client.player.getMainHandStack();
            ItemStack offHand = client.player.getOffHandStack();
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

                    // Iterate through the schematic container
                    for (int x = 0; x < size.getX(); x++) {
                        for (int y = 0; y < size.getY(); y++) {
                            for (int z = 0; z < size.getZ(); z++) {
                                BlockState schematicState = container.get(x, y, z);
                                
                                // We are looking for Nether Portal blocks in the schematic
                                if (schematicState.getBlock() == Blocks.NETHER_PORTAL) {
                                    // Calculate world position
                                    BlockPos relPos = subRegionPos.add(x, y, z);
                                    // Transform using PositionUtils (Mirror then Rotation)
                                    BlockPos transformedPos = PositionUtils.getTransformedBlockPos(relPos, placement.getMirror(), placement.getRotation());
                                    BlockPos worldPos = placement.getOrigin().add(transformedPos);

                                    // Check if the current portal block is visible in the layer range
                                    if (layerRange != null && !layerRange.isPositionWithinRange(worldPos)) {
                                        continue;
                                    }

                                    // Check distance
                                    // Using center of the block for distance check
                                    Vec3d targetCenter = new Vec3d(worldPos.getX() + 0.5, worldPos.getY() + 0.5, worldPos.getZ() + 0.5);
                                    Vec3d eyePos = client.player.getEyePos();
                                    double distanceSq = eyePos.squaredDistanceTo(targetCenter);
                                    
                                    // Use standard reach distance (4.5 blocks = 20.25 sq, 5.0 blocks = 25.0 sq)
                                    // Using 5.0 blocks as a safe upper limit for most servers
                                    double maxReachSq = 5.0 * 5.0; 
                                    if (client.interactionManager != null) {
                                         // Try to get actual reach distance if possible (not exposed directly in API, using safe fallback)
                                         // Or just use a slightly generous 5.0
                                    }

                                    if (distanceSq > maxReachSq) {
                                        continue; // Skip blocks that are too far
                                    }

                                    // Check the world block
                                    BlockState worldState = client.world.getBlockState(worldPos);

                                    // Only try to ignite if the space is AIR (don't click if fire or other blocks exist)
                                    if (worldState.isAir()) {
                                        // 1. Verify "Whole Portal Column" visibility
                                        // Scan UP in schematic to find top Y
                                        int scanY = y;
                                        while (scanY + 1 < size.getY()) {
                                            BlockState s = container.get(x, scanY + 1, z);
                                            if (s.getBlock() != Blocks.NETHER_PORTAL) break;
                                            scanY++;
                                        }
                                        // Check top visibility
                                        BlockPos highRel = subRegionPos.add(x, scanY, z);
                                        BlockPos highTrans = PositionUtils.getTransformedBlockPos(highRel, placement.getMirror(), placement.getRotation());
                                        BlockPos highWorld = placement.getOrigin().add(highTrans);
                                        if (layerRange != null && !layerRange.isPositionWithinRange(highWorld)) continue;

                                        // Scan DOWN in schematic to find bottom Y
                                        scanY = y;
                                        while (scanY - 1 >= 0) {
                                            BlockState s = container.get(x, scanY - 1, z);
                                            if (s.getBlock() != Blocks.NETHER_PORTAL) break;
                                            scanY--;
                                        }
                                        // Check bottom visibility
                                        BlockPos lowRel = subRegionPos.add(x, scanY, z);
                                        BlockPos lowTrans = PositionUtils.getTransformedBlockPos(lowRel, placement.getMirror(), placement.getRotation());
                                        BlockPos lowWorld = placement.getOrigin().add(lowTrans);
                                        if (layerRange != null && !layerRange.isPositionWithinRange(lowWorld)) continue;

                                        // 2. Iterate all adjacent blocks to find a valid Obsidian frame
                                        for (Direction dir : Direction.values()) {
                                            BlockPos neighborPos = worldPos.offset(dir);
                                            BlockState neighborState = client.world.getBlockState(neighborPos);

                                            if (neighborState.isOf(Blocks.OBSIDIAN)) {
                                                // Check neighbor visibility
                                                if (layerRange != null && !layerRange.isPositionWithinRange(neighborPos)) continue;

                                                // Calculate hit pos on the face facing the portal air block
                                                Direction face = dir.getOpposite();
                                                Vec3d hitPos = new Vec3d(
                                                    neighborPos.getX() + 0.5 + face.getOffsetX() * 0.5,
                                                    neighborPos.getY() + 0.5 + face.getOffsetY() * 0.5,
                                                    neighborPos.getZ() + 0.5 + face.getOffsetZ() * 0.5
                                                );

                                                double distSq = eyePos.squaredDistanceTo(hitPos);

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
            
            // If we found a valid target, interact with it
            if (bestHitResult != null) {
                Hand hand = mainHand.getItem() == Items.FLINT_AND_STEEL ? Hand.MAIN_HAND : Hand.OFF_HAND;
                
                client.interactionManager.interactBlock(client.player, hand, bestHitResult);
                client.player.swingHand(hand);
            }
        });
    }
}
