package io.github.nuclearfarts.chunkyeet.loadmap;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.ChunkPos;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

//ticketmanager-like thing. but with less mojank.
public class LoadManager {
	private static final PrevData PLACEHOLDER = new PrevData(0, 0, 0);

	private final Map<LoadSource, PrevData> changedSources = new ConcurrentHashMap<>();
	private final Long2ObjectMap<Set<LoadMarker>> map = new Long2ObjectOpenHashMap<>();
	private final Queue<LoadSource> removes = new ConcurrentLinkedQueue<>();
	private final LoadManagerTickResult tickResult = new LoadManagerTickResult();

	/**
	 * Isn't threadsafe if two threads use the same LoadSource, but that shouldn't
	 * happen. If weird things happen... slap a synchronized on this.
	 */
	public void updateSource(LoadSource source, int x, int z, int r) {
		PrevData data = new PrevData(source.x, source.z, source.radius);
		source.x = x;
		source.z = z;
		source.radius = r;
		changedSources.putIfAbsent(source, data);
	}

	public void updateSource(LoadSource source, int x, int z) {
		updateSource(source, x, z, source.radius);
	}

	public void addSource(LoadSource source, int x, int z) {
		source.x = x;
		source.z = z;
		changedSources.put(source, PLACEHOLDER);
	}

	public void removeSource(LoadSource source) {
		removes.offer(source);
	}

	public LoadManagerTickResult tick() {
		LongSet unloads = new LongOpenHashSet();
		LongSet loads = new LongOpenHashSet();
		LongSet playerUpdates = new LongOpenHashSet();
		Iterator<Map.Entry<LoadSource, PrevData>> iter = changedSources.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<LoadSource, PrevData> entry = iter.next();
			iter.remove();
			if (entry.getValue() == PLACEHOLDER) {
				add(entry.getKey(), unloads, loads, playerUpdates);
			} else {
				update(entry.getKey(), entry.getValue(), unloads, loads, playerUpdates);
			}
		}
		while (!removes.isEmpty()) {
			LoadSource src = removes.poll();
			remove(src, unloads, loads, playerUpdates);
		}
		tickResult.loads = loads;
		tickResult.unloads = unloads;
		tickResult.updatePlayers = playerUpdates;
		return tickResult;
	}

	public boolean isLoaded(long pos) {
		return map.containsKey(pos);
	}
	
	public boolean shouldTickEntities(long pos) {
		Set<LoadMarker> markers;
		if((markers = map.get(pos)) != null) {
			for(LoadMarker marker : markers) {
				if(marker.processesEntities(pos)) {
					return true;
				}
			}
		}
		return false;
	}

	public Stream<ServerPlayerEntity> getPlayersWatching(long pos) {
		Set<LoadMarker> markers;
		if ((markers = map.get(pos)) != null) {
			return markers.stream().filter(LoadMarker::isPlayer).map(LoadMarker::getPlayer);
		} else {
			return Stream.empty();
		}
	}

	private void update(LoadSource source, PrevData prev, LongSet unloads, LongSet loads, LongSet playerUpdates) {
		for (int x = prev.x - prev.r; x <= prev.x + prev.r; x++) {
			for (int z = prev.z - prev.r; z <= prev.z + prev.r; z++) {
				if (!test(x, z, source)) {
					long l = ChunkPos.toLong(x, z);
					Set<LoadMarker> markers = map.get(l);
					markers.remove(source.marker);
					if (markers.size() == 0) {
						map.remove(l);
						unloads.add(l);
						loads.remove(l);
					}
					if (source.player != null) {
						playerUpdates.add(l);
					}
				}
			}
		}
		for (int x = source.x - source.radius; x <= source.x + source.radius; x++) {
			for (int z = source.z - source.radius; z <= source.z + source.radius; z++) {
				if (!test(x, z, prev)) {
					long pos = ChunkPos.toLong(x, z);
					Set<LoadMarker> markers;
					if ((markers = map.get(pos)) == null) {
						markers = new HashSet<>();
						map.put(pos, markers);
						loads.add(pos);
						unloads.remove(pos);
					}
					markers.add(source.marker);
					if (source.player != null) {
						playerUpdates.add(pos);
					}
				}
			}
		}
	}

	private void remove(LoadSource source, LongSet unloads, LongSet loads, LongSet playerUpdates) {
		for (int x = source.x - source.radius; x <= source.x + source.radius; x++) {
			for (int z = source.z - source.radius; z <= source.z + source.radius; z++) {
				long l = ChunkPos.toLong(x, z);
				Set<LoadMarker> markers = map.get(l);
				markers.remove(source.marker);
				if (markers.size() == 0) {
					map.remove(l);
					loads.remove(l);
					unloads.add(l);
				}
				if (source.player != null) {
					playerUpdates.add(l);
				}
			}
		}
	}

	private void add(LoadSource source, LongSet unloads, LongSet loads, LongSet playerUpdates) {
		for (int x = source.x - source.radius; x <= source.x + source.radius; x++) {
			for (int z = source.z - source.radius; z <= source.z + source.radius; z++) {
				long pos = ChunkPos.toLong(x, z);
				Set<LoadMarker> markers;
				if ((markers = map.get(pos)) == null) {
					markers = new HashSet<>();
					map.put(pos, markers);
					loads.add(pos);
					unloads.remove(pos);
				}
				markers.add(source.marker);
				if (source.player != null) {
					playerUpdates.add(pos);
				}
			}
		}
	}

	private boolean test(int x, int z, LoadSource source) {
		return x >= source.x - source.radius && x <= source.x + source.radius && z >= source.z - source.radius
				&& z <= source.z + source.radius;
	}

	private boolean test(int x, int z, PrevData source) {
		return x >= source.x - source.r && x <= source.x + source.r && z >= source.z - source.r
				&& z <= source.z + source.r;
	}

	private static class PrevData {
		final int x;
		final int z;
		final int r;

		PrevData(int x, int z, int r) {
			this.x = x;
			this.z = z;
			this.r = r;
		}
	}
}
