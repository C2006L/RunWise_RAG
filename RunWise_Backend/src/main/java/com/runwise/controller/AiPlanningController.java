package com.runwise.controller;
import com.runwise.service.*;
import com.runwise.common.Result;
import com.runwise.dto.PlanDraftRequest;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/qa")
public class AiPlanningController {
    private final RunnerContextService context;private final PlanDraftService drafts;
    public AiPlanningController(RunnerContextService context,PlanDraftService drafts){this.context=context;this.drafts=drafts;}
    @GetMapping("/training-context") public Result<Map<String,Object>> context(@RequestAttribute("userId") Long user){return Result.success(context.build(user));}
    @PostMapping("/plan-drafts") public Result<Map<String,Object>> generate(@RequestAttribute("userId") Long user,@Valid @RequestBody PlanDraftRequest request){return Result.success(drafts.generate(user,request));}
    @GetMapping("/plan-drafts/{id}") public Result<Map<String,Object>> detail(@RequestAttribute("userId") Long user,@PathVariable String id){return Result.success(drafts.detail(user,id));}
    @PostMapping("/plan-drafts/{id}/confirm") public Result<Map<String,Object>> confirm(@RequestAttribute("userId") Long user,@PathVariable String id){return Result.success(drafts.confirm(user,id));}
}
