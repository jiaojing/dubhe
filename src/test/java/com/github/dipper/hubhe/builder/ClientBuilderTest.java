package com.github.dipper.hubhe.builder;

import static org.jboss.netty.buffer.ChannelBuffers.copiedBuffer;
import static org.jboss.netty.channel.Channels.pipeline;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.Delimiters;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;
import org.junit.Test;

import com.github.dipper.hubhe.builder.ClientBuilder;
import com.github.dipper.hubhe.builder.Server;
import com.github.dipper.hubhe.builder.ServerBuilder;
import com.github.dipper.hubhe.protocol.Codec;
import com.github.dipper.hubhe.protocol.CodecFactory;
import com.github.dipper.hubhe.protocol.Request;
import com.github.dipper.hubhe.protocol.Response;
import com.github.dipper.hubhe.service.ClusterService;
import com.github.dipper.hubhe.service.Service;
import com.github.dipper.hubhe.service.SimpleService;
import com.github.dipper.hubhe.util.Hope;
import com.github.dipper.hubhe.util.fp.Def;
import com.google.common.base.Charsets;

public class ClientBuilderTest {

	private static final ExecutorService executor = Executors
			.newCachedThreadPool();

	public static final AtomicInteger ridGen = new AtomicInteger(0);

	public static int rid() {
		return ridGen.getAndIncrement();
	}
	
	@Test
	public void test() throws Exception{
		TestCodecFactory codecFactory = new TestCodecFactory();

		Server server = ServerBuilder.codec(codecFactory).name("server")
				.bindTo(10086).build(new SimpleService<StrReq, StrRep>() {
					@Override
					public Hope<StrRep> apply(StrReq req) {
						int rid = req.rid();
						return Hope.<StrRep> create().set(
								new StrRep(rid, rid + 1));
					}
				});

		final ClusterService<StrReq, StrRep> cluster = ClientBuilder
				.codec(codecFactory).name("client")
				.hosts("127.0.0.1:10086;127.0.0.1:10086,127.0.0.1:10086")
				.build();

		final Service<StrReq, List<StrRep>> all = cluster.all();

		for (int i = 0; i < 100; i++) {
			executor.submit(new Runnable() {
				@Override
				public void run() {
					for (int j = 0; j < 100; j++) {
						int rid = rid();
						try {
							Hope<StrRep> rep = all.apply(new StrReq(rid)).map(
									new Def<List<StrRep>, StrRep>() {
										@Override
										public StrRep apply(List<StrRep> input) {
											return input.get(0);
										}
									});
							StrRep str = rep.get();
							System.out.println(str);
							Assert.assertEquals(str.rid()+1, str.status());
							
						} catch (Exception e) {
							System.out.println(e);
						}
					}
				}
			});
		}

		executor.shutdown();
		executor.awaitTermination(10, TimeUnit.HOURS);
		cluster.close();
		server.close();
	}

}

class TestCodecFactory implements CodecFactory<StrReq, StrRep> {

	@Override
	public Codec<StrReq, StrRep> client() {
		return new Codec<StrReq, StrRep>() {
			private final ChannelPipelineFactory piplineFactory = new ChannelPipelineFactory() {
				@Override
				public ChannelPipeline getPipeline() throws Exception {
					ChannelPipeline p = pipeline();
					p.addLast("framer", new DelimiterBasedFrameDecoder(8192,
							Delimiters.lineDelimiter()));
					p.addLast("decoder", new StrRepDecoder());
					p.addLast("encoder", new StrReqEncoder());
					return p;
				}
			};

			@Override
			public ChannelPipelineFactory piplineFactory() {
				return piplineFactory;
			}
		};
	}

	@Override
	public Codec<StrReq, StrRep> server() {
		return new Codec<StrReq, StrRep>() {
			private final ChannelPipelineFactory piplineFactory = new ChannelPipelineFactory() {
				@Override
				public ChannelPipeline getPipeline() throws Exception {
					ChannelPipeline p = pipeline();
					p.addLast("framer", new DelimiterBasedFrameDecoder(8192,
							Delimiters.lineDelimiter()));
					p.addLast("decoder", new StrReqDecoder());
					p.addLast("encoder", new StrRepEncoder());
					return p;
				}
			};

			@Override
			public ChannelPipelineFactory piplineFactory() {
				return piplineFactory;
			}
		};
	}

}

class StrReq extends Request {

	private final int rid;

	StrReq(int rid) {
		this.rid = rid;
	}

	@Override
	public int rid() {
		return this.rid;
	}

	@Override
	public String toString() {
		return rid() + "\r\n";
	}
}

class StrRep extends Response {

	private final int rid;
	private final int status;

	StrRep(int rid, int status) {
		this.rid = rid;
		this.status = status;
	}

	@Override
	public int rid() {
		return rid;
	}

	@Override
	public int status() {
		return this.status;
	}

	@Override
	public String toString() {
		return rid + "|" + status + "\r\n";
	}
}

class StrReqEncoder extends OneToOneEncoder {

	@Override
	protected Object encode(ChannelHandlerContext ctx, Channel channel,
			Object msg) throws Exception {
		if (!(msg instanceof StrReq)) {
			return null;
		} else {
			return copiedBuffer(((StrReq) msg).toString(), Charsets.UTF_8);
		}
	}
}

class StrReqDecoder extends OneToOneDecoder {

	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel,
			Object msg) throws Exception {
		if (!(msg instanceof ChannelBuffer)) {
			return msg;
		}
		String str = ((ChannelBuffer) msg).toString(Charsets.UTF_8);
		return new StrReq(Integer.valueOf(str));
	}

}

class StrRepEncoder extends OneToOneEncoder {

	@Override
	protected Object encode(ChannelHandlerContext ctx, Channel channel,
			Object msg) throws Exception {
		if (!(msg instanceof StrRep)) {
			return null;
		} else {
			return copiedBuffer(((StrRep) msg).toString(), Charsets.UTF_8);
		}
	}
}

class StrRepDecoder extends OneToOneDecoder {

	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel,
			Object msg) throws Exception {
		if (!(msg instanceof ChannelBuffer)) {
			return msg;
		}
		String str = ((ChannelBuffer) msg).toString(Charsets.UTF_8);
		String[] ridAndStatus = str.split("\\|");
		return new StrRep(Integer.valueOf(ridAndStatus[0]),
				Integer.valueOf(ridAndStatus[1]));
	}

}
