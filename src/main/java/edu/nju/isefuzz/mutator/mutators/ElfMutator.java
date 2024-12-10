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
    int mutationCount = random.nextInt(3) + 1;
    // 在单次变异中改为1种策略
    mutationCount = 1;
    for (int i = 0; i < mutationCount; i++) {
      content = applyRandomMutation(content);
    }

    return new Seed(content, seed.getFileType(), false);
  }

  /**
   * 综合多种变异方法，随机选择一个执行。
   */
  private byte[] applyRandomMutation(byte[] content) {
    // 定义变异类型及其对应权重（总和为100）
    int[] weights = {5, 30, 25, 15, 15, 10}; // 对应6种变异的权重
    int[] cumulativeWeights = new int[weights.length];

    // 计算累积权重
    cumulativeWeights[0] = weights[0];
    for (int i = 1; i < weights.length; i++) {
      cumulativeWeights[i] = cumulativeWeights[i - 1] + weights[i];
    }

    // 随机生成一个0到99的数，根据累积权重选择变异类型
    int choice = random.nextInt(100);
    for (int i = 0; i < cumulativeWeights.length; i++) {
      if (choice < cumulativeWeights[i]) {
        switch (i) {
          case 0:
            // 变异ELF Header（10%概率）
            content = BasicMutators.mutateRange(content, 0, Math.min(16, content.length));
            break;
          case 1:
            // 变异Program Header（30%概率）
            content = BasicMutators.mutateRange(content, 0x34, Math.min(0x34 + 16, content.length));
            break;
          case 2:
            // 变异Section Header（25%概率）
            content = BasicMutators.mutateRange(content, 0x50, Math.min(0x50 + 16, content.length));
            break;
          case 3:
            // 截断文件（15%概率）
            content = BasicMutators.truncate(content, Math.max(1, content.length - random.nextInt(10)));
            break;
          case 4:
            // 插入随机字节（15%概率）
            content = BasicMutators.havocMutate(content, random.nextInt(10) + 1);
            break;
          case 5:
            // 替换部分内容为随机字节（10%概率）
            content = BasicMutators.replaceSection(content, (byte) random.nextInt(256));
            break;
          default:
            break;
        }
        break;
      }
    }

    return content;
  }


}