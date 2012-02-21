package com.github.dipper.hubhe.util;

import java.util.concurrent.TimeUnit;

import com.github.dipper.hubhe.util.fp.Def;

public class MapedHope<V, W> extends Promise<W> {

	private final Hope<V> hope;
	private final Def<V, W> fun;

	MapedHope(Hope<V> hope, Def<V, W> fun) {
		this.hope = hope;
		this.fun = fun;
	}

	@Override
	public W get(long timeout, TimeUnit unit) {
		V value = hope.get(timeout, unit);
		if (value == null) {
			return null;
		} else {
			return fun.apply(value);
		}
	}
}
