package org.pharosnet.vertx.faas.commons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.annotation.Annotation;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassUtils {

    private static final Logger log = LoggerFactory.getLogger(ClassUtils.class);

    public static List<Class<?>> scan(String packageName, Class<? extends Annotation> annotation) throws Exception {
        List<Class<?>> targets = new ArrayList<>();
        try {
            Enumeration<URL> urlEnumeration = Thread.currentThread().getContextClassLoader().getResources(packageName.replace(".", "/"));
            while (urlEnumeration.hasMoreElements()) {
                URL url = urlEnumeration.nextElement();
                if (url.getProtocol().equals("jar")) {
                    targets.addAll(scan(url, annotation));
                } else {
                    File file = new File(url.toURI());
                    if (!file.exists()) {
                        continue;
                    }
                    targets.addAll(scan(packageName, file, annotation));
                }
            }
        } catch (Exception e) {
            log.error("扫描类错误 {} {}", packageName, annotation, e);
            throw new Exception("扫描类错误", e);
        }
        return targets;
    }

    public static List<Class<?>> scan(URL url, Class<? extends Annotation> annotation) throws Exception {
        List<Class<?>> targets = new ArrayList<>();
        try {
            JarURLConnection urlConnection = (JarURLConnection) url.openConnection();
            JarFile jarFile = urlConnection.getJarFile();
            Enumeration<JarEntry> jarEntries = jarFile.entries();
            while (jarEntries.hasMoreElements()) {
                JarEntry jarEntry = jarEntries.nextElement();
                String jarEntryName = jarEntry.getName();
                if (jarEntry.isDirectory() || !jarEntryName.endsWith(".class")) {
                    continue;
                }
                String className = jarEntryName.replace(".class", "").replace("/", ".");
                Class<?> clazz = Class.forName(className);
                if (!clazz.isAnnotationPresent(annotation)) {
                    continue;
                }
                if (log.isDebugEnabled()) {
                    log.debug("找到类 {} {}", annotation, clazz);
                }
                targets.add(clazz);
            }
        } catch (Exception e) {
            log.error("扫描类错误, {} {}", url, annotation, e);
            throw new Exception("扫描类错误", e);
        }
        return targets;
    }

    private static List<Class<?>> scan(String packageName, File currentFile, Class<? extends Annotation> annotation) throws Exception {
        List<Class<?>> targets = new ArrayList<>();
        try {
            File[] fileList = currentFile.listFiles(pathname -> {
                if (pathname.isDirectory()) {
                    return true;
                }
                return pathname.getName().endsWith(".class");
            });
            if (fileList == null) {
                return targets;
            }
            for (File file : fileList) {
                if (file.isDirectory()) {
                    targets.addAll(scan(packageName + "." + file.getName(), file, annotation));
                } else {
                    String fileName = file.getName().replace(".class", "");
                    String className = packageName + "." + fileName;
                    Class<?> clazz = Class.forName(className);
                    if (!clazz.isAnnotationPresent(annotation)) {
                        continue;
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("找到类 {} {}", annotation, clazz);
                    }
                    targets.add(clazz);
                }
            }
        } catch (Exception e) {
            log.error("扫描类错误, {} {}", packageName, currentFile, e);
            throw new Exception("扫描类错误", e);
        }
        return targets;
    }

}
