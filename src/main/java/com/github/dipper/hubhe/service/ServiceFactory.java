package com.github.dipper.hubhe.service;

public abstract class ServiceFactory<Req, Rep> {

	/**
	 * 创建服务实例
	 * @return 
	 */
	public abstract Service<Req, Rep> make();

	public abstract void close();

	public boolean isAvailable() {
		return true;
	}

}
