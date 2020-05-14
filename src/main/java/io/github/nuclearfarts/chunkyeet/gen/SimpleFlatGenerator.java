package io.github.nuclearfarts.chunkyeet.gen;

import java.util.EnumSet;
import java.util.Set;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.ProtoChunk;

public class SimpleFlatGenerator implements SimplifiedChunkGenerator {

	private static final BlockState STATE = Blocks.DIRT.getDefaultState();
	private static final Set<Heightmap.Type> HEIGHTMAP_TYPES = EnumSet.of(Heightmap.Type.MOTION_BLOCKING, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, Heightmap.Type.OCEAN_FLOOR, Heightmap.Type.WORLD_SURFACE);
	
	@Override
	public void generate(ProtoChunk chunk) {
		BlockPos.Mutable pos = new BlockPos.Mutable();
		for(int x = 0; x < 16; x++) {
			for(int y = 0; y < 32; y++) {
				for(int z = 0; z < 16; z++) {
					chunk.setBlockState(pos.set(x, y, z), STATE, false);
				}
			}
		}
		Heightmap.populateHeightmaps(chunk, HEIGHTMAP_TYPES);
	}

}
