package io.github.nuclearfarts.chunkyeet.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.world.ThreadedAnvilChunkStorage;

@Mixin(ThreadedAnvilChunkStorage.class)
public class ThreadedAnvilChunkStorageMixin {
	
	@Inject(at = @At("HEAD"), method = "handlePlayerAddedOrRemoved", cancellable = true)
	private void cancelIt(CallbackInfo info) {
		info.cancel();
	}
}
