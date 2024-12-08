package edu.nju.isefuzz.util;

import edu.nju.isefuzz.model.Seed;

import java.util.Map;

/**
 * 优先级计算器，用于计算种子的优先级分数。
 * @author 邱凯旋
 */
public class PriorityCalculator {
    // 标准化后的权重参数
    private double crashWeight = 30.0;
    private double favoredWeight = 20.0;
    private double blockCountWeight = 0.1;
    private double executionCountWeight = 5.0;
    private double entropyWeight = 3.0;
    private double newBlockWeight = 2.0;
    private double executeTimeWeight = 0.01;

    /**
     * 计算并更新种子的优先级分数。
     *
     * @param seed 种子对象
     */
    public void calculateAndUpdateScore(Seed seed) {
        Map<String, Object> metadata = seed.getMetadata();

        boolean isCrash = seed.isCrash();
        boolean isFavored = seed.isFavored();
        int blockCount = seed.getBlockCount();
        double lastExecutionTime = seed.getLastExecutionTime().isEmpty() ? 0 : Double.parseDouble(seed.getLastExecutionTime());

        // 从 metadata 中获取执行次数
        int executionCount = getIntegerFromMetadata(metadata, "executionCount");

        // 从 metadata 中获取熵值
        double entropy = getDoubleFromMetadata(metadata, "entropy");

        // 从 metadata 中获取新覆盖块数
        int newBlocks = getIntegerFromMetadata(metadata, "newBlocks");

        // 计算优先级分数
        double score =
                (isCrash ? crashWeight : 0) +
                        (isFavored ? favoredWeight : 0) +
                        (blockCount * blockCountWeight) -
                        (executionCount * executionCountWeight) +
                        (entropy * entropyWeight) +
                        (newBlocks * newBlockWeight) +
                        ((1.0 / lastExecutionTime) * executeTimeWeight);

        seed.setPriorityScore(score);
    }


    private int getIntegerFromMetadata(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        return 0;
    }

    private double getDoubleFromMetadata(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value instanceof Double) {
            return (Double) value;
        }
        return 0.0;
    }
}

