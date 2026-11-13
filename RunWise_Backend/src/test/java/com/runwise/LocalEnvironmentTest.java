package com.runwise;
import com.runwise.config.LocalEnvironment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class LocalEnvironmentTest {
    @TempDir Path dir;
    @Test void parsesLocalLiteralValuesWithoutExecutingExpressions() throws Exception {
        Path file=dir.resolve(".env");
        Files.write(file,Arrays.asList("# local","DB_PASSWORD='test-only=x#y'","RAG_ENABLED='true'","EMPTY=","export SERVER_PORT=8081 # local","LITERAL=$(echo test)"),StandardCharsets.UTF_8);
        Map<String,Object> values=LocalEnvironment.read(file);
        assertEquals("test-only=x#y",values.get("DB_PASSWORD"));assertEquals("true",values.get("RAG_ENABLED"));
        assertEquals("",values.get("EMPTY"));assertEquals("8081",values.get("SERVER_PORT"));assertEquals("$(echo test)",values.get("LITERAL"));
    }
    @Test void malformedConfigurationErrorDoesNotExposeSecret() throws Exception {
        Path file=dir.resolve(".env");Files.write(file,Collections.singletonList("DB_PASSWORD='test-secret"),StandardCharsets.UTF_8);
        Exception error=assertThrows(IllegalStateException.class,()->LocalEnvironment.read(file));assertFalse(error.getMessage().contains("test-secret"));
    }
    @Test void bootDiscoversLocalEnvironmentProcessor() {
        assertTrue(org.springframework.core.io.support.SpringFactoriesLoader.loadFactoryNames(org.springframework.boot.env.EnvironmentPostProcessor.class,getClass().getClassLoader()).contains(LocalEnvironment.class.getName()));
    }
}
