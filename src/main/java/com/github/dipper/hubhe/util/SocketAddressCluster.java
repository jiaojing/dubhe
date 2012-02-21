package com.github.dipper.hubhe.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;

import com.github.dipper.hubhe.builder.Cluster;
import com.github.dipper.hubhe.service.ServiceFactory;
import com.github.dipper.hubhe.util.fp.Def;
import com.google.common.collect.Lists;

public class SocketAddressCluster implements Cluster {

	private final List<SocketAddress> underlying;

	public SocketAddressCluster(List<SocketAddress> underlying) {
		this.underlying = checkNotNull(underlying);
	}

	@Override
	public <Req, Rep> List<ServiceFactory<Req, Rep>> mkFactories(
			Def<SocketAddress, ServiceFactory<Req, Rep>> def) {
		List<ServiceFactory<Req, Rep>> factories = Lists.newArrayList();

		for (SocketAddress address : underlying) {
			factories.add(def.apply(address));
		}
		return factories;
	}

	@Override
	public void join(InetSocketAddress address) {
		underlying.add(address);
	}

}
