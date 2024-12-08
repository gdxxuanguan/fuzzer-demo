package edu.nju.isefuzz.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import java.util.logging.Logger;

public class TempFileHandler implements AutoCloseable {
    private static final Logger logger = Logger.getLogger(TempFileHandler.class.getName());
    private Path tempFilePath;

    public TempFileHandler(byte[] data, String tempDir) throws IOException {
        String fileName = "temp_input_" + UUID.randomUUID();
        Path tempDirPath = Paths.get(tempDir);
        if (!Files.exists(tempDirPath)) {
            Files.createDirectories(tempDirPath);
            logger.info("Created temporary directory at: " + tempDirPath.toAbsolutePath());
        }
        tempFilePath = tempDirPath.resolve(fileName);
        Files.write(tempFilePath, data, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        logger.info("Wrote test input to temp file: " + tempFilePath.toAbsolutePath());
    }

    public String getTempFilePath() {
        return tempFilePath.toAbsolutePath().toString();
    }

    @Override
    public void close() {
        try {
            Files.deleteIfExists(tempFilePath);
            logger.info("Deleted temporary file: " + tempFilePath.toAbsolutePath());
        } catch (IOException e) {
            logger.severe("Failed to delete temporary file: " + e.getMessage());
        }
    }
}
