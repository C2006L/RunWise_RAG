package com.runwise;

import com.runwise.admin.AdminInterceptor;
import com.runwise.config.WebMvcConfig;
import com.runwise.controller.FileController;
import com.runwise.interceptor.JwtInterceptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UploadResourceTest {
    @TempDir Path temporary;

    @Configuration
    @EnableWebMvc
    @Import({WebMvcConfig.class, FileController.class})
    static class Config {
        @Bean JwtInterceptor jwtInterceptor() { return mock(JwtInterceptor.class); }
        @Bean AdminInterceptor adminInterceptor() { return mock(AdminInterceptor.class); }
    }

    @Test void firstUploadIsReadableEvenWhenDirectoryDidNotExistAtStartup() throws Exception {
        Path uploads = temporary.resolve("new uploads");
        assertFalse(Files.exists(uploads));
        try (AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context,
                    "runwise.upload-dir=" + uploads.toString().replace('\\', '/'));
            context.register(Config.class);
            context.refresh();
            assertFalse(Files.exists(uploads));
            byte[] png = Base64.getDecoder().decode(
                    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a9WQAAAAASUVORK5CYII=");
            context.getBean(FileController.class).uploadCheckinImage(
                    new MockMultipartFile("file", "sample.png", "image/png", png), 1L);
            Path saved;
            try (java.util.stream.Stream<Path> files = Files.list(uploads.resolve("checkin/1"))) {
                saved = files.findFirst().orElseThrow(() -> new AssertionError("Upload was not saved"));
            }
            MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
            mvc.perform(get("/uploads/checkin/1/" + saved.getFileName()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("image/png"))
                    .andExpect(result -> assertNotNull(javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(result.getResponse().getContentAsByteArray()))));
            mvc.perform(get("/uploads/checkin/missing.png")).andExpect(status().isNotFound());
        }
    }
}
