package com.runwise.service;
import com.runwise.common.*;
import com.runwise.dto.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;

/** 确定性的保存边界，模型不能绕过。保守上限不代表医学审核。 */
@Component
public class PlanDraftValidator {
    public static final String VERSION="conservative-week-v1";
    public void validateRequest(PlanDraftRequest request,LocalDate today) {
        if(request.getStartDate().isBefore(today.plusDays(1)) || request.getStartDate().plusDays(6).isAfter(today.with(DayOfWeek.MONDAY).plusDays(27)))
            throw new BusinessException(ResultCode.PARAM_ERROR,"开始日期最早为明天，且完整7天须位于当前四周课表内");
    }
    public List<String> missing(PlanDraftRequest request,Map<String,Object> context) {
        List<String> questions=new ArrayList<>();
        if(!Boolean.TRUE.equals(request.getNoDiscomfortConfirmed()) || (request.getConstraints()!=null&&request.getConstraints().matches("(?s).*(疼|痛|受伤|发热|发烧|头晕|胸闷).*")))
            questions.add("你提到或尚未排除身体不适，请先说明当前体感；暂不生成可执行课表。");
        if(((Number)context.get("recordCount")).intValue()<2)
            questions.add("近28天可用记录不足。请补录真实训练，或先向助手说明训练经历；当前不生成精确跑量课表。");
        String last=(String)context.get("lastTrainingDate");
        if(last!=null && LocalDate.parse(last).isBefore(LocalDate.now().minusDays(7)))
            questions.add("最近一周没有训练记录，请先补充近期训练或恢复情况。");
        return questions;
    }
    public void validatePlan(GeneratedPlan plan,PlanDraftRequest input,Map<String,Object> context) {
        if(plan==null || plan.getExplanation()==null || plan.getExplanation().trim().isEmpty() || plan.getExplanation().length()>12000
                || plan.getDays()==null || plan.getDays().size()!=7) invalid("需要完整7天和计划说明");
        BigDecimal total=BigDecimal.ZERO;
        BigDecimal budget=new BigDecimal(context.get("weeklyAverageKm").toString());
        BigDecimal longest=new BigDecimal(context.get("longestRunKm").toString());
        int runs=0,quality=0,consecutive=0;boolean previousQuality=false;
        for(int i=0;i<7;i++) {
            PlanItemDTO day=plan.getDays().get(i);
            if(day==null || day.getDate()==null || !day.getDate().equals(input.getStartDate().plusDays(i))) invalid("日期必须连续且与请求一致");
            if(!Arrays.asList("easy","tempo","interval","long","rest").contains(day.getType()) || day.getTargetKm()==null) invalid("训练类型或距离无效");
            BigDecimal km=day.getTargetKm();boolean rest="rest".equals(day.getType());
            if(km.stripTrailingZeros().scale()>2 || km.signum()<0 || (rest&&km.signum()!=0) || (!rest&&km.compareTo(new BigDecimal("0.01"))<0)
                    || km.compareTo(longest)>0 || km.compareTo(new BigDecimal("300"))>0) invalid("单次距离超过现有记录依据或休息日不为0");
            if(day.getNote()!=null&&day.getNote().length()>255) invalid("训练说明过长");
            boolean hard="tempo".equals(day.getType())||"interval".equals(day.getType());
            if(hard){quality++;if(previousQuality) invalid("质量训练之间需要恢复");}
            consecutive=rest?0:consecutive+1;
            if(consecutive>2) invalid("连续训练日过多，需要安排恢复日");
            previousQuality=hard;runs+=rest?0:1;total=total.add(km);
        }
        if(runs<1||runs>input.getSessionsPerWeek()||quality>1||total.compareTo(budget)>0)
            invalid("训练次数或周跑量超出本次保守上限");
    }
    private void invalid(String reason){throw new BusinessException(ResultCode.PARAM_ERROR,"生成草稿未通过校验："+reason+"。请调整要求后重试。");}
}
