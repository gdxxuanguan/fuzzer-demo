package edu.nju.isefuzz.mutator.mutators;

import java.util.Random;
import edu.nju.isefuzz.model.Seed;
import edu.nju.isefuzz.mutator.Mutator;

/**
 * 针对JPEG文件的特化Mutator。
 */
public class JpegMutator implements Mutator {

  private final Random random = new Random();

  @Override
  public Seed mutate(Seed seed) {
    byte[] content = seed.getContent();

    // 在单次变异中综合多种策略
    int mutationCount = random.nextInt(3) + 1; // 每次执行1-3种变异
    mutationCount = 1;
    for (int i = 0; i < mutationCount; i++) {
      content = applyRandomMutation(content);
    }

    // 确保JPEG文件头部合法
    content = ensureJpegMagicBytes(content);

    return new Seed(content, seed.getFileType(), false);
  }

  /**
   * 综合多种变异方法，随机选择一个执行。
   */
  private byte[] applyRandomMutation(byte[] content) {
    // 定义合法与非法变异的权重比例
    int choice = random.nextInt(100);
    if (choice < 80) {
      // 合法变异（80%概率）
      return applyValidMutation(content);
    } else {
      // 非法变异（20%概率）
      return applyInvalidMutation(content);
    }
  }

  /**
   * 合法变异操作，保持JPEG文件解析性。
   */
  private byte[] applyValidMutation(byte[] content) {
    int choice = random.nextInt(3);
    switch (choice) {
      case 0:
        // 修改非关键字段
        return mutateHeader(content);
      case 1:
        // 变异数据块
        return mutateDataBlocks(content);
      case 2:
        // 修改段标记
        return mutateMarkers(content);
      default:
        return content;
    }
  }

  /**
   * 非法变异操作，可能破坏JPEG结构。
   */
  private byte[] applyInvalidMutation(byte[] content) {
    int choice = random.nextInt(3);
    switch (choice) {
      case 0:
        // 截断文件
        return BasicMutators.truncate(content, Math.max(1, content.length - random.nextInt(10)));
      case 1:
        // 在文件末尾插入随机数据
        return BasicMutators.havocMutate(content, random.nextInt(10) + 1);
      case 2:
        // 随机替换部分内容
        return BasicMutators.mutateRange(content, 0, content.length / 2);
      default:
        return content;
    }
  }

  /**
   * 确保文件的SOI和EOI标记合法。
   */
  private byte[] ensureJpegMagicBytes(byte[] content) {
    if (content.length >= 4) {
      // SOI标记（文件开头）
      content[0] = (byte) 0xFF;
      content[1] = (byte) 0xD8;

      // EOI标记（文件结尾）
      content[content.length - 2] = (byte) 0xFF;
      content[content.length - 1] = (byte) 0xD9;
    }
    return content;
  }

  /**
   * 变异JPEG文件头部。
   */
  private byte[] mutateHeader(byte[] content) {
    // 随机修改文件头的非关键字段
    return BasicMutators.mutateRange(content, 2, Math.min(20, content.length));
  }

  /**
   * 变异数据块（压缩数据部分）。
   */
  private byte[] mutateDataBlocks(byte[] content) {
    // 随机插入、替换或删除数据块内容
    int start = Math.min(20, content.length / 4); // 假设数据块从20字节后开始
    int end = Math.min(content.length - 2, content.length * 3 / 4);
    return BasicMutators.mutateRange(content, start, end);
  }

  /**
   * 变异段标记（Markers）。
   */
  private byte[] mutateMarkers(byte[] content) {
    // 检查 content 的长度是否合法
    if (content == null || content.length <= 4) {
      System.err.println("Content length is too short for mutation. Skipping mutation.");
      return content; // 返回原内容，不进行变异
    }
    // 随机插入伪造的段标记
    int offset = random.nextInt(content.length - 4);
    content[offset] = (byte) 0xFF;
    content[offset + 1] = (byte) (0xE0 + random.nextInt(15)); // APPx标记
    return content;
  }
}
