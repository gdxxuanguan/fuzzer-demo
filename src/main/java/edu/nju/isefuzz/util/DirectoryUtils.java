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

    /**
     * 确保指定的目录被重新创建。
     * <p>
     * 如果目录存在，则先删除该目录及其所有内容，然后重新创建目录。
     * 如果目录不存在，则直接创建目录。
     *
     * @param dirPath 目录的路径，可以是相对路径或绝对路径。
     * @throws IOException 如果在删除或创建目录时发生IO错误。
     */
    public static void recreateDirectory(String dirPath) throws IOException {
        Path path = Paths.get(dirPath);

        if (Files.exists(path)) {
            if (Files.isDirectory(path)) {
                // 删除目录及其所有内容
                deleteDirectoryRecursively(path);
                System.out.println("已删除现有目录: " + dirPath);
            } else {
                throw new IOException("路径存在但不是目录: " + dirPath);
            }
        }

        // 创建目录
        Files.createDirectories(path);
        System.out.println("已创建目录: " + dirPath);
    }

    /**
     * 递归删除目录及其所有内容。
     *
     * @param path 要删除的目录路径。
     * @throws IOException 如果在删除过程中发生IO错误。
     */
    private static void deleteDirectoryRecursively(Path path) throws IOException {
        // 使用 Files.walkFileTree 递归遍历并删除目录
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            // 删除文件
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            // 删除目录
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc == null) {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                } else {
                    // 目录访问过程中发生错误
                    throw exc;
                }
            }
        });
    }


}



