package com.github.dipper.hubhe.service.channel;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dipper.hubhe.service.Service;
import com.github.dipper.hubhe.util.Hope;

public class ServiceToChannelHandler<Req, Rep> extends
		SimpleChannelUpstreamHandler {

	private static final Logger log = LoggerFactory
			.getLogger(ServiceToChannelHandler.class);

	private final Service<Req, Rep> service;

	public ServiceToChannelHandler(Service<Req, Rep> service) {
		this.service = service;
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
		Channel channel = e.getChannel();
		// 客户端因为各种原因,已经断开连接,那就不处理了.
		if (!channel.isConnected()) {
			log.error("客户端已主动断开连接");
			return;
		}
		@SuppressWarnings("unchecked")
		Req req = (Req) e.getMessage();
		Hope<Rep> hope = service.apply(req);
		if (channel.isConnected()) { // 连接中,发送response
			Rep rep = hope.get();
			e.getChannel().write(rep);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
		log.error("server handle 捕获了未知异常,关闭连接.{}", e);
		e.getChannel().close();
	}

}
