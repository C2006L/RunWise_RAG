package com.runwise;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.runwise.common.BusinessException;
import com.runwise.controller.ImageRecognitionController;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class ImageRecognitionControllerTest {
    @Test void rejectsEmptyOversizedAndUnsupportedFilesBeforeCallingService() {
        ImageRecognitionController controller = new ImageRecognitionController(new ObjectMapper());
        for (MockMultipartFile file : new MockMultipartFile[]{
                new MockMultipartFile("file", "a.png", "image/png", new byte[0]),
                new MockMultipartFile("file", "a.png", "image/png", new byte[5 * 1024 * 1024 + 1]),
                new MockMultipartFile("file", "a.svg", "image/svg+xml", new byte[]{1})})
            assertThrows(BusinessException.class, () -> controller.recognize(file));
    }

    @Test void forwardsImageBytesAndReturnsPreviewWithoutPersistence() throws Exception {
        ObjectMapper json = new ObjectMapper();
        AtomicReference<String> request = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/rag/image-metrics", exchange -> {
            request.set(new String(org.springframework.util.StreamUtils.copyToByteArray(exchange.getRequestBody()), StandardCharsets.UTF_8));
            byte[] body = "{\"distanceKm\":5.2,\"durationMin\":32,\"paceSecondsPerKm\":369,\"warnings\":[]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            ImageRecognitionController controller = new ImageRecognitionController(json);
            ReflectionTestUtils.setField(controller, "baseUrl", "http://127.0.0.1:" + server.getAddress().getPort());
            byte[] bytes = {1, 2, 3};
            Map<String,Object> result = controller.recognize(new MockMultipartFile("file", "a.png", "image/png", bytes)).getData();
            assertEquals(5.2, result.get("distanceKm"));
            assertArrayEquals(bytes, Base64.getDecoder().decode(json.readTree(request.get()).get("imageBase64").asText()));
        } finally { server.stop(0); }
    }
}
