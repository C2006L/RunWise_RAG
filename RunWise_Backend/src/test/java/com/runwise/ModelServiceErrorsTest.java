package com.runwise;

import com.runwise.common.ModelServiceErrors;
import com.runwise.common.BusinessException;
import com.runwise.dto.PlanDraftRequest;
import com.runwise.service.PlanGenerationGateway;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;

class ModelServiceErrorsTest {
    @Test void overloadIsShownWithSafeCodeAndWithoutQuotaAdvice() {
        String message=ModelServiceErrors.fromResponse("{\"detail\":{\"code\":\"MODEL_OVERLOADED\",\"providerCode\":\"1305\",\"message\":\"secret\"}}").getMessage();
        assertTrue(message.contains("访问量过大"));assertTrue(message.contains("1305"));assertFalse(message.contains("充值"));assertFalse(message.contains("secret"));
        assertFalse(ModelServiceErrors.fromResponse("{\"detail\":{\"providerCode\":\"secret-value\"}}").getMessage().contains("secret"));
    }
    @Test void onlyRecognizedCodesProduceSpecificSafeMessages() {
        assertTrue(ModelServiceErrors.fromResponse("{\"detail\":{\"code\":\"MODEL_AUTH\",\"message\":\"secret\"}}").getMessage().contains("认证"));
        for (String body : new String[]{"not json", "{\"detail\":\"secret\"}", "{\"detail\":{\"code\":\"unknown\",\"message\":\"secret\"}}"}) {
            assertFalse(ModelServiceErrors.fromResponse(body).getMessage().contains("secret"));
        }
    }

    @Test void planGatewayPreservesSafeRateLimitMessageInsteadOfGenericTimeout() throws Exception {
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/api/rag/plan",exchange->{
            byte[] body="{\"detail\":{\"code\":\"MODEL_RATE_LIMIT\",\"message\":\"private provider response\"}}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(503,body.length);exchange.getResponseBody().write(body);exchange.close();
        });server.start();
        try {
            PlanGenerationGateway gateway=new PlanGenerationGateway(new ObjectMapper());
            ReflectionTestUtils.setField(gateway,"baseUrl","http://127.0.0.1:"+server.getAddress().getPort());
            BusinessException error=assertThrows(BusinessException.class,()->gateway.generate(new PlanDraftRequest(),Collections.emptyMap()));
            assertTrue(error.getMessage().contains("速率限制"));assertFalse(error.getMessage().contains("private"));
        } finally {server.stop(0);}
    }
}
