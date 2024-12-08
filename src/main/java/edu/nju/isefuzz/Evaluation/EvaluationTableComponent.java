package edu.nju.isefuzz.Evaluation;

import java.io.*;
import java.util.ArrayList;

public class EvaluationTableComponent {
    public static void main(String[] args) {
        ArrayList<String[]> data = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            try {
                File file = new File("D:\\Data\\data\\data" + i + ".txt");
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String target = reader.readLine();
                String line;
                String lastLine = null;
                while ((line = reader.readLine())!= null) {
                    lastLine = line;
                }
                String[] parts = lastLine.split(" ");
                String coverage = parts[0];
                String[] row = {"T0" + i, target, coverage};
                data.add(row);
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
        try (FileWriter writer = new FileWriter("D:\\Data\\data\\table_data.csv")) {
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
            System.out.println("表格数据已成功保存为CSV文件：table_data.csv");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
