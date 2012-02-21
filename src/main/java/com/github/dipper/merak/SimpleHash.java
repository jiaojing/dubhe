package com.github.dipper.merak;


public class SimpleHash<T> implements Partition<T> {

	@Override
	public T nodeForHash(long hash) {
		return null;
	}

	@Override
	public int slice() {
		return 0;
	}

}
