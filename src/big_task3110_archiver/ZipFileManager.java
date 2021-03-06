package big_task3110_archiver;

import big_task3110_archiver.exception.NoSuchZipFileException;
import big_task3110_archiver.exception.PathNotFoundException;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipFileManager {
    private Path zipFile;

    public void addFile(Path absolutePath) throws Exception {
        addFiles(Collections.singletonList(absolutePath));
    }
    public void addFiles(List<Path> absolutePathList) throws Exception {
        if (!Files.isRegularFile(zipFile)) {
            throw new NoSuchZipFileException();
        }
        Path tempFile = Files.createTempFile("", ".tmp");
        List<Path> localList = new ArrayList<>();

        try (ZipInputStream inputStream = new ZipInputStream(Files.newInputStream(zipFile));
             ZipOutputStream outputStream = new ZipOutputStream(Files.newOutputStream(tempFile))) {
            ZipEntry entry = inputStream.getNextEntry();
            while (entry != null) {
                Path path = Paths.get(entry.getName());
                localList.add(path);
                outputStream.putNextEntry(new ZipEntry(entry.getName()));
                copyData(inputStream, outputStream);
                inputStream.closeEntry();
                outputStream.closeEntry();
                entry = inputStream.getNextEntry();
            }

            for (Path file : absolutePathList) {
                if (Files.isRegularFile(file))
                {
                    if (localList.contains(file.getFileName()))
                        ConsoleHelper.writeMessage(String.format("File '%s' already exists in the archive.", file.toString()));
                    else {
                        addNewZipEntry(outputStream, file.getParent(), file.getFileName());
                        ConsoleHelper.writeMessage(String.format("File '%s' was added to the archive.", file.toString()));
                    }
                }
                else
                    throw new PathNotFoundException();
            }
        }
        Files.move(tempFile, zipFile, StandardCopyOption.REPLACE_EXISTING);
    }

    public void removeFile(Path path) throws Exception {
        removeFiles(Collections.singletonList(path));
    }
    public void removeFiles(List<Path> pathList) throws Exception {
        if (!Files.isRegularFile(zipFile)) {
            throw new NoSuchZipFileException();
        }
        Path tempFile = Files.createTempFile("", ".tmp");
        try (ZipInputStream inputStream = new ZipInputStream(Files.newInputStream(zipFile));
             ZipOutputStream outputStream = new ZipOutputStream(Files.newOutputStream(tempFile))) {
            ZipEntry entry = inputStream.getNextEntry();
            while (entry != null) {
                Path path = Paths.get(entry.getName());

                if (!pathList.contains(path)) {
                    outputStream.putNextEntry(new ZipEntry(entry.getName()));
                    copyData(inputStream, outputStream);
                    inputStream.closeEntry();
                    outputStream.closeEntry();
                } else {
                    ConsoleHelper.writeMessage(String.format("File '%s' was removed from the archive.", path.toString()));
                }
                entry = inputStream.getNextEntry();
            }
        }
        Files.move(tempFile, zipFile, StandardCopyOption.REPLACE_EXISTING);
    }
    public void extractAll(Path outputFolder) throws Exception {
        if (!Files.isRegularFile(zipFile)) {
            throw new NoSuchZipFileException();
        }

        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zipFile))) {
            if (Files.notExists(outputFolder)) {
                Files.createDirectories(outputFolder);
            }
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                String fileName = zipEntry.getName();
                Path fileFullName = outputFolder.resolve(fileName);
                Path parent = fileFullName.getParent();
                if (Files.notExists(parent)) {
                    Files.createDirectories(parent);
                }
                try (OutputStream outputStream = Files.newOutputStream(fileFullName)) {
                    copyData(zipInputStream, outputStream);
                }
                zipEntry = zipInputStream.getNextEntry();
            }
        }
    }

    public List<FileProperties> getFileList() throws Exception {
        if (!Files.isRegularFile(zipFile)) {
            throw new NoSuchZipFileException();
        }
        List<FileProperties> fileProperties = new ArrayList<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry nextEntry = zipInputStream.getNextEntry();
            while (nextEntry != null) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                copyData(zipInputStream, buffer);
                fileProperties.add(new FileProperties(nextEntry.getName(), nextEntry.getSize(), nextEntry.getCompressedSize(), nextEntry.getMethod()));
                nextEntry = zipInputStream.getNextEntry();
            }
        }
        return fileProperties;
    }

    public void createZip(Path source) throws Exception {
        final Path parent = zipFile.getParent();
        if (Files.notExists(parent)) {
            Files.createDirectories(parent);
        }
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            if (Files.isRegularFile(source)) {
                addNewZipEntry(zipOutputStream, source.getParent(), source.getFileName());
            } else if (Files.isDirectory(source)) {
                FileManager fileManager = new FileManager(source);
                List<Path> fileNames = fileManager.getFileList();
                for (Path file : fileNames) {
                    addNewZipEntry(zipOutputStream, source, file);
                }
            } else {
                throw new PathNotFoundException();
            }
        }
    }
    private void addNewZipEntry(ZipOutputStream zipOutputStream, Path filePath, Path fileName) throws Exception {
        try (InputStream inputStream = Files.newInputStream(filePath.resolve(fileName))) {
            ZipEntry zipEntry = new ZipEntry(fileName.toString());
            zipOutputStream.putNextEntry(zipEntry);
            copyData(inputStream, zipOutputStream);
            zipOutputStream.closeEntry();
        }
    }
    private void copyData(InputStream in, OutputStream out) throws Exception {
        byte[] buffer = new byte[8 * 1024];
        int len;
        while ((len = in.read(buffer)) > 0) {
            out.write(buffer, 0, len);
        }
    }
    public ZipFileManager(final Path zipFile) {
        this.zipFile = zipFile;
    }
}