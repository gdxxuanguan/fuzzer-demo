package edu.nju.isefuzz.mutator.mutators;


import java.util.Random;
import edu.nju.isefuzz.model.Seed;
import edu.nju.isefuzz.mutator.Mutator;

/**
 * 针对XML文件的特化Mutator。
 */
public class XmlMutator implements Mutator {

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
    int choice = random.nextInt(6); // 扩展变异方法的种类
    switch (choice) {
      case 0:
        // 删除随机节点
        content = removeRandomNode(content);
        break;
      case 1:
        // 插入随机节点
        content = insertRandomNode(content);
        break;
      case 2:
        // 修改节点文本内容
        content = mutateNodeText(content);
        break;
      case 3:
        // 添加随机属性
        content = addRandomAttribute(content);
        break;
      case 4:
        // 删除或修改XML声明
        content = mutateXmlDeclaration(content);
        break;
      case 5:
        // 插入非法字符或截断文件
        content = BasicMutators.havocMutate(content, random.nextInt(10) + 1);
        break;
      default:
        break;
    }
    return content;
  }

  /**
   * 删除随机节点。
   */
  private byte[] removeRandomNode(byte[] content) {
    // 模拟删除一个随机节点
    String xml = new String(content);
    int start = xml.indexOf("<", random.nextInt(xml.length()));
    int end = xml.indexOf(">", start);
    if (start >= 0 && end > start) {
      xml = xml.substring(0, start) + xml.substring(end + 1);
    }
    return xml.getBytes();
  }

  /**
   * 插入随机节点。
   */
  private byte[] insertRandomNode(byte[] content) {
    // 模拟在随机位置插入节点
    String xml = new String(content);
    int pos = random.nextInt(xml.length());
    String randomNode = "<random>data</random>";
    xml = xml.substring(0, pos) + randomNode + xml.substring(pos);
    return xml.getBytes();
  }

  /**
   * 修改节点文本内容。
   */
  private byte[] mutateNodeText(byte[] content) {
    String xml = new String(content);
    int start = xml.indexOf(">", random.nextInt(xml.length())) + 1;
    int end = xml.indexOf("<", start);
    if (start >= 0 && end > start) {
      String randomText = "mutated" + random.nextInt(1000);
      xml = xml.substring(0, start) + randomText + xml.substring(end);
    }
    return xml.getBytes();
  }

  /**
   * 添加随机属性。
   */
  private byte[] addRandomAttribute(byte[] content) {
    String xml = new String(content);
    int pos = xml.indexOf("<", random.nextInt(xml.length()));

    // 确保找到了 "<" 标签
    if (pos >= 0) {
      String randomAttr = " attr" + random.nextInt(100) + "=\"value\"";
      int insertPos = xml.indexOf(">", pos);

      // 确保找到了 ">" 标签
      if (insertPos >= 0) {
        xml = xml.substring(0, insertPos) + randomAttr + xml.substring(insertPos);
      } else {
        // 如果没有找到 ">"，则可能是一个不完整的标签，处理逻辑可以根据需要修改
        System.out.println("Warning: No closing '>' found for tag starting at position " + pos);
      }
    } else {
      // 如果没有找到 "<"，则输出警告
      System.out.println("Warning: No '<' found in XML content.");
    }

    return xml.getBytes();
  }

  /**
   * 修改或删除XML声明。
   */
  private byte[] mutateXmlDeclaration(byte[] content) {
    String xml = new String(content);
    if (xml.startsWith("<?xml")) {
      if (random.nextBoolean()) {
        // 删除声明
        int end = xml.indexOf("?>");
        if (end >= 0) {
          xml = xml.substring(end + 2);
        }
      } else {
        // 修改声明内容
        xml = "<?xml version=\"1.1\"?>" + xml.substring(xml.indexOf("?>") + 2);
      }
    }
    return xml.getBytes();
  }
}
