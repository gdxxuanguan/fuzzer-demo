package edu.nju.isefuzz;

import edu.nju.isefuzz.energyScheduler.EnergyScheduler;
import edu.nju.isefuzz.executor.ExecutorUtils;
import edu.nju.isefuzz.executor.SeedHandler;
import edu.nju.isefuzz.model.ExecutionResult;
import edu.nju.isefuzz.model.Seed;
import edu.nju.isefuzz.mutator.MutatorUtils;
import edu.nju.isefuzz.seedSorter.SeedSorter;
import edu.nju.isefuzz.seedSorter.SortingStrategy;
import edu.nju.isefuzz.util.DirectoryUtils;
import edu.nju.isefuzz.util.PriorityCalculator;
import edu.nju.isefuzz.util.TempFileHandler;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
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

    private static final String COVERAGE_COLLECTOR_PATH = "/cpptest/coverage_collector";

    private static final long runDurationMillis = 24 * 60 * 60 * 1000L; // 24小时

    private static long totalExecs = 0;     // 总共执行过多少次测试输入
    private static long savedCrashesCount = 0; // 保存了多少crash种子

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
            // 调用 DirectoryUtils.ensureDirectoryExists 来处理 outputDir
            DirectoryUtils.ensureDirectoryExists(outputDir);
        } catch (IOException e) {
            System.err.println("处理目录时发生错误: " + e.getMessage());
            System.exit(1);
        }

        //获取target名字,创建结果存储文件
        String[] tmp = targetProgramPath.split("/");
        // 获取当前时间
        LocalDateTime now = LocalDateTime.now();
        // 定义时间格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("_yyyy_MM_dd_HH_mm_ss");
        String timestamp = now.format(formatter);
        File file = new File(outputDir + "/" + tmp[tmp.length-1] + "_plot_data_" + timestamp + ".txt");
        // 写入表头
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            // 如果文件是新创建的或为空，则写入表头
            if (file.length() == 0) {
                // 根据需要添加列名称，这里仅作为示例
                writer.write("# relative_time cycles_done queue_size covered_blocks saved_crashes execs_per_sec total_execs");
                writer.newLine();
            }
        }

        // 创建带有时间戳的 queue 目录
        String favorDirName = "queue_" + tmp[tmp.length-1] + "_" + timestamp;
        Path favorDir = Paths.get(outputDir, favorDirName);
        try {
            Files.createDirectories(favorDir);
        } catch (IOException e) {
            System.err.println("创建目录时发生错误: " + e.getMessage());
            System.exit(1);
        }
        File favorFile;
        long favorSeedIndex = 1L;

        // 创建带有时间戳的 crashes 目录
        String crashDirName = "crashes_" + tmp[tmp.length-1] + "_" + timestamp;
        Path crashDir = Paths.get(outputDir, crashDirName);
        try {
            Files.createDirectories(crashDir);
        } catch (IOException e) {
            System.err.println("创建目录时发生错误: " + e.getMessage());
            System.exit(1);
        }

        File crashFile;
        long crashSeedIndex = 1L;

        //获取测试用例名字
        String[] tmp2 = initialSeedPath.split("/");
        String testCaseName = tmp2[tmp2.length-1];

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

