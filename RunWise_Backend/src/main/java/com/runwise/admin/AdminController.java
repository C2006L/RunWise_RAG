package com.runwise.admin;

import com.runwise.common.Result;
import com.runwise.entity.InjuryArticle;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.util.Map;

@RestController
@Validated
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminAuthService auth;
    private final AdminArticleService articles;
    public AdminController(AdminAuthService auth,AdminArticleService articles) { this.auth=auth;this.articles=articles; }
    @PostMapping("/auth/login")
    public Result<Map<String,Object>> login(@Valid @RequestBody AdminRequests.Login input) {
        return Result.success(auth.login(input.getUsername(),input.getPassword()));
    }
    @GetMapping("/auth/me")
    public Result<AdminAccount> me(@RequestAttribute("admin") AdminAccount admin) { return Result.success(admin); }
    @PostMapping("/auth/logout")
    public Result<Void> logout(@RequestAttribute("admin") AdminAccount admin) { auth.logout(admin);return Result.success(); }
    @GetMapping("/articles")
    public Result<Map<String,Object>> list(@RequestParam(required=false) @Size(max=128) String keyword,
            @RequestParam(required=false) String category,@RequestParam(required=false) String status,
            @RequestParam(defaultValue="1") @Min(1) @Max(100000) int page,
            @RequestParam(defaultValue="10") @Min(1) @Max(50) int size) {
        return Result.success(articles.list(keyword,category,status,page,size));
    }
    @GetMapping("/articles/{id}")
    public Result<InjuryArticle> detail(@PathVariable Long id) { return Result.success(articles.detail(id)); }
    @PostMapping("/articles")
    public Result<InjuryArticle> create(@Valid @RequestBody AdminRequests.Article input,@RequestAttribute("admin") AdminAccount admin) {
        return Result.success(articles.create(input,admin));
    }
    @PutMapping("/articles/{id}")
    public Result<InjuryArticle> update(@PathVariable Long id,@Valid @RequestBody AdminRequests.Article input,@RequestAttribute("admin") AdminAccount admin) {
        return Result.success(articles.update(id,input,admin));
    }
    @PutMapping("/articles/{id}/status")
    public Result<InjuryArticle> status(@PathVariable Long id,@Valid @RequestBody AdminRequests.Status input,@RequestAttribute("admin") AdminAccount admin) {
        return Result.success(articles.status(id,input,admin));
    }
    @GetMapping("/audit-logs")
    public Result<Map<String,Object>> logs(@RequestParam(defaultValue="1") @Min(1) @Max(100000) int page,
            @RequestParam(defaultValue="10") @Min(1) @Max(50) int size) { return Result.success(articles.logs(page,size)); }
}
