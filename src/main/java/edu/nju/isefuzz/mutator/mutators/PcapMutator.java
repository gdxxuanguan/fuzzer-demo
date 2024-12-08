package edu.nju.isefuzz.mutator.mutators;


import java.util.Random;
import edu.nju.isefuzz.model.Seed;
import edu.nju.isefuzz.mutator.Mutator;

/**
 * 针对PCAP文件的特化Mutator。
 */
public class PcapMutator implements Mutator {

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
    int choice = random.nextInt(5); // 扩展变异方法的种类
    switch (choice) {
      case 0:
        // 变异全局头
        content = mutateGlobalHeader(content);
        break;
      case 1:
        // 变异数据包头
        content = mutatePacketHeader(content);
        break;
      case 2:
        // 变异数据包数据
        content = mutatePacketData(content);
        break;
      case 3:
        // 插入随机数据包
        content = insertRandomPacket(content);
        break;
      case 4:
        // 截断文件
        content = BasicMutators.truncate(content, Math.max(1, content.length - random.nextInt(50)));
        break;
      default:
        break;
    }
    return content;
  }

  /**
   * 变异全局头（Global Header）。
   */
  private byte[] mutateGlobalHeader(byte[] content) {
    // 假设全局头为文件前24字节
    return BasicMutators.mutateRange(content, 0, Math.min(24, content.length));
  }

  /**
   * 变异数据包头（Packet Header）。
   */
  private byte[] mutatePacketHeader(byte[] content) {
    // 假设数据包头从第25字节开始，单个数据包头长度为16字节
    int offset = 24 + random.nextInt(Math.max(1, content.length - 24 - 16));
    return BasicMutators.mutateRange(content, offset, Math.min(offset + 16, content.length));
  }

  /**
   * 变异数据包数据（Packet Data）。
   */
  private byte[] mutatePacketData(byte[] content) {
    // 假设数据包数据从40字节后开始
    int start = random.nextInt(Math.max(1, content.length / 2));
    int end = Math.min(content.length, start + random.nextInt(50));
    return BasicMutators.mutateRange(content, start, end);
  }

  /**
   * 插入随机数据包。
   */
  private byte[] insertRandomPacket(byte[] content) {
    // 生成伪造的数据包（假设长度为64字节）
    byte[] fakePacket = new byte[64];
    random.nextBytes(fakePacket);

    // 随机选择插入位置
    int pos = random.nextInt(content.length);

    // 创建新的字节数组，包含插入后的内容
    byte[] newContent = new byte[content.length + fakePacket.length];
    System.arraycopy(content, 0, newContent, 0, pos); // 原数据前半部分
    System.arraycopy(fakePacket, 0, newContent, pos, fakePacket.length); // 插入伪造数据包
    System.arraycopy(content, pos, newContent, pos + fakePacket.length, content.length - pos); // 原数据后半部分

    return newContent;
  }

}
