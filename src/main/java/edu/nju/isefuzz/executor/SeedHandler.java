package edu.nju.isefuzz.executor;

import edu.nju.isefuzz.model.ExecutionResult;
import edu.nju.isefuzz.model.Seed;
import edu.nju.isefuzz.util.PriorityCalculator;

import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * 种子处理器，用于处理执行结果并更新种子的 metadata 和优先级分数。
 * 优先级分数的计算由 PriorityCalculator 完成。
 * @author 邱凯旋
 */
public class SeedHandler {
    private PriorityCalculator priorityCalculator;

    public SeedHandler(PriorityCalculator priorityCalculator) {
        this.priorityCalculator = priorityCalculator;
    }

    private Logger logger = Logger.getLogger(SeedHandler.class.getName());

    /**
     * 处理执行结果，更新种子的 metadata 并计算优先级分数。
     * 默认 isInitial 为 false。
     *
     * @param seed        执行的种子
     * @param execRes     执行结果
     * @param observedRes 已观察到的执行结果集合
     */
    public void handleExecutionResult(Seed seed, ExecutionResult execRes, Set<ExecutionResult> observedRes) {
        handleExecutionResult(seed, execRes, observedRes, false);
    }

    /**
     * 处理执行结果，更新种子的 metadata 并计算优先级分数。
     *
     * @param seed        执行的种子
     * @param execRes     执行结果
     * @param observedRes 已观察到的执行结果集合
     * @param isInitial   是否为初始执行
     */
    public void handleExecutionResult(Seed seed, ExecutionResult execRes, Set<ExecutionResult> observedRes, boolean isInitial) {
        Map<String, Object> metadata = seed.getMetadata();

        // 更新执行次数
        int executionCount = getIntegerFromMetadata(metadata, "executionCount");
        executionCount++;
        metadata.put("executionCount", executionCount);

        // 设置覆盖块数
        seed.setBlockCount(execRes.getCntOfBlocks());

        // 更新熵值（如果尚未计算）
        if (!metadata.containsKey("entropy")) {
            double entropy = calculateEntropy(seed.getContent());
            metadata.put("entropy", entropy);
        }

        // 更新新覆盖块数
        if (execRes.isReachNewBlock() && !isInitial) {
            int newBlocks = execRes.getNewBlocks();
            metadata.put("newBlocks", newBlocks);
            seed.setFavored(); // 标记为优先种子
        }

        // 更新最近执行时间
        String executeTime = execRes.getExecuteTime();
        seed.setLastExecutionTime(executeTime);

        // 标记崩溃
        if (execRes.isHasFatal()) {
            seed.setCrashed();
            logger.info(String.format("[FUZZER] Seed %s caused a crash. Marked as crashed.", seed));
        }

        // 处理新的执行结果
        if (!observedRes.contains(execRes)) {
            seed.setFavored(); // 标记为优先种子
            observedRes.add(execRes);
        }

        // 计算并更新优先级分数
        priorityCalculator.calculateAndUpdateScore(seed);
    }

    private int getIntegerFromMetadata(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        return 0;
    }


    private double calculateEntropy(byte[] data) {
        if (data.length == 0) return 0.0;
        int[] freq = new int[256];
        for (byte b : data) {
            freq[b & 0xFF]++;
        }
        double entropy = 0.0;
        for (int f : freq) {
            if (f > 0) {
                double p = (double) f / data.length;
                entropy -= p * (Math.log(p) / Math.log(2));
            }
        }
        return entropy;
    }
}
