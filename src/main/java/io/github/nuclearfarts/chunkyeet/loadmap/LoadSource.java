package io.github.nuclearfarts.chunkyeet.loadmap;

import net.minecraft.server.network.ServerPlayerEntity;

public class LoadSource {
	int radius;
	int x;
	int z;
	final LoadMarker marker;
	boolean tracked = false;
	ServerPlayerEntity player;
	
	public LoadSource() {
		marker = new LoadMarker(this, false);
	}
	
	public LoadSource(int radius) {
		this();
		this.radius = radius;
	}
	
	public LoadSource(ServerPlayerEntity player) {
		this.player = player;
		marker = new LoadMarker(this, true);
	}
	
	public LoadSource(ServerPlayerEntity player, int radius) {
		this(player);
		this.radius = radius;
	}
	
	public final int getX() {
		return x;
	}
	
	public final int getZ() {
		return z;
	}
	
	public final int getRadius() {
		return radius;
	}
}
