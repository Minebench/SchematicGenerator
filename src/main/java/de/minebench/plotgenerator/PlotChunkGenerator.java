package de.minebench.plotgenerator;

/*
 * Copyright 2016 Max Lee (https://github.com/Phoenix616/)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Mozilla Public License as published by
 * the Mozilla Foundation, version 2.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Mozilla Public License v2.0 for more details.
 * 
 * You should have received a copy of the Mozilla Public License v2.0
 * along with this program. If not, see <http://mozilla.org/MPL/2.0/>.
 */

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.blocks.BaseBlock;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

import java.util.Random;

public class PlotChunkGenerator extends ChunkGenerator {

    private final PlotGenerator plugin;
    private final PlotGeneratorConfig config;

    public PlotChunkGenerator() {
        plugin = PlotGenerator.getPlugin(PlotGenerator.class);
        config = null;
    }

    public PlotChunkGenerator(PlotGenerator plugin, String id) {
        this.plugin = plugin;
        config = PlotGeneratorConfig.fromId(plugin, id);
    }

    @Override
    public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
        ChunkData data = createChunkData(world);
        PlotGeneratorConfig config = getConfig(world);
        if (config != null && config.getSchematic() != null && !BlockVector.ZERO.equals(config.getSchematic().getSize())) {
            CuboidClipboard schematic = config.getSchematic();
            BlockVector center = config.getCenter();
            int width = schematic.getWidth() - config.getOverlap();
            int startX = (x * 16 + center.getBlockX()) % width;
            while (startX < 0) {
                startX = schematic.getWidth() + startX;
            }
            int length = schematic.getLength() - config.getOverlap();
            int startZ = (z * 16 + center.getBlockZ()) % length;
            while (startZ < 0) {
                startZ = schematic.getLength() + startZ;
            }

            int regionX = -1;
            int regionZ = -1;
            for (int chunkX = 0; chunkX < 16; chunkX++) {
                int schemX = (startX + chunkX) % schematic.getWidth();
                if (schemX == 0) {
                    regionX = chunkX;
                }
                for (int chunkZ = 0; chunkZ < 16; chunkZ++) {
                    int schemZ = (startZ + chunkZ) % schematic.getLength();
                    if (schemZ == 0) {
                        regionZ = schemZ;
                    }
                    for (int chunkY = 0; chunkY < schematic.getHeight(); chunkY++) {
                        BaseBlock block = schematic.getBlock(new BlockVector(schemX, chunkY, schemZ));
                        data.setBlock(chunkX, chunkY, chunkZ, block.getId(), (byte) block.getData());
                    }
                }
            }

            if (plugin.getWorldGuard() != null && config.getRegionName() != null && regionX != -1 && regionZ != -1) {
                BlockVector minPoint = new BlockVector(
                        x * 16 + regionX + config.getRegionInset(),
                        config.getRegionMinY(),
                        z * 16 + regionZ + config.getRegionInset()
                );
                BlockVector maxPoint = new BlockVector(
                        minPoint.getBlockX() + schematic.getWidth() - 2 * config.getRegionInset(),
                        config.getRegionMaxY(),
                        minPoint.getBlockZ() + schematic.getLength() - 2 * config.getRegionInset()
                );
                plugin.registerRegionIntent(new RegionIntent(world, config.getRegionName(), minPoint, maxPoint));
            }
        }
        return data;
    }

    @Override
    public Location getFixedSpawnLocation(World world, Random random) {
        if (getConfig(world) == null) {
            return null;
        }
        Location loc = new Location(world, getConfig(world).getCenter().getX(), getConfig(world).getCenter().getY(), getConfig(world).getCenter().getZ());
        if (!loc.getChunk().isLoaded()) {
            loc.getChunk().load();
        }
        loc.setY(world.getHighestBlockYAt(loc));

        return loc;
    }

    public PlotGeneratorConfig getConfig(World world) {
        if (config == null) {
            return plugin.getGeneratorConfig(world);
        }
        return config;
    }
}
