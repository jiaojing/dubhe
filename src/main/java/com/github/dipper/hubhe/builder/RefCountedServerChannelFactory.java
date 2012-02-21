package com.github.dipper.hubhe.builder;

import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ServerChannel;
import org.jboss.netty.channel.ServerChannelFactory;

import com.github.dipper.hubhe.util.fp.Exp;

class RefCountedServerChannelFactory implements ServerChannelFactory {
	private final ServerChannelFactory underlying;
	private int refCount = 0;

	RefCountedServerChannelFactory(ServerChannelFactory underlying) {
		this.underlying = underlying;
	}

	/**
	 * 增加引用计数
	 */
	synchronized void acquire() {
		this.refCount += 1;
	}

	@Override
	public ServerChannel newChannel(ChannelPipeline pipeline) {
		return underlying.newChannel(pipeline);
	}

	@Override
	public synchronized void releaseExternalResources() {
		refCount -= 1;
		if (refCount == 0) {
			underlying.releaseExternalResources();
		}
	}

}

class LazyRevivableServerChannelFactory implements ServerChannelFactory {

	private final Exp<ServerChannelFactory> make;
	private volatile ServerChannelFactory underlying = null;

	LazyRevivableServerChannelFactory(Exp<ServerChannelFactory> make) {
		this.make = make;
	}

	@Override
	public ServerChannel newChannel(ChannelPipeline pipeline) {
		synchronized (this) {
			if (underlying == null) {
				underlying = make.apply();
			}
		}

		return underlying.newChannel(pipeline);
	}

	@Override
	public synchronized void releaseExternalResources() {
		Thread thread = null;
		if (underlying != null) {
			// releaseExternalResources must be called in a non-Netty
			// thread, otherwise it can lead to a deadlock
			final ChannelFactory _underlying = underlying;
			thread = new Thread() {
				@Override
				public void run() {
					_underlying.releaseExternalResources();
				}
			};
			thread.start();
			underlying = null;
		}
	}

}
