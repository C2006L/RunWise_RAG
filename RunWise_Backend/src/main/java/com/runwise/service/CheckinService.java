package com.runwise.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.runwise.common.PageResult;
import com.runwise.dto.CheckinDTO;
import com.runwise.entity.Checkin;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface CheckinService extends IService<Checkin> {

    /**
     * 提交打卡
     */
    Long createCheckin(Long userId, CheckinDTO dto);

    /**
     * 查询今日打卡状态
     */
    Checkin getTodayCheckin(Long userId);

    /**
     * 打卡列表(分页)
     */
    PageResult<Checkin> getCheckinList(Long userId, Integer pageNum, Integer pageSize);

    /**
     * 月历打卡数据
     */
    List<Map<String, Object>> getCalendarData(Long userId, String month);

    /**
     * 统计数据
     */
    Map<String, Object> getStats(Long userId);

    /**
     * 删除打卡(软删除)
     */
    void deleteCheckin(Long userId, Long id);
}
