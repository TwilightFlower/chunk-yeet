package io.github.nuclearfarts.chunkyeet;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.registry.CommandRegistry;

public class Entrypoint implements ModInitializer {

	@Override
	public void onInitialize() {
		CommandRegistry.INSTANCE.register(false, dispatcher -> {
			dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("serverlight").executes(this::light));
			dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("heightmaps").executes(this::heightmaps));
		});
	}
	
	private int light(CommandContext<ServerCommandSource> ctx) {
		try {
			ServerCommandSource src = ctx.getSource();
		src.sendFeedback(new LiteralText(String.format("l: %d", src.getWorld().getLightLevel(new BlockPos(src.getPosition())))), false);
		} catch(Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	public static void lol() {
		
	}

	private int heightmaps(CommandContext<ServerCommandSource> ctx) {
		try {
			ServerCommandSource src = ctx.getSource();
			ServerWorld world = src.getWorld();
			BlockPos pos = new BlockPos(src.getPosition());
			src.sendFeedback(new LiteralText(String.format("mb-noleaves: %d", world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos).getY())), false);
			src.sendFeedback(new LiteralText(String.format("mb: %d", world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, pos).getY())), false);
			src.sendFeedback(new LiteralText(String.format("ws: %d", world.getTopPosition(Heightmap.Type.WORLD_SURFACE, pos).getY())), false);
		} catch(Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
}
