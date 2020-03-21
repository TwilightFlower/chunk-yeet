package io.github.nuclearfarts.chunkyeet.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.util.math.ChunkPos;

@Mixin(ServerLightingProvider.class)
public interface ServerLightingProviderAccessor {
	@Invoker
	void invokeUpdateChunkStatus(ChunkPos pos);
}
