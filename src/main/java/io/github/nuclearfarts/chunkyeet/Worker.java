package io.github.nuclearfarts.chunkyeet;

import java.util.concurrent.BlockingQueue;
import java.util.function.BiConsumer;

import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.server.world.ServerLightingProvider.Stage;
import net.minecraft.util.Pair;

public class Worker extends Thread {

	private final BiConsumer<Runnable, Runnable> taskConsumer;
	private final BlockingQueue<Pair<ServerLightingProvider.Stage, Runnable>> taskQueue;
	public boolean work = true;

	public Worker(BiConsumer<Runnable, Runnable> taskConsumer, BlockingQueue<Pair<ServerLightingProvider.Stage, Runnable>> taskQueue) {
		this.taskConsumer = taskConsumer;
		this.taskQueue = taskQueue;
		setDaemon(true);
	}

	@Override
	public void run() {
		while (work) {
			try {
				Pair<ServerLightingProvider.Stage, Runnable> pair = taskQueue.take();
				if (pair.getLeft() == ServerLightingProvider.Stage.POST_UPDATE) {
					taskConsumer.accept(() -> {}, pair.getRight());
				} else {
					taskConsumer.accept(pair.getRight(), () -> {});
				}
			} catch (InterruptedException e) {
				work = false;
			}
		}
	}
}