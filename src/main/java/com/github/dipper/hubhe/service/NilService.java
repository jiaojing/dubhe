package com.github.dipper.hubhe.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dipper.hubhe.service.channel.ChannelService;
import com.github.dipper.hubhe.util.Hope;
import com.github.dipper.hubhe.util.Promise;

public class NilService<Req, Rep> extends SimpleService<Req, Rep> {

	@SuppressWarnings("rawtypes")
	private static final NilService instance = new NilService();

	@SuppressWarnings("unchecked")
	public static <Req1, Rep1> NilService<Req1, Rep1> instance() {
		return instance;
	}

	private static final Logger log = LoggerFactory
			.getLogger(ChannelService.class);

	private NilService() {
	}

	@Override
	public Hope<Rep> apply(Req req) {
		log.error("fall in nilService...");
		Promise<Rep> hope = Hope.create();
		hope.set(null);
		return hope;
	}

}
