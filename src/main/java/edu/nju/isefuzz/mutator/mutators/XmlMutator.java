package edu.nju.isefuzz.mutator.mutators;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Random;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import edu.nju.isefuzz.model.Seed;
import edu.nju.isefuzz.mutator.Mutator;

/**
 * 针对XML文件的特化Mutator，支持合法与崩溃样本的生成。
 */
public class XmlMutator implements Mutator {

  private final Random random = new Random();

  @Override
  public Seed mutate(Seed seed) {
    try {
      // 提取原始声明
      String originalDeclaration = extractXmlDeclaration(seed.getContent());

      // 判断是生成合法XML还是崩溃样本，但xmllint对崩溃的范围较宽松，不一定崩溃
      boolean generateValidXml = random.nextInt(10) < 8; // 80%生成合法XML，20%生成崩溃样本
      int mutationCount = random.nextInt(3) + 1; // 随机选择1到3次变异
      mutationCount = 1; // 修改为随机选择1次变异

      if (generateValidXml) {
        System.out.println("====合法样本处理====");
        Document document = parseXml(seed.getContent());
        for (int i = 0; i < mutationCount; i++) {
          applyRandomMutation(document);
        }
        byte[] mutatedContent = serializeXml(document, originalDeclaration);
        return new Seed(mutatedContent, seed.getFileType(), false);
      } else {
        System.out.println("====崩溃样本处理====");
        byte[] mutatedContent = havocMutate(seed.getContent(), originalDeclaration);
        return new Seed(mutatedContent, seed.getFileType(), false);
      }
    } catch (Exception e) {
      System.err.println("Mutation failed: " + e.getMessage());
      return seed; // 如果变异中发生了失败，返回原始种子
    }
  }

  /**
   * 使用XML解析器解析字节数组。
   */
  private Document parseXml(byte[] content) throws Exception {
    ByteArrayInputStream inputStream = new ByteArrayInputStream(content);
    return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream);
  }

  /**
   * 提取初始种子的XML声明。
   */
  private String extractXmlDeclaration(byte[] content) {
    String xml = new String(content);
    if (xml.startsWith("<?xml")) {
      int end = xml.indexOf("?>") + 2;
      return xml.substring(0, end); // 提取原始声明
    }
    return ""; // 如果没有声明，则返回空
  }

  /**
   * 将XML Document序列化为字节数组，同时保持原始声明。
   */
  private byte[] serializeXml(Document document, String originalDeclaration) throws Exception {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    Transformer transformer = TransformerFactory.newInstance().newTransformer();
    transformer.setOutputProperty("omit-xml-declaration", "yes"); // 不自动生成声明
    transformer.transform(new DOMSource(document), new StreamResult(outputStream));
    String serializedContent = new String(outputStream.toByteArray());

    // 添加原始声明
    if (!originalDeclaration.isEmpty()) {
      serializedContent = originalDeclaration + "\n" + serializedContent;
    }

    return serializedContent.getBytes();
  }

  /**
   * 在XML文档树上随机应用变异操作。
   */
  private void applyRandomMutation(Document document) {
    int choice = random.nextInt(4);
    switch (choice) {
      case 0:
        removeRandomNode(document);
        break;
      case 1:
        insertRandomNode(document);
        break;
      case 2:
        mutateNodeText(document);
        break;
      case 3:
        addRandomAttribute(document);
        break;
    }
  }

  /**
   * 删除随机节点。
   */
  private void removeRandomNode(Document document) {
    NodeList nodes = document.getElementsByTagName("*");
    if (nodes.getLength() > 0) {
      int index = random.nextInt(nodes.getLength());
      Element node = (Element) nodes.item(index);
      if (node.getParentNode() != null) {
        node.getParentNode().removeChild(node);
      }
    }
  }

  /**
   * 插入随机节点。
   */
  private void insertRandomNode(Document document) {
    NodeList nodes = document.getElementsByTagName("*");
    if (nodes.getLength() > 0) {
      int index = random.nextInt(nodes.getLength());
      Element parent = (Element) nodes.item(index);

      Element newNode = document.createElement("random");
      newNode.setTextContent("data");
      parent.appendChild(newNode);
    }
  }

  /**
   * 修改节点文本内容。
   */
  private void mutateNodeText(Document document) {
    NodeList nodes = document.getElementsByTagName("*");
    if (nodes.getLength() > 0) {
      int index = random.nextInt(nodes.getLength());
      Element node = (Element) nodes.item(index);
      node.setTextContent("mutated" + random.nextInt(1000));
    }
  }

  /**
   * 添加随机属性。
   */
  private void addRandomAttribute(Document document) {
    NodeList nodes = document.getElementsByTagName("*");
    if (nodes.getLength() > 0) {
      int index = random.nextInt(nodes.getLength());
      Element node = (Element) nodes.item(index);

      // 生成随机属性
      String attrName = "attr" + random.nextInt(100);
      String attrValue = "value" + random.nextInt(1000);
      node.setAttribute(attrName, attrValue);
    }
  }

  /**
   * 执行崩溃样本的随机破坏性变异。
   * @return 变异后的二进制内容
   */
  private byte[] havocMutate(byte[] originalContent, String originalDeclaration) {
    String xml = new String(originalContent);

    try {
      // 添加原始声明
      if (!originalDeclaration.isEmpty() && !xml.startsWith("<?xml")) {
        xml = originalDeclaration + "\n" + xml;
      }

      // 随机选择一种破坏性变异操作
      int mutationType = random.nextInt(5);
      switch (mutationType) {
        case 0: // 插入未转义的非法字符
          int pos = random.nextInt(xml.length());
          char randomChar = (char) (random.nextInt(256));
          xml = xml.substring(0, pos) + randomChar + xml.substring(pos);
          break;

        case 1: // 删除根节点
          xml = xml.replaceFirst("<[^>]+>", "").replaceFirst("</[^>]+>", "");
          break;

        case 2: // 插入未闭合的标签
          int insertPos = random.nextInt(xml.length());
          String unclosedTag = "<unclosed>";
          xml = xml.substring(0, insertPos) + unclosedTag + xml.substring(insertPos);
          break;

        case 3: // 修改XML声明为非法内容
          xml = xml.replaceFirst("<\\?xml.*?\\?>", "<?xml version=\"1.invalid\"?>");
          break;

        case 4: // 删除随机部分的标签或内容
          int randomTagStart = xml.indexOf('<', random.nextInt(xml.length()));
          int randomTagEnd = xml.indexOf('>', randomTagStart);
          if (randomTagStart >= 0 && randomTagEnd > randomTagStart) {
            xml = xml.substring(0, randomTagStart) + xml.substring(randomTagEnd + 1);
          }
          break;
      }
    } catch (Exception e) {
      System.err.println("Havoc mutation failed: " + e.getMessage());
    }
    return xml.getBytes();
  }
}
