package com.github.dipper.hubhe.builder;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;

import com.github.dipper.hubhe.service.ServiceFactory;
import com.github.dipper.hubhe.util.fp.Def;

public interface Cluster {

	public <Req, Rep> List<ServiceFactory<Req, Rep>> mkFactories(
			Def<SocketAddress, ServiceFactory<Req, Rep>> def);

	public void join(InetSocketAddress address);

}
