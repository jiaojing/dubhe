package com.github.dipper.hubhe.util;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class CollectedHope<V, W> extends JoinedHope<V, W> {

	CollectedHope(List<Hope<W>> hopes, final V initialValue,
			final Reduce<V, W> reduce) {
		super(hopes, initialValue, reduce);
	}

	@Override
	public V get(long timeout, TimeUnit unit) {
		V value = super.get(timeout, unit);
		if (value == null) {
			value = this.semiValue;
		}
		return value;
	}

}