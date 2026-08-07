package com.runwise.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.runwise.common.PageResult;
import com.runwise.dto.AskDTO;
import com.runwise.entity.QaRecord;

import java.util.List;
import java.util.Map;

public interface QaService extends IService<QaRecord> {

    /**
     * 提问
     */
    Map<String, Object> ask(Long userId, AskDTO dto);

    /**
     * 热门问题
     */
    List<Map<String, Object>> hotQuestions();

    /**
     * 问答历史(分页)
     */
    PageResult<QaRecord> getHistory(Long userId, Integer pageNum, Integer pageSize);

    /**
     * 回答反馈
     */
    void feedback(Long userId, Long id, Integer feedback);
}
