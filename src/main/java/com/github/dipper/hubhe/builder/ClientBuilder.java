package com.github.dipper.hubhe.builder;

import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;

import com.github.dipper.hubhe.protocol.Codec;
import com.github.dipper.hubhe.protocol.CodecFactory;
import com.github.dipper.hubhe.protocol.Request;
import com.github.dipper.hubhe.protocol.Response;
import com.github.dipper.hubhe.service.ClusterService;
import com.github.dipper.hubhe.service.LoadBalancedCluster;
import com.github.dipper.hubhe.service.Service;
import com.github.dipper.hubhe.service.ServiceFactory;
import com.github.dipper.hubhe.service.channel.ChannelService;
import com.github.dipper.hubhe.service.channel.ChannelServiceFactory;
import com.github.dipper.hubhe.util.SocketAddressCluster;
import com.github.dipper.hubhe.util.SocketAddressUtil;
import com.github.dipper.hubhe.util.fp.Def;
import com.github.dipper.hubhe.util.fp.Exp;
import com.google.common.collect.Maps;

public class ClientBuilder<Req extends Request, Rep extends Response> {

	public static <ReqA extends Request, RepA extends Response> ClientBuilder<ReqA, RepA> codec(
			Codec<ReqA, RepA> codec) {
		return new ClientBuilder<ReqA, RepA>(codec);
	}

	public static <ReqB extends Request, RepB extends Response> ClientBuilder<ReqB, RepB> codec(
			CodecFactory<ReqB, RepB> codecFactory) {
		return new ClientBuilder<ReqB, RepB>(codecFactory);
	}

	private ClientConfig<Req, Rep> config;

	private ClientBuilder(ClientConfig<Req, Rep> config) {
		this.config = config;
	}

	private ClientBuilder(Codec<Req, Rep> codec) {
		this.config = new ClientConfig<Req, Rep>();
		config.codec = codec;
	}

	private ClientBuilder(CodecFactory<Req, Rep> codecFactory) {
		this.config = new ClientConfig<Req, Rep>();
		config.codec = codecFactory.client();
	}

	public ClientBuilder<Req, Rep> hosts(String hosts) {
		return hosts(SocketAddressUtil.parseHost(hosts));
	}

	public ClientBuilder<Req, Rep> hosts(List<SocketAddress> addresses) {
		return cluster(new SocketAddressCluster(addresses));
	}

	public ClientBuilder<Req, Rep> cluster(Cluster cluster) {
		return new ClientBuilder<Req, Rep>(this.config.copy(cluster));
	}

	public ClientBuilder<Req, Rep> name(String name) {
		return new ClientBuilder<Req, Rep>(this.config.copy(name));
	}

	public ClientBuilder<Req, Rep> option(String key, Object value) {
		return new ClientBuilder<Req, Rep>(this.config.copy(key, value));
	}

	public ClusterService<Req,Rep> build() {
		List<ServiceFactory<Req, Rep>> factories = config.cluster
				.mkFactories(new Def<SocketAddress, ServiceFactory<Req, Rep>>() {
					@Override
					public ServiceFactory<Req, Rep> apply(SocketAddress host) {
						ClientBootstrap bootstrap = buildBootstrap(
								config.codec, host);
						return new ChannelServiceFactory<Req, Rep>(bootstrap,
								host);
					}
				});

		return new LoadBalancedCluster<Req, Rep>(factories);
	}

	private ClientBootstrap buildBootstrap(final Codec<Req, Rep> codec,
			SocketAddress host) {
		RefCountedChannelFactory cf = this.channelFactory;
		cf.acquire(); // 引用计数加1
		ClientBootstrap bs = new ClientBootstrap(cf);

		bs.setPipelineFactory(new ChannelPipelineFactory() {
			@Override
			public ChannelPipeline getPipeline() throws Exception {
				ChannelPipelineFactory codecFactory = config.codec
						.piplineFactory();
				ChannelPipeline pipline = codecFactory.getPipeline();
				pipline.addLast("dipper_client_handler",
						new ChannelService<Req, Rep>());
				return pipline;
			}
		});
		bs.setOption("remoteAddress", host);
		bs.setOption("tcpNoDelay", true);
		bs.setOption("reuseAddress", true);
		// bs.setOption("sendBufferSize", s);
		// bs.setOption("receiveBufferSize", s);
		// 使用方自行设置设置为null，即为取消
		for (Map.Entry<String, Object> entry : config.options.entrySet()) {
			bs.setOption(entry.getKey(), entry.getValue());
		}
		return bs;
	}

	private RefCountedChannelFactory channelFactory = new RefCountedChannelFactory(
			new LazyRevivableChannelFactory(new Exp<ChannelFactory>() {
				@Override
				public ChannelFactory apply() {
					return new NioClientSocketChannelFactory(
							Executors.newCachedThreadPool(),
							Executors.newCachedThreadPool());
				}
			}));
}

class ClientConfig<Req, Rep> {
	String name;
	Cluster cluster;
	Codec<Req, Rep> codec;

	Map<String, Object> options = Maps.newHashMap();

	ClientConfig<Req, Rep> copy() {
		ClientConfig<Req, Rep> config = new ClientConfig<Req, Rep>();
		config.name = this.name;
		config.cluster = this.cluster;
		config.codec = this.codec;
		config.options = Maps.newHashMap(this.options);
		return config;
	}

	ClientConfig<Req, Rep> copy(String name) {
		ClientConfig<Req, Rep> config = copy();
		config.name = name;
		return config;
	}

	ClientConfig<Req, Rep> copy(Cluster cluster) {
		ClientConfig<Req, Rep> config = copy();
		config.cluster = cluster;
		return config;
	}

	ClientConfig<Req, Rep> copy(String key, Object value) {
		ClientConfig<Req, Rep> config = copy();
		config.options.put(key, value);
		return config;
	}

}
