package edu.nju.isefuzz.seedSorter;

import edu.nju.isefuzz.model.Seed;
import edu.nju.isefuzz.seedSorter.Comparator.CoverageComparator;
import edu.nju.isefuzz.seedSorter.Comparator.ExecutionTimeComparator;
import edu.nju.isefuzz.seedSorter.Comparator.PriorityComparator;

import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.logging.Logger;

/**
 * 种子排序器。
 * 用于对种子进行排序，根据不同的策略进行排序。
 * @author 邱凯旋
 */
public class SeedSorter {
    private static final Logger logger = Logger.getLogger(SeedSorter.class.getName());

    private PriorityQueue<Seed> seedQueue;
    private SortingStrategy currentStrategy;

    public SeedSorter(SortingStrategy initialStrategy) {
        this.currentStrategy = initialStrategy;
        this.seedQueue = new PriorityQueue<>(getComparator(initialStrategy));
    }

    /**
     * 添加种子到队列中。
     */
    public void addSeed(Seed seed) {
        seedQueue.offer(seed);
    }

    /**
     * 从队列中获取并移除优先级最高的种子。
     */
    public Seed pollSeed() {
        return seedQueue.poll();
    }

    public Seed removeSeed(Seed seed) {
        seedQueue.remove(seed);
        return seed;
    }

    /**
     * 切换排序策略，并重新构建队列。
     */
    public void switchStrategy(SortingStrategy newStrategy) {
        if (newStrategy == null || newStrategy == this.currentStrategy) {
            return;
        }

        logger.info("Switching sorting strategy from " + this.currentStrategy + " to " + newStrategy);

        // 获取当前队列中的所有种子
        Queue<Seed> currentSeeds = new PriorityQueue<>(seedQueue);
        seedQueue.clear();

        // 更新当前策略
        this.currentStrategy = newStrategy;

        // 重新构建 PriorityQueue 使用新的比较器
        this.seedQueue = new PriorityQueue<>(getComparator(newStrategy));

        // 重新添加所有种子到新的队列中
        for (Seed seed : currentSeeds) {
            this.seedQueue.offer(seed);
        }

        logger.info("Sorting strategy switched successfully.");
    }

    /**
     * 获取对应策略的比较器。
     */
    private Comparator<Seed> getComparator(SortingStrategy strategy) {
        switch (strategy) {
            case COVERAGE:
                return new CoverageComparator();
            case EXECUTION_TIME:
                return new ExecutionTimeComparator();
            case PRIORITY:
                return new PriorityComparator();
            default:
                throw new IllegalArgumentException("Unsupported sorting strategy: " + strategy);
        }
    }

    /**
     * 获取当前队列大小。
     */
    public int getQueueSize() {
        return seedQueue.size();
    }

    /**
     * 判断队列中是否包含某个种子。
     */
    public boolean contains(Seed seed) {
        return seedQueue.contains(seed);
    }

    /**
     * 获取队列的迭代器。
     */
    public Iterator<Seed> getIterator() {
        return seedQueue.iterator();
    }

    /**
     * 根据当前轮次决定新的排序策略。
     * 这里只是一个示例，您可以根据实际需求设计策略切换逻辑。
     */
    public SortingStrategy determineNewStrategy(int fuzzRound) {
        switch (fuzzRound % 3) {
            case 0:
                return SortingStrategy.COVERAGE;
            case 1:
                return SortingStrategy.EXECUTION_TIME;
            case 2:
            default:
                return SortingStrategy.PRIORITY;
        }
    }
}
