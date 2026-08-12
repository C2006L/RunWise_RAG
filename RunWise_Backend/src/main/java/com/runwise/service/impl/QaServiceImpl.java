package com.runwise.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.runwise.common.BusinessException;
import com.runwise.common.PageResult;
import com.runwise.common.ResultCode;
import com.runwise.dto.AskDTO;
import com.runwise.entity.QaRecord;
import com.runwise.mapper.QaRecordMapper;
import com.runwise.service.QaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class QaServiceImpl extends ServiceImpl<QaRecordMapper, QaRecord> implements QaService {

    @Value("${runwise.llm.provider:dashscope}")
    private String provider;

    @Value("${runwise.llm.dashscope.base-url:}")
    private String dashscopeBaseUrl;

    @Value("${runwise.llm.dashscope.api-key:}")
    private String dashscopeApiKey;

    @Value("${runwise.llm.dashscope.model:}")
    private String dashscopeModel;

    @Value("${runwise.llm.dashscope.max-tokens:500}")
    private int dashscopeMaxTokens;

    @Value("${runwise.llm.dashscope.temperature:0.7}")
    private double dashscopeTemperature;

    @Value("${runwise.llm.ollama.base-url:}")
    private String ollamaBaseUrl;

    @Value("${runwise.llm.ollama.model:}")
    private String ollamaModel;

    @Override
    public Map<String, Object> ask(Long userId, AskDTO dto) {
        Map<String, Object> result;
        try {
            String systemPrompt = "你是一位专业的跑步教练，名字叫'RunWise助手'。" +
                "请用中文回答用户的跑步相关问题，回答要专业、实用、简洁，" +
                "字数控制在200-400字之间。如果涉及伤痛问题，请给出安全提示。";

            String answer;

            if ("ollama".equalsIgnoreCase(provider)) {
                answer = callOllama(systemPrompt, dto.getQuestion());
            } else {
                answer = callDashScope(systemPrompt, dto.getQuestion());
            }

            if (answer == null || answer.isEmpty()) {
                throw new BusinessException(ResultCode.RAG_SERVICE_ERROR, "模型返回空答案");
            }

            result = new HashMap<>();
            result.put("answer", answer.trim());

            List<String> defaultSources = Arrays.asList("Qwen3.5-Flash大模型生成");
            result.put("sources", defaultSources);

            boolean hasInjuryKeyword = dto.getQuestion().matches(".*[疼痛伤膝盖小腿脚踝拉伤].*");
            if (hasInjuryKeyword) {
                result.put("safetyTip", "以上建议仅供参考，若疼痛持续或加重请及时就医。");
            }

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用LLM服务失败", e);
            throw new BusinessException(ResultCode.RAG_SERVICE_ERROR,
                "调用AI服务失败：" + e.getMessage());
        }

        String answer = (String) result.get("answer");
        log.info("准备返回给前端 - answer长度: {}, sources: {}, safetyTip: {}",
                answer.length(),
                result.get("sources"),
                result.get("safetyTip"));

        return result;
    }

    private String callDashScope(String systemPrompt, String question) {
        log.info("调用DashScope API，问题：{}", question);

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", dashscopeModel);
        requestBody.put("max_tokens", dashscopeMaxTokens);
        requestBody.put("temperature", dashscopeTemperature);

        JSONArray messages = new JSONArray();
        JSONObject systemMsg = new JSONObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);
        messages.add(systemMsg);

        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", question);
        messages.add(userMsg);

        requestBody.put("messages", messages);

        String url = dashscopeBaseUrl + "/v1/chat/completions";

        long startTime = System.currentTimeMillis();

        String response = HttpRequest.post(url)
                .header("Authorization", "Bearer " + dashscopeApiKey)
                .header("Content-Type", "application/json")
                .body(requestBody.toString())
                .timeout(30000)
                .execute()
                .body();

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("DashScope API响应耗时: {}ms", elapsed);

        JSONObject json = JSONUtil.parseObj(response);

        if (json.containsKey("error")) {
            String errorMsg = json.getByPath("error.message", String.class);
            log.error("DashScope API返回错误: {}", errorMsg);
            throw new BusinessException(ResultCode.RAG_SERVICE_ERROR,
                "AI服务返回错误：" + errorMsg);
        }

        JSONArray choices = json.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new BusinessException(ResultCode.RAG_SERVICE_ERROR, "AI服务返回空结果");
        }

        String answer = choices.getJSONObject(0)
                .getByPath("message.content", String.class);

        log.info("DashScope 返回答案长度：{}", answer != null ? answer.length() : 0);

        return answer;
    }

    private String callOllama(String systemPrompt, String question) {
        log.info("调用Ollama API，问题：{}", question);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", ollamaModel);
        requestBody.put("prompt", systemPrompt + "\n\n用户问题：" + question);
        requestBody.put("stream", false);

        Map<String, Object> options = new HashMap<>();
        options.put("temperature", 0.7);
        options.put("num_predict", 500);
        requestBody.put("options", options);

        String response = HttpUtil.post(
            ollamaBaseUrl + "/api/generate",
            JSONUtil.toJsonStr(requestBody),
            60000
        );

        JSONObject json = JSONUtil.parseObj(response);
        String answer = json.getStr("response");

        log.info("Ollama 返回答案长度：{}", answer != null ? answer.length() : 0);

        return answer;
    }

    @Override
    public List<Map<String, Object>> getCategories() {
        List<Map<String, Object>> categories = new ArrayList<>();
        String[][] categoryData = {
                {"训练计划", "📋"},
                {"装备选择", "👟"},
                {"伤痛预防", "🩹"},
                {"跑步技术", "🏃"}
        };
        for (String[] cat : categoryData) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", cat[0]);
            item.put("icon", cat[1]);
            categories.add(item);
        }
        return categories;
    }

    @Override
    public List<Map<String, Object>> hotQuestions() {
        List<Map<String, Object>> list = new ArrayList<>();
        String[][] questions = {
                {"初学者应该怎么开始跑步？", "beginner"},
                {"跑步膝盖疼怎么办？", "injury"},
                {"如何选择合适的跑鞋？", "equipment"},
                {"5K训练计划是什么？", "training"},
                {"跑步时心率多少合适？", "training"},
                {"跑步前吃什么？", "nutrition"},
                {"如何提高跑步配速？", "training"},
                {"跑步后怎么拉伸恢复？", "recovery"}
        };
        for (String[] q : questions) {
            Map<String, Object> item = new HashMap<>();
            item.put("question", q[0]);
            item.put("category", q[1]);
            list.add(item);
        }
        return list;
    }

    @Override
    public PageResult<QaRecord> getHistory(Long userId, Integer pageNum, Integer pageSize) {
        Page<QaRecord> page = new Page<>(pageNum, pageSize);
        page(page, new LambdaQueryWrapper<QaRecord>()
                .eq(QaRecord::getUserId, userId)
                .orderByDesc(QaRecord::getCreateTime));
        return new PageResult<>(page.getRecords(), page.getTotal(), pageNum, pageSize);
    }

    @Override
    public void feedback(Long userId, Long id, Integer feedback) {
        QaRecord record = getById(id);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "问答记录不存在");
        }
        record.setFeedback(feedback);
        updateById(record);
    }
}