package com.github.dipper.hubhe.util.fp;

public abstract class Exp<V> implements Lambda<Void, V> {
	public abstract V apply();

	public static <W> Exp<W> value(final W value) {
		return new Exp<W>() {
			@Override
			public W apply() {
				return value;
			}
		};
	}
}
