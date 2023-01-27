ALTER TABLE coding_failed ADD COLUMN valid_start TIMESTAMP;
ALTER TABLE coding_failed ADD COLUMN valid_end TIMESTAMP;
ALTER TABLE coding_failed DROP CONSTRAINT coding_failed_system_code_key