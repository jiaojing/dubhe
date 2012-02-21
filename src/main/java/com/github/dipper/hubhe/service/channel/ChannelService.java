package com.github.dipper.hubhe.service.channel;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dipper.hubhe.protocol.Request;
import com.github.dipper.hubhe.protocol.Response;
import com.github.dipper.hubhe.service.Service;
import com.github.dipper.hubhe.util.Hope;
import com.github.dipper.hubhe.util.Promise;
import com.google.common.collect.MapMaker;

public class ChannelService<Req extends Request, Rep extends Response> extends
		SimpleChannelUpstreamHandler implements Service<Req, Rep> {

	private static final Logger log = LoggerFactory
			.getLogger(ChannelService.class);

	/**
	 * 异步回调集合。默认20秒就会自动remove掉。
	 */
	private final ConcurrentMap<Integer, Promise<Rep>> callbacks = new MapMaker()
			.expireAfterWrite(20, TimeUnit.SECONDS).makeMap();

	private Channel channel;

	private final AtomicBoolean avaliable;

	public ChannelService() {
		this.avaliable = new AtomicBoolean(true);
	}

	public ChannelService<Req, Rep> setChannel(Channel channel) {
		this.channel = channel;
		this.avaliable.set(true);
		return this;
	}

	@Override
	public Hope<Rep> apply(Req req) {
		Promise<Rep> hope = Hope.create();
		callbacks.putIfAbsent(req.rid(), hope);
		// 也许可以提供一个cancle接口
		if (channel.isConnected()) {
			channel.write(req);
		} else {
			this.release();
		}
		return hope;
	}

	@Override
	public boolean isAvaliable() {
		return this.avaliable.get();
	}

	@Override
	public void release() {
		this.channel.close().awaitUninterruptibly();
		this.avaliable.compareAndSet(true, false);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
		log.error("channelService exceptionCaught() has been invoked.. channel closed.");
		this.release();
	}

	/**
	 * 收到服务器返回数据,进行callback
	 */
	@Override
	public void messageReceived(ChannelHandlerContext ctx, final MessageEvent e) {
		@SuppressWarnings("unchecked")
		Rep rep = (Rep) e.getMessage();
		Promise<Rep> promise = callbacks.get(rep.rid());
		if (promise != null) {
			promise.set(rep);
		}
	}
}
