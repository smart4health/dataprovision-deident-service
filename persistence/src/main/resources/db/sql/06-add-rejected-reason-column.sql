ALTER TABLE resource
ADD COLUMN rejected_reason VARCHAR;

CREATE OR REPLACE VIEW resource_with_job AS
SELECT r.id as resource_id, r.hash as hash, j.id as job_id, j.user_id, j.status, j.created_at, j.updated_at, r.rejected_reason
FROM resource r
JOIN job j
ON j.id = r.job_id;