package com.github.dipper.merak;

public interface KeyHasher<T> {
	public long hashKey(T key);
}
