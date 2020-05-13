package io.github.nuclearfarts.chunkyeet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Either;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkRenderDistanceCenterS2CPacket;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.structure.StructureManager;
import net.minecraft.util.TypeFilterableList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.thread.ThreadExecutor;
import net.minecraft.world.ChunkSerializer;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkProvider;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.chunk.UpgradeData;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import io.github.nuclearfarts.chunkyeet.gen.SimpleFlatGenerator;
import io.github.nuclearfarts.chunkyeet.gen.SimplifiedChunkGenerator;
import io.github.nuclearfarts.chunkyeet.loadmap.LoadManagerTickResult;
import io.github.nuclearfarts.chunkyeet.loadmap.LoadSource;
import io.github.nuclearfarts.chunkyeet.loadmap.LoadManager;
import io.github.nuclearfarts.chunkyeet.mixin.ServerLightingProviderAccessor;
import io.github.nuclearfarts.chunkyeet.mixin.ThreadedAnvilChunkStorageAccessor;
import io.github.nuclearfarts.chunkyeet.util.MiscUtil;
import it.unimi.dsi.fastutil.longs.LongIterator;

public class YeetChunkStorage extends ThreadedAnvilChunkStorage {
	protected static final LoadSource SPAWNCHUNKS_LOAD_SOURCE = new LoadSource(10);

	private final Map<Long, CompletableFuture<ChunkMapEntry>> chunks = new ConcurrentHashMap<>();
	private final Map<ServerPlayerEntity, LoadSource> playerLoadSources = new HashMap<>();
	private final Queue<ChunkMapEntry> newChunks = new ConcurrentLinkedQueue<>();
	private final Lock chunkIOLock = new ReentrantLock();
	// private final RegionBasedStorage diskStorage;

	protected int loaded;
	protected int viewDistance;
	protected final ServerWorld world;
	protected final StructureManager structureManager;
	protected final WorldGenerationProgressListener progressListener;
	protected final LoadManager loadManager = new LoadManager();
	protected ChunkPos spawnPos;

	protected final SimplifiedChunkGenerator generator = new SimpleFlatGenerator();

	public YeetChunkStorage(ServerWorld world, File file, DataFixer dataFixer, StructureManager structureManager,
			Executor workerExecutor, ThreadExecutor<Runnable> mainThreadExecutor, ChunkProvider chunkProvider,
			ChunkGenerator<?> chunkGenerator, WorldGenerationProgressListener worldGenerationProgressListener,
			Supplier<PersistentStateManager> supplier, int i) {
		super(world, file, dataFixer, structureManager, workerExecutor, mainThreadExecutor, chunkProvider,
				chunkGenerator, worldGenerationProgressListener, supplier, i);
		viewDistance = i;
		this.world = world;
		this.structureManager = structureManager;
		this.progressListener = worldGenerationProgressListener;
		/*
		 * try { ((VersionedChunkStorageAccessor) this).getWorker().close(); } catch
		 * (IOException e) { e.printStackTrace(); } diskStorage =
		 * RegionBasedStorageAccessor.create(new
		 * File(world.getDimension().getType().getSaveDirectory(file), "region"));
		 */
		((ThreadedAnvilChunkStorageAccessor) this).setChunkHolders(null);
		((ThreadedAnvilChunkStorageAccessor) this).setCurrentChunkHolders(null);
		((ThreadedAnvilChunkStorageAccessor) this).setMainThreadExecutor(null);
	}

	@Override
	@Environment(EnvType.CLIENT)
	public String method_23272(ChunkPos chunkPos) {
		return "";
	}

	protected void save(boolean flush) {
		System.out.println("save");
		chunks.values().stream().filter(CompletableFuture::isDone).map(cf -> cf.join().chunk).forEach(this::writeChunk);
	}

	private void writeChunk(Chunk chunk) {
		/*
		 * ChunkPos pos = chunk.getPos(); chunkIOLock.lock();
		 * ((ThreadedAnvilChunkStorageAccessor)
		 * this).getPointOfInterestStorage().method_20436(pos); if (chunk.needsSaving())
		 * { chunk.setLastSaveTime(this.world.getTime()); chunk.setShouldSave(false);
		 * try { ((ThreadedAnvilChunkStorageAccessor) this).invokeSave(chunk);
		 * //((RegionBasedStorageAccessor) (Object) diskStorage).invokeWrite(pos,
		 * ChunkSerializer.serialize(world, chunk)); } catch (IOException e) {
		 * e.printStackTrace(); } } chunkIOLock.unlock();
		 */
		((ThreadedAnvilChunkStorageAccessor) this).invokeSave(chunk);
	}

