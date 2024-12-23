package edu.nju.isefuzz.model;

/**
 * @ClassName ExecutionResult
 * @Description
 * @Author 74707
 * @Date 2024/12/6 14:10
 * @Version V1.0
 */

import java.util.HashSet;
import java.util.Objects;

/**
 * ExecutionResult类，单次运行程序后的返回包装
 * 参考了DemoMutationBlackBoxFuzzer的静态类和static
 */
public class ExecutionResult {
  protected String info; // 信息
  protected int exitVal;  // 退出码
  protected int cntOfBlocks; //执行块数
  protected boolean hasFatal=false; //运行是否出错
  protected boolean reachNewBlock=false;  //是否执行新的块
  protected String executeTime; //执行时间
  protected int newBlocks; //新块数

  // 无参构造方法
  public ExecutionResult() {
  }

  // 全参构造方法
  public ExecutionResult(String info, int exitVal, int cntOfBlocks, boolean hasFatal, boolean reachNewBlock, String executeTime,
                         int newBlocks) {
    this.info = info;
    this.exitVal = exitVal;
    this.cntOfBlocks = cntOfBlocks;
    this.hasFatal = hasFatal;
    this.reachNewBlock = reachNewBlock;
    this.executeTime = executeTime;
    this.newBlocks = newBlocks;
  }

  // Getter 和 Setter 方法
  public String getInfo() {
    return info;
  }

  public void setInfo(String info) {
    this.info = info;
  }

  public int getExitVal() {
    return exitVal;
  }

  public void setExitVal(int exitVal) {
    this.exitVal = exitVal;
  }

  public int getCntOfBlocks() {
    return cntOfBlocks;
  }

  public void setCntOfBlocks(int cntOfBlocks) {
    this.cntOfBlocks = cntOfBlocks;
  }

  public boolean isHasFatal() {
    return hasFatal;
  }

  public void setHasFatal(boolean hasFatal) {
    this.hasFatal = hasFatal;
  }

  public boolean isReachNewBlock() {
    return reachNewBlock;
  }

  public void setReachNewBlock(boolean reachNewBlock) {
    this.reachNewBlock = reachNewBlock;
  }

  public String getExecuteTime() {
    return executeTime;
  }

  public void setExecuteTime(String executeTime) {
    this.executeTime = executeTime;
  }

  public int getNewBlocks() {
    return newBlocks;
  }

  public void setNewBlocks(int newBlocks) {
    this.newBlocks = newBlocks;
  }

  // toString 方法，方便调试和日志输出
  @Override
  public String toString() {
    return "ExecutionResult{" +
        "info='" + info + '\'' +
        ", exitVal=" + exitVal +
        ", cntOfBlocks=" + cntOfBlocks +
        ", hasFatal=" + hasFatal +
        ", reachNewBlock=" + reachNewBlock +
        ", executeTime='" + executeTime + '\'' +
        ", newBlocks=" + newBlocks +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true; // 检查是否为同一个对象
    if (o == null || getClass() != o.getClass()) return false; // 检查类型
    ExecutionResult that = (ExecutionResult) o;
    return exitVal == that.exitVal && // 比较 exitVal
            info.equals(that.info); // 比较 info，处理 null 情况
  }

  @Override
  public int hashCode() {
    return Objects.hash(info, exitVal);
  }
}
