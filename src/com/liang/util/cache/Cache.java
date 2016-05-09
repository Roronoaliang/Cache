package com.liang.util.cache;

import java.util.List;

/**
 * @Description Cache接口
 * @Date 2016年5月8日 下午8:45:27
 */
public interface Cache<K, V> {

	/**
	 * 添加缓存记录，如果有旧值，返回旧value值，否则返回null
	 * 
	 * @param k
	 * @param v
	 * @return
	 */
	public V put(K k, V v);

	/**
	 * 移除指定Key的缓存记录,返回旧value值，不区分旧值不存在和旧值等于Null的情况
	 * 
	 * @param k
	 * @return
	 */
	public V remove(K k);

	/**
	 * 查询缓存记录
	 * 
	 * @param k
	 * @return
	 */
	public V get(K k);

	/**
	 * 返回缓存记录条数
	 * 
	 * @return
	 */
	public int size();

	/**
	 * 返回缓存池容量
	 * 
	 * @return
	 */
	public int getCapacity();

	/**
	 * 清空所有缓存
	 */
	public void clear();
	
	/**
	 * 获取缓存的值集合
	 * @return
	 */
	public List<V> getAllValue();

	// /**
	// * 缓存池是否已满
	// * @return
	// */
	// public boolean isFull();

	// /**
	// * 删除过期缓存
	// */
	// public void removeExpiredElement();
}
