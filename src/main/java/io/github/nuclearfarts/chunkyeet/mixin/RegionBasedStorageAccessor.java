package io.github.nuclearfarts.chunkyeet.mixin;

import java.io.File;
import java.io.IOException;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.storage.RegionBasedStorage;

@Mixin(RegionBasedStorage.class)
public interface RegionBasedStorageAccessor {
	@Invoker
	void invokeWrite(ChunkPos pos, CompoundTag tag) throws IOException;
	@Invoker("<init>")
	static RegionBasedStorage create(File file) { return null; }
}
