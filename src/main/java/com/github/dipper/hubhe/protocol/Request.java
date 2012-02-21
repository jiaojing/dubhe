package com.github.dipper.hubhe.protocol;

public abstract class Request implements RID {

	@Override
	public abstract int rid();

	@Override
	public <T> boolean isType(Class<T> clazz) {
		return clazz.isInstance(this);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T cast(Class<T> clazz) {
		return (T)this;
	}

}
