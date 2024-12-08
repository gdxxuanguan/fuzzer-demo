package edu.nju.isefuzz.mutator.mutators;


import java.util.Random;
import edu.nju.isefuzz.model.Seed;
import edu.nju.isefuzz.mutator.Mutator;
/**
 * 针对PNG文件的特化Mutator。
 */
public class PngMutator implements Mutator {

  private final Random random = new Random();

  @Override
  public Seed mutate(Seed seed) {
    byte[] content = seed.getContent();

    // 在单次变异中综合多种策略
    int mutationCount = random.nextInt(3) + 1; // 每次执行1-3种变异
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
    int choice = random.nextInt(5); // 扩展变异方法的种类
    switch (choice) {
      case 0:
        // 变异PNG文件头部
        content = mutateHeader(content);
        break;
      case 1:
        // 修改IHDR块
        content = mutateIHDR(content);
        break;
      case 2:
        // 随机修改IDAT块数据
        content = mutateIDAT(content);
        break;
      case 3:
        // 插入伪造块
        content = insertFakeChunk(content);
        break;
      case 4:
        // 截断文件
        content = BasicMutators.truncate(content, Math.max(1, content.length - random.nextInt(10)));
        break;
      default:
        break;
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
   * 变异文件头部。
   */
  private byte[] mutateHeader(byte[] content) {
    // 随机修改文件头的部分字节
    return BasicMutators.mutateRange(content, 8, Math.min(16, content.length));
  }

  /**
   * 变异IHDR块。
   */
  private byte[] mutateIHDR(byte[] content) {
    // 假设IHDR块在文件的前32字节内
    return BasicMutators.mutateRange(content, 16, Math.min(32, content.length));
  }

  /**
   * 随机修改IDAT块数据。
   */
  private byte[] mutateIDAT(byte[] content) {
    // 假设IDAT块从文件的64字节开始
    int start = Math.min(64, content.length / 4);
    int end = Math.min(content.length - 4, content.length * 3 / 4);
    return BasicMutators.mutateRange(content, start, end);
  }

  /**
   * 插入伪造的块。
   */
  private byte[] insertFakeChunk(byte[] content) {
    // 插入一个伪造的块类型和数据
    byte[] fakeChunk = new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x04, // 长度
        (byte) 0x66, (byte) 0x61, (byte) 0x6B, (byte) 0x65, // 类型 "fake"
        (byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF, // 数据
        (byte) 0x12, (byte) 0x34, (byte) 0x56, (byte) 0x78  // CRC
    };
    return BasicMutators.replaceSection(content, fakeChunk[random.nextInt(fakeChunk.length)]);
  }
}
