package io.github.nuclearfarts.chunkyeet.loadmap;

import net.minecraft.server.network.ServerPlayerEntity;

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
	
	public int hashCode() {
		return source.hashCode();
	}
	
	public boolean equals(Object object) {
		return object instanceof LoadMarker && ((LoadMarker) object).source.equals(source);
	}
}
