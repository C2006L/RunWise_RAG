package com.runwise.service;

import com.runwise.entity.*;
import com.runwise.mapper.*;
import com.runwise.common.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.math.*;
import java.time.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

/** 只读取认证用户的真实记录。不发送昵称、身高体重或其他用户的原始数据。 */
@Service
public class RunnerContextService {
    private final CheckinMapper checkins;
    private final UserMapper users;
    private final JdbcTemplate jdbc;
    public RunnerContextService(CheckinMapper checkins, UserMapper users, JdbcTemplate jdbc) {
        this.checkins=checkins;this.users=users;this.jdbc=jdbc;
    }
    public Map<String,Object> build(Long userId) {
        if(userId==null) throw new BusinessException(ResultCode.UNAUTHORIZED);
        LocalDate today=LocalDate.now();
        User user=users.selectById(userId);
        if(user==null) throw new BusinessException(ResultCode.UNAUTHORIZED);
        List<Checkin> records=checkins.selectList(new LambdaQueryWrapper<Checkin>().eq(Checkin::getUserId,userId)
                .between(Checkin::getCheckinDate,today.minusDays(55),today).orderByAsc(Checkin::getCheckinDate));
        return summarize(records,user,today,jdbc.queryForList("SELECT training_date,type,target_km,note FROM t_training_plan_item WHERE user_id=? AND training_date BETWEEN ? AND ? ORDER BY training_date",userId,today,today.with(DayOfWeek.MONDAY).plusDays(27)));
    }
    public static Map<String,Object> summarize(List<Checkin> records,User user,LocalDate today,List<Map<String,Object>> plans) {
        List<Checkin> valid=new ArrayList<>();
        for(Checkin row:records) if(row.getCheckinDate()!=null && !row.getCheckinDate().isAfter(today)
                && row.getDistance()!=null && row.getDistance().signum()>0 && row.getDuration()!=null && row.getDuration()>0) valid.add(row);
        valid.sort(Comparator.comparing(Checkin::getCheckinDate).thenComparing(c->c.getId()==null?0L:c.getId()));
        List<Checkin> recent=new ArrayList<>();
        for(Checkin row:valid) if(!row.getCheckinDate().isBefore(today.minusDays(27))) recent.add(row);
        BigDecimal total=BigDecimal.ZERO,longest=BigDecimal.ZERO;long minutes=0;
        StringBuilder fingerprint=new StringBuilder(today.toString()).append('|').append(user.getRunningLevel()).append('|').append(user.getRunningGoal());
        for(Checkin row:valid) fingerprint.append('|').append(row.getId()).append(':').append(row.getCheckinDate()).append(':').append(row.getDistance()).append(':').append(row.getDuration()).append(':').append(row.getUpdateTime());
        for(Checkin row:recent){total=total.add(row.getDistance());minutes+=row.getDuration();longest=longest.max(row.getDistance());}
        Map<String,Object> context=new LinkedHashMap<>();
        context.put("asOf",today.toString());context.put("periodStart",today.minusDays(27).toString());context.put("recordCount",recent.size());
        context.put("totalKm",total);context.put("totalMinutes",minutes);context.put("longestRunKm",longest);
        context.put("averagePaceMinPerKm",total.signum()==0?null:BigDecimal.valueOf(minutes).divide(total,2,RoundingMode.HALF_UP));
        context.put("weeklyAverageKm",total.divide(BigDecimal.valueOf(4),2,RoundingMode.DOWN));
        context.put("lastTrainingDate",valid.isEmpty()?null:valid.get(valid.size()-1).getCheckinDate().toString());
        context.put("runningLevel",user.getRunningLevel());context.put("weeklyFrequencyGoal",user.getRunningGoal());
        List<Map<String,Object>> weeks=new ArrayList<>();
        for(int i=0;i<4;i++) {
            LocalDate start=today.minusDays(27-i*7L),end=start.plusDays(6);
            Map<String,Object> week=TrainingStats.period(recent,start,end,"dates");week.remove("dates");week.remove("distances");week.remove("streakDays");
            week.put("start",start.toString());week.put("end",end.toString());weeks.add(week);
        }
        context.put("weeks",weeks);context.put("existingPlans",plans);
        context.put("dataNote","近28天真实打卡；没有记录不等于没有运动。周均按4个完整七天窗口计算。记录不能证明健康或比赛能力。");
        context.put("recordVersion",cn.hutool.crypto.digest.DigestUtil.sha256Hex(fingerprint.toString().getBytes(StandardCharsets.UTF_8)));
        return context;
    }
}
