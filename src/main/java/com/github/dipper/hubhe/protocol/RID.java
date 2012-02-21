package com.github.dipper.hubhe.protocol;

public interface RID {

	public int rid();
	
	public <T> boolean isType(Class<T> clazz);

	public <T> T cast(Class<T> clazz);
}
