-- 在 admin_v1.sql 之后执行一次。备份后再运行，不删除原数据。
CREATE TABLE t_web_credential (
 user_id BIGINT NOT NULL PRIMARY KEY,
 username VARCHAR(32) NOT NULL UNIQUE,
 password_hash VARCHAR(255) NOT NULL,
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE t_training_plan_item (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL,
 training_date DATE NOT NULL,
 type VARCHAR(16) NOT NULL,
 target_km DECIMAL(6,2) NOT NULL DEFAULT 0,
 note VARCHAR(255) NOT NULL DEFAULT '',
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 UNIQUE KEY uk_plan_user_date (user_id,training_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE t_qa_record ADD COLUMN conversation_id VARCHAR(80) NULL,
 ADD INDEX idx_qa_user_conversation (user_id,conversation_id,create_time);

-- 只约束未删除记录，同一天已删除后允许重新打卡。
-- 若已有重复记录，此步会失败；先人工核对重复数据，禁止自动删记录。
ALTER TABLE t_checkin
 ADD COLUMN active_checkin_date DATE GENERATED ALWAYS AS (IF(deleted=0,checkin_date,NULL)) VIRTUAL,
 ADD UNIQUE KEY uk_checkin_user_active_date (user_id,active_checkin_date);
