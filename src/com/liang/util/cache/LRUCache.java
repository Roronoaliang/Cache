package com.liang.util.cache;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @Description 使用LRU(最近最少使用)淘汰策略的Cache
 * @author wuyiliang
 * @Date 2015年12月9日 上午11:10:14
 */
public class LRUCache<K, V> extends AbstractCache<K, V> {

	public LRUCache() {
		this(1000, 0);
	}

	public LRUCache(int maxCapacity, int timeToLiveSeconds) {
		if (maxCapacity <= 0) {
			throw new IllegalArgumentException("Illegal maxCapacity: " + maxCapacity);
		}
		if (timeToLiveSeconds < 0) {
			throw new IllegalArgumentException("Illegal timeToLiveSeconds: "
					+ timeToLiveSeconds);
		}
		this.capacity = maxCapacity; //缓存池能保存的最大记录数
		this.timeToLiveSeconds = timeToLiveSeconds;
		int capacity = 1;
		while(capacity < maxCapacity) { //实际的LinkedHashMap中的数组长度,总是比缓存池容量大且是2的倍数，避免发生再哈希操作
			capacity <<= 1;
		}
		map = new LinkedHashMap<K, Element>(capacity, 0.75f, true) {

			private static final long serialVersionUID = 304088992010122618L;

			@Override
			protected boolean removeEldestEntry(Map.Entry<K, Element> eldest) {
				return size() > getCapacity();
			}
		};
	}

	@Override
	protected void eliminate() {
		//doNothing
	}

}
