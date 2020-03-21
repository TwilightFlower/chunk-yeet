package io.github.nuclearfarts.chunkyeet.mixin;

import java.util.concurrent.Executor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.WorldSaveHandler;
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.DimensionType;

import io.github.nuclearfarts.chunkyeet.YeetChunkManager;

@Mixin(ServerWorld.class)
public class ServerWorldMixin {
	// lambda injection: top of constructor.
	@Inject(at = @At("HEAD"), cancellable = true, method = "method_14168(Lnet/minecraft/world/WorldSaveHandler;Ljava/util/concurrent/Executor;Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/WorldGenerationProgressListener;Lnet/minecraft/world/World;Lnet/minecraft/world/dimension/Dimension;)Lnet/minecraft/world/chunk/ChunkManager;")
	private static void injectChunkManager(WorldSaveHandler worldSaveHandler, Executor workerExecutor, MinecraftServer server,
			WorldGenerationProgressListener listener, World world, Dimension dimension,
			CallbackInfoReturnable<ChunkManager> callback) {
		callback.setReturnValue(new YeetChunkManager((ServerWorld) world, worldSaveHandler.getWorldDir(), worldSaveHandler.getDataFixer(),
				worldSaveHandler.getStructureManager(), workerExecutor, dimension.createChunkGenerator(),
				server.getPlayerManager().getViewDistance(), listener,
				() -> server.getWorld(DimensionType.OVERWORLD).getPersistentStateManager()));
	}
}
