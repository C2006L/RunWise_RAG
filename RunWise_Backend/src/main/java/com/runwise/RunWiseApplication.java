package com.runwise;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * RunWise 后端服务启动类
 */
@SpringBootApplication
@MapperScan("com.runwise.mapper")
public class RunWiseApplication {

    public static void main(String[] args) {
        SpringApplication.run(RunWiseApplication.class, args);
        System.out.println("" +
                "\n========================================" +
                "\n  RunWise Backend 启动成功" +
                "\n  API文档: http://localhost:8080/doc.html" +
                "\n========================================");
    }
}
