CREATE TABLE job
(
    id uuid NOT NULL PRIMARY KEY,
    user_id VARCHAR NOT NULL,
    status VARCHAR NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);