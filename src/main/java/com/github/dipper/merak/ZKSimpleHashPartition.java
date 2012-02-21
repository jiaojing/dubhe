package com.github.dipper.merak;

import java.util.List;

import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;

public class ZKSimpleHashPartition<T> implements Partition<T> {

	private int slice;

	private final ZkClient client;
	private final String serviceNode;

	ZKSimpleHashPartition(ZkClient client, String zkRoot, String service) {
		this.client = client;
		this.serviceNode = zkRoot + "/" + service;
		this.listen();
	}

	@Override
	public T nodeForHash(long hash) {
		return null;
	}

	@Override
	public int slice() {
		return this.slice;
	}

	// digger:4/[1|127.0.0.1:2345,1|127.0.0.1:4325]
	private void listen() {
		// 1.
		client.subscribeDataChanges(serviceNode, new IZkDataListener() {
			@Override
			public void handleDataChange(String dataPath, Object data)
					throws Exception {
			}

			@Override
			public void handleDataDeleted(String dataPath) throws Exception {

			}
		});

		// 分片数改了。
		client.subscribeChildChanges(serviceNode, new IZkChildListener() {
			@Override
			public void handleChildChange(String parentPath,
					List<String> currentChilds) throws Exception {

			}
		});

	}
}
