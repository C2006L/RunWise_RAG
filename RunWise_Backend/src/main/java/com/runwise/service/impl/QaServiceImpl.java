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

    @Value("${runwise.llm.ollama.base-url:}")
    private String ollamaBaseUrl;

    @Value("${runwise.llm.ollama.model:}")
    private String ollamaModel;

    @Value("${runwise.llm.ollama.temperature:0.7}")
    private double ollamaTemperature;

    @Value("${runwise.llm.ollama.num-predict:500}")
    private int ollamaNumPredict;

    @Value("${runwise.rag.base-url:http://localhost:5000}")
    private String ragBaseUrl;

    @Value("${runwise.rag.enabled:true}")
    private boolean ragEnabled;

    @Override
    public Map<String, Object> ask(Long userId, AskDTO dto) {
        Map<String, Object> result;
        try {
            if (ragEnabled) {
                try {
                    result = callRag(userId, dto.getQuestion());
                } catch (Exception ragException) {
                    // 本地开发时 Python 服务可能尚未启动；保留 Ollama 降级，
                    // 但来源会明确显示为模型直连，避免伪装成 RAG 结果。
                    log.warn("RAG 服务不可用，降级为 Ollama 直连: {}", ragException.getMessage());
                    result = callOllamaFallback(dto.getQuestion());
                }
            } else {
                result = callOllamaFallback(dto.getQuestion());
            }

            // 保存问答记录（历史 tab 数据源，之前漏写导致历史永远为空）
            QaRecord record = new QaRecord();
            record.setUserId(userId);
            record.setQuestion(dto.getQuestion());
            record.setAnswer((String) result.get("answer"));
            Object sources = result.get("sources");
            record.setSources(sources instanceof List ? String.join("、", (List<String>) sources) : "");
            record.setFeedback(0);
            this.save(record);

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

    /**
     * 调用 Python Chroma RAG 服务，并携带最近三轮真实问答作为上下文。
     */
    private Map<String, Object> callRag(Long userId, String question) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("question", question);
        requestBody.put("top_k", 4);

        List<QaRecord> recentRecords = list(new LambdaQueryWrapper<QaRecord>()
                .eq(QaRecord::getUserId, userId)
                .orderByDesc(QaRecord::getCreateTime)
                .last("LIMIT 3"));
        Collections.reverse(recentRecords);
        List<Map<String, String>> history = new ArrayList<>();
        for (QaRecord record : recentRecords) {
            Map<String, String> item = new HashMap<>();
            item.put("user", record.getQuestion());
            item.put("assistant", record.getAnswer());
            history.add(item);
        }
        requestBody.put("history", history);

        String response = HttpRequest.post(ragBaseUrl + "/api/rag/ask")
                .header("Content-Type", "application/json; charset=utf-8")
                .body(JSONUtil.toJsonStr(requestBody))
                .timeout(130000)
                .execute()
                .body();
        JSONObject json = JSONUtil.parseObj(response);
        String answer = json.getStr("answer");
        if (answer == null || answer.trim().isEmpty()) {
            throw new IllegalStateException("RAG 返回空答案: " + json.getStr("detail", "未知错误"));
        }

        List<String> sources = new ArrayList<>();
        JSONArray sourceArray = json.getJSONArray("sources");
        if (sourceArray != null) {
            for (Object source : sourceArray) {
                if (source != null) {
                    sources.add(String.valueOf(source));
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("answer", answer.trim());
        result.put("sources", sources);
        String safetyTip = json.getStr("safetyTip");
        if (safetyTip != null && !safetyTip.trim().isEmpty()) {
            result.put("safetyTip", safetyTip);
        }
        return result;
    }

    private Map<String, Object> callOllamaFallback(String question) {
        String systemPrompt = "你是一位专业的跑步教练，名字叫'RunWise助手'。" +
                "请用中文回答用户的跑步相关问题，回答要专业、实用、简洁，" +
                "字数控制在200-400字之间。如果涉及伤痛问题，请给出安全提示。";
        String answer = callOllama(systemPrompt, question);
        if (answer == null || answer.trim().isEmpty()) {
            throw new BusinessException(ResultCode.RAG_SERVICE_ERROR, "模型返回空答案");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("answer", answer.trim());
        result.put("sources", Arrays.asList("Ollama本地模型·" + ollamaModel));
        if (containsInjuryKeyword(question)) {
            result.put("safetyTip", "以上建议仅供参考，若疼痛持续或加重请及时就医。");
        }
        return result;
    }

    private boolean containsInjuryKeyword(String question) {
        String[] keywords = {"疼", "疼痛", "膝盖", "小腿", "脚踝", "跟腱", "足底",
                "拉伤", "扭伤", "肿胀", "刺痛", "伤病", "受伤"};
        for (String keyword : keywords) {
            if (question.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String callOllama(String systemPrompt, String question) {
        log.info("调用Ollama API，问题：{}", question);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", ollamaModel);
        requestBody.put("prompt", systemPrompt + "\n\n用户问题：" + question);
        requestBody.put("stream", false);

        Map<String, Object> options = new HashMap<>();
        // 参数从 application.yml 读取（runwise.llm.ollama.*），改配置重启即可调参
        options.put("temperature", ollamaTemperature);
        options.put("num_predict", ollamaNumPredict);
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
