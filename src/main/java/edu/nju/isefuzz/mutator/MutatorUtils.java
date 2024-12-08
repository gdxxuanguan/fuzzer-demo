package edu.nju.isefuzz.mutator;

import edu.nju.isefuzz.model.Seed;
import edu.nju.isefuzz.mutator.mutators.CxxFiltMutator;
import edu.nju.isefuzz.mutator.mutators.ElfMutator;
import edu.nju.isefuzz.mutator.mutators.JpegMutator;
import edu.nju.isefuzz.mutator.mutators.JsMutator;
import edu.nju.isefuzz.mutator.mutators.LuaMutator;
import edu.nju.isefuzz.mutator.mutators.PcapMutator;
import edu.nju.isefuzz.mutator.mutators.PngMutator;
import edu.nju.isefuzz.mutator.mutators.XmlMutator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * MutatorUtils，变异seed
 * 主要方法：Seed mutateSeed(Seed seed)
 * 调试和观察：mutateFile(String fileType, String inputFilePath, String outputDirPath, int iterations)
 * fileType是10个target的名称 如cxxfilt，objdump
 */

public class MutatorUtils {
  private static final Logger logger = Logger.getLogger(MutatorUtils.class.getName());

  private static final Map<String, Mutator> mutatorRegistry = new HashMap<>();
  private static final Map<Class<? extends Mutator>, String> mutatorFileExtensions = new HashMap<>();
  static {
    // 注册所有支持的 Mutator
    mutatorRegistry.put("cxxfilt",new CxxFiltMutator());
    mutatorRegistry.put("djpeg",new JpegMutator());
    mutatorRegistry.put("luac", new LuaMutator());
    mutatorRegistry.put("mjs",new JsMutator());
    mutatorRegistry.put("nm-new",new ElfMutator());
    mutatorRegistry.put("objdump", new ElfMutator());
    mutatorRegistry.put("readelf", new ElfMutator());
    mutatorRegistry.put("readpng", new PngMutator());
    mutatorRegistry.put("tcpdump", new PcapMutator());
    mutatorRegistry.put("xmllint", new XmlMutator());

    // 注册变异器对应的文件扩展名
    mutatorFileExtensions.put(CxxFiltMutator.class, "txt");
    mutatorFileExtensions.put(JpegMutator.class, "jpg");
    mutatorFileExtensions.put(LuaMutator.class, "lua");
    mutatorFileExtensions.put(JsMutator.class, "js");
    mutatorFileExtensions.put(ElfMutator.class, "elf");
    mutatorFileExtensions.put(PngMutator.class, "png");
    mutatorFileExtensions.put(PcapMutator.class, "pcap");
    mutatorFileExtensions.put(XmlMutator.class, "xml");
  }

  /**
   * 根据原始Seed文件返回变异后的Seed。
   * @param mutator 对应的变异器
   * @param seed 原始Seed对象
   * @return 变异后的Seed对象
   * @throws Exception 如果发生异常，抛出
   */
  public static Seed mutateSeed(Mutator mutator, Seed seed) throws Exception {
    // 执行变异操作，返回变异后的Seed

      return mutator.mutate(seed);
  }

  /**
   * 变异指定的Seed对象，自动选择对应的变异器。
   *
   * @param seed 传入的种子对象
   * @return 变异后的Seed对象
   * @throws Exception 如果发生异常，抛出
   */
  public static Seed mutateSeed(Seed seed) throws Exception {
    // 获取文件类型
    String fileType = seed.getFileType();
    // 查找对应的变异器
    Mutator mutator = mutatorRegistry.get(fileType);
    if (mutator == null) {
      throw new IllegalArgumentException("未注册该类型的变异器: " + fileType);
    }
    // 执行变异，返回变异后的Seed
    return mutator.mutate(seed);
  }


  /**
   * 变异指定文件。
   * @param fileType 文件类型（例如：png, jpeg, lua）
   * @param inputFilePath 输入文件的绝对路径
   * @param outputDirPath 输出文件的目录路径
   * @param iterations 变异次数
   * @throws Exception 如果发生异常，抛出
   */
  public static void mutateFile(String fileType, String inputFilePath, String outputDirPath, int iterations) throws Exception {
    // 检查是否有对应的变异器
    Mutator mutator = mutatorRegistry.get(fileType);
    if (mutator == null) {
      throw new IllegalArgumentException("未注册该类型的变异器: " + fileType);
    }

    // 读取种子文件
    Path inputPath = Paths.get(inputFilePath);
    if (!Files.exists(inputPath)) {
      throw new IllegalArgumentException("输入文件不存在: " + inputFilePath);
    }
    byte[] initialContent = Files.readAllBytes(inputPath);

    // 创建Seed对象
    Seed seed = new Seed(initialContent, fileType, true);  // 假设所有种子都是优先种子
    // 测试变异
    for (int i = 0; i < iterations; i++) {
      // 执行变异，返回变异后的Seed
      Seed mutatedSeed = mutateSeed(seed);
      // 保存变异后的文件
      String outputFileName = outputDirPath + "/mutated_seed_" + i + "." + mutatorFileExtensions.get(mutator.getClass());
      Path outputPath = Paths.get(outputFileName);
      Files.write(outputPath, mutatedSeed.getContent());

      // 打印输出
      logger.info("Mutated seed " + i + " written to: " + outputFileName);
      printHexContent(mutatedSeed.getContent());
    }
  }

  /**
   * 打印文件内容的十六进制表示，便于调试。
   * @param content 文件内容
   */
  private static void printHexContent(byte[] content) {
    StringBuilder hexOutput = new StringBuilder();
    for (int i = 0; i < content.length; i++) {
      hexOutput.append(String.format("%02X ", content[i]));
      if ((i + 1) % 16 == 0) {
        hexOutput.append("\n");
      }
    }
    logger.info("Hex Content of Mutated Seed:\n" + hexOutput.toString());
  }

  public static void demoMutatorOperations() {
    try {
      // 基于当前项目目录的相对路径，假设运行时工作目录是项目的根目录
      Path baseDir = Paths.get("testcases");  // testcases 为存放种子文件的根目录

      // 确保临时输出目录存在，如果不存在则创建
      Path tempDirPath = Paths.get("temp");  // 临时输出目录
      if (Files.notExists(tempDirPath)) {
        Files.createDirectories(tempDirPath);  // 创建临时目录
      }

      // 示例：测试变异PNG文件
      logger.info("开始测试PNG文件变异...");
      Path pngInputPath = baseDir.resolve(Paths.get("images", "png", "not_kitty.png"));
      mutateFile(
          "readpng",  // 文件类型
          pngInputPath.toString(),  // 输入文件路径（相对路径）
          tempDirPath.toString(),  // 临时输出目录（相对路径）
          1  // 变异次数
      );

      // 示例：测试变异XML文件
      logger.info("开始测试XML文件变异...");
      Path xmlInputPath = baseDir.resolve(Paths.get("others", "xml", "small_document.xml"));
      mutateFile(
          "xmllint",  // 文件类型
          xmlInputPath.toString(),  // 输入文件路径（相对路径）
          tempDirPath.toString(),  // 临时输出目录（相对路径）
          1  // 变异次数
      );

    } catch (Exception e) {
      logger.severe("在变异过程中发生了错误: " + e.getMessage());
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    // 直接调用 demoMutatorOperations 来演示使用方法
    demoMutatorOperations();
  }


}
