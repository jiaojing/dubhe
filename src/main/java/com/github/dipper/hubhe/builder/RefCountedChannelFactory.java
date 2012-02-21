package com.github.dipper.hubhe.builder;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;

import com.github.dipper.hubhe.util.fp.Exp;

class RefCountedChannelFactory implements ChannelFactory {
	private final ChannelFactory underlying;
	private int refCount = 0;

	RefCountedChannelFactory(ChannelFactory underlying) {
		this.underlying = underlying;
	}

	/**
	 * 增加引用计数
	 */
	synchronized void acquire() {
		this.refCount += 1;
	}

	@Override
	public Channel newChannel(ChannelPipeline pipeline) {
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


class LazyRevivableChannelFactory implements ChannelFactory {

	private final Exp<ChannelFactory> make;
	private volatile ChannelFactory underlying = null;

	LazyRevivableChannelFactory(Exp<ChannelFactory> make) {
		this.make = make;
	}

	@Override
	public Channel newChannel(ChannelPipeline pipeline) {
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