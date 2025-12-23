package com.jing.monitor.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 单例模式 (Singleton) 的配置加载器
 * 负责读取 src/main/resources/application.properties
 */
public class AppConfig {

    private static final Properties properties = new Properties();

    // 静态代码块：类加载时自动执行一次
    static {
        try (InputStream input = AppConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                System.err.println("[Config] Sorry, unable to find application.properties");
            } else {
                // 加载属性文件
                properties.load(input);
                System.out.println("[Config] Configuration loaded successfully.");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // 提供静态方法获取值
    public static String get(String key) {
        return properties.getProperty(key);
    }

    public static String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public static int getInt(String key) {
        return Integer.parseInt(properties.getProperty(key));
    }
}