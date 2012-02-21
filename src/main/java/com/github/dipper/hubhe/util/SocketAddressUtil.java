package com.github.dipper.hubhe.util;

import static com.google.common.base.CharMatcher.is;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

public class SocketAddressUtil {
	private static Splitter splitter = Splitter
			.on(CharMatcher.WHITESPACE.or(is(',')).or(is(';'))).trimResults()
			.omitEmptyStrings();

	public static List<SocketAddress> parseHost(String hosts) {
		List<SocketAddress> address = Lists.newArrayList();
		Iterable<String> hostStrs = splitter.split(hosts);
		for (String host : hostStrs) {
			String[] hostPort = host.split(":");
			if (hostPort.length != 2) {
				break;
			}
			address.add(new InetSocketAddress(hostPort[0], Integer
					.valueOf(hostPort[1])));
		}
		return address;
	}

	public static void main(String[] args) {
		Iterable<String> hostStrs = splitter.split("1:2   3:4, 5:6;;;; 7:8 ");
		for (String host : hostStrs) {
			System.out.println(host);
		}
	}
}
