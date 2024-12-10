package edu.nju.isefuzz.model;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Seed类，表示模糊测试中的种子数据。
 * 统一使用byte[]存储数据，无论是文本还是二进制内容。
 */
public class Seed {

  // 核心字段
  protected byte[] content;    // 种子内容，统一为byte[]
  protected String fileType;   // 文件类型 (如 txt, png, elf)
  protected boolean isFavored; // 是否为优先种子
  protected boolean isCrash = false; // 是否触发崩溃
  protected int blockCount = 0; // 覆盖块数量
  protected String lastExecutionTime; // 最近一次执行时间
  protected double priorityScore; // 优先级分数

  // 动态元数据
  protected Map<String, Object> metadata = new ConcurrentHashMap<>();

  /**
   * 构造函数：创建种子
   *
   * @param content   种子内容
   * @param fileType  文件类型 (如 txt, png, elf)
   * @param isFavored 是否为优先种子
   */
  public Seed(byte[] content, String fileType, boolean isFavored) {
    this.content = content;
    this.fileType = fileType;
    this.isFavored = isFavored;
    initializeMetadata();
  }

  /**
   * 从字符串创建种子
   *
   * @param text      文本内容
   * @param isFavored 是否为优先种子
   */
  public Seed(String text, boolean isFavored) {
    this.content = text.getBytes(); // 将字符串转为byte[]
    this.fileType = "txt";
    this.isFavored = isFavored;
    initializeMetadata();
  }

  /**
   * 初始化 metadata 字段，设置优先级相关的默认值。
   */
  private void initializeMetadata() {
    metadata.put("executionCount", 0);           // 执行次数
    metadata.put("newBlocks", 0);                // 新覆盖块数
    metadata.put("entropy", calculateEntropy(content)); // 熵值
  }

  // 获取内容
  public byte[] getContent() {
    return content;
  }

  // 设置内容
  public void setContent(byte[] content) {
    this.content = content;
  }

  // 获取文件类型
  public String getFileType() {
    return fileType;
  }

  // 状态管理
  public boolean isFavored() {
    return isFavored;
  }

  public void setFavored() {
    this.isFavored = true;
  }

  public boolean isCrash() {
    return isCrash;
  }

  public void setCrashed() {
    this.isCrash = true;
  }

  // 覆盖块数量
  public int getBlockCount() {
    return blockCount;
  }

  public void setBlockCount(int blockCount) {
    this.blockCount = blockCount;
  }

  // 最近一次执行时间
  public String getLastExecutionTime() {
    return lastExecutionTime;
  }

  public void setLastExecutionTime(String lastExecutionTime) {
    this.lastExecutionTime = lastExecutionTime;
  }

  // 优先级分数
  public double getPriorityScore() {
    return priorityScore;
  }

  public void setPriorityScore(double priorityScore) {
    this.priorityScore = priorityScore;
  }

  // 动态元数据
  public Map<String, Object> getMetadata() {
    return metadata;
  }

  public void setMetadata(String key, Object value) {
    this.metadata.put(key, value);
  }

  // 工具方法：获取内容的字符串形式
  public String getContentAsString() {
    return new String(content); // 将byte[]转为String
  }

  // 工具方法：设置内容为字符串
  public void setContentFromString(String text) {
    this.content = text.getBytes(); // 将String转为byte[]
  }

  // 计算熵值的方法
  private double calculateEntropy(byte[] data) {
    if (data.length == 0) return 0.0;
    int[] freq = new int[256];
    for (byte b : data) {
      freq[b & 0xFF]++;
    }
    double entropy = 0.0;
    for (int f : freq) {
      if (f > 0) {
        double p = (double) f / data.length;
        entropy -= p * (Math.log(p) / Math.log(2));
      }
    }
    return entropy;
  }

  @Override
  public boolean equals(Object that) {
    if (this == that) return true;
    if (that == null || getClass() != that.getClass()) return false;
    Seed seed = (Seed) that;
    return Arrays.equals(content, seed.content) &&
        Objects.equals(fileType, seed.fileType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(Arrays.hashCode(content), fileType);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append("Seed [Type=").append(fileType)
            .append(", Size=").append(content.length).append(" bytes")
            .append(", IsFavored=").append(isFavored ? "Yes" : "No")
            .append(", IsCrash=").append(isCrash ? "Yes" : "No")
            .append(", BlockCount=").append(blockCount)
            .append(", LastExecutionTime=").append(lastExecutionTime != null ? lastExecutionTime : "N/A")
            .append(", PriorityScore=").append(priorityScore)
            .append(", Metadata=").append(metadata)
            .append("]");

    return sb.toString();
  }

}

