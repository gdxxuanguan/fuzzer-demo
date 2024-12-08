package edu.nju.isefuzz;

import edu.nju.isefuzz.energyScheduler.EnergyScheduler;
import edu.nju.isefuzz.executor.ExecutorUtils;
import edu.nju.isefuzz.executor.SeedHandler;
import edu.nju.isefuzz.model.ExecutionResult;
import edu.nju.isefuzz.model.Seed;
import edu.nju.isefuzz.mutator.MutatorUtils;
import edu.nju.isefuzz.seedSorter.*;
import edu.nju.isefuzz.seedSorter.SortingStrategy;
import edu.nju.isefuzz.util.DirectoryUtils;
import edu.nju.isefuzz.util.PriorityCalculator;
import edu.nju.isefuzz.util.TempFileHandler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.util.*;
import java.util.logging.Logger;

/**
 * CoverageBasedMutationFuzzer 是一个基于覆盖率的变异模糊测试工具。
 * 它通过对初始种子进行变异，执行目标程序，并根据执行结果更新种子的优先级，
 * 以便更有效地探索目标程序的代码路径和发现潜在漏洞。
 *
 * 使用方法:
 * java CoverageBasedMutationFuzzer <target_program_path> <initial_seed_path> <output_directory>
 *
 * 参数:
 * <target_program_path>    - 目标程序的可执行文件路径
 * <initial_seed_path>      - 初始种子的文件路径
 * <output_directory>       - 输出目录
 */
public class CoverageBasedMutationFuzzer {
    private static final Logger logger = Logger.getLogger(CoverageBasedMutationFuzzer.class.getName());
    // 定义最大种子队列大小，用于触发过滤步骤
    private static final int MAX_QUEUE_SIZE = 500;

    private static final int ENERGY_PER_MUTATION = 10;

    // 定义硬编码的临时目录路径
    private static final String TEMP_DIR = "/tmp/fuzzing_temp/";

    private static final String COVERAGE_COLLECTOR_PATH = "/cpptest/coverage_collector2";

    public static void main(String[] args) throws Exception {
        // 检查命令行参数的数量是否正确
        if (args.length < 3) {
            System.err.println("Usage: java CoverageBasedMutationFuzzer <target_program_path> <initial_seed_path> <output_directory>");
            System.exit(1);
        }

        String projectRootPath = System.getProperty("user.dir");
        // 从命令行参数中获取目标程序路径、初始种子路径和输出目录
        String targetProgramPath = projectRootPath + args[0];
        String initialSeedPath = projectRootPath + args[1];
        String outputDir = args[2];

        try {
            // 调用 DirectoryUtils.resetDirectory 来处理 outputDir
            DirectoryUtils.resetDirectory(outputDir);
        } catch (IOException e) {
            System.err.println("处理目录时发生错误: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        //获取target名字,创建结果存储文件
        String[] tmp = targetProgramPath.split("/");
        File file = new File(outputDir + "/" + tmp[tmp.length-1] + ".txt");

        //判断目标程序是否存在
        Path targetPath = Paths.get(targetProgramPath);
        if (!Files.exists(targetPath) || !Files.isRegularFile(targetPath)) {
            System.err.println("Target program does not exist or is not a regular file: " + targetProgramPath);
            System.exit(1);
        }

        // 加载初始种子
        Seed initialSeed = loadInitialSeed(initialSeedPath, targetProgramPath);
        if (initialSeed == null) {
            System.err.println("Failed to load the initial seed from: " + initialSeedPath);
            System.exit(1);
        }
        // 初始化已覆盖块集合，用于跟踪覆盖的代码块
        Set<Integer> coveredBlocks = new HashSet<>();

        // 初始化 PriorityCalculator，用于计算种子的优先级分数
        PriorityCalculator priorityCalculator = new PriorityCalculator();

        // 初始化 SeedHandler，用于处理执行结果并更新种子状态
        SeedHandler seedHandler = new SeedHandler(priorityCalculator);

        // 初始化 SeedSorter 和选择的排序策略（如基于优先级的排序策略）
        SeedSorter seedSorter = new SeedSorter(SortingStrategy.PRIORITY); // 默认使用优先级排序策略


        // 初始化能量调度器
        EnergyScheduler energyScheduler = new EnergyScheduler();
        energyScheduler.startEnergyRecovery();

        // 将初始种子添加到能量调度器中
        energyScheduler.addSeed(initialSeed);

        // 初始化已观察到的执行结果集合，用于标记优先种子
        Set<ExecutionResult> observedResults = new HashSet<>();

        // 将初始种子添加到种子排序器和能量调度器中
        seedSorter.addSeed(initialSeed);

        // 记录模糊测试的轮次
        int fuzzRound = 0;

        // 添加 Shutdown Hook 以在程序终止时进行后处理
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown detected. Performing postprocess...");
//            postprocess(outputDirectory, energyScheduler);
        }));

