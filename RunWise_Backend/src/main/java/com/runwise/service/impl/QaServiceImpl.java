package com.runwise.service.impl;

import cn.hutool.http.HttpUtil;
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

    @Override
    public Map<String, Object> ask(Long userId, AskDTO dto) {
        Map<String, Object> result;
        try {
            String systemPrompt = "你是一位专业的跑步教练，名字叫'RunWise助手'。" +
                "请用中文回答用户的跑步相关问题，回答要专业、实用、简洁，" +
                "字数控制在200-400字之间。如果涉及伤痛问题，请给出安全提示。";

            String answer = callOllama(systemPrompt, dto.getQuestion());

            if (answer == null || answer.isEmpty()) {
                throw new BusinessException(ResultCode.RAG_SERVICE_ERROR, "模型返回空答案");
            }

            result = new HashMap<>();
            result.put("answer", answer.trim());

            // 来源标签如实展示当前使用的本地模型（跟随 yml 的 model 配置联动）
            result.put("sources", Arrays.asList("Ollama本地模型·" + ollamaModel));

            boolean hasInjuryKeyword = dto.getQuestion().matches(".*[疼痛伤膝盖小腿脚踝拉伤].*");
            if (hasInjuryKeyword) {
                result.put("safetyTip", "以上建议仅供参考，若疼痛持续或加重请及时就医。");
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