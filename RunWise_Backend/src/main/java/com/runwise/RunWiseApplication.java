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
        org.springframework.context.ConfigurableApplicationContext context=SpringApplication.run(RunWiseApplication.class, args);
        String port=context.getEnvironment().getProperty("local.server.port",context.getEnvironment().getProperty("server.port","8081"));
        System.out.println("" +
                "\n========================================" +
                "\n  RunWise Backend 启动成功" +
                "\n  API文档: http://localhost:"+port+"/doc.html" +
                "\n========================================");
    }
}
