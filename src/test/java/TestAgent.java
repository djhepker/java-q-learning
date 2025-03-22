import hepker.ai.Agent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestAgent {
    private final Path directoryPath = Paths.get("src/main/resources");

//    @Test
//    void testGetMaxQValue() {
//        Agent zero = new Agent(1.0);
//        String testKey = "test";
//        zero.setStateKey(testKey);
//        double reward = 10.0;
//        int maxQIndex = 10;
//        zero.giveReward(reward);
//        zero.processData(testKey, maxQIndex);
//        Agent.pushQTableUpdate();
//
//        Agent zero2 = new Agent(0.0);
//        zero2.setStateKey(testKey);
//        int bluffActionInt = zero2.getActionInt(0);
//        Agent.closeDatabase();
//        assertEquals(maxQIndex, bluffActionInt);
//    }

    @AfterEach
    void cleanUp() throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directoryPath, "*.db")) {
            for (Path file : stream) {
                Files.delete(file);
            }
        }
    }
}
