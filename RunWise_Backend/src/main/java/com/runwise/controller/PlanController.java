package com.runwise.controller;
import com.runwise.common.Result;
import com.runwise.dto.PlanItemDTO;
import com.runwise.service.PlanService;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.*;

@RestController
@RequestMapping("/api/plan")
public class PlanController {
    private final PlanService service;
    public PlanController(PlanService service) {this.service=service;}
    @GetMapping("/list") public Result<List<Map<String,Object>>> list(@RequestAttribute("userId") Long user) {return Result.success(service.weeks(user));}
    @PostMapping("/item") public Result<Void> add(@RequestAttribute("userId") Long user,@Valid @RequestBody PlanItemDTO input) {service.add(user,input);return Result.success();}
    @PutMapping("/item/{id}") public Result<Void> update(@RequestAttribute("userId") Long user,@PathVariable Long id,@Valid @RequestBody PlanItemDTO input) {service.update(user,id,input);return Result.success();}
    @DeleteMapping("/item/{id}") public Result<Void> remove(@RequestAttribute("userId") Long user,@PathVariable Long id) {service.remove(user,id);return Result.success();}
}
