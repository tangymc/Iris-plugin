/*
 * Iris is a World Generator for Minecraft Bukkit Servers
 * Copyright (c) 2021 Arcane Arts (Volmit Software)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.volmit.iris.engine.platform;

import com.volmit.iris.Iris;
import com.volmit.iris.core.project.loader.IrisData;
import com.volmit.iris.engine.IrisEngine;
import com.volmit.iris.engine.data.chunk.TerrainChunk;
import com.volmit.iris.engine.framework.Engine;
import com.volmit.iris.engine.framework.EngineTarget;
import com.volmit.iris.engine.framework.WrongEngineBroException;
import com.volmit.iris.engine.object.common.IrisWorld;
import com.volmit.iris.engine.object.dimensional.IrisDimension;
import com.volmit.iris.util.collection.KList;
import com.volmit.iris.util.hunk.Hunk;
import com.volmit.iris.util.io.ReactiveFolder;
import com.volmit.iris.util.scheduling.ChronoLatch;
import com.volmit.iris.util.scheduling.J;
import com.volmit.iris.util.scheduling.Looper;
import com.volmit.iris.util.scheduling.PrecisionStopwatch;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.data.BlockData;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;

@EqualsAndHashCode(callSuper = true)
@Data
public class BukkitChunkGenerator extends ChunkGenerator implements PlatformChunkGenerator {
    private static final int LOAD_LOCKS = 1_000_000;
    private final Semaphore loadLock;
    private final Engine engine;
    private final IrisWorld world;
    private final File dataLocation;
    private final String dimensionKey;
    private final ReactiveFolder folder;
    private final KList<BlockPopulator> populators;
    private final ChronoLatch hotloadChecker;
    private final Looper hotloader;
    private final boolean studio;

    public BukkitChunkGenerator(IrisWorld world, boolean studio, File dataLocation, String dimensionKey) {
        populators = new KList<>();
        loadLock = new Semaphore(LOAD_LOCKS);
        this.world = world;
        this.hotloadChecker = new ChronoLatch(1000, false);
        this.studio = studio;
        this.dataLocation = dataLocation;
        this.dimensionKey = dimensionKey;
        this.folder = new ReactiveFolder(dataLocation, (_a, _b, _c) -> hotload());
        IrisData data = IrisData.get(dataLocation);
        IrisDimension dimension = data.getDimensionLoader().load(dimensionKey);
        this.engine = new IrisEngine(new EngineTarget(world, dimension, data), studio);
        populators.add((BlockPopulator) engine);
        this.hotloader = new Looper() {
            @Override
            protected long loop() {
                if (hotloadChecker.flip()) {
                    folder.check();
                }

                return 250;
            }
        };
        hotloader.setPriority(Thread.MIN_PRIORITY);
        hotloader.start();
        hotloader.setName(getTarget().getWorld().name() + " Hotloader");
    }

    @Override
    public boolean isHeadless() {
        return false;
    }

    @Override
    public void close() {
        withExclusiveControl(() -> {
            hotloader.interrupt();
            getEngine().close();
        });
    }

    @Override
    public boolean isStudio() {
        return studio;
    }

    @Override
    public void hotload() {
        withExclusiveControl(() -> getEngine().hotload());
    }

    public void withExclusiveControl(Runnable r)
    {
        J.a(() -> {
            try {
                loadLock.acquire(LOAD_LOCKS);
                r.run();
                loadLock.release(LOAD_LOCKS);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public @NotNull ChunkData generateChunkData(@NotNull World world, @NotNull Random ignored, int x, int z, @NotNull BiomeGrid biome) {
        try {
            loadLock.acquire();
            PrecisionStopwatch ps = PrecisionStopwatch.start();
            TerrainChunk tc = TerrainChunk.create(world, biome);
            Hunk<BlockData> blocks = Hunk.view((ChunkData) tc);
            Hunk<Biome> biomes = Hunk.view((BiomeGrid) tc);
            this.world.bind(world);
            getEngine().generate(x * 16, z * 16, blocks, biomes, true);
            ChunkData c = tc.getRaw();
            Iris.debug("Generated " + x + " " + z);
            loadLock.release();
            return c;
        }

        catch (Throwable e) {
            loadLock.release();
            Iris.error("======================================");
            e.printStackTrace();
            Iris.reportErrorChunk(x, z, e, "CHUNK");
            Iris.error("======================================");

            ChunkData d = Bukkit.createChunkData(world);

            for (int i = 0; i < 16; i++) {
                for (int j = 0; j < 16; j++) {
                    d.setBlock(i, 0, j, Material.RED_GLAZED_TERRACOTTA.createBlockData());
                }
            }

            return d;
        }
    }

    @NotNull
    @Override
    public List<BlockPopulator> getDefaultPopulators(@NotNull World world) {
        return populators;
    }

    @Override
    public boolean isParallelCapable() {
        return true;
    }

    @Override
    public boolean shouldGenerateCaves() {
        return false;
    }

    @Override
    public boolean shouldGenerateDecorations() {
        return false;
    }

    @Override
    public boolean shouldGenerateMobs() {
        return false;
    }

    @Override
    public boolean shouldGenerateStructures() {
        return false;
    }
}