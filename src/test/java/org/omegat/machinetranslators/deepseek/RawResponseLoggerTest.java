package org.omegat.machinetranslators.deepseek;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

public class RawResponseLoggerTest {

    @Test
    public void appendRawWritesOnlyRawValues() throws Exception {
        File temp = File.createTempFile("deepseek_raw_test", ".log");
        temp.deleteOnExit();

        RawResponseLogger.appendRaw(temp, "{\"choices\":[{\"message\":{\"content\":\"Hallo\"}}]}");
        RawResponseLogger.appendRaw(temp, "{\"choices\":[{\"message\":{\"content\":\"Welt\"}}]}");

        String content = new String(Files.readAllBytes(temp.toPath()), StandardCharsets.UTF_8);
        assertEquals("{\"choices\":[{\"message\":{\"content\":\"Hallo\"}}]}\n"
                + "{\"choices\":[{\"message\":{\"content\":\"Welt\"}}]}\n", content);
    }
}
