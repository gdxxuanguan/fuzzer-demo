package edu.nju.isefuzz.Evaluation;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
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

    static ArrayList<DataPoint> dataPoints = new ArrayList<>();
    static String programName;

    public static void draw() {
        XYSeries series = new XYSeries("Execution Blocks over Time");
        for (int i = 0; i < dataPoints.size(); i++) {
            DataPoint dp = dataPoints.get(i);
            series.add(dp.getExecuteTime(), dp.getCntOfBlocks());
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series);

        JFreeChart chart = ChartFactory.createXYLineChart(
                 programName+" "+"Region Coverage",
                "Time (Hour)",
                programName+" "+"Region Coverage",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        ChartFrame frame = new ChartFrame("Chart", chart);
        frame.pack();
        frame.setVisible(true);

        // 选择要保存的图片格式，这里以PNG为例
        String format = "PNG";
        try {
            // 创建一个BufferedImage对象，将图表绘制到这个对象上
            BufferedImage image = chart.createBufferedImage(800, 600);
            // 指定保存的文件路径和文件名，这里保存到项目根目录下名为output.png的文件，可以按需修改路径
            File file = new File("D:\\Data\\output.png");
            // 使用ImageIO将BufferedImage保存为指定格式的图片文件
            ImageIO.write(image, format, file);
            System.out.println("图表已成功保存为 " + format + " 格式！");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("D:\\Data\\data.txt"));
            // 读取第一行作为程序名
            programName = reader.readLine();
            String line;
            while ((line = reader.readLine())!= null) {
                String[] parts = line.split(" ");
                if (parts.length == 2) {
                    int cntOfBlocks = Integer.parseInt(parts[0]);
                    double executeTime = Double.parseDouble(parts[1]);
                    dataPoints.add(new DataPoint(cntOfBlocks, executeTime));
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        draw();
    }
}
