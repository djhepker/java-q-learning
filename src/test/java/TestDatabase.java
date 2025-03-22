import org.junit.jupiter.api.AfterEach;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


class TestDatabase {
    private final Path directoryPath = Paths.get("src/main/resources/data");

    @AfterEach
    void cleanUp() throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directoryPath, "*.dat")) {
            for (Path file : stream) {
                Files.delete(file);
            }
        }
        Files.delete(directoryPath);
    }
}
