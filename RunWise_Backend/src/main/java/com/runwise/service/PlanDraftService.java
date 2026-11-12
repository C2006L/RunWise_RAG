package com.runwise.service;

import com.runwise.common.*;
import com.runwise.dto.*;
import com.runwise.entity.QaRecord;
import com.runwise.mapper.QaRecordMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.sql.Timestamp;
import java.util.*;

@Service
public class PlanDraftService {
    private final RunnerContextService context;
    private final PlanGenerationGateway generator;
    private final PlanDraftValidator validator;
    private final PlanService plans;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final QaRecordMapper history;
    private final TransactionTemplate transaction;
    public PlanDraftService(RunnerContextService context,PlanGenerationGateway generator,PlanDraftValidator validator,
            PlanService plans,JdbcTemplate jdbc,ObjectMapper json,QaRecordMapper history,PlatformTransactionManager tx) {
        this.context=context;this.generator=generator;this.validator=validator;this.plans=plans;this.jdbc=jdbc;this.json=json;this.history=history;
        this.transaction=new TransactionTemplate(tx);
    }
    public Map<String,Object> generate(Long user,PlanDraftRequest request) {
        validator.validateRequest(request,LocalDate.now());
        Map<String,Object> snapshot=context.build(user);
        snapshot.put("planningRulesVersion",PlanDraftValidator.VERSION);
        List<String> questions=validator.missing(request,snapshot);
        if(!questions.isEmpty()) {
            Map<String,Object> result=new LinkedHashMap<>();result.put("status","NEEDS_INPUT");result.put("questions",questions);result.put("trainingContext",snapshot);return result;
        }
        GeneratedPlan plan=generator.generate(request,snapshot);
        if(plan!=null && "NEEDS_INPUT".equals(plan.getStatus())) {
            Map<String,Object> result=new LinkedHashMap<>();result.put("status","NEEDS_INPUT");
            result.put("questions",plan.getQuestions()==null?Collections.singletonList("请补充可训练日期或调整要求，再生成草稿。"):plan.getQuestions());
            result.put("trainingContext",snapshot);return result;
        }
        validator.validatePlan(plan,request,snapshot);
        String id=UUID.randomUUID().toString();
        String conversation=request.getConversationId()==null?UUID.randomUUID().toString():request.getConversationId();
        // 模型请求在事务之外，只有草稿与问答记录持久化使用短事务。
        return transaction.execute(status->{
            jdbc.update("INSERT INTO t_ai_plan_draft(id,user_id,request_json,context_json,plan_json,expires_at) VALUES (?,?,?,?,?,?)",id,user,encode(request),encode(snapshot),encode(plan),Timestamp.valueOf(LocalDateTime.now().plusHours(24)));
            QaRecord record=new QaRecord();record.setUserId(user);record.setConversationId(conversation);record.setPlanDraftId(id);
            record.setQuestion("基于我的训练记录制定7天计划，目标："+request.getGoal()+"，每周最多"+request.getSessionsPerWeek()+"次");
            record.setAnswer(plan.getExplanation());record.setSources(plan.getSources()==null?"":String.join("、",plan.getSources()));record.setFeedback(0);
            history.insert(record);
            Map<String,Object> result=detail(user,id);result.put("recordId",record.getId());result.put("conversationId",conversation);return result;
        });
    }
    public Map<String,Object> detail(Long user,String id) {return decode(find(user,id,false));}
    @Transactional
    public Map<String,Object> confirm(Long user,String id) {
        Map<String,Object> row=find(user,id,true);
        if("APPLIED".equals(row.get("status"))) return decode(row);
        if(((Timestamp)row.get("expires_at")).toLocalDateTime().isBefore(LocalDateTime.now()))
            throw new BusinessException(ResultCode.PARAM_ERROR,"草稿已过期，请重新生成");
        PlanDraftRequest input=read((String)row.get("request_json"),PlanDraftRequest.class);
        GeneratedPlan plan=read((String)row.get("plan_json"),GeneratedPlan.class);
        Map<String,Object> oldContext=read((String)row.get("context_json"),Map.class),fresh=context.build(user);
        if(!PlanDraftValidator.VERSION.equals(oldContext.get("planningRulesVersion")))
            throw new BusinessException(ResultCode.PARAM_ERROR,"计划规则已更新，请重新生成草稿");
        if(!Objects.equals(oldContext.get("recordVersion"),fresh.get("recordVersion")))
            throw new BusinessException(ResultCode.PARAM_ERROR,"训练记录或日期已变化，请重新生成计划");
        validator.validatePlan(plan,input,fresh);
        Integer conflicts=jdbc.queryForObject("SELECT COUNT(*) FROM t_training_plan_item WHERE user_id=? AND training_date BETWEEN ? AND ?",Integer.class,user,input.getStartDate(),input.getStartDate().plusDays(6));
        if(conflicts!=null && conflicts>0) throw new BusinessException(ResultCode.PARAM_ERROR,"这些日期已有课表，未覆盖任何训练；请调整开始日期或先处理已有安排。");
        for(PlanItemDTO day:plan.getDays()) plans.add(user,day);
        jdbc.update("UPDATE t_ai_plan_draft SET status='APPLIED',applied_at=CURRENT_TIMESTAMP WHERE id=? AND user_id=?",id,user);
        return detail(user,id);
    }
    private Map<String,Object> find(Long user,String id,boolean lock) {
        if(id==null||!id.matches("[A-Za-z0-9-]{36}")) throw new BusinessException(ResultCode.NOT_FOUND);
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT * FROM t_ai_plan_draft WHERE id=? AND user_id=?"+(lock?" FOR UPDATE":""),id,user);
        if(rows.isEmpty()) throw new BusinessException(ResultCode.NOT_FOUND,"草稿不存在");return rows.get(0);
    }
    private Map<String,Object> decode(Map<String,Object> row) {
        Map<String,Object> result=new LinkedHashMap<>();result.put("id",row.get("id"));result.put("status",row.get("status"));
        result.put("request",read((String)row.get("request_json"),PlanDraftRequest.class));
        result.put("trainingContext",read((String)row.get("context_json"),Map.class));result.put("plan",read((String)row.get("plan_json"),GeneratedPlan.class));
        result.put("expiresAt",((Timestamp)row.get("expires_at")).toLocalDateTime().toString());return result;
    }
    private String encode(Object value) {try{return json.writeValueAsString(value);}catch(Exception e){throw new IllegalStateException("草稿序列化失败",e);}}
    private <T>T read(String value,Class<T> type){try{return json.readValue(value,type);}catch(Exception e){throw new IllegalStateException("草稿数据读取失败",e);}}
}
