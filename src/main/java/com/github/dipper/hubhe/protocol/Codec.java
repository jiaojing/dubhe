package com.github.dipper.hubhe.protocol;

import org.jboss.netty.channel.ChannelPipelineFactory;

public interface Codec<Req, Rep> {

	ChannelPipelineFactory piplineFactory();
}
