package edu.nju.isefuzz.util;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * 管理目录的实用工具类。
 */
public class DirectoryUtils {

    /**
     * 确保指定的目录存在。
     * <p>
     * 如果目录不存在，则创建一个新的目录。
     * 如果目录已存在，则不执行任何操作。
     *
     * @param dirPath 目录的路径，可以是相对路径或绝对路径。
     * @throws IOException 如果在创建目录时发生IO错误。
     */
    public static void ensureDirectoryExists(String dirPath) throws IOException {
        Path path = Paths.get(dirPath);

        if (Files.exists(path)) {
            if (Files.isDirectory(path)) {
                System.out.println("目录已存在: " + dirPath);
            } else {
                throw new IOException("路径存在但不是目录: " + dirPath);
            }
        } else {
            // 创建目录
            Files.createDirectories(path);
            System.out.println("已创建目录: " + dirPath);
        }
    }
}



