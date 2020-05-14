package io.github.nuclearfarts.chunkyeet.loadmap;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.ChunkPos;

import io.github.nuclearfarts.chunkyeet.util.MiscUtil;

final class LoadMarker {
	final LoadSource source;
	final boolean isPlayer;
	
	LoadMarker(LoadSource source, boolean isPlayer) {
		this.source = source;
		this.isPlayer = isPlayer;
	}
	
	boolean isPlayer() {
		return isPlayer;
	}
	
	ServerPlayerEntity getPlayer() {
		return source.player;
	}
	
	boolean processesEntities(long pos) {
		return distanceFromEdge(pos) >= 2;
	}
	
	int distanceFromEdge(long pos) {
		return source.radius - distanceFromSource(pos);
	}
	
	int distanceFromSource(long pos) {
		return MiscUtil.getWeirdDistance(ChunkPos.getPackedX(pos), ChunkPos.getPackedZ(pos), source.x, source.z);
	}
	
	public int hashCode() {
		return source.hashCode();
	}
	
	public boolean equals(Object object) {
		return object instanceof LoadMarker && ((LoadMarker) object).source.equals(source);
	}
}
