package io.github.nuclearfarts.chunkyeet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Either;

import org.apache.commons.lang3.mutable.MutableBoolean;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkRenderDistanceCenterS2CPacket;
import net.minecraft.network.packet.s2c.play.LightUpdateS2CPacket;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.structure.StructureManager;
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
import io.github.nuclearfarts.chunkyeet.mixin.ServerLightingProviderAccessor;
import io.github.nuclearfarts.chunkyeet.mixin.ThreadedAnvilChunkStorageAccessor;
import io.github.nuclearfarts.chunkyeet.util.MiscUtil;

public class YeetChunkStorage extends ThreadedAnvilChunkStorage {

	private final Map<Long, CompletableFuture<ChunkMapEntry>> chunks = new ConcurrentHashMap<>();
	private final Set<ChunkMapEntry> chunksToUnload = new HashSet<>();
	private final Map<ServerPlayerEntity, Set<ChunkMapEntry>> playerToLoadedChunks = new HashMap<>();
	private final Executor chunkGenExecutor = Executors.newFixedThreadPool(4);
	private final Set<ChunkMapEntry> newChunks = Collections.synchronizedSet(new HashSet<>());
	private final Lock deserializationLock = new ReentrantLock();

	protected int viewDistance;
	protected final ServerWorld world;
	protected final StructureManager structureManager;

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
	}

	@Override
	@Environment(EnvType.CLIENT)
	public String method_23272(ChunkPos chunkPos) {
		return "";
	}

	protected void save(boolean flush) {
		chunks.values().stream().filter(CompletableFuture::isDone).map(cf -> cf.join().chunk).forEach(this::writeChunk);
	}

	private void writeChunk(Chunk chunk) {
		System.out.println("writing chunk " + chunk.getPos());
		((ThreadedAnvilChunkStorageAccessor) this).getPointOfInterestStorage().method_20436(chunk.getPos());
		if (chunk.needsSaving()) {
			chunk.setLastSaveTime(this.world.getTime());
			chunk.setShouldSave(false);
			setTagAt(chunk.getPos(), ChunkSerializer.serialize(this.world, chunk));
		}
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
		return chunks.size();
	}

	public int getLoadedChunkCount() {
		return chunks.size();
	}

	/*
	 * protected ThreadedAnvilChunkStorage.TicketManager getTicketManager() { return
	 * this.ticketManager; }
	 */

	// void exportChunks(Writer writer)

	public Stream<ServerPlayerEntity> getPlayersWatchingChunk(ChunkPos chunkPos, boolean onlyOnWatchDistanceEdge) {
		return chunks.get(chunkPos.toLong()).join().players.stream();
	}

	public void setViewDistance(int watchDistance) {
		viewDistance = watchDistance;
		if (playerToLoadedChunks != null) {
			recheckPlayerAssociations();
		}
	}

	public void updateCameraPosition(ServerPlayerEntity player) {
		int lastChunkX = player.chunkX;
		int lastChunkZ = player.chunkZ;
		player.chunkX = MathHelper.floor(player.getX()) >> 4;
		player.chunkZ = MathHelper.floor(player.getZ()) >> 4;
		/*
		 * for(ChunkMapEntry chunk : playerToLoadedChunks.get(player)) {
		 * player.sendInitialChunkPackets(chunk.objectPos, new
		 * ChunkDataS2CPacket(chunk.chunk, 65535), new
		 * LightUpdateS2CPacket(chunk.objectPos, world.getLightingProvider())); }
		 */
		if (player.chunkX != lastChunkX || player.chunkZ != lastChunkZ) {
			System.out.println("Player changed chunk");
			sendChunkPositionPacket(player);
			int lazyDistance = viewDistance - 2;
			for (Object c : playerToLoadedChunks.get(player).toArray()) { // copy it to an array to prevent CME
				ChunkMapEntry chunk = (ChunkMapEntry) c;
				int dist = getWeirdDistance(chunk.x, chunk.z, player.chunkX, player.chunkZ);
				// System.out.println("Player chunk distance: " + dist + " (" + chunk.x + "," +
				// chunk.z + ")");
				if (dist > viewDistance) {
					chunk.playerUnload(player);
				} else if (dist > lazyDistance) {
					chunk.playerLoadLazy(player);
				}
			}
			loadChunksForPlayer(player);
		}

	}

	private void recheckPlayerAssociations() {
		for (Map.Entry<ServerPlayerEntity, Set<ChunkMapEntry>> e : playerToLoadedChunks.entrySet()) {
			ServerPlayerEntity player = e.getKey();
			for (ChunkMapEntry chunk : e.getValue()) {
				chunk.playerUnload(player);
			}
			loadChunksForPlayer(player);
		}
	}

	private void loadChunksForPlayer(ServerPlayerEntity player) {
		int lazyDistance = viewDistance - 2;
		for (int i = -viewDistance; i <= viewDistance; i++) {
			for (int j = -viewDistance; j <= viewDistance; j++) {
				int dist = Math.max(Math.abs(i), Math.abs(j));
				int x = i + player.chunkX;
				int z = j + player.chunkZ;
				long chunkPos = ChunkPos.toLong(x, z);
				CompletableFuture<ChunkMapEntry> chunkF;
				if ((chunkF = chunks.get(chunkPos)) != null && chunkF.isDone()) {
					ChunkMapEntry chunk = chunkF.join();
					if (dist > lazyDistance) {
						chunk.playerLoadLazy(player);
					} else {
						chunk.playerLoad(player);
					}
				} else {
					System.out.println("Chunk at " + x + ", " + z + " not loaded, loading offthread");
					loadChunkOffThread(x, z);
				}
			}
		}
	}

	private static int getWeirdDistance(int chunkX, int chunkZ, int x, int z) {
		return Math.max(Math.abs(chunkX - x), Math.abs(chunkZ - z));
	}

	public void loadEntity(Entity entity) {
		if (entity instanceof ServerPlayerEntity) {
			playerToLoadedChunks.put((ServerPlayerEntity) entity, new HashSet<>());
			sendChunkPositionPacket((ServerPlayerEntity) entity);
			loadChunksForPlayer((ServerPlayerEntity) entity);
		}
	}

	public void unloadEntity(Entity entity) {
		if (entity instanceof ServerPlayerEntity) {
			Set<ChunkMapEntry> entries = playerToLoadedChunks.get(entity);
			Collection<ChunkMapEntry> javaPls = new ArrayList<>(entries.size());
			javaPls.addAll(entries);
			for (ChunkMapEntry c : javaPls) {
				c.playerUnload((ServerPlayerEntity) entity);
			}
			playerToLoadedChunks.remove(entity);
		}
	}

	@Override
	public void tick(BooleanSupplier supplier) {
		tick();
	}

	public void tick() {
		Collection<ChunkMapEntry> chunksToAdd = new ArrayList<>(newChunks.size() + 5); //extra size for async shenanigans
		synchronized (newChunks) {
			chunksToAdd.addAll(newChunks);
			newChunks.clear();
		}
		for (ChunkMapEntry entry : chunksToAdd) {
			initChunk(entry);
		}
		for (ChunkMapEntry chunk : chunksToUnload) {
			System.out.println("Unloading chunk at " + chunk.x + ", " + chunk.z);
			chunk.chunk.setLoadedToWorld(false);
			world.unloadEntities(chunk.chunk);
			((ServerLightingProviderAccessor) getLightProvider()).invokeUpdateChunkStatus(chunk.objectPos);
			getLightProvider().tick();
			writeChunk(chunk.chunk);
			chunks.remove(chunk.pos);
		}
		chunksToUnload.clear();

		chunks.values().stream().filter(CompletableFuture::isDone).map(CompletableFuture::join).forEach(ChunkMapEntry::sendUpdates);
	}

	public WorldChunk getChunk(int x, int z) {
		long pos = ChunkPos.toLong(x, z);
		MutableBoolean flag = new MutableBoolean(false);
		CompletableFuture<ChunkMapEntry> chunkFuture = chunks.computeIfAbsent(pos, l -> {
			flag.setTrue();
			return new CompletableFuture<>();
		});
		if(flag.booleanValue()) {
			ChunkMapEntry chunk = new ChunkMapEntry(loadChunk(pos), pos);
			chunkFuture.complete(chunk);
			newChunks.add(chunk); // we might be offthread!
			return chunk.chunk;
		} else {
			return chunkFuture.join().chunk;
		}
	}

	public WorldChunk getLoadedChunk(int x, int z) {
		CompletableFuture<ChunkMapEntry> future;
		if((future = chunks.get(ChunkPos.toLong(x, z))) != null && future.isDone()) {
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

	private void loadChunkOffThread(int x, int z) {
		//computeIfAbsent is an atomic operation here. we don't use the result (since who cares).
		chunks.computeIfAbsent(ChunkPos.toLong(x, z), l -> CompletableFuture.supplyAsync(() -> {
			ChunkMapEntry chunk = new ChunkMapEntry(loadChunk(l), l);
			newChunks.add(chunk);
			return chunk;
		}));
	}

	private WorldChunk loadChunk(long lpos) {
		try {
			ChunkPos pos = new ChunkPos(lpos);
			deserializationLock.lock();
			CompoundTag tag = ((ThreadedAnvilChunkStorageAccessor) this).invokeGetUpdatedChunkTag(pos);
			deserializationLock.unlock();
			if (tag != null) {
				return MiscUtil.ensureFull(world,
						ChunkSerializer.deserialize(world, structureManager, getPointOfInterestStorage(), pos, tag));
			} else {
				ProtoChunk proto = new ProtoChunk(pos, UpgradeData.NO_UPGRADE_DATA);
				generator.generate(proto);
				return MiscUtil.ensureFull(world, proto);
			}
		} catch (IOException e) {
			throw new RuntimeException("Error loading chunk", e);
		}
	}

	private void initChunk(ChunkMapEntry entry) {
		int lazyDistance = viewDistance - 2;
		WorldChunk chunk = entry.chunk;
		chunk.setLoadedToWorld(true);
		chunk.setLevelTypeProvider(() -> ChunkHolder.LevelType.ENTITY_TICKING);
		getLightProvider().light(chunk, false);
		for (ServerPlayerEntity player : playerToLoadedChunks.keySet()) {
			// System.out.println("Checking " + player);
			int dist = getWeirdDistance(entry.x, entry.z, player.chunkX, player.chunkZ);
			if (dist < lazyDistance) {
				entry.playerLoad(player);
			} else if (dist < viewDistance) {
				entry.playerLoadLazy(player);
			}
			entry.checkScheduleUnload();
		}
	}

	private void sendChunkPositionPacket(ServerPlayerEntity player) {
		player.networkHandler.sendPacket(new ChunkRenderDistanceCenterS2CPacket(player.chunkX, player.chunkZ));
	}

	private class ChunkMapEntry {
		private final Set<ServerPlayerEntity> players = new HashSet<>();
		private final Set<ServerPlayerEntity> nonLazyPlayers = new HashSet<>();
		private boolean scheduledToUnload = false;
		private boolean forced = false;
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

		public boolean tickEntities() {
			return forced || nonLazyPlayers.size() != 0;
		}

		public void force() {
			forced = true;
			unscheduleUnload();
		}

		public void unforce() {
			forced = false;
			checkScheduleUnload();
		}

		public void playerLoad(ServerPlayerEntity player) {
			nonLazyPlayers.add(player);
			players.add(player);
			playerToLoadedChunks.get(player).add(this);
			player.sendInitialChunkPackets(objectPos, new ChunkDataS2CPacket(chunk, 65535),
					new LightUpdateS2CPacket(objectPos, world.getLightingProvider()));
			unscheduleUnload();
		}

		public void playerUnload(ServerPlayerEntity player) {
			players.remove(player);
			nonLazyPlayers.remove(player);
			playerToLoadedChunks.get(player).remove(this);
			player.sendUnloadChunkPacket(objectPos);
			checkScheduleUnload();
			// checkLazy();
		}

		public void playerLoadLazy(ServerPlayerEntity player) {
			players.add(player);
			nonLazyPlayers.remove(player);
			unscheduleUnload();
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

		private void unscheduleUnload() {
			if (scheduledToUnload) {
				scheduledToUnload = false;
				chunksToUnload.remove(this);
			}
		}

		public void checkScheduleUnload() {
			if (!forced && players.size() == 0) {
				scheduledToUnload = true;
				chunksToUnload.add(this);
			}
		}

		private void sendPacketToPlayers(Packet<?> packet) {
			for (ServerPlayerEntity player : nonLazyPlayers) {
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
}
