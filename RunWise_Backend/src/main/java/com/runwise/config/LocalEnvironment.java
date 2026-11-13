package com.runwise.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.*;

/** Optional local .env support for direct IDEA runs; process/IDE variables win. */
public class LocalEnvironment implements EnvironmentPostProcessor, Ordered {
    @Override public int getOrder() { return Ordered.LOWEST_PRECEDENCE; }
    @Override public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path cwd=Paths.get("").toAbsolutePath();
        Path path=Files.isRegularFile(cwd.resolve("RunWise_Backend/pom.xml"))
                ?cwd.resolve("RunWise_Backend/.env"):cwd.resolve(".env");
        if(!Files.isRegularFile(path)) return;
        try { environment.getPropertySources().addLast(new MapPropertySource("runwiseLocalEnv",read(path))); }
        catch(IOException e) { throw new IllegalStateException("无法读取本机 .env 配置文件",e); }
    }
    public static Map<String,Object> read(Path path) throws IOException {
        Map<String,Object> values=new LinkedHashMap<>();int number=0;
        for(String raw:Files.readAllLines(path,StandardCharsets.UTF_8)) {
            number++;String line=raw.trim();if(number==1)line=line.replace("\uFEFF","");
            if(line.isEmpty()||line.startsWith("#"))continue;
            if(line.startsWith("export "))line=line.substring(7).trim();
            int equals=line.indexOf('=');
            if(equals<1)throw new IllegalStateException(".env 第 "+number+" 行格式错误");
            String key=line.substring(0,equals).trim(),value=line.substring(equals+1).trim();
            if(!key.matches("[A-Za-z_][A-Za-z0-9_]*"))throw new IllegalStateException(".env 第 "+number+" 行变量名错误");
            if(value.startsWith("'")||value.startsWith("\"")) {
                char quote=value.charAt(0);
                if(value.length()<2||value.charAt(value.length()-1)!=quote)
                    throw new IllegalStateException(".env 第 "+number+" 行引号不完整；请使用单行值");
                value=value.substring(1,value.length()-1);
                if(quote=='\'')value=value.replace("\\'","'").replace("\\\\","\\");
            } else { int comment=value.indexOf(" #");if(comment>=0)value=value.substring(0,comment).trim(); }
            values.put(key,value);
        }
        return values;
    }
}
