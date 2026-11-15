package com.runwise;

import com.runwise.common.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.web.bind.annotation.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DependencyFailureWebTest {
    @RestController static class Fixture {
        @GetMapping("/test/session") public String session() { throw new RedisConnectionFailureException("private-host-and-credentials"); }
    }
    @Test void redisOutageIsUnavailableInsteadOfExpiredOrGeneric500() throws Exception {
        MockMvcBuilders.standaloneSetup(new Fixture()).setControllerAdvice(new GlobalExceptionHandler()).build()
                .perform(get("/test/session"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(503))
                .andExpect(jsonPath("$.message").value("会话服务暂不可用，请稍后重试；无需重新注册账号"));
    }
}
