package edu.nju.isefuzz.Evaluation;

import java.io.*;
import java.util.ArrayList;

public class EvaluationTableComponent {
    public static void main(String[] args) {
        ArrayList<String[]> data = new ArrayList<>();

        // 指定输入目录
        String inputDirectory = "D:\\work tools\\project\\fuzzer-demo\\graphic_data";
        File directory = new File(inputDirectory);

        // 获取所有txt文件
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".txt"));
        if (files == null || files.length == 0) {
            System.err.println("目录中没有找到任何 .txt 文件: " + inputDirectory);
            return;
        }

        // 处理每个文件
        int tidCounter = 1; // 用于生成递增的TID
        for (File file : files) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String target = reader.readLine(); // 读取第一行作为Target
                String line;
                String lastLine = null;

                // 读取最后一行
                while ((line = reader.readLine()) != null) {
                    lastLine = line;
                }

                if (lastLine != null) {
                    String[] parts = lastLine.split(" ");
                    String coverage = parts[1];
                    String tid = String.format("T%02d", tidCounter++); // 格式化为T01, T02...
                    String[] row = {tid, target, coverage};
                    data.add(row);
                }

                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // 将ArrayList转换为二维数组
        String[][] tableData = new String[data.size() + 1][3];
        // 设置表头
        tableData[0] = new String[]{"TID", "Target", "Fuzzer #Reg."};
        for (int i = 0; i < data.size(); i++) {
            tableData[i + 1] = data.get(i);
        }

        // 打印数据行
        for (String[] row : tableData) {
            System.out.println(row[0] + "\t" + row[1] + "\t" + row[2]);
        }

        // 保存为CSV文件的逻辑
        try (FileWriter writer = new FileWriter("D:\\work tools\\project\\fuzzer-demo\\graphic_data_table.csv")) {
            // 写入表头
            for (int i = 0; i < tableData[0].length; i++) {
                writer.write(tableData[0][i]);
                if (i < tableData[0].length - 1) {
                    writer.write(",");
                }
            }
            writer.write("\n");

            // 写入数据行
            for (int rowIndex = 1; rowIndex < tableData.length; rowIndex++) {
                for (int colIndex = 0; colIndex < tableData[rowIndex].length; colIndex++) {
                    writer.write(tableData[rowIndex][colIndex]);
                    if (colIndex < tableData[rowIndex].length - 1) {
                        writer.write(",");
                    }
                }
                writer.write("\n");
            }
            System.out.println("表格数据已成功保存为CSV文件：graphic_data_table.csv");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
