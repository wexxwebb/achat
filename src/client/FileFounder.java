package client;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class FileFounder {

    private class FileVisitor extends SimpleFileVisitor<Path> {

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            fileList.add(file.getFileName().toString());
            return FileVisitResult.CONTINUE;
        }
    }

    List<String> fileList;

    public FileFounder() {
        fileList = new ArrayList<>();
    }

    public List<String> getFileList(String folder) {
        try {
            Files.walkFileTree(Paths.get(folder), new FileVisitor());
        } catch (IOException e) {
            System.out.println("Can't get file list");
        }
        return fileList;
    }
}
