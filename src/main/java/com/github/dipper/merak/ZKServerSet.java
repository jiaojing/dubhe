package com.github.dipper.merak;

import org.I0Itec.zkclient.ZkClient;

public class ZKServerSet implements ServerSet{

	private final ZkClient client;
	private final String servicePath;

	public ZKServerSet(ZkClient client, String zkRoot, String service) {
		this.client = client;
		this.servicePath = zkRoot + "/" + service;
	}

	@Override
	public void join(int slice, String host, int port) {
		String sliceNode = sliceNode(slice);
		if (!client.exists(sliceNode)) {
			client.createPersistent(sliceNode, true);
		}
		client.createEphemeral(serviceNode(slice, host, port));
	}

	@Override
	public void leave(int slice, String host, int port) {
		client.delete(serviceNode(slice, host, port));
	}

	private String sliceNode(int slice) {
		return servicePath + "/" + slice;
	}

	private String serviceNode(int slice, String host, int port) {
		return servicePath + "/" + slice + "/" + host + ":" + port;
	}
}
