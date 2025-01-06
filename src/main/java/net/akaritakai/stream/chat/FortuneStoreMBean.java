package net.akaritakai.stream.chat;

import java.io.File;
import java.util.List;

public interface FortuneStoreMBean {
    boolean removeFile(File file);
    boolean addFile(File file);
    List<String> randomFortune();
}
