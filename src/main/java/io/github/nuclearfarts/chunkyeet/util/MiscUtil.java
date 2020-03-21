package io.github.nuclearfarts.chunkyeet.util;

import java.util.Arrays;

import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.biome.source.BiomeArray;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.chunk.WorldChunk;

public class MiscUtil {

	public static WorldChunk ensureFull(World world, ProtoChunk chunk) {
		if(chunk.getBiomeArray() == null) {
			Biome[] biomes = new Biome[256];
			Arrays.fill(biomes, Biomes.PLAINS);
			chunk.method_22405(new BiomeArray(biomes));
		}
		chunk.setStatus(ChunkStatus.FULL);
		return new WorldChunk(world, chunk);
	}

}