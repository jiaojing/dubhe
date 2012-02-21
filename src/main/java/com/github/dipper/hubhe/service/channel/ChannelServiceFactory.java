package com.github.dipper.hubhe.service.channel;

import java.net.SocketAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dipper.hubhe.protocol.Request;
import com.github.dipper.hubhe.protocol.Response;
import com.github.dipper.hubhe.service.NilService;
import com.github.dipper.hubhe.service.Service;
import com.github.dipper.hubhe.service.ServiceFactory;

/**
 * 维持ChannelService的实例。
 * 
 * @author tony
 * 
 * @param <Req>
 * @param <Rep>
 */
public class ChannelServiceFactory<Req extends Request, Rep extends Response>
		extends ServiceFactory<Req, Rep> {

	private static final Logger log = LoggerFactory
			.getLogger(ChannelService.class);

	private final AtomicBoolean available = new AtomicBoolean(true);
	
	private final ClientBootstrap bootstrap;

	private final SocketAddress host;

	private final AtomicReference<FutureTask<Service<Req, Rep>>> singleton = new AtomicReference<FutureTask<Service<Req, Rep>>>();

	public ChannelServiceFactory(ClientBootstrap bootstrap, SocketAddress host) {
		this.bootstrap = bootstrap;
		this.host = host;
	}

	@Override
	public Service<Req, Rep> make() {
		Service<Req, Rep> service = null;
		FutureTask<Service<Req, Rep>> task = singleton.get();
		// 没初始化或者因为失败已移除
		if (task == null) {
			FutureTask<Service<Req, Rep>> newtask = lazyConnect();
			// 如果没有被其他线程设置
			if (singleton.compareAndSet(null, newtask)) {
				// 进行connect，最多会阻塞2秒
				newtask.run();
			}
			service = make();// task已经跑过，再次调用就会进入下一个分支。
		} else {
			try {
				// 短暂等待.刚开始时可能会有请求没法处理。
				// 因为链接没有建立。可在启动时建立链接来规避。
				service = task.get(1, TimeUnit.MILLISECONDS);
			} catch (TimeoutException toe) {
				// 只是本次请求超时了而已
				log.error("just timeout");
			} catch (ExecutionException ee) {
				// 执行出错。设置为null。等待下次初始化
				singleton.compareAndSet(task, null);
				log.error("connect to server fail", ee);
			} catch (InterruptedException ie) {
				log.error("task.get has been interrupted", ie);
			}
			// 如果取到了，但不可用
			if (service != null && !service.isAvaliable()) {
				service = null;
				singleton.compareAndSet(task, null); // 清空异步建立链接，等待下次建立。
			}
		}
		return (service != null) ? service : NilService.<Req, Rep> instance();
	}

	private FutureTask<Service<Req, Rep>> lazyConnect() {
		return new FutureTask<Service<Req, Rep>>(
				new Callable<Service<Req, Rep>>() {
					@Override
					@SuppressWarnings("unchecked")
					public Service<Req, Rep> call() throws Exception {
						final CountDownLatch latch = new CountDownLatch(1);
						final ChannelFutureListener listener = new ChannelFutureListener() {
							@Override
							public void operationComplete(ChannelFuture future)
									throws Exception {
								latch.countDown();
							}
						};

						ChannelFuture future = bootstrap.connect(host);
						future.addListener(listener);

						ChannelService<Req, Rep> service = null;
						// 建立链接，默认等待2秒
						if (!latch.await(2, TimeUnit.SECONDS)) {
							future.removeListener(listener);
							future.cancel();
							throw new ExecutionException(new TimeoutException(
									"创建链接2秒超时"));
						} else {
							Channel channel = future.getChannel();
							service = channel.getPipeline().get(
									ChannelService.class);
							service.setChannel(channel);
						}
						return service;
					}
				});
	}

	@Override
	public void close() {
		FutureTask<Service<Req, Rep>> task = singleton.get();
		if (task != null) {
			if (task.isDone()) {
				try {
					Service<Req, Rep> service = task.get();
					service.release();
				} catch (Exception e) {
					log.error("service release fail", e);
				}
			} else {
				task.cancel(true);
			}
		}
		bootstrap.releaseExternalResources();
		available.compareAndSet(true, false);
	}

	@Override
	public boolean isAvailable() {
		return available.get();
	}

}
