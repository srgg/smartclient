CREATE TABLE simpleentity
(
    id      INT AUTO_INCREMENT,
    name VARCHAR(40),

    CONSTRAINT pk_simple_entity PRIMARY KEY (id)
);

-- DROP TABLE IF EXISTS locations;

CREATE TABLE locations(
    id          INT AUTO_INCREMENT,
    country     VARCHAR(40),
    city        VARCHAR(100),

    CONSTRAINT pk_locations PRIMARY KEY (id)
);

-- DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id              INT AUTO_INCREMENT,
    name            VARCHAR(100),
    email           VARCHAR(50),
    location_id     INT,
    firedAt         TIMESTAMP(6),

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT fk_users_locations foreign key (location_id) REFERENCES locations(id)
);

-- Test Data

INSERT INTO locations VALUES (1, 'Ukraine', 'Kharkiv');
INSERT INTO locations VALUES (2, 'Ukraine', 'Lviv');
INSERT INTO locations VALUES (3, 'USA', 'USA');


INSERT INTO users VALUES (1, 'user1', 'u1@acmE.org', 1, '2000-01-02 03:04:05');
INSERT INTO users VALUES (2, 'user2', 'u2@acme.org', 2, null);
INSERT INTO users VALUES (3, 'user3', 'u3@emca.org', 3, null);
INSERT INTO users VALUES (4, 'user4', 'u4@acmE.org', 1, null);
INSERT INTO users VALUES (5, 'user5', 'u5@acme.org', 2, '2000-05-04 03:02:01');

-- Schema for JPA Tests
-- DROP TABLE IF EXISTS client;

CREATE TABLE client
(
    id int not null,
    name varchar(255) not null,

    CONSTRAINT pk_clients PRIMARY KEY (id)
);


CREATE TABLE client_data
(
    id int not null,
    data varchar(255) not null,
    client_id int NOT NULL,

    CONSTRAINT pk_client_data PRIMARY KEY (id),

    CONSTRAINT fkClientData_ClientId
        FOREIGN KEY (client_id) REFERENCES CLIENT (id)
);

-- DROP TABLE IF EXISTS project;
CREATE TABLE project
(
    id INT NOT NULL,
    client_id INT,
    name VARCHAR(255) NOT NULL,
--     manager_id int null,

    CONSTRAINT pk_projects PRIMARY KEY (id),

    CONSTRAINT fkProject_ClientId
        FOREIGN KEY (client_id) REFERENCES CLIENT (id)

--     constraint FKdmtefyns5kjj5u2p99o3pk37y
--         foreign key (manager_id) references employee (id)
);


-- Test Data for JPA

INSERT INTO client VALUES (1, 'client 1');
INSERT INTO client VALUES (2, 'client 2');

INSERT INTO client_data VALUES (1, 'Data1: client 1', 1);
INSERT INTO client_data VALUES (2, 'Data2: client 2', 2);

INSERT INTO project VALUES (1, 1, 'Project 1 for client 1');
INSERT INTO project VALUES (2, 1, 'Project 2 for client 1');
INSERT INTO project VALUES (3, 2, 'Project 1 for client 2');
INSERT INTO project VALUES (4, 2, 'Project 2 for client 2');
INSERT INTO project VALUES (5, 2, 'Project 3 for client 2');
