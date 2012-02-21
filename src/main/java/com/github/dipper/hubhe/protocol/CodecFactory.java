package com.github.dipper.hubhe.protocol;


public interface CodecFactory<Req, Rep> {

	Codec<Req, Rep> client();

	Codec<Req, Rep> server();
}
