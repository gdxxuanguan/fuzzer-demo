package edu.nju.isefuzz.mutator.mutators;


import java.util.Random;
import edu.nju.isefuzz.model.Seed;
import edu.nju.isefuzz.mutator.Mutator;

/**
 * 针对Lua文件的特化Mutator。
 */
public class LuaMutator implements Mutator {

  private final Random random = new Random();

  @Override
  public Seed mutate(Seed seed) {
    byte[] content = seed.getContent();

    // 在单次变异中综合多种策略
    int mutationCount = random.nextInt(3) + 1; // 每次执行1-3种变异
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
    int choice = random.nextInt(5); // 扩展变异方法的种类
    switch (choice) {
      case 0:
        // 修改变量或函数名
        content = mutateIdentifiers(content);
        break;
      case 1:
        // 插入随机代码片段
        content = insertRandomCode(content);
        break;
      case 2:
        // 删除随机部分代码
        content = deleteRandomCode(content);
        break;
      case 3:
        // 修改注释内容
        content = mutateComments(content);
        break;
      case 4:
        // 插入语法错误
        content = insertSyntaxError(content);
        break;
      default:
        break;
    }
    return content;
  }

  /**
   * 修改变量或函数名。
   */
  private byte[] mutateIdentifiers(byte[] content) {
    String lua = new String(content);
    // 简单替换变量名或函数名为随机值
    lua = lua.replaceAll("\\b[a-zA-Z_][a-zA-Z0-9_]*\\b", "var_" + random.nextInt(1000));
    return lua.getBytes();
  }

  /**
   * 插入随机代码片段。
   */
  private byte[] insertRandomCode(byte[] content) {
    String lua = new String(content);
    int pos = random.nextInt(lua.length());
    String randomCode = "print('mutated!')\n";
    lua = lua.substring(0, pos) + randomCode + lua.substring(pos);
    return lua.getBytes();
  }

  /**
   * 删除随机部分代码。
   */
  private byte[] deleteRandomCode(byte[] content) {
    String lua = new String(content);
    int start = random.nextInt(lua.length() / 2);
    int end = start + random.nextInt(lua.length() / 2);
    if (end > start && end < lua.length()) {
      lua = lua.substring(0, start) + lua.substring(end);
    }
    return lua.getBytes();
  }

  /**
   * 修改注释内容。
   */
  private byte[] mutateComments(byte[] content) {
    String lua = new String(content);
    // 插入随机注释
    int pos = random.nextInt(lua.length());
    String randomComment = "-- This is a mutated comment " + random.nextInt(1000) + "\n";
    lua = lua.substring(0, pos) + randomComment + lua.substring(pos);
    return lua.getBytes();
  }

  /**
   * 插入语法错误。
   */
  private byte[] insertSyntaxError(byte[] content) {
    String lua = new String(content);
    int pos = random.nextInt(lua.length());
    String syntaxError = "!!!syntax_error!!!";
    lua = lua.substring(0, pos) + syntaxError + lua.substring(pos);
    return lua.getBytes();
  }
}
