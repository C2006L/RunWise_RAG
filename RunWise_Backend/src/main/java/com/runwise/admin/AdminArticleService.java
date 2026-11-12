package com.runwise.admin;

import com.runwise.common.*;
import com.runwise.entity.InjuryArticle;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.support.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.*;
import java.util.*;

@Service
public class AdminArticleService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    public AdminArticleService(JdbcTemplate jdbc, ObjectMapper json) { this.jdbc = jdbc; this.json = json; }

    public Map<String, Object> list(String keyword, String category, String status, int page, int size) {
        String where = " WHERE deleted=0";
        List<Object> args = new ArrayList<>();
        if (keyword != null && !keyword.trim().isEmpty()) { where += " AND title LIKE ?"; args.add("%" + keyword.trim() + "%"); }
        if (category != null && !category.isEmpty()) { where += " AND category=?"; args.add(category); }
        if (status != null && !status.isEmpty()) { where += " AND status=?"; args.add(status); }
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM t_injury_article" + where, Long.class, args.toArray());
        args.add(size); args.add((page-1)*size);
        List<InjuryArticle> items = jdbc.query("SELECT id,title,summary,category,tags,read_minutes,sort,status,revision,updated_by,create_time,update_time FROM t_injury_article"
                + where + " ORDER BY update_time DESC,id DESC LIMIT ? OFFSET ?", new BeanPropertyRowMapper<>(InjuryArticle.class), args.toArray());
        return page(items, total, page, size);
    }
    public InjuryArticle detail(Long id) {
        List<InjuryArticle> items = jdbc.query("SELECT * FROM t_injury_article WHERE id=? AND deleted=0",
                new BeanPropertyRowMapper<>(InjuryArticle.class), id);
        if (items.isEmpty()) throw new BusinessException(ResultCode.NOT_FOUND, "文章不存在");
        return items.get(0);
    }
    @Transactional
    public InjuryArticle create(AdminRequests.Article input, AdminAccount admin) {
        KeyHolder key = new GeneratedKeyHolder();
        String sections = encode(input.getSections());
        jdbc.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement("INSERT INTO t_injury_article(title,category,summary,tags,sections,read_minutes,sort,status,revision,updated_by) VALUES (?,?,?,?,?,?,?,'DRAFT',1,?)", new String[]{"id"});
            stmt.setString(1,input.getTitle().trim()); stmt.setString(2,input.getCategory());
            stmt.setString(3,input.getSummary()); stmt.setString(4,input.getTags()); stmt.setString(5,sections);
            stmt.setInt(6,input.getReadMinutes()); stmt.setInt(7,input.getSort()); stmt.setLong(8,admin.getId());
            return stmt;
        }, key);
        Long id = Objects.requireNonNull(key.getKey()).longValue();
        audit(admin,"ARTICLE_CREATE",id,"创建草稿：" + input.getTitle());
        return detail(id);
    }
    @Transactional
    public InjuryArticle update(Long id, AdminRequests.Article input, AdminAccount admin) {
        int changed = jdbc.update("UPDATE t_injury_article SET title=?,category=?,summary=?,tags=?,sections=?,read_minutes=?,sort=?,status='DRAFT',revision=revision+1,updated_by=?,update_time=CURRENT_TIMESTAMP WHERE id=? AND revision=? AND deleted=0",
                input.getTitle().trim(),input.getCategory(),input.getSummary(),input.getTags(),encode(input.getSections()),
                input.getReadMinutes(),input.getSort(),admin.getId(),id,input.getRevision());
        checkRevision(changed);
        audit(admin,"ARTICLE_EDIT",id,"保存草稿，原版本 " + input.getRevision());
        return detail(id);
    }
    @Transactional
    public InjuryArticle status(Long id, AdminRequests.Status input, AdminAccount admin) {
        int changed = jdbc.update("UPDATE t_injury_article SET status=?,revision=revision+1,updated_by=?,update_time=CURRENT_TIMESTAMP WHERE id=? AND revision=? AND deleted=0",
                input.getStatus(),admin.getId(),id,input.getRevision());
        checkRevision(changed);
        audit(admin,"PUBLISHED".equals(input.getStatus()) ? "ARTICLE_PUBLISH" : "ARTICLE_OFFLINE",id,"原版本 " + input.getRevision());
        return detail(id);
    }
    public Map<String, Object> logs(int page, int size) {
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM t_admin_audit_log", Long.class);
        return page(jdbc.queryForList("SELECT id,username,action,target_id,detail,create_time FROM t_admin_audit_log ORDER BY id DESC LIMIT ? OFFSET ?", size,(page-1)*size),total,page,size);
    }
    private void audit(AdminAccount admin,String action,Long id,String detail) {
        jdbc.update("INSERT INTO t_admin_audit_log(admin_id,username,action,target_id,detail) VALUES (?,?,?,?,?)",admin.getId(),admin.getUsername(),action,id,detail);
    }
    private void checkRevision(int changed) {
        if (changed != 1) throw new BusinessException(ResultCode.PARAM_ERROR, "文章已被其他管理员修改或不存在，请重新加载");
    }
    private String encode(Object value) {
        try {
            String encoded = json.writeValueAsString(value);
            if (encoded.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 60000)
                throw new BusinessException(ResultCode.PARAM_ERROR,"正文过长，请控制在60000字节以内");
            return encoded;
        } catch (JsonProcessingException e) { throw new BusinessException(ResultCode.PARAM_ERROR,"正文格式错误"); }
    }
    private Map<String,Object> page(Object list,Long total,int page,int size) {
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("list",list); result.put("total",total); result.put("page",page); result.put("size",size); return result;
    }
}
