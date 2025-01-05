package edu.nju.isefuzz.Evaluation;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.imageio.ImageIO;

public class EvaluationComponent {
    static class DataPoint {
        private int cntOfBlocks;
        private double executeTime;

        public DataPoint(int cntOfBlocks, double executeTime) {
            this.cntOfBlocks = cntOfBlocks;
            this.executeTime = executeTime;
        }

        public int getCntOfBlocks() {
            return cntOfBlocks;
        }

        public double getExecuteTime() {
            return executeTime;
        }
    }

    public static void draw(String programName, List<DataPoint> dataPoints, String outputFileName) {
        XYSeries series = new XYSeries("Execution Blocks over Time");
        for (DataPoint dp : dataPoints) {
            series.add(dp.getExecuteTime(), dp.getCntOfBlocks());
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series);

        JFreeChart chart = ChartFactory.createXYLineChart(
                programName + " Region Coverage",
                "Time (Hour)",
                "Region Coverage",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        // 保存图表为图片
        String format = "PNG";
        try {
            BufferedImage image = chart.createBufferedImage(800, 600);
            File file = new File(outputFileName);
            ImageIO.write(image, format, file);
            System.out.println("图表已成功保存为: " + outputFileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<DataPoint> readDataPoints(String filePath) {
        List<DataPoint> dataPoints = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            // 读取第一行作为程序名（忽略）
            String programName = reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ");
                if (parts.length >= 2) {
                    double executeTime = Double.parseDouble(parts[0]);
                    int cntOfBlocks = Integer.parseInt(parts[1]);
                    dataPoints.add(new DataPoint(cntOfBlocks, executeTime));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dataPoints;
    }

    public static void processFilesInDirectory(String directoryPath, String outputDirectoryPath) {
        File directory = new File(directoryPath);
        if (!directory.isDirectory()) {
            System.err.println("提供的路径不是目录: " + directoryPath);
            return;
        }

        File outputDirectory = new File(outputDirectoryPath);
        if (!outputDirectory.exists()) {
            if (!outputDirectory.mkdirs()) {
                System.err.println("无法创建输出目录: " + outputDirectoryPath);
                return;
            }
        }

        File[] files = directory.listFiles((dir, name) -> name.endsWith(".txt"));
        if (files == null || files.length == 0) {
            System.err.println("目录中没有找到任何 .txt 文件: " + directoryPath);
            return;
        }

        for (File file : files) {
            System.out.println("正在处理文件: " + file.getName());
            List<DataPoint> dataPoints = readDataPoints(file.getAbsolutePath());

            String programName = file.getName().replaceFirst("\\.txt$", ""); // 去掉扩展名作为程序名
            String outputFileName = outputDirectoryPath + File.separator + programName + ".png"; // 输出图片名

            draw(programName, dataPoints, outputFileName);
        }
    }

    public static void main(String[] args) {
        String inputDirectoryPath = "D:\\work tools\\project\\fuzzer-demo\\graphic_data";
        String outputDirectoryPath = "D:\\work tools\\project\\fuzzer-demo\\graphic_result";
        processFilesInDirectory(inputDirectoryPath, outputDirectoryPath);
    }
}
