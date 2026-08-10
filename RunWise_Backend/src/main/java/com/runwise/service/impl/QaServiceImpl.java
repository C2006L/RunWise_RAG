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

/**
 * 问答服务实现
 * 调用 Ollama 本地大模型 API 获取答案（调试模式）
 */
@Slf4j
@Service
public class QaServiceImpl extends ServiceImpl<QaRecordMapper, QaRecord> implements QaService {

    @Value("${runwise.ollama.base-url}")
    private String ollamaBaseUrl;

    @Value("${runwise.ollama.model}")
    private String ollamaModel;

    @Override
    public Map<String, Object> ask(Long userId, AskDTO dto) {
        Map<String, Object> result;
        try {
            // 构建系统提示词（让AI扮演跑步教练）
            String systemPrompt = "你是一位专业的跑步教练，名字叫'RunWise助手'。" +
                "请用中文回答用户的跑步相关问题，回答要专业、实用、简洁，" +
                "字数控制在200-400字之间。如果涉及伤痛问题，请给出安全提示。";

            // 构建 Ollama API 请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", ollamaModel);
            requestBody.put("prompt", systemPrompt + "\n\n用户问题：" + dto.getQuestion());
            requestBody.put("stream", false);  // 非流式输出

            // 添加模型参数（可调优）
            Map<String, Object> options = new HashMap<>();
            options.put("temperature", 0.7);      // 创造性：0-1
            options.put("num_predict", 500);       // 最大生成长度
            options.put("top_p", 0.9);             // 核采样
            options.put("repeat_penalty", 1.1);    // 防止重复
            requestBody.put("options", options);

            log.info("调用Ollama API，问题：{}", dto.getQuestion());

            // 调用 Ollama /api/generate 接口
            String response = HttpUtil.post(
                ollamaBaseUrl + "/api/generate",
                JSONUtil.toJsonStr(requestBody),
                60000  // 超时60秒
            );

            JSONObject json = JSONUtil.parseObj(response);
            String answer = json.getStr("response");

            if (answer == null || answer.isEmpty()) {
                throw new BusinessException(ResultCode.RAG_SERVICE_ERROR, "模型返回空答案");
            }

            log.info("Ollama 返回答案长度：{}", answer.length());

            // 构建返回结果
            result = new HashMap<>();
            result.put("answer", answer.trim());

            // Ollama 不提供来源信息，使用默认值
            List<String> defaultSources = Arrays.asList("Qwen2.5大模型生成");
            result.put("sources", defaultSources);

            // 安全提示检测
            boolean hasInjuryKeyword = dto.getQuestion().matches(".*[疼痛伤膝盖小腿脚踝拉伤].*");
            if (hasInjuryKeyword) {
                result.put("safetyTip", "以上建议仅供参考，若疼痛持续或加重请及时就医。");
            }

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用Ollama服务失败", e);
            throw new BusinessException(ResultCode.RAG_SERVICE_ERROR,
                "调用AI服务失败：" + e.getMessage() + "，请检查Ollama是否启动");
        }

        // 保存问答记录到数据库（调试阶段暂时跳过，避免数据库错误影响测试）
        // TODO: 配置正确数据库密码后恢复此功能

        // 调试日志：打印即将返回的数据
        String answer = (String) result.get("answer");
        log.info("准备返回给前端 - answer长度: {}, sources: {}, safetyTip: {}",
                answer.length(),
                result.get("sources"),
                result.get("safetyTip"));

        /*
        try {
            QaRecord record = new QaRecord();
            record.setUserId(userId);
            record.setQuestion(dto.getQuestion());
            record.setAnswer((String) result.get("answer"));
            record.setSources(result.get("sources") != null ? result.get("sources").toString() : null);
            record.setFeedback(0);
            save(record);

            result.put("recordId", record.getId());
        } catch (Exception e) {
            log.warn("保存问答记录失败，但不影响返回结果", e);
        }
        */

        return result;
    }

    @Override
    public List<Map<String, Object>> hotQuestions() {
        // 预设热门问题列表
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