--DROP TABLE composition_status;
CREATE TABLE IF NOT EXISTS composition_status (id varchar(20) NOT NULL, revision varchar(20) DEFAULT 0, name blob(1024), is_changed varchar(2), PRIMARY KEY (id, revision));
CREATE TABLE IF NOT EXISTS test (id varchar(20) NOT NULL, name varchar(20), is_changed varchar(2), PRIMARY KEY (id));