	protected boolean updateHolderMap() {
		return false;
	}

	protected void releaseLightTicket(ChunkPos pos) {
	}

	public CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> createChunkFuture(ChunkHolder chunkHolder,
			ChunkStatus chunkStatus) {
		throw new RuntimeException("ew chunkholder");
	}

	public int getTotalChunksLoadedCount() {
		this.tick();
		return loaded;
	}

	public int getLoadedChunkCount() {
		return loaded;
	}

	/*
	 * protected ThreadedAnvilChunkStorage.TicketManager getTicketManager() { return
	 * this.ticketManager; }
	 */

	// void exportChunks(Writer writer)

	public Stream<ServerPlayerEntity> getPlayersWatchingChunk(ChunkPos chunkPos, boolean onlyOnWatchDistanceEdge) {
		return chunks.get(chunkPos.toLong()).join().getPlayerStream();
	}

	public void setViewDistance(int watchDistance) {
		viewDistance = watchDistance;

	}

	public void updateCameraPosition(ServerPlayerEntity player) {
		int lastChunkX = player.chunkX;
		int lastChunkZ = player.chunkZ;
		player.chunkX = MathHelper.floor(player.getX()) >> 4;
		player.chunkZ = MathHelper.floor(player.getZ()) >> 4;
		if (player.chunkX != lastChunkX || player.chunkZ != lastChunkZ) {
			System.out.println("Player changed chunk");
			sendChunkPositionPacket(player);
			loadManager.updateSource(playerLoadSources.get(player), player.chunkX, player.chunkZ);
		}

	}

	public void loadEntity(Entity entity) {
		if (entity instanceof ServerPlayerEntity) {
			ServerPlayerEntity player = (ServerPlayerEntity) entity;
			LoadSource loadSource = new LoadSource(player, viewDistance);
			playerLoadSources.put(player, loadSource);
			loadManager.addSource(loadSource, player.chunkX, player.chunkZ);
			sendChunkPositionPacket((ServerPlayerEntity) entity);
		}
		super.loadEntity(entity);
	}

	public void unloadEntity(Entity entity) {
		if (entity instanceof ServerPlayerEntity) {
			loadManager.removeSource(playerLoadSources.remove(entity));
		}
		super.unloadEntity(entity);
	}

	@Override
	public void tick(BooleanSupplier supplier) {
		tick();
	}

	public void tick() {
		LoadManagerTickResult loadTickResult = loadManager.tick();
		while (!newChunks.isEmpty()) {
			ChunkMapEntry entry = newChunks.poll();
			if (loadManager.isLoaded(entry.pos)) {
				initChunk(entry);
			} else {
				System.out.println("chunk at " + entry.objectPos + " loaded without being in load manager");
				loaded++; // don't get the var out of sync
				loadTickResult.getUnloads().add(entry.pos);
			}
		}
		for (LongIterator iter = loadTickResult.getLoads().iterator(); iter.hasNext();) {
			long l = iter.nextLong();
			loadChunkOffThread(l);
		}
		getLightProvider().tick();
		super.tickPlayerMovement();

		for (LongIterator iter = loadTickResult.getPlayerUpdates().iterator(); iter.hasNext();) {
			long update = iter.nextLong();
			CompletableFuture<ChunkMapEntry> chunkFuture = chunks.get(update);
			if (chunkFuture != null && chunkFuture.isDone()) {
				chunkFuture.join().updatePlayers();
			}
		}

		for (LongIterator iter = loadTickResult.getUnloads().iterator(); iter.hasNext();) {
			long unload = iter.nextLong();
			CompletableFuture<ChunkMapEntry> chunkFuture = chunks.remove(unload);
			if (chunkFuture != null && chunkFuture.isDone()) {
				ChunkMapEntry chunk = chunkFuture.join();
				chunk.chunk.setLoadedToWorld(false);
				loaded--;
				writeChunk(chunk.chunk);
				world.unloadEntities(chunk.chunk);
				((ServerLightingProviderAccessor) getLightProvider()).invokeUpdateChunkStatus(chunk.objectPos);
				getLightProvider().tick();
				progressListener.setChunkStatus(chunk.objectPos, null);
			}
		}

		chunks.values().stream().filter(CompletableFuture::isDone).map(CompletableFuture::join)
				.forEach(ChunkMapEntry::sendUpdates);
	}

	public boolean ticksEntities(long pos) {
		return loadManager.shouldTickEntities(pos);
	}

