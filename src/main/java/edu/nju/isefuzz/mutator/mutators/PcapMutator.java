package edu.nju.isefuzz.mutator.mutators;

import edu.nju.isefuzz.model.Seed;
import edu.nju.isefuzz.mutator.Mutator;
import java.util.Random;

/**
 * 针对PCAP文件的特化Mutator。
 */
public class PcapMutator implements Mutator {

  private final Random random = new Random();

  @Override
  public Seed mutate(Seed seed) {
    byte[] content = seed.getContent();

    // 每次执行1-3次变异操作
    int mutationCount = random.nextInt(3) + 1;
    for (int i = 0; i < mutationCount; i++) {
      content = applyRandomMutation(content);
    }

    return new Seed(content, seed.getFileType(), false);
  }

  /**
   * 综合多种变异方法，合法变异和非法变异的比例为8:2。
   */
  private byte[] applyRandomMutation(byte[] content) {
    int choice = random.nextInt(100); // 生成0-99的随机数
    if (choice < 80) {
      return applyValidMutation(content); // 80%概率执行合法变异
    } else {
      return applyInvalidMutation(content); // 20%概率执行非法变异
    }
  }

  /**
   * 合法变异操作。
   */
  private byte[] applyValidMutation(byte[] content) {
    switch (random.nextInt(4)) { // 合法变异类型
      case 0:
        return mutateGlobalHeaderFields(content); // 合法修改全局头的字段
      case 1:
        return mutatePacketHeader(content); // 修改数据包头
      case 2:
        return mutatePacketData(content); // 修改数据包数据
      default:
        return insertRandomPacket(content); // 插入随机数据包
    }
  }

  /**
   * 非法变异操作。
   */
  private byte[] applyInvalidMutation(byte[] content) {
    switch (random.nextInt(3)) { // 非法变异类型
      case 0:
        return corruptGlobalHeader(content); // 故意破坏全局头
      case 1:
        return truncateFile(content); // 截断文件
      default:
        return insertCorruptedPacket(content); // 插入非法数据包
    }
  }

  /**
   * 合法修改全局头的字段。
   */
  private byte[] mutateGlobalHeaderFields(byte[] content) {
    // 假设全局头的魔数、版本号、时间戳字段需要保持基本合法
    if (content.length >= 24) {
      byte[] modifiedContent = content.clone();
      // 仅修改时间戳字段（8-15字节）
      BasicMutators.mutateRange(modifiedContent, 8, 16);
      return modifiedContent;
    }
    return content;
  }

  /**
   * 故意破坏全局头。
   */
  private byte[] corruptGlobalHeader(byte[] content) {
    if (content.length >= 24) {
      byte[] corruptedContent = content.clone();
      // 直接破坏魔数字段（0-4字节），设置为随机值
      corruptedContent[0] = (byte) 0xFF;
      corruptedContent[1] = (byte) 0xFF;
      corruptedContent[2] = (byte) random.nextInt(256);
      corruptedContent[3] = (byte) random.nextInt(256);
      return corruptedContent;
    }
    return content;
  }

  /**
   * 修改数据包头。
   */
  private byte[] mutatePacketHeader(byte[] content) {
    // 假设每个数据包头从第25字节开始，单个数据包头长度为16字节
    int offset = 24 + random.nextInt(Math.max(1, content.length - 24 - 16));
    return BasicMutators.mutateRange(content, offset, Math.min(offset + 16, content.length));
  }

  /**
   * 修改数据包数据。
   */
  private byte[] mutatePacketData(byte[] content) {
    int start = random.nextInt(Math.max(1, content.length / 2));
    int end = Math.min(content.length, start + random.nextInt(50));
    return BasicMutators.mutateRange(content, start, end);
  }

  /**
   * 截断文件。
   */
  private byte[] truncateFile(byte[] content) {
    return BasicMutators.truncate(content, Math.max(1, content.length - random.nextInt(100)));
  }

  /**
   * 插入随机数据包。
   */
  private byte[] insertRandomPacket(byte[] content) {
    byte[] randomPacket = generateRandomPacket();
    return insertPacketAt(content, randomPacket, random.nextInt(content.length));
  }

  /**
   * 插入非法数据包。
   */
  private byte[] insertCorruptedPacket(byte[] content) {
    byte[] corruptedPacket = generateCorruptedPacket();
    return insertPacketAt(content, corruptedPacket, random.nextInt(content.length));
  }

  /**
   * 生成随机数据包。
   */
  private byte[] generateRandomPacket() {
    byte[] packet = new byte[64];
    random.nextBytes(packet);
    return packet;
  }

  /**
   * 生成非法数据包。
   */
  private byte[] generateCorruptedPacket() {
    byte[] packet = new byte[64];
    random.nextBytes(packet);
    packet[0] = (byte) 0xFF; // 设置非法头部标记
    return packet;
  }

  /**
   * 在指定位置插入数据包。
   */
  private byte[] insertPacketAt(byte[] content, byte[] packet, int pos) {
    byte[] newContent = new byte[content.length + packet.length];
    System.arraycopy(content, 0, newContent, 0, pos); // 原数据前半部分
    System.arraycopy(packet, 0, newContent, pos, packet.length); // 插入数据包
    System.arraycopy(content, pos, newContent, pos + packet.length, content.length - pos); // 原数据后半部分
    return newContent;
  }
}
