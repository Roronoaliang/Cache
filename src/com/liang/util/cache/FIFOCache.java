package com.liang.util.cache;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

/**
 * @Description FIFO先进先出策略实现
 * @author wuyiliang
 * @Date 2016年5月8日 下午10:31:45
 */
public class FIFOCache<K, V> extends AbstractCache<K, V> {

	public FIFOCache() {
		this(1400, 0);
	}

	public FIFOCache(int maxCapacity, int timeToLiveSeconds) {
		if (maxCapacity <= 0) {
			throw new IllegalArgumentException("Illegal maxCapacity: "
					+ maxCapacity);
		}
		if (timeToLiveSeconds < 0) {
			throw new IllegalArgumentException("Illegal timeToLiveSeconds: "
					+ timeToLiveSeconds);
		}
		this.capacity = maxCapacity;
		this.timeToLiveSeconds = timeToLiveSeconds;
		int capacity = 1;
		while (capacity < maxCapacity) {
			capacity <<= 1;
		}
		map = new LinkedHashMap<K, Element>(capacity, 0.75f);
	}

	@Override
	protected void eliminate() {
		Iterator<Entry<K, Element>> it = map.entrySet().iterator();
		it.next();
		it.remove();
	}

}
