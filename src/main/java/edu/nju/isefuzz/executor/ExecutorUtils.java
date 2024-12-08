package edu.nju.isefuzz.executor;


import edu.nju.isefuzz.model.ExecutionResult;
import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ExecutorUtils，目前是:1.ProcessBuilder调用cpp文件 -> 2.cpp运行单个target
 * 【cpp文件的string command = string(program) + " < " + input_file部分还需要修改才能适配其他命令，
 * 其他target不需要标准输入】-> 3.java程序接下cpp输出，得到ExecutionResult
 * 主要方法 ： ExecutionResult result = executeCpp(coverageCollectorPath, targetProgramPath, inputFilePath);
 */
public class ExecutorUtils {

  private static final int MAP_SIZE = 65536; // 覆盖信息的最大块数

  /**
   * 执行 C++ 工具并获取执行结果
   *
   * @param coverageCollectorPath coverage_collector 程序路径
   * @param targetProgramPath     目标程序路径
   * @param inputFilePath         输入文件路径
   * @param coveredBlocks         覆盖块的集合
   * @return 执行结果（ExecutionResult 对象）
   * @throws IOException          如果执行失败
   * @throws InterruptedException 如果进程被中断
   */
  public static ExecutionResult executeCpp(String coverageCollectorPath, String targetProgramPath, String inputFilePath,
                                           Set<Integer> coveredBlocks)
      throws IOException, InterruptedException {

    // 使用 ProcessBuilder 构建命令
    ProcessBuilder processBuilder = new ProcessBuilder(
        coverageCollectorPath, targetProgramPath, inputFilePath);
    processBuilder.redirectErrorStream(true); // 合并标准错误流和输出流

    Process process = processBuilder.start();

    // 读取输出
    StringBuilder output = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
      String line;
      while ((line = reader.readLine()) != null) {
        output.append(line).append("\n");
      }
    }

    int exitCode = process.waitFor(); // 等待进程执行完成

    // 提取目标程序的退出码
    int targetExitCode = parseTargetExitCode(output.toString());

//    System.out.println("====== coverage_collector 退出码 ====== " + exitCode);
//    System.out.println("====== targetProgramPath 退出码 ====== " + targetExitCode);

    // 解析输出结果
    return parseExecutionOutput(output.toString(), targetExitCode, coveredBlocks);
  }

  /**
   * 从输出中提取目标程序的退出码
   *
   * @param output C++ 程序的输出内容
   * @return 目标程序的退出码
   */
  private static int parseTargetExitCode(String output) {
    // 匹配目标程序的退出码 (比如 C++ 程序可能会输出 "Exit code: 0")
    Pattern exitCodePattern = Pattern.compile("Exit code: (\\d+)");
    Matcher matcher = exitCodePattern.matcher(output);
    if (matcher.find()) {
      return Integer.parseInt(matcher.group(1));
    }
    return -1; // 如果没有找到退出码，返回 -1 表示未知
  }

  /**
   * 解析 C++ 工具的输出，生成 ExecutionResult 对象
   *
   * @param output   C++ 程序的输出内容
   * @param exitCode 进程的退出码
   * @param coveredBlocks 覆盖块的集合
   * @return 包含解析结果的 ExecutionResult 对象
   */
  private static ExecutionResult parseExecutionOutput(String output, int exitCode, Set<Integer> coveredBlocks) {
    int cntOfBlocks = 0;
    int newBlocks = 0; //新的覆盖块数
    boolean hasFatal = (exitCode != 0); // 非 0 退出码表示崩溃
    boolean reachNewBlock = false;
    String executeTime = "";

    // 解析输出时间和覆盖块
    Pattern timePattern = Pattern.compile("Execution time: ([0-9.]+) seconds");
    Pattern blockPattern = Pattern.compile("Block (\\d+) executed (\\d+) times");

    // 匹配执行时间
    Matcher timeMatcher = timePattern.matcher(output);
    if (timeMatcher.find()) {
      executeTime = timeMatcher.group(1); // 获取执行时间
    }

    // 匹配覆盖块信息
    Matcher blockMatcher = blockPattern.matcher(output);
    while (blockMatcher.find()) {
      cntOfBlocks++; // 统计覆盖的块数
      int blockId = Integer.parseInt(blockMatcher.group(1));
      // 检查是否是新的覆盖块
      if (!coveredBlocks.contains(blockId)) {
        newBlocks++;
        coveredBlocks.add(blockId);
      }
    }
    reachNewBlock = newBlocks > 0;

    // 创建并返回 ExecutionResult 对象
    ExecutionResult result = new ExecutionResult();
    result.setInfo(output);
    result.setExitVal(exitCode); // 设置目标程序的退出码
    result.setCntOfBlocks(cntOfBlocks);
    result.setHasFatal(hasFatal);
    result.setReachNewBlock(reachNewBlock);
    result.setExecuteTime(executeTime);
    result.setNewBlocks(newBlocks);

    return result;
  }

  // qkx 修改过路径
  public static void main(String[] args) {
    String projectRootPath = System.getProperty("user.dir");
    String coverageCollectorPath = projectRootPath + "/cpptest/coverage_collector"; ; // 你要执行的工具路径
    String targetProgramPath = projectRootPath+"/targets/readpng"; // 目标程序路径
    String inputFilePath = projectRootPath+"/targets/not_kitty.png"; // 输入文件路径
    Set<Integer> coveredBlocks = new HashSet<>(); // 覆盖块的集合
    try {
      // 执行 C++ 程序并获取执行结果
      ExecutionResult result = executeCpp(coverageCollectorPath, targetProgramPath, inputFilePath, coveredBlocks);

      // 输出执行结果
      System.out.println("执行结果：");
      System.out.println("退出码: " + result.getExitVal());
      System.out.println("执行的块数: " + result.getCntOfBlocks());
      System.out.println("是否发生崩溃: " + (result.isHasFatal() ? "是" : "否"));
      System.out.println("是否执行了新块: " + (result.isReachNewBlock() ? "是" : "否"));
      System.out.println("执行时间: " + result.getExecuteTime() + " 秒");

    } catch (IOException | InterruptedException e) {
      System.err.println("执行过程中发生了错误: " + e.getMessage());
      e.printStackTrace();
    }

  }
}
