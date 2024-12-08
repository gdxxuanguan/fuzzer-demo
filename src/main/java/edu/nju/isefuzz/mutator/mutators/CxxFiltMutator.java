package edu.nju.isefuzz.mutator.mutators;

import edu.nju.isefuzz.model.Seed;
import edu.nju.isefuzz.mutator.Mutator;
import java.util.Random;

public class CxxFiltMutator implements Mutator {

  private static final Random random = new Random();

  @Override
  public Seed mutate(Seed seed) {
    String inputString = seed.getContentAsString(); // 获取种子的字符串内容
    String mutatedString;

    // 根据随机策略选择变异类型
    switch (random.nextInt(4)) {
      case 0:
        mutatedString = generateValidMangledName(); // 生成合法混淆名称
        break;
      case 1:
        mutatedString = BasicMutators.replaceRandomChar(inputString); // 替换随机字符
        break;
      case 2:
        mutatedString = BasicMutators.deleteRandomSection(inputString); // 删除随机部分
        break;
      default:
        mutatedString = BasicMutators.insertRandomString(inputString, new String[]{"@", "#", "$", "123"}); // 插入无效字符
        break;
    }

      return new Seed(mutatedString.getBytes(), seed.getFileType(), false);
  }

  // 生成合法的混淆名称
  private String generateValidMangledName() {
    StringBuilder mangledName = new StringBuilder("_Z");
    int nameLength = random.nextInt(20) + 1; // 随机长度
    mangledName.append(nameLength);
    for (int i = 0; i < nameLength; i++) {
      mangledName.append((char) ('a' + random.nextInt(26)));
    }
    mangledName.append("v"); // 随机返回值类型
    return mangledName.toString();
  }
}
