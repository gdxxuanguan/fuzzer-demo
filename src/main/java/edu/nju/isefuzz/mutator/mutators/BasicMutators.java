package edu.nju.isefuzz.mutator.mutators;

import java.util.Random;

/**
 * BasicMutators 提供通用的变异工具方法。
 */
public class BasicMutators {

  private static final Random random = new Random();

  // 位翻转变异
  public static byte[] bitFlipMutate(byte[] content) {
    if (content == null || content.length == 0) return content;
    byte[] mutatedContent = content.clone();
    int bitIndex = random.nextInt(content.length * 8); // 随机选择一个位
    int byteIndex = bitIndex / 8;
    int bitOffset = bitIndex % 8;

    mutatedContent[byteIndex] ^= (1 << bitOffset); // 翻转该位
    return mutatedContent;
  }

  // 数值加减变异
  public static byte[] arithMutate(byte[] content, int maxDelta) {
    if (content == null || content.length == 0) return content;
    byte[] mutatedContent = content.clone();
    int index = random.nextInt(content.length); // 随机选择一个字节
    int delta = random.nextInt(maxDelta * 2 + 1) - maxDelta; // [-maxDelta, maxDelta]

    mutatedContent[index] += delta;
    return mutatedContent;
  }

  // 插入感兴趣值
  public static byte[] interestMutate(byte[] content, int[] interestingValues) {
    if (content == null || content.length == 0) return content;
    byte[] mutatedContent = content.clone();
    int index = random.nextInt(content.length);
    mutatedContent[index] = (byte) interestingValues[random.nextInt(interestingValues.length)];
    return mutatedContent;
  }

  // 随机修改多个字节
  public static byte[] havocMutate(byte[] content, int mutations) {
    if (content == null || content.length == 0) return content;
    byte[] mutatedContent = content.clone();
    for (int i = 0; i < mutations; i++) {
      int index = random.nextInt(content.length);
      mutatedContent[index] ^= random.nextInt(256);
    }
    return mutatedContent;
  }

  // 针对指定范围的随机修改
  public static byte[] mutateRange(byte[] content, int start, int end) {
    if (content == null || content.length == 0 || start >= end) return content;
    byte[] mutatedContent = content.clone();
    for (int i = start; i < end && i < content.length; i++) {
      mutatedContent[i] ^= random.nextInt(256);
    }
    return mutatedContent;
  }

  // 截断内容
  public static byte[] truncate(byte[] content, int newLength) {
    if (content == null || content.length == 0) return content;
    newLength = Math.max(0, Math.min(newLength, content.length)); // 限制范围
    byte[] truncated = new byte[newLength];
    System.arraycopy(content, 0, truncated, 0, newLength);
    return truncated;
  }

  // 替换内容中的随机部分
  public static byte[] replaceSection(byte[] content, byte replacement) {
    if (content == null || content.length == 0) return content;
    byte[] mutatedContent = content.clone();
    int index = random.nextInt(content.length); // 随机选择一个位置
    mutatedContent[index] = replacement; // 替换为指定值
    return mutatedContent;
  }

  // 针对字符串的插入操作
  public static String insertRandomString(String input, String[] candidates) {
    if (input == null || candidates == null || candidates.length == 0) return input;
    int position = random.nextInt(input.length() + 1); // 随机插入位置
    String insertString = candidates[random.nextInt(candidates.length)];
    return input.substring(0, position) + insertString + input.substring(position);
  }

  // 针对字符串的随机替换操作
  public static String replaceRandomChar(String input) {
    if (input == null || input.length() == 0) return input;
    char[] inputChars = input.toCharArray();
    int position = random.nextInt(inputChars.length);
    inputChars[position] = (char) ('a' + random.nextInt(26)); // 随机替换为字母
    return new String(inputChars);
  }

  // 针对字符串的删除操作
  public static String deleteRandomSection(String input) {
    if (input == null || input.length() <= 1) return input;
    int start = random.nextInt(input.length());
    int end = Math.min(start + random.nextInt(input.length() - start), input.length());
    return input.substring(0, start) + input.substring(end);
  }
}
