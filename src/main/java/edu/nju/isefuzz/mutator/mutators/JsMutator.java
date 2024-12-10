package edu.nju.isefuzz.mutator.mutators;

import java.util.Random;
import edu.nju.isefuzz.model.Seed;
import edu.nju.isefuzz.mutator.Mutator;

/**
 * 针对JavaScript文件的优化版Mutator。
 */
public class JsMutator implements Mutator {

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
    // 合法与非法变异的权重控制
    int choice = random.nextInt(100);
    if (choice < 80) {
      // 合法变异（80%概率）
      return applyValidMutation(content);
    } else {
      // 非法变异（20%概率）
      return applyInvalidMutation(content);
    }
  }

  /**
   * 合法变异操作。
   */
  private byte[] applyValidMutation(byte[] content) {
    int choice = random.nextInt(5);
    switch (choice) {
      case 0:
        return mutateIdentifiers(content);
      case 1:
        return insertRandomCode(content);
      case 2:
        return deleteRandomCode(content);
      case 3:
        return mutateComments(content);
      case 4:
        return correctEqualityOperators(content);
      default:
        return content;
    }
  }

  /**
   * 非法变异操作。
   */
  private byte[] applyInvalidMutation(byte[] content) {
    int choice = random.nextInt(3);
    switch (choice) {
      case 0:
        return insertSyntaxError(content);
      case 1:
        return removeEssentialSymbols(content);
      case 2:
        return truncateCode(content);
      default:
        return content;
    }
  }

  /**
   * 修改变量或函数名，避免引入未定义变量。
   */
  private byte[] mutateIdentifiers(byte[] content) {
    String js = new String(content);
    js = js.replaceAll("\\b[a-zA-Z_][a-zA-Z0-9_]*\\b", "mutatedVar_" + random.nextInt(1000));
    return js.getBytes();
  }

  /**
   * 插入随机代码片段，确保语义简单且正确。
   */
  private byte[] insertRandomCode(byte[] content) {
    String js = new String(content);

    // 定义简单的随机代码片段
    String[] randomCodes = {
        "console.log('debug message');",
        "let temp = 42;",
        "if (true) { console.log('always true'); }",
        "function hello() { console.log('Hello, world!'); } hello();",
        "console.warn('This is a warning');"
    };

    // 随机选择代码片段
    String randomCode = randomCodes[random.nextInt(randomCodes.length)];

    // 随机插入到文件中的位置
    int pos = random.nextInt(js.length());

    // 插入代码片段
    js = js.substring(0, pos) + randomCode + "\n" + js.substring(pos);
    return js.getBytes();
  }

  /**
   * 删除随机部分代码。
   */
  private byte[] deleteRandomCode(byte[] content) {
    String js = new String(content);
    int start = random.nextInt(js.length() / 2);
    int end = start + random.nextInt(js.length() / 2);
    if (end > start && end < js.length()) {
      js = js.substring(0, start) + js.substring(end);
    }
    return js.getBytes();
  }

  /**
   * 修改注释内容。
   */
  private byte[] mutateComments(byte[] content) {
    String js = new String(content);
    int pos = random.nextInt(js.length());
    String randomComment = "// Mutated comment: " + random.nextInt(1000);
    js = js.substring(0, pos) + randomComment + js.substring(pos);
    return js.getBytes();
  }

  /**
   * 自动修正双等号为三等号。
   */
  private byte[] correctEqualityOperators(byte[] content) {
    String js = new String(content);
    js = js.replaceAll("==", "==="); // 替换所有双等号为三等号
    return js.getBytes();
  }

  /**
   * 插入语法错误。
   */
  private byte[] insertSyntaxError(byte[] content) {
    String js = new String(content);
    int pos = random.nextInt(js.length());
    String syntaxError = "{!!!SYNTAX_ERROR!!!}";
    js = js.substring(0, pos) + syntaxError + js.substring(pos);
    return js.getBytes();
  }

  /**
   * 删除必要的符号（非法变异）。
   */
  private byte[] removeEssentialSymbols(byte[] content) {
    String js = new String(content);
    js = js.replaceAll(";", ""); // 删除所有分号
    js = js.replaceAll("\\{", ""); // 删除左花括号
    js = js.replaceAll("\\}", ""); // 删除右花括号
    return js.getBytes();
  }

  /**
   * 截断代码。
   */
  private byte[] truncateCode(byte[] content) {
    int truncatePoint = random.nextInt(content.length / 2);
    return new String(content).substring(0, truncatePoint).getBytes();
  }
}
