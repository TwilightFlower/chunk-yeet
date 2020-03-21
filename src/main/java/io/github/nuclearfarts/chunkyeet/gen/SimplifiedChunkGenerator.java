package io.github.nuclearfarts.chunkyeet.gen;

import net.minecraft.world.chunk.ProtoChunk;

@FunctionalInterface
public interface SimplifiedChunkGenerator {
	void generate(ProtoChunk chunk);
}
