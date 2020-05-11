package io.github.nuclearfarts.chunkyeet.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.storage.StorageIoWorker;
import net.minecraft.world.storage.VersionedChunkStorage;

@Mixin(VersionedChunkStorage.class)
public interface VersionedChunkStorageAccessor {
	@Accessor
	StorageIoWorker getWorker();
}
