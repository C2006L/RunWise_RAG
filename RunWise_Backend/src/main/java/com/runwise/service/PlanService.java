package com.runwise.service;

import com.runwise.common.*;
import com.runwise.dto.PlanItemDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;

@Service
public class PlanService {
    private final JdbcTemplate jdbc;
    private static final String[] WEEKDAYS={"周一","周二","周三","周四","周五","周六","周日"};
    public PlanService(JdbcTemplate jdbc) {this.jdbc=jdbc;}
    public List<Map<String,Object>> weeks(Long user) {
        LocalDate monday=LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        List<Map<String,Object>> items=jdbc.queryForList("SELECT * FROM t_training_plan_item WHERE user_id=? AND training_date BETWEEN ? AND ?",user,monday,monday.plusDays(27));
        Map<LocalDate,Map<String,Object>> byDate=new HashMap<>();
        for(Map<String,Object> item:items) byDate.put(((java.sql.Date)item.get("training_date")).toLocalDate(),item);
        Map<LocalDate,BigDecimal> actual=new HashMap<>();
        if(!items.isEmpty()) {
            List<Map<String,Object>> records=jdbc.queryForList("SELECT checkin_date,SUM(distance) AS actual_km FROM t_checkin WHERE user_id=? AND deleted=0 AND checkin_date BETWEEN ? AND ? GROUP BY checkin_date",user,monday,LocalDate.now());
            for(Map<String,Object> record:records) actual.put(((java.sql.Date)record.get("checkin_date")).toLocalDate(),(BigDecimal)record.get("actual_km"));
        }
        List<Map<String,Object>> result=new ArrayList<>();
        for(int w=0;w<4;w++) {
            Map<String,Object> week=new LinkedHashMap<>();week.put("weekNo",w+1);week.put("weekStart",monday.plusWeeks(w).toString());
            List<Map<String,Object>> days=new ArrayList<>();
            for(int d=0;d<7;d++) {LocalDate date=monday.plusDays(w*7L+d);days.add(day(date,byDate.get(date),actual.getOrDefault(date,BigDecimal.ZERO)));}
            week.put("days",days);result.add(week);
        }
        return result;
    }
    private Map<String,Object> day(LocalDate date,Map<String,Object> row,BigDecimal actualKm) {
        String type=row==null?"rest":(String)row.get("type");
        Map<String,String> labels=new HashMap<>();labels.put("easy","轻松跑");labels.put("tempo","节奏跑");labels.put("interval","间歇跑");labels.put("long","长距离");labels.put("rest","休息");
        Map<String,Object> result=new LinkedHashMap<>();result.put("id",row==null?"empty-"+date:row.get("id"));
        result.put("date",date.toString());result.put("weekday",WEEKDAYS[date.getDayOfWeek().getValue()-1]);
        result.put("type",type);result.put("label",labels.get(type));result.put("targetKm",row==null?BigDecimal.ZERO:row.get("target_km"));
        result.put("note",row==null?"":row.get("note"));result.put("paceRange","");
        BigDecimal target=row==null?BigDecimal.ZERO:(BigDecimal)row.get("target_km");
        if(actualKm==null) actualKm=BigDecimal.ZERO;
        String status="rest".equals(type)?"rest":actualKm.signum()<=0?"planned":actualKm.compareTo(target)>=0?"distance_met":"in_progress";
        result.put("actualKm",actualKm);result.put("status",status);
        result.put("statusNote","distance_met".equals(status)?"同日有效打卡里程达标；未核验训练课型和配速，不等同于任务完成。":"in_progress".equals(status)?"同日有效打卡尚未达到目标里程。":"");
        return result;
    }
    @Transactional
    public void add(Long user,PlanItemDTO input) {
        validate(input);
        jdbc.update("INSERT INTO t_training_plan_item(user_id,training_date,type,target_km,note) VALUES (?,?,?,?,?)",user,input.getDate(),input.getType(),input.getTargetKm(),input.getNote()==null?"":input.getNote());
    }
    @Transactional
    public void update(Long user,Long id,PlanItemDTO input) {
        validate(input);
        int changed=jdbc.update("UPDATE t_training_plan_item SET training_date=?,type=?,target_km=?,note=? WHERE id=? AND user_id=?",input.getDate(),input.getType(),input.getTargetKm(),input.getNote()==null?"":input.getNote(),id,user);
        if(changed!=1) throw new BusinessException(ResultCode.NOT_FOUND,"训练不存在");
    }
    public void remove(Long user,Long id) {
        if(jdbc.update("DELETE FROM t_training_plan_item WHERE id=? AND user_id=?",id,user)!=1)
            throw new BusinessException(ResultCode.NOT_FOUND,"训练不存在");
    }
    private void validate(PlanItemDTO input) {
        LocalDate monday=LocalDate.now().with(DayOfWeek.MONDAY);
        if(input.getDate().isBefore(monday)||input.getDate().isAfter(monday.plusDays(27)))
            throw new BusinessException(ResultCode.PARAM_ERROR,"日期需在当前四周计划内");
        if(("rest".equals(input.getType()) && input.getTargetKm().signum()!=0)
                || (!"rest".equals(input.getType()) && input.getTargetKm().signum()<=0))
            throw new BusinessException(ResultCode.PARAM_ERROR,"训练距离需大于0，休息日距离需为0");
    }
}
