package com.github.dipper.hubhe.builder;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ServerChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import com.github.dipper.hubhe.protocol.Codec;
import com.github.dipper.hubhe.protocol.CodecFactory;
import com.github.dipper.hubhe.protocol.Request;
import com.github.dipper.hubhe.protocol.Response;
import com.github.dipper.hubhe.service.Service;
import com.github.dipper.hubhe.service.channel.ServiceToChannelHandler;
import com.github.dipper.hubhe.util.fp.Exp;
import com.google.common.collect.Maps;

public class ServerBuilder<Req extends Request, Rep extends Response> {

	public static <ReqA extends Request, RepA extends Response> ServerBuilder<ReqA, RepA> codec(
			Codec<ReqA, RepA> codec) {
		return new ServerBuilder<ReqA, RepA>(codec);
	}

	public static <ReqB extends Request, RepB extends Response> ServerBuilder<ReqB, RepB> codec(
			CodecFactory<ReqB, RepB> codecFactory) {
		return new ServerBuilder<ReqB, RepB>(codecFactory);
	}

	private ServerConfig<Req, Rep> config;

	private ServerBuilder(ServerConfig<Req, Rep> config) {
		this.config = config;
	}

	private ServerBuilder(Codec<Req, Rep> codec) {
		this.config = new ServerConfig<Req, Rep>();
		config.codec = codec;
	}

	private ServerBuilder(CodecFactory<Req, Rep> codecFactory) {
		this.config = new ServerConfig<Req, Rep>();
		config.codec = codecFactory.server();
	}

	public ServerBuilder<Req, Rep> name(String name) {
		return new ServerBuilder<Req, Rep>(this.config.copy(name));
	}

	public ServerBuilder<Req, Rep> option(String key, Object value) {
		return new ServerBuilder<Req, Rep>(this.config.copy(key, value));
	}

	public ServerBuilder<Req, Rep> bindTo(int port) {
		return bindTo(new InetSocketAddress(port));
	}

	public ServerBuilder<Req, Rep> bindTo(SocketAddress address) {
		return new ServerBuilder<Req, Rep>(this.config.copy(address));
	}

	public Server build(final Service<Req, Rep> service) {
		final ServerBootstrap bs = buildBootstrap();

		final ServiceToChannelHandler<Req, Rep> handler = new ServiceToChannelHandler<Req, Rep>(
				service);
		bs.setPipelineFactory(new ChannelPipelineFactory() {
			@Override
			public ChannelPipeline getPipeline() throws Exception {
				ChannelPipelineFactory codecFactory = config.codec
						.piplineFactory();
				ChannelPipeline pipline = codecFactory.getPipeline();
				pipline.addLast("dipper_server_handler", handler);
				return pipline;
			}
		});
		final Channel channel = bs.bind(config.address);

		return new Server() {
			@Override
			public void close() {
				service.release();
				channel.close().awaitUninterruptibly();
				bs.releaseExternalResources();
			}
		};
	}

	private ServerBootstrap buildBootstrap() {
		RefCountedServerChannelFactory cf = this.channelFactory;
		cf.acquire(); // 引用计数加1
		ServerBootstrap bs = new ServerBootstrap(cf);

		bs.setOption("reuseAddress", true);
		bs.setOption("backlog", 1000);
		bs.setOption("child.tcpNoDelay", true);
		// // bs.setOption("child.sendBufferSize", s);
		// // bs.setOption("child.receiveBufferSize", s);
		// 使用方自行设置设置为null，即为取消
		for (Map.Entry<String, Object> entry : config.options.entrySet()) {
			bs.setOption(entry.getKey(), entry.getValue());
		}
		return bs;
	}

	private RefCountedServerChannelFactory channelFactory = new RefCountedServerChannelFactory(
			new LazyRevivableServerChannelFactory(
					new Exp<ServerChannelFactory>() {
						@Override
						public ServerChannelFactory apply() {
							return new NioServerSocketChannelFactory(
									Executors.newCachedThreadPool(),
									Executors.newCachedThreadPool());
						}
					}));
}

class ServerConfig<Req, Rep> {
	String name;
	Codec<Req, Rep> codec;
	SocketAddress address;

	Map<String, Object> options = Maps.newHashMap();

	ServerConfig<Req, Rep> copy() {
		ServerConfig<Req, Rep> config = new ServerConfig<Req, Rep>();
		config.name = this.name;
		config.codec = this.codec;
		config.address = this.address;
		config.options = Maps.newHashMap(this.options);
		return config;
	}

	ServerConfig<Req, Rep> copy(String name) {
		ServerConfig<Req, Rep> config = copy();
		config.name = name;
		return config;
	}

	ServerConfig<Req, Rep> copy(SocketAddress address) {
		ServerConfig<Req, Rep> config = copy();
		config.address = address;
		return config;
	}

	ServerConfig<Req, Rep> copy(String key, Object value) {
		ServerConfig<Req, Rep> config = copy();
		config.options.put(key, value);
		return config;
	}

}
