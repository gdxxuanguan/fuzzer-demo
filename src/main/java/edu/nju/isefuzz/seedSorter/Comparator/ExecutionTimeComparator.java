package edu.nju.isefuzz.seedSorter.Comparator;

import edu.nju.isefuzz.model.Seed;

import java.util.Comparator;
import java.util.logging.Logger;

/**
 * 执行时间比较器，用于 PriorityQueue 中根据种子的最后执行时间排序。
 * 执行时间越短的种子优先级越高。
 *
 * @author 邱凯旋
 */
public class ExecutionTimeComparator implements Comparator<Seed> {
    private static final Logger logger = Logger.getLogger(ExecutionTimeComparator.class.getName());

    @Override
    public int compare(Seed s1, Seed s2) {
        double execTime1 = parseExecutionTime(s1.getLastExecutionTime());
        double execTime2 = parseExecutionTime(s2.getLastExecutionTime());

        return Double.compare(execTime1, execTime2); // 执行时间短的优先
    }

    /**
     * 解析种子的最后执行时间，将 String 转换为 double。
     * 如果解析失败，则返回一个默认值（例如，Double.MAX_VALUE），以确保无法解析的种子排在队列末尾。
     *
     * @param executionTimeStr 种子的最后执行时间字符串
     * @return 解析后的执行时间，如果解析失败则返回默认值
     */
    private double parseExecutionTime(String executionTimeStr) {
        if (executionTimeStr == null || executionTimeStr.isEmpty()) {
            logger.warning("Execution time is null or empty. Defaulting to Double.MAX_VALUE.");
            return Double.MAX_VALUE;
        }

        try {
            return Double.parseDouble(executionTimeStr);
        } catch (NumberFormatException e) {
            logger.warning("Failed to parse execution time: " + executionTimeStr + ". Defaulting to Double.MAX_VALUE.");
            return Double.MAX_VALUE;
        }
    }
}
