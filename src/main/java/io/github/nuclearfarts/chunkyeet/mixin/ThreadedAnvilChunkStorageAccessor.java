package io.github.nuclearfarts.chunkyeet.mixin;

import java.io.IOException;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.poi.PointOfInterestStorage;

@Mixin(ThreadedAnvilChunkStorage.class)
public interface ThreadedAnvilChunkStorageAccessor {
	@Invoker
	CompoundTag invokeGetUpdatedChunkTag(ChunkPos pos) throws IOException;
	@Accessor
	PointOfInterestStorage getPointOfInterestStorage();
}
