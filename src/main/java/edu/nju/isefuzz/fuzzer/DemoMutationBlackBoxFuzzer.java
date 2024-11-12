package edu.nju.isefuzz.fuzzer;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class DemoMutationBlackBoxFuzzer {


    /**
     * The entry point of fuzzing.
     */
    public static void main(String[] args) throws Exception {

        // Initialize. Parse args and prepare seed queue
        if (args.length != 2) {
            System.out.println("DemoMutationBlackBoxFuzzer: <classpath> <out_dir>");
            System.exit(0);
        }
        String cp = args[0];
        File outDir = new File(args[1]);
        System.out.println("[FUZZER] cp: " + cp);
        System.out.println("[FUZZER] outDir: " + outDir.getAbsolutePath());
        List<Seed> seedQueue = prepare();
//        List<Seed> crashingInputs = new ArrayList<>();

        // Dry-run phase. Run all the given seeds once.
//        List<Seed> seedQueue = prepare();

        // Main fuzzing loop.
        int fuzzRnd = 0;
        boolean findCrash = false;
        Set<ExecutionResult> observedRes = new HashSet<>();
        while (true) {
            // Seed scheduling: no seed prioritication and pick next seed. Update round number.
            Seed nextSeed = pickSeed(seedQueue, ++fuzzRnd);
            System.out.printf("[FUZZER] Pick seed `%s`, queue_size `%d`\n",
                    nextSeed, seedQueue.size());

            // Generate offspring inputs.
            Set<String> testInputs = generate(nextSeed,nextSeed.fileType,new HashSet<>());

            // Execute each test input.
            for (String ti : testInputs) {
                String path=writePngToTempFile(ti.getBytes());
                System.out.printf("[FUZZER] FuzzRnd No.%d, execute the target with input `%s`",
                        fuzzRnd, path);
                ExecutionResult execRes = execute(cp, path);
                System.out.println(execRes.info);

                // Output analysis.
                // Update queue.
                Seed potentialSeed = new Seed(ti);
                if (seedQueue.contains(potentialSeed))
                    continue;
                // Identify crash
                if (execRes.isCrash()) {
                    // Exit directly once find a crash.
//                    System.out.printf("[FUZZER] Find a crashing input `%s`\n", ti);
//                    System.exit(0);
                    // Try to record these seeds.
                    findCrash = true;
                    potentialSeed.markCrashed();
                }
                // Identify favored seeds.
                if (!observedRes.contains(execRes)) {
                    potentialSeed.markFavored();
                    observedRes.add(execRes);
                    System.out.printf("[FUZZER] Find a favored seed `%s`\n", potentialSeed);
                }
                seedQueue.add(potentialSeed);
            }

            // Seed scheduling: seed retirement.
            if (seedQueue.size() > 500 || findCrash) {
                int oriSize = seedQueue.size();

                // Remove previously unfavored seeds.
                List<Seed> unfavoredSeeds = new ArrayList<>();
                seedQueue.stream()
                         .filter(s -> !s.isFavored)
                         .forEach(unfavoredSeeds::add);
                seedQueue.removeAll(unfavoredSeeds);
                System.out.printf("[FUZZER] Shrink queue, size: %d -> %d\n",
                        oriSize, seedQueue.size());

            }

            // Break to reach postprocess
            if (findCrash)
                break;

        } /* End of the main fuzzing loop */

        // Postprocess. Seed preservation (favored & crashed).
        postprocess(outDir, seedQueue);

    }

    /**
     * A simple ADT for seed inputs.
     */
    private static class Seed {

        String content;
        boolean isFavored;
        String fileType;
        boolean isCrash;

        Seed(String content, boolean isFavored) {
            this.content = content;
            this.isFavored = isFavored;
            this.isCrash = false;
            this.fileType = "unknown";
        }
        Seed(File pngFile) throws IOException {
            if (pngFile == null || !pngFile.exists() || !pngFile.isFile()) {
                throw new IllegalArgumentException("Invalid file provided.");
            }

            // 读取 PNG 文件内容为字节数组并转换为字符串表示
            byte[] fileBytes = Files.readAllBytes(pngFile.toPath());
            this.content = new String(fileBytes);
            this.isFavored = true; // 默认设置为未受欢迎
            this.isCrash = false;
            this.fileType = "png";
        }

        Seed(String content) {
            this(content, false);
        }

        public void markFavored() {
            this.isFavored = true;
        }

        public void markCrashed() {
            this.isCrash = true;
        }

        @Override
        public boolean equals(Object that) {
            if (that instanceof Seed)
                return ((Seed) that).content.equals(this.content);
            return false;
        }

        @Override
        public String toString() {
            String suffix = this.isFavored ? "@favored" : "@unfavored";
            return this.content + suffix;
        }
    }

    /**
     * An exemplified seed.
     */
//    static Seed initSeed = new Seed("abcde", true);
    static Seed initSeed = new Seed("helln", true);
    static File pngFile = new File("testcases/images/png/not_kitty.png");
    static Seed pngSeed;

    static {
        try {
            pngSeed = new Seed(pngFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The preparation stage for fuzzing. At this stage, we tend to
     * collect seeds to build and corpus and minimize the corpus to
     * produce a selective seed queue for fuzzing
     */
    private static List<Seed> prepare() {
        return new ArrayList<>(Collections.singletonList(pngSeed));
    }


    /**
     * Pick the next seed. Avoid out of bound.
     */
    private static Seed pickSeed(List<Seed> seeds, int rnd) {
        int pos = (rnd - 1) % seeds.size();
        return seeds.get(pos);
    }

    /**
     * The essential component of a mutation-based fuzzer. This method
     * mutates the given seed once to produce an offspring test input.
     * Here the method implements a simple mutator by adding the character
     * at the given position by step. Besides, this method ensures the
     * mutated character is in [a-z];
     *
     * @param sCont the content of the parent seed input.
     * @param pos   the position of the character to be mutated
     * @param step  the step of character flipping.
     * @return an offspring test input
     */
    private static String mutate(String sCont, int pos, int step) {
        // Locate target character
        char[] charArr = sCont.toCharArray();
        char oriChar = charArr[pos];

        // Mutate this char and make sure the result is in [a-z].
        char mutChar = oriChar + step > 'z' ?
                (char) ((oriChar + step) % 'z' - 1 + 'a') :
                (char) (oriChar + step);

        // Replace the char and return offspring test input.
        charArr[pos] = mutChar;
        return new String(charArr);
    }

    /**
     * Call (different flavors of) mutation methods/mutators several times
     * to produce a set of test inputs for subsequent test executions. This
     * method also showcases a simple power scheduling. The power, i.e., the
     * number of mutations, is affected by the flag {@link Seed#isFavored}.
     * A favored seed is mutated 10 times as an unfavored seed.
     *
     * @param seed  the parent seed input
     * @return a set of offspring test inputs.
     */
    private static Set<String> generate(Seed seed) {

        // Draw seed content, i.e., the real test input.
        String sCont = seed.content;

        // Power scheduling.
        int basePower = 5;
        int power = seed.isFavored ? basePower * 10 : basePower;

        // Test generation.
        Set<String> testInputs = new HashSet<>(power);
        for (int i = 0; i < power; i++) {
            // Avoid array out of bound.
            int pos  = i % sCont.length();
            int step = i / sCont.length() + 1;
            // Mutate.
            testInputs.add(mutate(sCont, pos, step));
        }

        return testInputs;
    }

    public static Set<String> generate(Seed seed, String fileType, Set<String> otherSeeds) {
        String sCont = seed.content;
        int basePower = 5;
        int power = seed.isFavored ? basePower * 10 : basePower; // 权重影响生成数量
        Set<String> testInputs = new HashSet<>(power);

        for (int i = 0; i < power; i++) {
            // 这里可以随机选择使用哪个变异算子
            String mutatedInput = Mutator.mutate(sCont, fileType, otherSeeds);
            testInputs.add(mutatedInput);
        }

        return testInputs;
    }

    /**
     * A simple wrapper for execution result
     */
    private static class ExecutionResult {
        String info;
        int exitVal;

        ExecutionResult(String info, int exitVal) {
            this.info = info;
            this.exitVal = exitVal;
        }

        public boolean isCrash() {
            return exitVal != 0;
        }

        @Override
        public boolean equals(Object that) {
            if (that instanceof ExecutionResult)
                return ((ExecutionResult) that).info.equals(this.info);
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(info);
        }
    }


    /**
     * An execution method for Java-main fuzz targets/drivers. The method
     * execute the given fuzz target once and return the output of the
     * fuzz target.
     *
     * @param cp classpath to the fuzz target
     *
     * @param ti (the content of) the test input
     * @return the output of the fuzz target.
     * @throws IOException if the executor starts wrongly.
     */
    private static ExecutionResult execute(
            String cp, String ti) throws IOException, InterruptedException {
        // Construct executor
        String projectRoot = System.getProperty("user.dir");  // 获取项目根目录
        cp = projectRoot + cp;
        ti=projectRoot+ti;
        ProcessBuilder pb = new ProcessBuilder(cp, ti);

        // Redirect execution result to here and execute.
        pb.redirectErrorStream(true);
        Process p = pb.start();
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        // Wait for execution to finish, or we cannot get exit value.
        p.waitFor();

        // Read execution info
        StringBuilder infoBuilder = new StringBuilder();
        String line;
        while (true) {
            line = br.readLine();
            if (line == null)
                break;
            else
                infoBuilder.append('\n');
            infoBuilder.append(line);
        }

        // Wrap and return execution result
        return new ExecutionResult(infoBuilder.toString(), p.exitValue());
    }

    private static void postprocess(File outDir, List<Seed> seeds) throws IOException {
        // Delete old outDir
        if (outDir.exists()) {
            FileUtils.forceDelete(outDir);
            System.out.println("[FUZZER] Delete old output directory.");
        }
        boolean res = outDir.mkdirs();
        if (res)
            System.out.println("[FUZZER] Create output directory.");
        File queueDir = new File(outDir, "queue");
        File crashDir = new File(outDir, "crash");
        res = queueDir.mkdir();
        if (res)
            System.out.println("[FUZZER] Create queue directory: " + queueDir.getAbsolutePath());
        res = crashDir.mkdir();
        if (res)
            System.out.println("[FUZZER] Create crash directory: " + crashDir.getAbsolutePath());
        // Record seeds.
        for (Seed s : seeds) {
            File seedFile;
            if (s.isCrash)
                seedFile = new File(crashDir, s.content);
            else
                seedFile = new File(queueDir, s.content);
            FileWriter fw = new FileWriter(seedFile);
            fw.write(s.content);
            fw.close();
            System.out.println("[FUZZER] Write test input to: " + seedFile.getAbsolutePath());
        }

    }

    public static String writePngToTempFile(byte[] pngData) throws IOException {
        // 创建一个临时文件存储变异后的 PNG 数据
        File tempFile = File.createTempFile("mutated_", ".png", new File(System.getProperty("java.io.tmpdir")));
        tempFile.deleteOnExit(); // 程序结束后自动删除临时文件

        // 将数据写入临时文件
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(pngData);
        }

        System.out.println("临时文件路径: " + tempFile.getAbsolutePath());
        return tempFile.getCanonicalPath();
    }


}
