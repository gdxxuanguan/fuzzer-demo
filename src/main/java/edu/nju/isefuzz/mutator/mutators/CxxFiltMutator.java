package edu.nju.isefuzz.mutator.mutators;

import edu.nju.isefuzz.model.Seed;
import edu.nju.isefuzz.mutator.Mutator;
import java.util.Random;

public class CxxFiltMutator implements Mutator {

  private static final Random random = new Random();

  @Override
  public Seed mutate(Seed seed) {
    String inputString = ensureQuoted(seed.getContentAsString()); // 确保种子内容以引号包裹
    String mutatedString;

    // 根据合法和非法变异的比例进行选择
    boolean isInvalidMutation = random.nextInt(100) < 20; // 20%概率为非法变异
    if (isInvalidMutation) {
      // 执行非法变异
      mutatedString = applyInvalidMutation(inputString);
    } else {
      // 执行合法变异
      mutatedString = applyValidMutation(inputString);
    }

    // 确保变异后的字符串仍以引号包裹
    mutatedString = ensureQuoted(mutatedString);
    return new Seed(mutatedString.getBytes(), seed.getFileType(), false);
  }

  /**
   * 合法变异操作。
   */
  private String applyValidMutation(String inputString) {
    switch (random.nextInt(3)) { // 随机选择一个合法变异策略
      case 0:
        return generateComplexMangledName();
      case 1:
        return appendParameterType(inputString);
      default:
        return changeReturnType(inputString);
    }
  }

  /**
   * 非法变异操作。
   */
  private String applyInvalidMutation(String inputString) {
    switch (random.nextInt(3)) { // 使用您保留的非法变异策略
      case 0:
        return BasicMutators.replaceRandomChar(inputString); // 替换随机字符
      case 1:
        return BasicMutators.deleteRandomSection(inputString); // 删除随机部分
      default:
        return BasicMutators.insertRandomString(inputString, new String[]{"@", "#", "$", "123"}); // 插入无效字符
    }
  }

  /**
   * 确保字符串以引号包裹。
   */
  private String ensureQuoted(String input) {
    if (!input.startsWith("\"")) {
      input = "\"" + input;
    }
    if (!input.endsWith("\"")) {
      input = input + "\"";
    }
    return input;
  }

  /**
   * 生成复杂合法混淆名称。
   */
  private String generateComplexMangledName() {
    StringBuilder mangledName = new StringBuilder("_Z");
    int segmentCount = random.nextInt(3) + 1; // 随机生成1到3个命名空间或段
    for (int i = 0; i < segmentCount; i++) {
      int nameLength = random.nextInt(10) + 1; // 每段的随机长度
      mangledName.append(nameLength);
      for (int j = 0; j < nameLength; j++) {
        mangledName.append((char) ('a' + random.nextInt(26)));
      }
    }
    mangledName.append("v"); // 默认返回值为 void
    return mangledName.toString();
  }

  /**
   * 在混淆名称中添加参数类型。
   */
  private String appendParameterType(String input) {
    String core = input.substring(1, input.length() - 1); // 去掉引号
    String[] paramTypes = {"i", "f", "d", "c", "l", "s"}; // 支持的参数类型
    core += paramTypes[random.nextInt(paramTypes.length)];
    return "\"" + core + "\"";
  }

  /**
   * 修改返回值类型。
   */
  private String changeReturnType(String input) {
    String core = input.substring(1, input.length() - 1); // 去掉引号
    if (core.endsWith("v")) {
      String[] returnTypes = {"i", "f", "d", "c", "l", "s"}; // 支持的返回值类型
      core = core.substring(0, core.length() - 1) + returnTypes[random.nextInt(returnTypes.length)];
    }
    return "\"" + core + "\"";
  }
}
