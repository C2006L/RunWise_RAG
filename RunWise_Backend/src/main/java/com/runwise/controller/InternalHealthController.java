package com.runwise.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/** Container-only readiness; the gateway blocks /internal from public access. */
@RestController
public class InternalHealthController {
    private final JdbcTemplate jdbc;
    private final StringRedisTemplate redis;
    public InternalHealthController(JdbcTemplate jdbc, StringRedisTemplate redis) {
        this.jdbc = jdbc; this.redis = redis;
    }
    @GetMapping("/internal/health")
    public ResponseEntity<Map<String,Object>> health() {
        Map<String,Object> checks = new LinkedHashMap<>();
        try { jdbc.queryForObject("SELECT 1", Integer.class); checks.put("mysql", "up"); }
        catch (Exception e) { checks.put("mysql", "down"); }
        try {
            Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name IN ('t_user','t_checkin','t_injury_article','t_qa_record','t_web_credential','t_training_plan_item','t_ai_plan_draft','t_admin_account','t_admin_audit_log','t_badge','t_user_badge')", Integer.class);
            checks.put("schema", Integer.valueOf(11).equals(count) ? "up" : "down");
        } catch (Exception e) { checks.put("schema", "down"); }
        try {
            String pong = redis.execute((org.springframework.data.redis.core.RedisCallback<String>) connection -> connection.ping());
            checks.put("redis", "PONG".equals(pong) ? "up" : "down");
        } catch (Exception e) { checks.put("redis", "down"); }
        boolean ready = "up".equals(checks.get("mysql")) && "up".equals(checks.get("schema")) && "up".equals(checks.get("redis"));
        checks.put("status", ready ? "ok" : "degraded");
        return ResponseEntity.status(ready ? 200 : 503).body(checks);
    }
}
