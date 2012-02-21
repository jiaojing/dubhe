package com.github.dipper.hubhe.service;

import com.github.dipper.hubhe.util.Hope;
import com.google.common.base.Function;

public class Services {
	public static <Req,Rep> Service<Req,Rep> create(){
		return new SimpleService<Req, Rep>() {
			@Override
			public Hope<Rep> apply(Req req) {
				return null;
			}
		};
	}
	public static <Req1, Req, Rep> Service<Req1, Rep> map(
			final Service<Req, Rep> service, final Function<Req1, Req> f) {
		return new SimpleService<Req1, Rep>() {
			@Override
			public Hope<Rep> apply(Req1 req) {
				return null;
			}
		};
	}

	
}
