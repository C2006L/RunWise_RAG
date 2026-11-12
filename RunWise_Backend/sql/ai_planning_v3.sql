-- 在 admin_v1.sql 和 business_v2.sql 之后执行一次。先备份；不删除旧问答或计划。
CREATE TABLE t_ai_plan_draft (
 id VARCHAR(36) NOT NULL PRIMARY KEY,
 user_id BIGINT NOT NULL,
 status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
 request_json MEDIUMTEXT NOT NULL,
 context_json MEDIUMTEXT NOT NULL,
 plan_json MEDIUMTEXT NOT NULL,
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 expires_at DATETIME NOT NULL,
 applied_at DATETIME NULL,
 KEY idx_draft_user (user_id,create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
ALTER TABLE t_qa_record ADD COLUMN plan_draft_id VARCHAR(36) NULL;
