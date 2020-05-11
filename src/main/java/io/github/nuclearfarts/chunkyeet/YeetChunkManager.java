package io.github.nuclearfarts.chunkyeet;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Either;

import net.minecraft.entity.Entity;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.BlockView;
import net.minecraft.world.LightType;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.chunk.ChunkGenerator;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import io.github.nuclearfarts.chunkyeet.mixin.ServerChunkManagerAccessor;
import io.github.nuclearfarts.chunkyeet.util.UnimplementedThreadExecutor;

public class YeetChunkManager extends ServerChunkManager {

	protected final ReadWriteLock chunkCacheLock = new ReentrantReadWriteLock();
	protected final long[] chunkPosCache = new long[4];
	protected final Chunk[] chunkCache = new Chunk[4];
	
	protected final YeetChunkStorage storage;

	public YeetChunkManager(ServerWorld serverWorld, File file, DataFixer dataFixer, StructureManager structureManager,
			Executor workerExecutor, ChunkGenerator<?> chunkGenerator, int i,
			WorldGenerationProgressListener worldGenerationProgressListener,
			Supplier<PersistentStateManager> supplier) {
		super(serverWorld, file, dataFixer, structureManager, workerExecutor, chunkGenerator, i,
				worldGenerationProgressListener, supplier);
		storage = new YeetChunkStorage(serverWorld, file, dataFixer, structureManager, workerExecutor, new UnimplementedThreadExecutor(""), this, chunkGenerator, worldGenerationProgressListener, supplier, i);
		((ServerChunkManagerAccessor) this).setThreadedAnvilChunkStorage(null);
	}

	protected void cacheNewChunk(long pos, Chunk chunk) {
		chunkCacheLock.writeLock().lock();
		for (int i = 3; i > 0; i--) {
			chunkPosCache[i] = chunkPosCache[i - 1];
			chunkCache[i] = chunkCache[i - 1];
		}
		chunkPosCache[0] = pos;
		chunkCache[0] = chunk;
		chunkCacheLock.writeLock().unlock();
	}

	protected Chunk getChunkFromCache(long pos) {
		chunkCacheLock.readLock().lock();
		for (int i = 0; i < 4; i++) {
			if (chunkPosCache[i] == pos) {
				Chunk chunk = chunkCache[i]; // prevent a race condition where chunk cache could modify between unlock
												// and return.
				chunkCacheLock.readLock().unlock();
				return chunk;
			}
		}
		chunkCacheLock.readLock().unlock();
		return null;
	}

	@Override
	public Chunk getChunk(int x, int z, ChunkStatus leastStatus, boolean create) {
		Profiler profiler = getWorld().getProfiler();
		profiler.visit("getChunk");
		long pos = ChunkPos.toLong(x, z);
		Chunk chunk;
		if ((chunk = getChunkFromCache(pos)) == null) {
			profiler.visit("getChunkCacheMiss");
			chunk = getNonCachedChunk(x, z, create);
		}
		return chunk;
	}

	//getChunkForLight would be a better name
	@Override
	public BlockView getChunk(int x, int z) {
		return (BlockView) storage.getLoadedChunk(x, z);
	}
	
	@Override
	public WorldChunk getWorldChunk(int x, int z) {
		return (WorldChunk) getChunk(x, z, null, true);
	}
	
	protected Chunk getNonCachedChunk(int x, int z, boolean create) {
		WorldChunk chunk = storage.getLoadedChunk(x, z);
		if (create && chunk == null) {
			chunk = storage.getChunk(x, z);
		}
		return chunk;
	}
	
	@Override
	public void applyViewDistance(int dist) {
		storage.setViewDistance(dist);
	}
	
	@Override
	public void updateCameraPosition(ServerPlayerEntity player) {
		storage.updateCameraPosition(player);
	}
	
	@Override
	public void tick(BooleanSupplier shouldKeepTicking) {
		storage.tick();
	}
	
	@Override
	public void loadEntity(Entity e) {
		storage.loadEntity(e);
	}
	
	@Override
	public void unloadEntity(Entity e) {
		storage.unloadEntity(e);
	}
	
	@Override
	public void markForUpdate(BlockPos pos) {
		ChunkPos cPos = new ChunkPos(pos);
		storage.blockUpdate(cPos.x, cPos.z, pos);
	}
	
	@Override
	public boolean isChunkLoaded(int x, int z) {
		return storage.isChunkLoaded(x, z);
	}
	
	@Override
	public int getTotalChunksLoadedCount() {
		//System.out.println(storage.getTotalChunksLoadedCount());
		return storage.getTotalChunksLoadedCount();
	}
	
	@Override
	public <T> void addTicket(ChunkTicketType<T> chunkTicketType, ChunkPos chunkPos, int i, T object) {
		if(chunkTicketType == ChunkTicketType.START) {
			storage.setSpawnPos(chunkPos);
		}
	}
	
	@Override
	@Environment(EnvType.CLIENT)
	public CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> getChunkFutureSyncOnMainThread(int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create) {
		return CompletableFuture.completedFuture(Either.left(getChunk(chunkX, chunkZ, leastStatus, create)));
	}
	
	@Override
	public void onLightUpdate(LightType type, ChunkSectionPos chunkSectionPos) {
		//FIXME
	}
	
	@Override
	public boolean executeQueuedTasks() {
		//super.executeQueuedTasks();
		return true;
	}
	
	public void save(boolean flush) {
	      storage.save(flush);
	}
}
