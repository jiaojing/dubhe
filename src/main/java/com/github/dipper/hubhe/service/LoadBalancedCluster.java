package com.github.dipper.hubhe.service;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.dipper.hubhe.util.Hope;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public class LoadBalancedCluster<Req, Rep> implements ClusterService<Req, Rep> {

	private static AtomicInteger incr = new AtomicInteger(0);

	private static int position(int max) {
		Preconditions.checkArgument(max > 0);

		int num = Math.abs(incr.incrementAndGet());
		return num % max;
	}

	private final List<ServiceFactory<Req, Rep>> factories;

	private final SimpleService<Req, List<Rep>> all = new SimpleService<Req, List<Rep>>() {
		@Override
		public Hope<List<Rep>> apply(Req req) {
			List<Hope<Rep>> hopes = Lists.newArrayList();
			for (ServiceFactory<Req, Rep> factory : factories) {
				hopes.add(factory.make().apply(req));
			}
			return Hope.join(hopes);
		}
	};

	public LoadBalancedCluster(List<ServiceFactory<Req, Rep>> factories) {
		this.factories = checkNotNull(factories);
	}

	@Override
	public void close() {
		for (ServiceFactory<Req, Rep> factory : factories) {
			factory.close();
		}
	}

	@Override
	public Service<Req, Rep> someone() {
		int size = this.factories.size();
		if (size == 0) {
			return NilService.instance();
		} else {
			int position = position(size);
			ServiceFactory<Req, Rep> factory = this.factories.get(position);
			return factory.make();
		}
	}

	@Override
	public Service<Req, List<Rep>> all() {
		return all;
	}
}
