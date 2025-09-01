package com.matsuzaka.bigdata;

import com.matsuzaka.bigdata.ui.MainFrame;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.*;

@SpringBootApplication
public class BigDataApplication {

    public static void main(String[] args) {
        // 這是讓 Spring Boot 應用程式能以 "非 headless" 模式運行的方法
        // 這樣才能順利啟動 Swing GUI
        ConfigurableApplicationContext context = new SpringApplicationBuilder(BigDataApplication.class)
                .headless(false)
                .run(args);

        // 使用 SwingUtilities.invokeLater 確保 GUI 在事件分派線程上建立
        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = context.getBean(MainFrame.class);
            mainFrame.setVisible(true);
        });
    }

}
