package com.github.dipper.hubhe.service;

import com.github.dipper.hubhe.util.Hope;
import com.github.dipper.hubhe.util.fp.Def2;

//           (*  MyService  *)
// [ReqIn -> (ReqOut -> RepIn) -> RepOut]

public abstract class Filter<ReqIn, RepOut, ReqOut, RepIn> implements
		Def2<ReqIn, Service<ReqOut, RepIn>, Hope<RepOut>> {

	public abstract Hope<RepOut> apply(ReqIn reqin,
			Service<ReqOut, RepIn> service);

	public <Req2, Rep2> Filter<ReqIn, RepOut, Req2, Rep2> then(
			final Filter<ReqOut, RepIn, Req2, Rep2> next) {
		return new Filter<ReqIn, RepOut, Req2, Rep2>() {
			@Override
			public Hope<RepOut> apply(final ReqIn reqin,
					final Service<Req2, Rep2> service) {
				return Filter.this.apply(reqin, new SimpleService<ReqOut, RepIn>() {
					@Override
					public Hope<RepIn> apply(ReqOut reqOut) {
						return next.apply(reqOut, service);
					}
				});
			}
		};
	}

	public static void main(String[] args) {
		Service<String, Integer> service = new SimpleService<String, Integer>() {
			@Override
			public Hope<Integer> apply(String input) {
				System.out.println("service invoke");
				return null;
			}
		};
		Filter<Long, Long, String, Integer> filter = new Filter<Long, Long, String, Integer>() {
			@Override
			public Hope<Long> apply(Long reqin,
					Service<String, Integer> service) {
				System.out.println("Long -> string");
				service.apply("1");
				System.out.println("integer -> Long");
				return null;
			}
		};
		filter.then(null);
	}

}
