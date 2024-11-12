package edu.nju.isefuzz.fuzzer;

import java.util.Random;
import java.util.Set;

public class Mutator {
    private static final Random random = new Random();

    public static String mutate(String input, String fileType, Set<String> otherSeeds) {
        int mutationChoice = random.nextInt(5); // 5种变异策略
        int position = random.nextInt(input.length());
        int step = random.nextInt(10) + 1; // 随机步长，用于算术变异
        char[] content = input.toCharArray();

        if ("png".equalsIgnoreCase(fileType)) {
            return mutateForPNG(content, mutationChoice, otherSeeds);
        } else {
            switch (mutationChoice) {
                case 0:
                    return bitflip(content, position);
                case 1:
                    return arith(content, position, step);
                case 2:
                    return interest(content, position, fileType);
                case 3:
                    return havoc(input);
                case 4:
                    return splice(input, otherSeeds);
                default:
                    return input; // 默认不变异
            }
        }
    }

    private static String mutateForPNG(char[] content, int mutationChoice, Set<String> otherSeeds) {
        int pngHeaderSize = 8; // PNG文件的前8字节为文件头
        int position = random.nextInt(content.length - pngHeaderSize) + pngHeaderSize; // 避开文件头

        switch (mutationChoice) {
            case 0:
                return bitflip(content, position); // 对非头部的某个位翻转
            case 1:
                return arith(content, position, random.nextInt(5) + 1); // 对字符值进行小幅加减
            case 2:
                return interestForPNG(content, position); // 使用PNG特定的感兴趣值
            case 3:
                return havocForPNG(content); // 随机扰动文件的非关键区域
            case 4:
                return spliceForPNG(content, otherSeeds); // 拼接另一个PNG文件的数据块
            default:
                return new String(content);
        }
    }

    private static String bitflip(char[] content, int position) {
        int bitPos = random.nextInt(8); // 随机位
        content[position] ^= (1 << bitPos); // 翻转位
        return new String(content);
    }

    private static String arith(char[] content, int position, int step) {
        content[position] = (char) (content[position] + step);
        return new String(content);
    }

    private static String interestForPNG(char[] content, int position) {
        // 对于 PNG 文件，可以使用一些典型的感兴趣值，例如宽高限制或特殊的颜色值
        if (position >= 16 && position <= 24) { // 假设是IHDR块中的宽高部分
            content[position] = (char) (random.nextBoolean() ? 0 : 255); // 简单示例：最大或最小值
        }
        return new String(content);
    }

    private static String havocForPNG(char[] content) {
        // 对于PNG，随机改变多个位置，但避免前8字节的文件头
        int numChanges = random.nextInt(content.length / 4); // 控制变更数量
        for (int i = 0; i < numChanges; i++) {
            int pos = random.nextInt(content.length - 8) + 8; // 避开文件头
            content[pos] = (char) (random.nextInt(256)); // 随机字节
        }
        return new String(content);
    }

    private static String spliceForPNG(char[] content, Set<String> otherSeeds) {
        // 从其他PNG种子中选择一个块进行拼接
        if (otherSeeds.isEmpty()) return new String(content); // 没有其他种子时不变异

        String otherSeed = otherSeeds.stream()
                .skip(random.nextInt(otherSeeds.size()))
                .findFirst()
                .orElse(new String(content));
        byte[] otherContent = otherSeed.getBytes();
        int splicePoint = random.nextInt(content.length - 8) + 8; // 避开文件头
        int copyLength = Math.min(otherContent.length, content.length - splicePoint);

        System.arraycopy(otherContent, 0, content, splicePoint, copyLength);
        return new String(content);
    }

    private static String interest(char[] content, int position, String fileType) {
        // 通用感兴趣值变异
        if ("binary".equalsIgnoreCase(fileType)) {
            content[position] = (char) 0xFF;
        } else if ("text".equalsIgnoreCase(fileType)) {
            content[position] = (char) (random.nextBoolean() ? '0' : '1');
        } else {
            content[position] = (char) 0;
        }
        return new String(content);
    }

    private static String havoc(String input) {
        StringBuilder mutated = new StringBuilder(input);
        int numChanges = random.nextInt(input.length() / 2) + 1;
        for (int i = 0; i < numChanges; i++) {
            int pos = random.nextInt(mutated.length());
            mutated.setCharAt(pos, (char) (random.nextInt(26) + 'a'));
        }
        return mutated.toString();
    }

    private static String splice(String input, Set<String> otherSeeds) {
        if (otherSeeds.isEmpty()) return input;

        String otherSeed = otherSeeds.stream()
                .skip(random.nextInt(otherSeeds.size()))
                .findFirst()
                .orElse(input);
        int splicePoint = random.nextInt(input.length());
        return input.substring(0, splicePoint) + otherSeed + input.substring(splicePoint);
    }
}
