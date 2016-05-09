# 自己实现简单的缓存

标签（空格分隔）： Cache

---

## 缓存的作用
　　
　　缓存的出现，是为了缓和CPU和主存之间速率不匹配的矛盾，缓存的容量比CPU寄存器容量大，而缓存的读写效率比主存大，因此缓存是总体性能时介于寄存器和主存之间的。一般来说缓存主要作用在以下三个场景：

> * 预读取，即预先读取可能要载入的数据
* 存储临时访问过的数据
* 对写入的数据进行临时存放

　　而在项目中一般做的业务层的缓存都是为了存储临时访问过的数据，避免频繁的读取数据库，从而提高效率。

## 基本缓存算法
　　
　　最基本的缓存算法有：
> * FIFO:先进先出
    * 根据先进先出原理淘汰数据，是实现上最简单的一种算法
    * 使用一个队列保存缓存数据，新加入元素放在队尾，队列满时淘汰队头元素。
* LRU：最近最少使用
    * 根据数据的最近访问记录来淘汰数据，依据的是如果数据最近被访问过，那么将来被访问的几率也相对较高，可以使用链表来保存缓存数据
    * 当添加新缓存记录时采用头插法插入链表头部
    * 命中缓存数据时，将该数据移动到链表头部
    * 当链表满时将链表尾部数据丢弃
* LFU：最不经常使用
    * 根据数据的访问频率来淘汰数据，依据的是如果数据过去被访问的次数较多，将来被访问的几率也较高，LFU算法要求每个数据块都有一个引用计数，所有的数据按照这个引用计数的大小排序，如果引用计数相同则按添加的时间排序。
    * 新加入的数据引用计数为1，插入队列尾部
    * 缓存命中的数据引用数加1，并且要对队列重新排序
    * 队列满时将队尾的数据淘汰。

　　FIFO算法的优点是实现简单，但是命中率较低，因此不怎么使用；LRU算法是实际中使用得较多，命中率也不错，但是偶发性、周期性的批量操作会使缓存受到污染。而LFU则能避免偶发性、周期性批量操作带来的缓存污染问题，但LFU需要维持额外的引用计数，并且当数据访问模式发生变化，由于缓存中记录的是旧模式下的热点数据，而使得新访问模式下的缓存命中率不高，即存在历史数据污染将来数据的情况。
　　在这三种算法之上还能扩展出更多缓存算法，如LRU2,LFU*。
　　
## 自定义缓存实现代码

**【Cache接口】**
```
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
```
    
**【AbstractCache抽象类】**
```
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
```

【FIFO实现】
```
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
```

【LRU实现】
```
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

```

【LFU实现】
```
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
			if (e.accessCount < min.accessCount
					|| (e.accessCount == min.accessCount && e.lastAccess < min.lastAccess)) {
				min = e;
			}
		}
		map.remove(min.k);
	}

}
```

