package com.github.dipper.hubhe.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.github.dipper.hubhe.util.fp.Block;

public class Promise<V> extends Hope<V> {

	private volatile V value;
	private final CountDownLatch latch;
	private Block<V> block;

	Promise() {
		this(1);
	}

	Promise(int num) {
		latch = new CountDownLatch(num);
	}

	@Override
	public V get(long timeout, TimeUnit unit) {
		V value = null;
		try {
			if (latch.await(timeout, unit)) {
				value = this.value;
			}
		} catch (InterruptedException e) {
		}
		return value;
	}

	public synchronized Promise<V> set(V value) {
		this.value = value;
		latch.countDown();
		if (isDone()) {
			if (block != null) {
				block.apply(value);
			}
		}
		return this;
	}

	@Override
	protected synchronized void linkTo(Block<V> block) {
		checkNotNull(block);

		if (isDone()) {
			block.apply(value);
		} else {
			this.block = block;
		}
	}

	private boolean isDone() {
		return latch.getCount() <= 0;
	}
}
