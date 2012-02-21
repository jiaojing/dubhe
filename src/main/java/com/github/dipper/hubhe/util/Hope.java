package com.github.dipper.hubhe.util;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.github.dipper.hubhe.util.fp.Block;
import com.github.dipper.hubhe.util.fp.Def;
import com.google.common.collect.Lists;

public abstract class Hope<V> {

	public static interface Reduce<ACCU, NOW> {
		ACCU apply(ACCU accu, NOW now);
	}

	public static <W> Promise<W> create() {
		return new Promise<W>();
	}

	public static <V, W> Hope<V> collect(List<Hope<W>> hopes,
			final V initialValue, final Reduce<V, W> reduce) {
		return new CollectedHope<V, W>(hopes, initialValue, reduce);
	}

	public static <V, W> Hope<V> join(List<Hope<W>> hopes,
			final V initialValue, final Reduce<V, W> reduce) {
		return new JoinedHope<V, W>(hopes, initialValue, reduce);
	}

	public static <W> Hope<List<W>> collect(List<Hope<W>> hopes) {
		return new CollectedHope<List<W>, W>(hopes, Lists.<W> newArrayList(),
				new Reduce<List<W>, W>() {
					@Override
					public List<W> apply(List<W> accu, W now) {
						accu.add(now);
						return accu;
					}
				});
	}

	public static <W> Hope<List<W>> join(List<Hope<W>> hopes) {
		return new JoinedHope<List<W>, W>(hopes, Lists.<W> newArrayList(),
				new Reduce<List<W>, W>() {
					@Override
					public List<W> apply(List<W> accu, W now) {
						accu.add(now);
						return accu;
					}
				});
	}

	public <W> Hope<W> map(Def<V, W> fun) {
		return new MapedHope<V, W>(this, fun);
	}

	public V get() {
		return get(20, TimeUnit.SECONDS);
	}

	public abstract V get(long timeout, TimeUnit unit);

	protected abstract void linkTo(Block<V> block);

}
