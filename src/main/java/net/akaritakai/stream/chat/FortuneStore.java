package net.akaritakai.stream.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FortuneStore implements FortuneStoreMBean{

    private static final Logger LOG = LoggerFactory.getLogger(FortuneStore.class);


    private final List<File> _fortuneFiles;
    private final Random _random;

    public FortuneStore() {
        _fortuneFiles = new ArrayList<>();
        try {
            _random = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean removeFile(File file) {
        return _fortuneFiles.remove(file);
    }

    @Override
    public boolean addFile(File file) {
        return _fortuneFiles.add(file);
    }

    @Override
    public List<String> randomFortune() {
        long totalLength = _fortuneFiles.stream().mapToLong(File::length).sum();
        long randomPosition = _random.nextLong(totalLength);

        ArrayList<String> lines = new ArrayList<>();

        nextFile: for (File f : _fortuneFiles) {
            if (randomPosition >= f.length()) {
                randomPosition -= f.length();
                continue;
            }
            try (InputStream is = new FileInputStream(f)) {
                randomPosition -= is.skip(randomPosition);

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

                    lines.clear();
                    String line;
                    do {
                        line = reader.readLine();
                        if (line == null) {
                            continue nextFile;
                        };
                    } while (!line.trim().equals("%"));

                    for(;;) {
                        line = reader.readLine();
                        if (line == null || line.trim().equals("%")) {
                            if (lines.isEmpty()) {
                                if (line == null) {
                                    continue nextFile;
                                }
                                continue;
                            }
                            return lines;
                        }
                        lines.add(line);
                    }
                }
            } catch (IOException ex) {
                LOG.warn("Failed to read fortune from {}", f, ex);
            }
        }
        return null;
    }

}
