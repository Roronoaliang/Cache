package com.liang.test;

import java.util.List;

import org.junit.Test;

import com.liang.util.cache.Cache;
import com.liang.util.cache.FIFOCache;
import com.liang.util.cache.LFUCache;
import com.liang.util.cache.LRUCache;

public class CacheTest {

	@Test
	public void testLRU1() {
		Cache<String, Object> cache = new LRUCache<String, Object>(16, 0);
		init(0, 16, cache);
		cache.get("3");
		cache.get("2");
		cache.get("17");
		cache.get("15");
		init(17, 19, cache);
		print(cache.getAllValue());
	}

	@Test
	public void testLRU2() {
		Cache<String, Object> cache = new LRUCache<String, Object>(10, 1);
		init(0, 11, cache);
		print(cache.getAllValue());
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		cache.put("key", "value");
		print(cache.getAllValue());
	}

	@Test
	public void testFIFO() {
		Cache<String, Object> cache = new FIFOCache<String, Object>(10, 1);
		init(0, 15, cache);
		System.out.println(cache.get("14"));
		System.out.println(cache.get("3"));
		print(cache.getAllValue());
		try {
			Thread.sleep(1000);
			init(25, 30, cache);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		print(cache.getAllValue());
	}

	@Test
	public void testFIFO2() {
		Cache<String, Object> cache = new FIFOCache<String, Object>();
		init(0, 1400, cache);
		print(cache.getAllValue());
		try {
			Thread.sleep(1000);
			init(10000, 10003, cache);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		print(cache.getAllValue());
	}

	@Test
	public void testLFU() {
		Cache<String, Object> cache = new LFUCache<String, Object>(10, 1);
		init(3, 11, cache);
		cache.get("3");
		init(0, 2, cache);
		init(11, 18, cache);
		print(cache.getAllValue());
		init(20, 25, cache);
		cache.get("20");
		print(cache.getAllValue());
	}

	private void init(int begin, int end, Cache<String, Object> cache) {
		for (int i = begin; i <= end; i++) {
			cache.put(String.valueOf(i), i);
		}
	}

	private void print(List<Object> list) {
		int count = 0;
		for (Object o : list) {
			if(count % 100 == 0) {
				System.out.println();
			}
			System.out.print(o + " ");
			count++;
		}
		System.out.println();
	}

}
