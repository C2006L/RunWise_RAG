-- Existing database upgrade, not a fresh-install/reset script.
-- Run from repository root using a MySQL administrator after making a backup.
-- Only for databases that have NOT applied admin_v1/business_v2/ai_planning_v3/badge_v4.
SET NAMES utf8mb4;
-- mysql SOURCE stops at errors only when the client is configured accordingly;
-- do not blindly rerun after a partial failure. Inspect the schema first.
SOURCE RunWise_Backend/sql/admin_v1.sql;
SOURCE RunWise_Backend/sql/business_v2.sql;
SOURCE RunWise_Backend/sql/ai_planning_v3.sql;
SOURCE RunWise_Backend/sql/badge_v4.sql;
