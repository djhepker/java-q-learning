import hepker.ai.Database;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TestDatabase {
    private final String dataPath = "src/main/resources/data";
    private final Path directoryPath = Paths.get("src/main/resources/data");

    @Test
    void testIntialize() throws Exception {
        Database db = new Database();
        db.close();
        File valueData = new File(dataPath + "/q_values.dat");
        File indexData = new File(dataPath + "/index_values.dat");
        File directory = new File(dataPath);
        assertTrue(valueData.exists() && indexData.exists() && directory.exists());
    }

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
