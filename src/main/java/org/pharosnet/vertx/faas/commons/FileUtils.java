package org.pharosnet.vertx.faas.commons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class FileUtils {

    private static final Logger log = LoggerFactory.getLogger(FileUtils.class);

    public static String getResource(String filepath) throws Exception {
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(filepath);
        if (inputStream == null) {
            log.error("读取资源文件错误，无法找到文件。{}", filepath);
            throw new IllegalArgumentException("读取资源文件错误，无法找到文件。" + filepath);
        }
        try {
            byte[] data = inputStream.readAllBytes();
            if (data == null) {
                return "";
            }
            String content = new String(data, StandardCharsets.UTF_8);
            if (log.isDebugEnabled()) {
                log.debug("读取资源文件 {} \n{}", filepath, content);
            }
            return content;
        } catch (Exception e) {
            log.error("读取资源文件失败。{}", filepath, e);
            throw new IllegalArgumentException("读取资源文件失败。" + filepath);
        } finally {
            inputStream.close();
        }
    }

}
