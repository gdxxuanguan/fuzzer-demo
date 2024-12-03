package edu.nju.isefuzz.fuzzer;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AFLAnalyzer {

    private static AFLAnalyzer instance;
    private Set<String> existingCoverage = new HashSet<>();
    private Set<String> seedPool = new HashSet<>();


    // 私有构造函数防止实例化
    private AFLAnalyzer() {}

    // 获取单例实例的方法
    public static synchronized AFLAnalyzer getInstance() {
        if (instance == null) {
            instance = new AFLAnalyzer();
        }
        return instance;
    }

    // 解析AFL输出
    public boolean parseAflOutput(String output) {
        boolean isNewCoverage = false;
        Pattern pattern = Pattern.compile("Block (\\d+) executed");
        Matcher matcher = pattern.matcher(output);

        while (matcher.find()) {
            if(!existingCoverage.contains(matcher.group(1))) {
                existingCoverage.add(matcher.group(1));
                isNewCoverage = true;
//                System.out.println("block "+matcher.group(1));
            }
        }
        return isNewCoverage;
    }
//
//    // 比较覆盖率
//    public boolean isInterestingSeed(Set<String> newBlocks) {
//
//        for (String block : newBlocks) {
//            if (!existingCoverage.contains(block)) {
//
//                existingCoverage.add(block);
//            }
//        }
//        return isNewCoverage;
//    }

    // 更新种子池
    public void updateSeedPool(String seed) {
        seedPool.add(seed);
        // 可以在这里添加清理逻辑
    }

//    public static void main(String[] args) {
//        AFLAnalyzer analyzer = new AFLAnalyzer();
//        String output = "Block 0 executed 1 times\nBlock 1 executed 2 times";
//        Set<String> blocks = analyzer.parseAflOutput(output);
//
//        if (analyzer.isInterestingSeed(blocks)) {
//            System.out.println("这是一个有趣种子");
//            analyzer.updateSeedPool("current_input");  // 使用实际的输入标识符
//        }
//    }
}