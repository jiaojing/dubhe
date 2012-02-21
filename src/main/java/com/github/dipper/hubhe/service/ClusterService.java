package com.github.dipper.hubhe.service;

import java.util.List;

public interface ClusterService<Req,Rep> {
	
	/**从集群中取得一个服务
	 * @return
	 */
	public Service<Req,Rep> someone();
	
	/**
	 * 取得所有服务
	 * @return
	 */
	public Service<Req,List<Rep>> all();
	
	/**
	 * 关闭集群链接
	 */
	public void close();
	
}
