package io.github.nuclearfarts.chunkyeet.util;

import java.util.Arrays;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.biome.source.BiomeArray;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.chunk.ReadOnlyChunk;
import net.minecraft.world.chunk.WorldChunk;

public class MiscUtil {

	public static WorldChunk ensureFull(World world, ProtoChunk chunk) {
		if(chunk instanceof ReadOnlyChunk) return ((ReadOnlyChunk) chunk).getWrappedChunk();
		if(chunk.getBiomeArray() == null) {
			Biome[] biomes = new Biome[256];
			Arrays.fill(biomes, Biomes.PLAINS);
			chunk.method_22405(new BiomeArray(biomes));
		}
		chunk.setStatus(ChunkStatus.FULL);
		return new WorldChunk(world, chunk);
	}
	
	public static int getWeirdDistance(int chunkX, int chunkZ, int x, int z) {
		return Math.max(Math.abs(chunkX - x), Math.abs(chunkZ - z));
	}
}