	public void setSpawnPos(ChunkPos pos) {
		System.out.println("set spawn pos to " + pos);
		progressListener.start(pos);
		if (spawnPos == null) {
			loadManager.addSource(SPAWNCHUNKS_LOAD_SOURCE, pos.x, pos.z);
		} else {
			loadManager.updateSource(SPAWNCHUNKS_LOAD_SOURCE, pos.x, pos.z);
		}
		spawnPos = pos;
	}

	public WorldChunk getChunk(int x, int z) {
		long pos = ChunkPos.toLong(x, z);
		CompletableFuture<ChunkMapEntry> chunkFuture = chunks.computeIfAbsent(pos, l -> {
			CompletableFuture<ChunkMapEntry> future = new CompletableFuture<>();
			ChunkMapEntry chunk = new ChunkMapEntry(loadChunk(l), l);
			future.complete(chunk);
			newChunks.add(chunk);
			return future;
		});
		return chunkFuture.join().chunk;
	}

	public WorldChunk getLoadedChunk(int x, int z) {
		CompletableFuture<ChunkMapEntry> future;
		if ((future = chunks.get(ChunkPos.toLong(x, z))) != null && future.isDone()) {
			return future.join().chunk;
		}
		return null;
	}

	public void blockUpdate(int x, int z, BlockPos pos) {
		chunks.get(ChunkPos.toLong(x, z)).join().blockUpdate(pos);
	}

	public boolean isChunkLoaded(int x, int z) {
		return chunks.containsKey(ChunkPos.toLong(x, z));
	}

	protected IntSupplier getCompletedLevelSupplier(long pos) {
		return () -> 0;
	}

	public void sendToOtherNearbyPlayers(Entity entity, Packet<?> packet) {
		super.sendToOtherNearbyPlayers(entity, packet);
	}

	public void sendToNearbyPlayers(Entity entity, Packet<?> packet) {
		super.sendToNearbyPlayers(entity, packet);
	}

	private CompletableFuture<ChunkMapEntry> loadChunkOffThread(long pos) {
		// computeIfAbsent is guaranteed to run only once, so no double gens
		return chunks.computeIfAbsent(pos, l -> CompletableFuture.supplyAsync(() -> {
			ChunkMapEntry chunk = new ChunkMapEntry(loadChunk(l), l);
			newChunks.add(chunk);
			return chunk;
		}));
	}

	private WorldChunk loadChunk(long lpos) {
		try {
			ChunkPos pos = new ChunkPos(lpos);
			chunkIOLock.lock();
			CompoundTag tag = ((ThreadedAnvilChunkStorageAccessor) this).invokeGetUpdatedChunkTag(pos);
			if (tag != null) {
				ProtoChunk chunk = ChunkSerializer.deserialize(world, structureManager, getPointOfInterestStorage(),
						pos, tag);
				chunkIOLock.unlock();
				return MiscUtil.ensureFull(world, chunk);
			} else {
				chunkIOLock.unlock();
				ProtoChunk proto = new ProtoChunk(pos, UpgradeData.NO_UPGRADE_DATA);
				generator.generate(proto);
				return MiscUtil.ensureFull(world, proto);
			}
		} catch (IOException e) {
			throw new RuntimeException("Error loading chunk", e);
		}
	}

	private void initChunk(ChunkMapEntry entry) {
		// System.out.println("load " + entry.objectPos)
		System.out.println("Loading " + entry.objectPos);
		if(entry.x == 16 && entry.z == 10) {
			System.out.println("16 10");
		}
		WorldChunk chunk = entry.chunk;
		chunk.setLoadedToWorld(true);
		chunk.setLevelTypeProvider(() -> loadManager.shouldTickEntities(entry.pos) ? ChunkHolder.LevelType.ENTITY_TICKING : ChunkHolder.LevelType.TICKING);
		chunk.loadToWorld();
		world.addBlockEntities(chunk.getBlockEntities().values());
		List<Entity> toRemove = null;
		for(TypeFilterableList<Entity> list : chunk.getEntitySectionArray()) {
			for(Entity e : list) {
				System.out.println(e);
				if(!world.loadEntity(e)) {
					System.out.println("could not load entity " + e);
					(toRemove == null ? (toRemove = new ArrayList<>()) : toRemove).add(e);
				}
			}
		}
		if(toRemove != null) {
			for(Entity e : toRemove) {
				chunk.remove(e);
			}
		}
		getLightProvider().light(chunk, false);
		entry.updatePlayers();
		loaded++;
		progressListener.setChunkStatus(chunk.getPos(), chunk.getStatus());
	}

	private void sendChunkPositionPacket(ServerPlayerEntity player) {
		player.networkHandler.sendPacket(new ChunkRenderDistanceCenterS2CPacket(player.chunkX, player.chunkZ));
	}

