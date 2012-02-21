package com.github.dipper.hubhe.util;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import com.github.dipper.hubhe.util.Hope;
import com.github.dipper.hubhe.util.Promise;
import com.github.dipper.hubhe.util.Hope.Reduce;
import com.github.dipper.hubhe.util.fp.Def;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

public class HopeTest {

	ExecutorService service = Executors.newCachedThreadPool();

	@Test
	public void test_all_succ() {
		List<Promise<Long>> hopes = _1000Hope();

		long sum = 0;
		for (final Promise<Long> hope : hopes) {
			service.submit(new Runnable() {
				@Override
				public void run() {
					hope.set(10L);
				}
			});
			sum += 10;
		}

		Hope<Long> lp = Hope.collect(Lists.transform(hopes,
				new Function<Promise<Long>, Hope<Long>>() {
					@Override
					public Hope<Long> apply(Promise<Long> promise) {
						return promise;
					}
				}), 0L, new Reduce<Long, Long>() {
			@Override
			public Long apply(Long accu, Long now) {
				return accu + now;
			}
		});
		long vs = lp.get(2, TimeUnit.SECONDS);
		System.out.println(vs);
		Assert.assertEquals(sum, vs);
	}

	@Test
	public void test_partial_succ() {
		List<Promise<Long>> hopes = _1000Hope();

		long sum = 0;
		for (final Promise<Long> hope : hopes) {
			service.submit(new Runnable() {
				@Override
				public void run() {
					hope.set(10L);
				}
			});
			sum += 10;
			if (sum == 5000) {
				break;
			}
		}

		Hope<Long> lp = Hope.collect(Lists.transform(hopes,
				new Function<Promise<Long>, Hope<Long>>() {
					@Override
					public Hope<Long> apply(Promise<Long> promise) {
						return promise;
					}
				}), 0L, new Reduce<Long, Long>() {
			@Override
			public Long apply(Long accu, Long now) {
				return accu + now;
			}
		});
		long vs = lp.get(2, TimeUnit.SECONDS);
		System.out.println(vs);
		Assert.assertEquals(sum, vs);
	}

	private List<Promise<Long>> _1000Hope() {
		List<Promise<Long>> hopes = Lists.newArrayList();

		for (long i = 1L; i <= 1000L; i++) {
			final Promise<Long> promise = Hope.create();
			hopes.add(promise);
		}
		return hopes;
	}

	@Test
	public void test_partial_fail() {
		List<Promise<Long>> hopes = _1000Hope();
		long sum = 0;
		for (final Promise<Long> promise : hopes) {
			service.submit(new Runnable() {
				@Override
				public void run() {
					promise.set(10L);
				}
			});
			sum += 10;
			if (sum == 5000) {
				break;
			}
		}

		Hope<Long> lp = Hope.join(Lists.transform(hopes,
				new Function<Promise<Long>, Hope<Long>>() {
					@Override
					public Hope<Long> apply(Promise<Long> promise) {
						return promise;
					}
				}), 0L, new Reduce<Long, Long>() {
			@Override
			public Long apply(Long accu, Long now) {
				return accu + now;
			}
		});
		Assert.assertNull(lp.get(2, TimeUnit.SECONDS));
	}

	public static void main(String[] args) {
		Promise<Long> hope = Hope.create();
		hope.set(10L);
		Hope<String> two = hope.map(new Def<Long, String>() {
			@Override
			public String apply(Long input) {
				return String.valueOf(input);
			}
		});
		System.out.println(two.get());
		System.out.println(two.get());
	}

}
