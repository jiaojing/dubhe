package com.github.dipper.merak;

public interface ServerSet {

	public void join(int slice, String host, int port);

	public void leave(int slice, String host, int port);
}