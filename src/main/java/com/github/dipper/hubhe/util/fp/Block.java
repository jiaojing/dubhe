package com.github.dipper.hubhe.util.fp;

public interface Block<P> extends Lambda<P, Void> {
	public Void apply(P p);
}
