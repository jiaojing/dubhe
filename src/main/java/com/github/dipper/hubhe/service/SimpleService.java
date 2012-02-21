package com.github.dipper.hubhe.service;

import com.github.dipper.hubhe.util.Hope;

public abstract class SimpleService<Req, Rep> implements Service<Req, Rep> {

	public abstract Hope<Rep> apply(Req req);

	@Override
	public void release() {

	}

	public boolean isAvaliable() {
		return true;
	};
}
