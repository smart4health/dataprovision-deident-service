CREATE TABLE coding_failed
(
    id uuid NOT NULL PRIMARY KEY,
    system VARCHAR NOT NULL,
    code VARCHAR NOT NULL,
    display VARCHAR NULL,
    occurrence INTEGER NOT NULL,
    UNIQUE(system, code)
);