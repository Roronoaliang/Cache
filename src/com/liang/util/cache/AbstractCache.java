package com.liang.util.cache;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @Description Cache抽象类，封装共用代码
 * @Date 2016年5月8日 下午8:45:13
 */
public abstract class AbstractCache<K, V> implements Cache<K, V> {

	protected ReadWriteLock lock = new ReentrantReadWriteLock();
	protected LinkedHashMap<K, Element> map;
	protected int capacity; // 缓存池容量
	protected int timeToLiveSeconds; // 缓存记录多长时间没命中失效，0表示永不过期

	protected class Element {

		volatile K k;
		volatile V v;
		volatile long lastAccess; // 最后访问时间
		volatile int accessCount; // 访问次数

		public Element(K k, V v, long lastAccess, int accessCount) {
			this.k = k;
			this.v = v;
			this.lastAccess = lastAccess;
			this.accessCount = accessCount;
		}
	}

	public V put(K k, V v) {
		lock.writeLock().lock();
		try {
			if (isFull() && !map.containsKey(k)) {
				removeExpiredElement();
				if (isFull()) {
					eliminate();
				}
			}
			Element e = new Element(k, v, System.currentTimeMillis(), 1);
			e = map.put(k, e);
			return e == null ? null : e.v;
		} finally {
			lock.writeLock().unlock();
		}
	}

	public V remove(K k) {
		lock.writeLock().lock();
		try {
			Element e = map.remove(k);
			if (e == null) {
				return null;
			}
			return e.v;
		} finally {
			lock.writeLock().unlock();
		}
	}

	public V get(K k) {
		lock.readLock().lock();
		try {
			Element e = map.get(k);
			if (e == null) {
				return null;
			}
			if (!isAlive(e)) {
				remove(k);
				return null;
			}
			update(k, e);
			return e.v;
		} finally {
			lock.readLock().unlock();
		}
	}

	protected void update(K k, Element e) {
		e.lastAccess = System.currentTimeMillis();
		e.accessCount++;
		map.put(k, e);
	}

	public int size() {
		lock.readLock().lock();
		try {
			return map.size();
		} finally {
			lock.readLock().unlock();
		}
	}

	public int getCapacity() {
		return capacity;
	}

	public void clear() {
		lock.writeLock().lock();
		try {
			map.clear();
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * 获取对应缓存记录距离最后一次访问的时间
	 * 
	 * @param e
	 * @return
	 */
	protected int getLiveTime(Element e) {
		if (timeToLiveSeconds != 0) {
			return (int) ((System.currentTimeMillis() - e.lastAccess) / 1000);
		}
		return 0;
	}

	/**
	 * 判断缓存记录是否过期存活
	 * 
	 * @param e
	 */
	protected boolean isAlive(Element e) {
		if (timeToLiveSeconds == 0) {
			return true;
		}
		return timeToLiveSeconds > getLiveTime(e);
	}

	protected boolean isFull() {
		lock.readLock().lock();
		try {
			return map.size() == capacity;
		} finally {
			lock.readLock().unlock();
		}
	}

	protected void removeExpiredElement() {
		if (timeToLiveSeconds > 0) {
			Iterator<Entry<K, Element>> it = map.entrySet().iterator();
			while (it.hasNext()) {
				Entry<K, Element> entry = it.next();
				if (!isAlive(entry.getValue())) {
					it.remove();
				}
			}
		}
	}

	public List<V> getAllValue() {
		List<V> list = new ArrayList<V>();
		Iterator<Entry<K, Element>> it = map.entrySet().iterator();
		while (it.hasNext()) {
			Element e = it.next().getValue();
			if (isAlive(e)) {
				list.add(e.v);
			} else {
				it.remove();
			}
		}
		return list;
	}

	protected abstract void eliminate();
}
