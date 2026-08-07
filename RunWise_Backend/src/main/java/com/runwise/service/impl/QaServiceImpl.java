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
 * 调用 Python RAG 微服务获取答案
 */
@Slf4j
@Service
public class QaServiceImpl extends ServiceImpl<QaRecordMapper, QaRecord> implements QaService {

    @Value("${runwise.rag.base-url}")
    private String ragBaseUrl;

    @Override
    public Map<String, Object> ask(Long userId, AskDTO dto) {
        // 1. 调用 Python RAG 微服务
        Map<String, Object> result;
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("question", dto.getQuestion());
            params.put("user_id", userId);

            String response = HttpUtil.post(ragBaseUrl + "/api/qa/ask",
                    JSONUtil.toJsonStr(params), 30000);
            JSONObject json = JSONUtil.parseObj(response);

            result = new HashMap<>();
            result.put("answer", json.getStr("answer"));
            result.put("sources", json.getJSONArray("sources"));
        } catch (Exception e) {
            log.error("调用RAG服务失败", e);
            throw new BusinessException(ResultCode.RAG_SERVICE_ERROR);
        }

        // 2. 保存问答记录
        QaRecord record = new QaRecord();
        record.setUserId(userId);
        record.setQuestion(dto.getQuestion());
        record.setAnswer((String) result.get("answer"));
        record.setSources(result.get("sources") != null ? result.get("sources").toString() : null);
        record.setFeedback(0);
        save(record);

        result.put("recordId", record.getId());
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
