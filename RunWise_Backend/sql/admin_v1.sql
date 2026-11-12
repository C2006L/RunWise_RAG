-- 首次安装执行一次。先备份数据库；本脚本不删除现有表或文章。
-- 请勿重新执行 injury_init.sql，它会 DROP 文章表。
CREATE TABLE IF NOT EXISTS t_admin_account (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  role VARCHAR(32) NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_admin_audit_log (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  admin_id BIGINT NOT NULL,
  username VARCHAR(64) NOT NULL,
  action VARCHAR(64) NOT NULL,
  target_id BIGINT NULL,
  detail VARCHAR(1000) NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_audit_time (create_time, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 旧文章保持已发布，新建文章由接口明确写入 DRAFT。
ALTER TABLE t_injury_article
  ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'PUBLISHED',
  ADD COLUMN revision INT NOT NULL DEFAULT 1,
  ADD COLUMN updated_by BIGINT NULL,
  ADD INDEX idx_article_status_sort (status, sort);
