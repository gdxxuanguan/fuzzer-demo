package edu.nju.isefuzz.mutator.mutators;

import edu.nju.isefuzz.model.Seed;
import edu.nju.isefuzz.mutator.Mutator;
import java.util.Random;


/**
 * 针对readelf的综合增强版特化Mutator。
 */
public class ElfMutator implements Mutator {

  private final Random random = new Random();

  @Override
  public Seed mutate(Seed seed) {
    byte[] content = seed.getContent();

    // 在单次变异中综合多种策略
    int mutationCount = random.nextInt(3) + 1; // 每次执行1-3种变异
    for (int i = 0; i < mutationCount; i++) {
      content = applyRandomMutation(content);
    }

    return new Seed(content, seed.getFileType(), false);
  }

  /**
   * 综合多种变异方法，随机选择一个执行。
   */
  private byte[] applyRandomMutation(byte[] content) {
    int choice = random.nextInt(6); // 扩展变异方法的种类
    switch (choice) {
      case 0:
        // 变异ELF Header
        content = BasicMutators.mutateRange(content, 0, Math.min(16, content.length));
        break;
      case 1:
        // 变异Program Header部分
        content = BasicMutators.mutateRange(content, 0x34, Math.min(0x34 + 16, content.length));
        break;
      case 2:
        // 变异Section Header部分
        content = BasicMutators.mutateRange(content, 0x50, Math.min(0x50 + 16, content.length));
        break;
      case 3:
        // 截断文件
        content = BasicMutators.truncate(content, Math.max(1, content.length - random.nextInt(10)));
        break;
      case 4:
        // 插入随机字节
        content = BasicMutators.havocMutate(content, random.nextInt(10) + 1);
        break;
      case 5:
        // 替换部分内容为随机字节
        content = BasicMutators.replaceSection(content, (byte) random.nextInt(256));
        break;
      default:
        break;
    }
    return content;
  }
}