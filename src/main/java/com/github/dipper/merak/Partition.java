package com.github.dipper.merak;


public interface Partition<T> {

	public T nodeForHash(long hash);
	
	public int slice();
}
