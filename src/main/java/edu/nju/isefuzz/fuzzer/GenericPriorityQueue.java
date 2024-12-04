package edu.nju.isefuzz.fuzzer;

import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;

/**
 * 通用优先队列，支持自定义比较器
 *
 * @param <T> 元素类型
 * @param <K> 键值类型
 */
public class GenericPriorityQueue<T, K> {
    private PriorityQueue<Element<T, K>> queue;
    private Comparator<K> comparator;

    // 内部类，用于存储元素及其键值
    private static class Element<T, K> {
        private T item;
        private K key;

        public Element(T item, K key) {
            this.item = item;
            this.key = key;
        }

        public T getItem() {
            return item;
        }

        public K getKey() {
            return key;
        }
    }

    /**
     * 构造函数，使用自定义的 Comparator
     *
     * @param comparator 用于比较键值的 Comparator
     */
    public GenericPriorityQueue(Comparator<K> comparator) {
        this.comparator = comparator;
        this.queue = new PriorityQueue<>(new Comparator<Element<T, K>>() {
            @Override
            public int compare(Element<T, K> e1, Element<T, K> e2) {
                return GenericPriorityQueue.this.comparator.compare(e1.getKey(), e2.getKey());
            }
        });
    }

    /**
     * 构造函数，假设键值类型 K 实现了 Comparable 接口
     */
    @SuppressWarnings("unchecked")
    public GenericPriorityQueue() {
        this.comparator = null;
        this.queue = new PriorityQueue<>(new Comparator<Element<T, K>>() {
            @Override
            public int compare(Element<T, K> e1, Element<T, K> e2) {
                if (GenericPriorityQueue.this.comparator != null) {
                    return GenericPriorityQueue.this.comparator.compare(e1.getKey(), e2.getKey());
                } else {
                    return ((Comparable<K>) e1.getKey()).compareTo(e2.getKey());
                }
            }
        });
    }

    /**
     * 添加一个元素及其键值到队列中
     *
     * @param item 元素
     * @param key  用于排序的键值
     */
    public void add(T item, K key) {
        queue.offer(new Element<>(item, key));
    }

    /**
     * 取出并移除队列中键值最高的元素
     *
     * @return 元素，若队列为空则返回 null
     */
    public T poll() {
        Element<T, K> e = queue.poll();
        return e == null ? null : e.getItem();
    }

    /**
     * 查看队列中键值最高的元素，但不移除
     *
     * @return 元素，若队列为空则返回 null
     */
    public T peek() {
        Element<T, K> e = queue.peek();
        return e == null ? null : e.getItem();
    }

    /**
     * 检查队列是否为空
     *
     * @return 如果队列为空返回 true，否则返回 false
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * 获取队列的大小
     *
     * @return 队列中元素的数量
     */
    public int size() {
        return queue.size();
    }

    /**
     * 移除指定的元素
     *
     * @param item 要移除的元素
     * @return 如果移除成功返回 true，否则返回 false
     */
    public boolean remove(T item) {
        return queue.removeIf(e -> e.getItem().equals(item));
    }

    /**
     * 清空队列
     */
    public void clear() {
        queue.clear();
    }

    /**
     * 将 PriorityQueue 转换为有序的 List
     *
     * @return 按键值排序的元素列表
     */
    public List<T> toList() {
        List<Element<T, K>> elements = new ArrayList<>(queue);
        elements.sort(new Comparator<Element<T, K>>() {
            @Override
            public int compare(Element<T, K> e1, Element<T, K> e2) {
                return GenericPriorityQueue.this.comparator != null
                        ? GenericPriorityQueue.this.comparator.compare(e1.getKey(), e2.getKey())
                        : ((Comparable<K>) e1.getKey()).compareTo(e2.getKey());
            }
        });
        List<T> list = new ArrayList<>();
        for (Element<T, K> e : elements) {
            list.add(e.getItem());
        }
        return list;
    }

    /**
     * 打印队列中的所有元素及其键值（用于调试）
     */
    public void printQueue() {
        List<Element<T, K>> elements = new ArrayList<>(queue);
        elements.sort(new Comparator<Element<T, K>>() {
            @Override
            public int compare(Element<T, K> e1, Element<T, K> e2) {
                return GenericPriorityQueue.this.comparator != null
                        ? GenericPriorityQueue.this.comparator.compare(e1.getKey(), e2.getKey())
                        : ((Comparable<K>) e1.getKey()).compareTo(e2.getKey());
            }
        });
        for (Element<T, K> e : elements) {
            System.out.println("Item: " + e.getItem() + ", Key: " + e.getKey());
        }
    }
}
