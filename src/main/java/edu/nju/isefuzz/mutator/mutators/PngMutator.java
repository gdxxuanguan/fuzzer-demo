package edu.nju.isefuzz.mutator.mutators;

import java.util.Random;
import edu.nju.isefuzz.model.Seed;
import edu.nju.isefuzz.mutator.Mutator;

/**
 * 针对PNG文件的优化版Mutator，支持权重控制。
 */
public class PngMutator implements Mutator {

  private final Random random = new Random();

  @Override
  public Seed mutate(Seed seed) {
    byte[] content = seed.getContent();

    // 在单次变异中综合多种策略
    int mutationCount = random.nextInt(3) + 1; // 每次执行1-3种变异
    mutationCount = 1; // 强制每次仅执行一次变异
    for (int i = 0; i < mutationCount; i++) {
      content = applyRandomMutation(content);
    }

    // 确保PNG文件头部合法
    content = ensurePngMagicBytes(content);

    return new Seed(content, seed.getFileType(), false);
  }

  /**
   * 综合多种变异方法，随机选择一个执行。
   */
  private byte[] applyRandomMutation(byte[] content) {
    // 定义变异类型及其对应权重（总和为100）
    int[] weights = {0, 30,30,20,20}; // 对应5种变异的权重
    int[] cumulativeWeights = calculateCumulativeWeights(weights);

    // 随机生成一个0到99的数，根据累积权重选择变异类型
    int choice = random.nextInt(100);
    for (int i = 0; i < cumulativeWeights.length; i++) {
      if (choice < cumulativeWeights[i]) {
        switch (i) {
          case 0:
            // 变异PNG文件头部
            return mutateHeader(content);
          case 1:
            // 修改IHDR块（30%概率）
            return mutateIHDR(content);
          case 2:
            // 随机修改IDAT块数据（25%概率）
            return mutateIDAT(content);
          case 3:
            // 插入伪造块（20%概率）
            return insertFakeChunk(content);
          case 4:
            // 截断文件（20%概率）
            return BasicMutators.truncate(content, Math.max(1, content.length - random.nextInt(10)));
          default:
            break;
        }
      }
    }
    return content;
  }

  /**
   * 确保文件的PNG签名合法。
   */
  private byte[] ensurePngMagicBytes(byte[] content) {
    if (content.length >= 8) {
      content[0] = (byte) 0x89;
      content[1] = (byte) 0x50;
      content[2] = (byte) 0x4E;
      content[3] = (byte) 0x47;
      content[4] = (byte) 0x0D;
      content[5] = (byte) 0x0A;
      content[6] = (byte) 0x1A;
      content[7] = (byte) 0x0A;
    }
    return content;
  }

  /**
   * 根据权重数组计算累积权重。
   */
  private int[] calculateCumulativeWeights(int[] weights) {
    int[] cumulativeWeights = new int[weights.length];
    cumulativeWeights[0] = weights[0];
    for (int i = 1; i < weights.length; i++) {
      cumulativeWeights[i] = cumulativeWeights[i - 1] + weights[i];
    }
    return cumulativeWeights;
  }

  /**
   * 变异文件头部。
   */
  private byte[] mutateHeader(byte[] content) {
    return BasicMutators.mutateRange(content, 8, Math.min(16, content.length));
  }

  /**
   * 变异IHDR块。
   */
  private byte[] mutateIHDR(byte[] content) {
    return BasicMutators.mutateRange(content, 16, Math.min(32, content.length));
  }

  /**
   * 随机修改IDAT块数据。
   */
  private byte[] mutateIDAT(byte[] content) {
    int start = Math.min(64, content.length / 4);
    int end = Math.min(content.length - 4, content.length * 3 / 4);
    return BasicMutators.mutateRange(content, start, end);
  }

  /**
   * 插入伪造的块。
   */
  private byte[] insertFakeChunk(byte[] content) {
    byte[] fakeChunk = new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x04,
        (byte) 0x66, (byte) 0x61, (byte) 0x6B, (byte) 0x65, // "fake"
        (byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF, // 数据
        (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78  // CRC
    };
    return BasicMutators.replaceSection(content, fakeChunk[random.nextInt(fakeChunk.length)]);
  }
}
