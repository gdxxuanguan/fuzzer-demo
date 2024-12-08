package edu.nju.isefuzz.util;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class DirectoryUtils {

    /**
     * 重置指定的目录。
     * <p>
     * 如果目录存在，则删除该目录及其所有内容。
     * 然后，创建一个新的空目录。
     *
     * @param dirPath 目录的路径，可以是相对路径或绝对路径。
     * @throws IOException 如果在删除或创建目录时发生IO错误。
     */
    public static void resetDirectory(String dirPath) throws IOException {
        Path path = Paths.get(dirPath);

        if (Files.exists(path)) {
            // 删除目录及其所有内容
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                        throws IOException {
                    if (exc != null) {
                        throw exc;
                    }
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
            System.out.println("已删除现有目录及其内容: " + dirPath);
        }

        // 创建目录
        Files.createDirectories(path);
        System.out.println("已创建目录: " + dirPath);
    }
}

