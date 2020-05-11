package io.github.nuclearfarts.chunkyeet.loadmap;

import it.unimi.dsi.fastutil.longs.LongSet;

public class LoadManagerTickResult {
	LongSet unloads;
	LongSet loads;
	LongSet updatePlayers;
	
	public LongSet getUnloads() {
		return unloads;
	}
	
	public LongSet getLoads() {
		return loads;
	}
	
	public LongSet getPlayerUpdates() {
		return updatePlayers;
	}
}