//        // 初始化已观察到的执行结果集合，用于标记优先种子
//        Set<ExecutionResult> observedResults = new HashSet<>();

        // 将初始种子添加到种子排序器和能量调度器中
        seedSorter.addSeed(initialSeed);

        // 记录模糊测试的轮次
        int fuzzRound = 0;

        // 添加 Shutdown Hook 以在程序终止时进行后处理
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown detected. Performing postprocess...");
//            postprocess(outputDirectory, energyScheduler);
        }));

        // 记录自测试开始时间
        Instant startInstant = Instant.now();
        // 主循环：持续执行直到24h或者手动停止（如 Ctrl+C）
        while (Duration.between(startInstant, Instant.now()).toMillis() < runDurationMillis) {
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

            Set<Seed> testInputs = new HashSet<>();
            for (int i = 0; i < mutationCount; i++) {
                Seed mutatedSeed = MutatorUtils.mutateSeed(selectedSeed);
                testInputs.add(mutatedSeed);
            }

            // 执行变异种子
            for (Seed testInput : testInputs) {
                totalExecs++;  // 增加执行次数计数
                // 使用 try-with-resources 确保 TempFileHandler 自动关闭
                try (TempFileHandler tempFileHandler = new TempFileHandler(testInput.getContent(), TEMP_DIR)) {
                    String tempFilePath = tempFileHandler.getTempFilePath();

                    if (tempFilePath == null) {
                        logger.warning("Failed to write test input to temp file. Skipping this inpucd t.");
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

                    // 计算已经运行的时间（小时）
                    Duration elapsedDuration = Duration.between(startInstant, Instant.now());
                    double elapsedHours = elapsedDuration.toMillis() / 3600000.0;
                    DecimalFormat df = new DecimalFormat("#.#######");
                    String elapsedHoursStr = df.format(elapsedHours);

                    // 计算 execs_per_sec
                    double execsPerSec = 1.0 / Double.parseDouble(execResult.getExecuteTime());

                    // 创建潜在种子对象
                    Seed potentialSeed = new Seed(testInput.getContent(), getFileExtension(targetProgramPath), false);

                    // 消耗种子的能量
                    energyScheduler.consumeEnergy(selectedSeed, ENERGY_PER_MUTATION);

                    // 如果种子列表中已存在该种子，则跳过
                    if (seedSorter.contains(potentialSeed)) {
                        continue;
                    }

                    // 处理执行结果
                    seedHandler.handleExecutionResult(potentialSeed, execResult);

                    //创建种子文件
                    //如果是favor，就将其创建到favor目录下
                    if (potentialSeed.isFavored() && execResult.isReachNewBlock()) {
                        favorFile = new File(favorDir+"/"+favorSeedIndex+"_"+testCaseName);
                        try (FileOutputStream fos = new FileOutputStream(favorFile)) {
                            fos.write(potentialSeed.getContent());
                            logger.info("已成功将favor种子存入" + favorFile.getAbsolutePath());
                            favorSeedIndex++;
                        } catch (IOException e) {
                            logger.severe("存入favor种子文件失败: " + e.getMessage());
                        }
                    }
                    //如果是crash，就将其创建到crash目录下
                    if (potentialSeed.isCrash() && execResult.isReachNewBlock()) {
                        savedCrashesCount++;
                        crashFile = new File(crashDir+"/"+crashSeedIndex+"_"+testCaseName);
                        try (FileOutputStream fos = new FileOutputStream(crashFile)) {
                            fos.write(potentialSeed.getContent());
                            logger.info("已成功将crash种子存入" + crashFile.getAbsolutePath());
                            crashSeedIndex++;
                        } catch (IOException e) {
                            logger.severe("存入crash种子文件失败: " + e.getMessage());
                        }
                    }

                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                        // 示例中写入 relative_time, fuzzRound, queue_size, covered_blocks, saved_crashes, execs_per_sec, total_execs
                        writer.write(
                                elapsedHoursStr + " " +
                                        fuzzRound + " " +
                                        seedSorter.getQueueSize() + " " +
                                        coveredBlocks.size() + " " +
                                        savedCrashesCount + " " +
                                        String.format("%.2f", execsPerSec) + " " +
                                        totalExecs
                        );
                        writer.newLine();
                    } catch (IOException e) {
                        logger.severe("存储失败: " + e.getMessage());
                    }

                    // 将潜在种子添加到能量调度器和种子列表中
                    energyScheduler.addSeed(potentialSeed);
                    seedSorter.addSeed(potentialSeed);
                    // 更新种子的能量状态
                    energyScheduler.updateEnergy(potentialSeed, execResult);

                } catch (IOException e) {
                    logger.severe("Failed to handle temporary file: " + e.getMessage());
                }
            }
            seedSorter.addSeed(selectedSeed);

            // 种子队列过滤：当队列大小超过阈值时，移除不优先的种子
            while (seedSorter.getQueueSize() > MAX_QUEUE_SIZE) { // MAX_QUEUE_SIZE = 500
                Seed leastPrioritySeed = null;
                Iterator<Seed> iterator = seedSorter.getIterator();
                while (iterator.hasNext()) {
                    leastPrioritySeed = iterator.next();
                }

                if (leastPrioritySeed != null) {
                    // 使用队列的内部方法 remove 来删除它
                    seedSorter.removeSeed(leastPrioritySeed);
                    energyScheduler.removeSeed(leastPrioritySeed);  // 如果你需要在调度器中移除它
                }
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