package com.liang.util.cache;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

/**
 * @Description 使用LFU（最不经常使用）淘汰策略的Cache
 * @author wuyiliang
 * @Date 2016年5月8日 下午9:37:42
 */
public class LFUCache<K, V> extends AbstractCache<K, V> {

	public LFUCache() {
		this(1000, 0);
	}

	public LFUCache(int maxCapacity, int timeToLiveSeconds) {
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
		map = new LinkedHashMap<K, Element>(capacity, 0.75f, true);
	}

	protected void eliminate() {
		Iterator<Entry<K, Element>> it = map.entrySet().iterator();
		Element min = null;
		if (it.hasNext()) {
			min = it.next().getValue();
		}
		while (it.hasNext()) {
			Element e = it.next().getValue();
			if (e.accessCount < min.accessCount || (e.accessCount == min.accessCount && e.lastAccess < min.lastAccess)) {
				min = e;
			}
		}
		map.remove(min.k);
	}

}