	private class ChunkMapEntry {
		private Set<ServerPlayerEntity> players = new HashSet<>();
		private int blockUpdates = 0;
		private int sectionUpdateMask = 0;
		private short[] blockUpdatePositions = new short[64];

		public final WorldChunk chunk;
		public final ChunkPos objectPos;
		public final long pos;
		public final int x;
		public final int z;

		ChunkMapEntry(WorldChunk chunk, long pos) {
			this.chunk = chunk;
			this.pos = pos;
			this.objectPos = new ChunkPos(pos);
			x = objectPos.x;
			z = objectPos.z;
		}

		public void updatePlayers() {
			Set<ServerPlayerEntity> currentPlayers = loadManager.getPlayersWatching(pos)
					.collect(Collectors.toCollection(HashSet::new));
			for (ServerPlayerEntity p : currentPlayers) {
				if (!players.contains(p)) {
					playerLoad(p);
				}
			}
			for (ServerPlayerEntity p : players) {
				if (!currentPlayers.contains(p)) {
					playerUnload(p);
				}
			}
			players = currentPlayers;
		}

		public Stream<ServerPlayerEntity> getPlayerStream() {
			return players.stream();
		}

		public void playerLoad(ServerPlayerEntity player) {
			((ThreadedAnvilChunkStorageAccessor) YeetChunkStorage.this).invokeSendChunkDataPackets(player,
					new Packet<?>[2], chunk);
			/*
			 * player.sendInitialChunkPackets(objectPos, new ChunkDataS2CPacket(chunk,
			 * 65535), new LightUpdateS2CPacket(objectPos, world.getLightingProvider()));
			 */
		}

		public void playerUnload(ServerPlayerEntity player) {
			player.sendUnloadChunkPacket(objectPos);
		}

		public void sendUpdates() {
			if (this.blockUpdates == 1) {
				int x = (blockUpdatePositions[0] >> 12 & 15) + this.x * 16;
				int y = blockUpdatePositions[0] & 255;
				int z = (blockUpdatePositions[0] >> 8 & 15) + this.z * 16;
				BlockPos blockPos = new BlockPos(x, y, z);
				sendPacketToPlayers(new BlockUpdateS2CPacket(world, blockPos));
				if (chunk.getBlockState(blockPos).getBlock().hasBlockEntity()) {
					sendBlockEntityUpdatePacket(world, blockPos);
				}
			} else if (blockUpdates == 64) {
				sendPacketToPlayers(new ChunkDataS2CPacket(chunk, sectionUpdateMask));
			} else if (blockUpdates != 0) {
				sendPacketToPlayers(new ChunkDeltaUpdateS2CPacket(blockUpdates, blockUpdatePositions, chunk));

				for (int i = 0; i < blockUpdates; i++) {
					int x = (blockUpdatePositions[i] >> 12 & 15) + this.x * 16;
					int y = blockUpdatePositions[i] & 255;
					int z = (blockUpdatePositions[i] >> 8 & 15) + this.z * 16;
					BlockPos blockPos = new BlockPos(x, y, z);
					if (chunk.getBlockState(blockPos).getBlock().hasBlockEntity()) {
						sendBlockEntityUpdatePacket(world, blockPos);
					}
				}
			}
		}

		public void blockUpdate(BlockPos pos) {
			sectionUpdateMask |= 1 << (pos.getY() >> 4);
			if (blockUpdates <= 63) {
				short encodedPos = (short) ((pos.getX() & 15) << 12 | (pos.getZ() & 15) << 8 | pos.getY());
				for (int i = 0; i < blockUpdates; i++) {
					if (blockUpdatePositions[i] == encodedPos) {
						return; // if we've already updated this position, don't add it again.
					}
				}
				blockUpdatePositions[blockUpdates++] = encodedPos;
			}
		}

		private void sendPacketToPlayers(Packet<?> packet) {
			for (ServerPlayerEntity player : players) {
				player.networkHandler.sendPacket(packet);
			}
		}

		private void sendBlockEntityUpdatePacket(World world, BlockPos pos) {
			BlockEntity blockEntity = world.getBlockEntity(pos);
			if (blockEntity != null) {
				BlockEntityUpdateS2CPacket blockEntityUpdateS2CPacket = blockEntity.toUpdatePacket();
				if (blockEntityUpdateS2CPacket != null) {
					this.sendPacketToPlayers(blockEntityUpdateS2CPacket);
				}
			}

		}
	}

	public void close() throws IOException {
		// diskStorage.close();
		super.close();
	}
}
