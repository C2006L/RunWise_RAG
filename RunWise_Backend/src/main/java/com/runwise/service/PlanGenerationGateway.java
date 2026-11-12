package com.runwise.service;
import com.runwise.dto.*;
import com.runwise.common.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.hutool.http.HttpRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class PlanGenerationGateway {
    private final ObjectMapper json;
    @Value("${runwise.rag.base-url:http://localhost:5000}") private String baseUrl;
    public PlanGenerationGateway(ObjectMapper json){this.json=json;}
    public GeneratedPlan generate(PlanDraftRequest input,Map<String,Object> context) {
        try {
            Map<String,Object> body=new LinkedHashMap<>();body.put("request",input);body.put("trainingContext",context);
            try(cn.hutool.http.HttpResponse response=HttpRequest.post(baseUrl+"/api/rag/plan").header("Content-Type","application/json")
                    .body(json.writeValueAsString(body)).timeout(85000).execute()) {
                if(!response.isOk()) throw ModelServiceErrors.fromResponse(response.body());
                return json.readValue(response.body(),GeneratedPlan.class);
            }
        } catch(BusinessException e) {throw e;}
        catch(Exception e) {throw new BusinessException(ResultCode.RAG_SERVICE_ERROR,"计划生成服务暂不可用，或返回格式不完整。未保存任何课表，请稍后重试。");}
    }
}
