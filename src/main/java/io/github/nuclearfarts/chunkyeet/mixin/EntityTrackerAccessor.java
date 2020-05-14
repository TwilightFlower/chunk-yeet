package io.github.nuclearfarts.chunkyeet.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.entity.Entity;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;

@Mixin(ThreadedAnvilChunkStorage.EntityTracker.class)
public interface EntityTrackerAccessor {
	@Accessor
	Entity getEntity();
}
