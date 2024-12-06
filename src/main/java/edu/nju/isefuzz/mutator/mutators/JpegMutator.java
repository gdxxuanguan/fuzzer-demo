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
    for (int i = 0; i < mutationCount; i++) {
      content = applyRandomMutation(content);
    }

    // 确保JPEG文件头部合法
    content = ensureJpegMagicBytes(content);

    seed.setContent(content);
    return seed;
  }

  /**
   * 综合多种变异方法，随机选择一个执行。
   */
  private byte[] applyRandomMutation(byte[] content) {
    int choice = random.nextInt(5); // 扩展变异方法的种类
    switch (choice) {
      case 0:
        // 变异JPEG文件头部
        content = mutateHeader(content);
        break;
      case 1:
        // 随机插入或删除数据块
        content = mutateDataBlocks(content);
        break;
      case 2:
        // 截断文件
        content = BasicMutators.truncate(content, Math.max(1, content.length - random.nextInt(10)));
        break;
      case 3:
        // 在文件末尾插入随机数据
        content = BasicMutators.havocMutate(content, random.nextInt(10) + 1);
        break;
      case 4:
        // 修改段标记
        content = mutateMarkers(content);
        break;
      default:
        break;
    }
    return content;
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
    // 随机插入伪造的段标记
    int offset = random.nextInt(content.length - 4);
    content[offset] = (byte) 0xFF;
    content[offset + 1] = (byte) (0xE0 + random.nextInt(15)); // APPx标记
    return content;
  }
}

