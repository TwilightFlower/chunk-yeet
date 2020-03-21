package io.github.nuclearfarts.chunkyeet.util;

import net.minecraft.util.thread.ThreadExecutor;

public class UnimplementedThreadExecutor extends ThreadExecutor<Runnable> {

	public UnimplementedThreadExecutor(String name) {
		super(name);
	}

	@Override
	protected Runnable createTask(Runnable runnable) {
		throw new UnsupportedOperationException("createTask");
	}

	@Override
	protected boolean canExecute(Runnable task) {
		throw new UnsupportedOperationException("canExecute");
	}

	@Override
	protected Thread getThread() {
		throw new UnsupportedOperationException("getThread");
	}

}
