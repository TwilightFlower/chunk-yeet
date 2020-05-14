package io.github.nuclearfarts.chunkyeet.mixin;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntSupplier;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.util.Pair;
import net.minecraft.world.chunk.light.LightingProvider;

import io.github.nuclearfarts.chunkyeet.Entrypoint;
import io.github.nuclearfarts.chunkyeet.Worker;
import it.unimi.dsi.fastutil.objects.ObjectList;

@Mixin(ServerLightingProvider.class)
public class ServerLightProviderMixin extends LightingProvider {

	public ServerLightProviderMixin() {
		super(null, false, false);
		throw new UnsupportedOperationException();
	}

	@Shadow
	private @Final ObjectList<Pair<Object, Runnable>> pendingTasks;
	@Shadow
	private volatile int taskBatchSize;
	@Shadow
	private @Final AtomicBoolean field_18812;

	@Shadow
	private void runTasks() {
	};

	private @Final LinkedBlockingQueue<Pair<ServerLightingProvider.Stage, Runnable>> taskQueue;
	private @Final Worker worker;
	
	@Inject(at = @At("RETURN"), method = "<init>")
	private void ctor(CallbackInfo info) {
		taskQueue = new LinkedBlockingQueue<>();
		worker = new Worker(this::taskHelper, taskQueue);
	}
	
	@Inject(at = @At("HEAD"), method = "runTasks")
	private void please(CallbackInfo info) {
		Entrypoint.lol();
		if(pendingTasks.size() != 0) {
			System.out.println(pendingTasks);
		}
	}
	
	@Overwrite
	private void enqueue(int x, int z, IntSupplier completedLevelSupplier, ServerLightingProvider.Stage stage,
			Runnable task) {
		taskQueue.add(new Pair<>(stage, task));
		System.out.println("enqueue " + x + " " + z);
		Entrypoint.lol();
		/*if (this.pendingTasks.size() >= this.taskBatchSize) {
			sansUndertale.execute(this::runTasks);
		}*/
	}
	
	@Unique
	private void taskHelper(Runnable preTask, Runnable postTask) {
		preTask.run();
		super.doLightUpdates(Integer.MAX_VALUE, true, true);
		postTask.run();
	}

	@Overwrite
	public void tick() {
		/*if(pendingTasks.size() != 0) {
			System.out.println(pendingTasks);
			Entrypoint.lol();
		}*/
		//runTasks();
	}
	
	@Inject(at = @At("HEAD"), method = "close")
	private void closeHelper(CallbackInfo ci) {
		worker.work = false;
		worker.interrupt();
		while(!taskQueue.isEmpty()) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {}
		}
	}
}
