package com.github.dipper.hubhe.service;

import com.github.dipper.hubhe.util.Hope;
import com.google.common.base.Function;

public interface Service<Req, Rep> extends Function<Req, Hope<Rep>> {

	@Override
	public abstract Hope<Rep> apply(Req req);

	public boolean isAvaliable();

	public void release();
}