        // 主循环：持续执行直到手动停止（如 Ctrl+C）
        while (true) {
            fuzzRound++; // 更新模糊测试轮次
            //从 SeedSorter 中获取优先级最高的种子
            Seed selectedSeed = seedSorter.pollSeed();

            System.out.printf("[FUZZER] Pick seed `%s`, queue_size `%d`\n",
                    selectedSeed, seedSorter.getQueueSize());


            // 根据能量调度器决定生成的变异种子数量
            int mutationCount = energyScheduler.determineMutationCount(selectedSeed);
            if (mutationCount == 0) {
                System.out.printf("[FUZZER] Seed `%s` has no energy left. Skipping...\n", selectedSeed);
                continue;
            }

            System.out.printf("[FUZZER] FuzzRnd No.%d, selected seed `%s`, mutation count `%d`\n",
                    fuzzRound, selectedSeed, mutationCount);
            Thread.sleep(5000);

            Set<Seed> testInputs = new HashSet<>();
            for (int i = 0; i < mutationCount; i++) {
                Seed mutatedSeed = MutatorUtils.mutateSeed(selectedSeed);
                testInputs.add(mutatedSeed);
            }

            // 执行变异种子
            for (Seed testInput : testInputs) {
                // 使用 try-with-resources 确保 TempFileHandler 自动关闭
                try (TempFileHandler tempFileHandler = new TempFileHandler(testInput.getContent(), TEMP_DIR)) {
                    String tempFilePath = tempFileHandler.getTempFilePath();

                    if (tempFilePath == null) {
                        logger.warning("Failed to write test input to temp file. Skipping this input.");
                        continue;
                    }
                    System.out.printf("[FUZZER] Temp file path: %s%n", tempFilePath);

                    // 执行目标程序并获取执行结果
                    ExecutionResult execResult;
                    try {
                        execResult = ExecutorUtils.executeCpp(
                                projectRootPath + COVERAGE_COLLECTOR_PATH,
                                targetProgramPath,
                                tempFilePath,
                                coveredBlocks
                        );
                        logger.info("Execution Result: " + execResult.getInfo());
                    } catch (Exception e) {
                        logger.severe("Execution failed: " + e.getMessage());
                        continue; // 跳过当前测试输入，继续下一个
                    }

                    //将覆盖率和执行时间写入存储文件
                    String executeTime = execResult.getExecuteTime();
                    int cntOfBlocks = execResult.getCntOfBlocks();

                    // 将秒数转换为小时数，保留两位小数
                    double executeTimeInSeconds = Double.parseDouble(executeTime);
                    double executeTimeInHours = executeTimeInSeconds / 3600;
                    DecimalFormat df = new DecimalFormat("#.##");
                    String executeHours = df.format(executeTimeInHours);

                    try {
                        // 以追加模式打开文件，第二个参数设置为true
                        BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
                        writer.write(cntOfBlocks + " " + executeHours + "\n");
                        writer.close();
                        System.out.println("数据已成功追加保存到 " + file.getAbsolutePath());
                    } catch (IOException e) {
                        logger.severe("存储失败: " + e.getMessage());
                    }

                    // 创建潜在种子对象
                    Seed potentialSeed = new Seed(testInput.getContent(), getFileExtension(targetProgramPath), false);

                    // 如果种子列表中已存在该种子，则跳过
                    if (seedSorter.contains(potentialSeed)) {
                        continue;
                    }

                    // 处理执行结果
                    seedHandler.handleExecutionResult(potentialSeed, execResult, observedResults);

                    // 将潜在种子添加到能量调度器和种子列表中
                    energyScheduler.addSeed(potentialSeed);
                    seedSorter.addSeed(potentialSeed);

                    // 更新种子的能量状态
                    energyScheduler.updateEnergy(potentialSeed, execResult);

                    // 消耗种子的能量
                    energyScheduler.consumeEnergy(selectedSeed, ENERGY_PER_MUTATION);


                } catch (IOException e) {
                    logger.severe("Failed to handle temporary file: " + e.getMessage());
                }
            }
            seedSorter.addSeed(selectedSeed);

            // 种子队列过滤：当队列大小超过阈值时，移除不优先的种子
            if (seedSorter.getQueueSize() > MAX_QUEUE_SIZE) { // MAX_QUEUE_SIZE = 500
                logger.info(String.format("[FUZZER] Queue size (%d) exceeds MAX_QUEUE_SIZE. Shrinking queue...",
                        seedSorter.getQueueSize()));

                // 移除不优先的种子
                Iterator<Seed> iterator = seedSorter.getIterator();
                int removedCount = 0;
                while (iterator.hasNext() && seedSorter.getQueueSize() > MAX_QUEUE_SIZE) {
                    Seed seed = iterator.next();
                    if (!seed.isFavored()) {
                        iterator.remove();
                        energyScheduler.removeSeed(seed);
                        removedCount++;
                    }
                }
                logger.info(String.format("[FUZZER] Shrink queue by removing %d unfavored seeds. New queue size: %d",
                        removedCount, seedSorter.getQueueSize()));
            }
            // 可选：根据某些条件切换排序策略，例如每100轮切换一次
            if (fuzzRound % 100 == 0) {
                SortingStrategy newStrategy = seedSorter.determineNewStrategy(fuzzRound);
                seedSorter.switchStrategy(newStrategy);
            }

        }

    }

    /**
     * 加载初始种子文件，创建 Seed 对象。
     *
     * @param seedPath 初始种子文件的路径
     * @return 加载的 Seed 对象，若加载失败则返回 null
     */
    private static Seed loadInitialSeed(String seedPath, String targetProgramPath) {
        Path path = Paths.get(seedPath);
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            System.out.println("Seed file does not exist or is not a regular file: " + seedPath);
            return null;
        }

        try {
            byte[] content = Files.readAllBytes(path);
            String fileType = getFileExtension(targetProgramPath);
            // 创建 Seed 对象，初始种子可以根据需要设置是否为优先种子
            Seed seed = new Seed(content, fileType, true);
            System.out.printf("Loaded initial seed: %s (%d bytes)\n", seed, content.length);
            return seed;
        } catch (IOException e) {
            logger.severe("Failed to read initial seed file: " + e.getMessage());
            return null;
        }
    }


    /**
     * 获取给定路径的最后一个部分（文件名）。
     *
     * @param path 要处理的路径字符串
     * @return 路径的最后一个部分（文件名），若路径为空则返回空字符串
     */
    private static String getFileExtension(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        Path p = Paths.get(path);
        Path fileName = p.getFileName();
        return (fileName != null) ? fileName.toString() : "";
    }
}