package io.github.nuclearfarts.chunkyeet;

import java.io.File;
import java.io.IOException;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.chunk.UpgradeData;
import net.minecraft.world.storage.RegionBasedStorage;

import net.fabricmc.api.ModInitializer;

import io.github.nuclearfarts.chunkyeet.gen.SimpleFlatGenerator;
import io.github.nuclearfarts.chunkyeet.mixin.RegionBasedStorageAccessor;
import io.github.nuclearfarts.chunkyeet.util.MiscUtil;

public class Entrypoint implements ModInitializer {
	@Override
	public void onInitialize() {
		ProtoChunk proto = new ProtoChunk(new ChunkPos(0, 0), UpgradeData.NO_UPGRADE_DATA);
		new SimpleFlatGenerator().generate(proto);
		RegionBasedStorage storage = RegionBasedStorageAccessor.create(new File("bad_test"));
		try {
			storage.getTagAt(new ChunkPos(0, 0));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		CompoundTag test = new CompoundTag();
		test.putInt("testInt", 10);
		try {
			((RegionBasedStorageAccessor) (Object) storage).invokeWrite(new ChunkPos(0, 0), test);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
