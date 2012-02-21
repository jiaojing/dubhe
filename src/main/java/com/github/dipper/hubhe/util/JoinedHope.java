package com.github.dipper.hubhe.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import com.github.dipper.hubhe.util.fp.Block;

class JoinedHope<V, W> extends Promise<V> implements Block<W> {

	private final Reduce<V, W> reduce;

	protected V semiValue;

	/**
	 * 
	 * @param promises
	 * @param initialValue
	 *            . 为累加初始值。
	 * @param reduce
	 */
	public JoinedHope(List<Hope<W>> hopes, final V initialValue,
			final Reduce<V, W> reduce) {
		super(hopes.size());

		this.reduce = checkNotNull(reduce);
		this.semiValue = checkNotNull(initialValue);

		for (Hope<W> hope : hopes) {
			hope.linkTo(this);
		}
	}

	public synchronized Void apply(W p) {
		try {
			this.semiValue = reduce.apply(semiValue, p);
		} finally {
			set(semiValue);
		}
		return Unit;
	};
}