CREATE TABLE resource
(
    id VARCHAR NOT NULL,
    job_id uuid NOT NULL,
    hash VARCHAR NOT NULL,
    PRIMARY KEY(id, job_id),
    CONSTRAINT fk FOREIGN KEY(job_id) REFERENCES job(id)
);