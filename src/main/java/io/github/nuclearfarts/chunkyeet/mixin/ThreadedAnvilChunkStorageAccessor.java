package io.github.nuclearfarts.chunkyeet.mixin;

import java.io.IOException;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Packet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.thread.ThreadExecutor;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.poi.PointOfInterestStorage;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

@Mixin(ThreadedAnvilChunkStorage.class)
public interface ThreadedAnvilChunkStorageAccessor {
	@Invoker
	CompoundTag invokeGetUpdatedChunkTag(ChunkPos pos) throws IOException;
	@Invoker
	boolean invokeSave(Chunk chunk);
	@Accessor
	PointOfInterestStorage getPointOfInterestStorage();
	@Accessor
	void setCurrentChunkHolders(Long2ObjectLinkedOpenHashMap<ChunkHolder> to);
	@Accessor
	void setChunkHolders(Long2ObjectLinkedOpenHashMap<ChunkHolder> to);
	@Accessor
	void setMainThreadExecutor(ThreadExecutor<Runnable> to);
	@Accessor
	Int2ObjectMap<ThreadedAnvilChunkStorage.EntityTracker> getEntityTrackers();
	@Invoker
	void invokeSendChunkDataPackets(ServerPlayerEntity player, Packet<?>[] packets, WorldChunk chunk);
}
