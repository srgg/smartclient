CREATE TABLE simpleentity
(
    id      INT AUTO_INCREMENT,
    name VARCHAR(40),

    CONSTRAINT pk_simple_entity PRIMARY KEY (id)
);

CREATE TABLE locations(
    id          INT AUTO_INCREMENT,
    country     VARCHAR(40),
    city        VARCHAR(100),

    CONSTRAINT pk_locations PRIMARY KEY (id)
);

CREATE TABLE employee (
    id              INT AUTO_INCREMENT,
    name            VARCHAR(100),
    email           VARCHAR(50),
    location_id     INT,
    firedAt         TIMESTAMP(6),

    CONSTRAINT pk_employee PRIMARY KEY (id),
    CONSTRAINT fkEmployeeLocations foreign key (location_id) REFERENCES locations(id)
);

CREATE TABLE employee_role
(
    employee_id int NOT NULL,
    role ENUM ('EMPLOYEE', 'PM', 'AM', 'C-Level', 'FM', 'Admin', 'Developer') NOT NULL,

    CONSTRAINT pkEmployeeRole PRIMARY KEY  (employee_id, role),

    CONSTRAINT fkEmployeeId
        FOREIGN KEY (employee_id) REFERENCES employee (id),

    CONSTRAINT uqUserRole UNIQUE (employee_id, role)
);


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

CREATE TABLE project_team
(
    project_id INT NOT NULL,
    employee_id INT NOT NULL,

    primary key (project_id, employee_id),

    constraint fkProjectTeamProject
        foreign key (project_id) references project (id),

    constraint fkProjectTeamEmployee
        foreign key (employee_id) references employee (id)
);

create table employee_status
(
    id int auto_increment primary key,
    employee_id int not null,
    status varchar(255) null,

    start_date date not null,
    end_date date null,

    constraint unqEmployeeStatus
        unique (employee_id, start_date),
    constraint fkEmployeeStatus_Employee
        foreign key (employee_id) references employee (id)
);


INSERT INTO locations VALUES (1, 'Ukraine', 'Kharkiv');
INSERT INTO locations VALUES (2, 'Ukraine', 'Lviv');
INSERT INTO locations VALUES (3, 'USA', 'USA');


INSERT INTO employee VALUES (1, 'admin', 'admin@acmE.org', 1, '2000-01-02 03:04:05');
INSERT INTO employee VALUES (2, 'developer', 'developer@acme.org', 2, null);
INSERT INTO employee VALUES (3, 'UseR3', 'u3@emca.org', 3, null);
INSERT INTO employee VALUES (4, 'user4', 'u4@acmE.org', 1, null);
INSERT INTO employee VALUES (5, 'user5', 'u5@acme.org', 2, '2000-05-04 03:02:01');

INSERT INTO employee_role VALUES (1, 'Admin');
INSERT INTO employee_role VALUES (1, 'Developer');
INSERT INTO employee_role VALUES (2, 'Developer');


INSERT INTO client VALUES (1, 'client 1');
INSERT INTO client VALUES (2, 'client 2');

INSERT INTO client_data VALUES (1, 'Data1: client 1', 1);
INSERT INTO client_data VALUES (2, 'Data2: client 2', 2);

INSERT INTO project VALUES (1, 1, 'Project 1 for client 1');
INSERT INTO project VALUES (2, 1, 'Project 2 for client 1');
INSERT INTO project VALUES (3, 2, 'Project 1 for client 2');
INSERT INTO project VALUES (4, 2, 'Project 2 for client 2');
INSERT INTO project VALUES (5, 2, 'Project 3 for client 2');

INSERT INTO project_team VALUES (1, 1);
INSERT INTO project_team VALUES (1, 2);

INSERT INTO project_team VALUES (2, 2);
INSERT INTO project_team VALUES (2, 3);

INSERT INTO project_team VALUES (3, 4);
INSERT INTO project_team VALUES (3, 5);

INSERT INTO employee_status VALUES (1, 1, 'status 1', '2000-05-04', '2000-06-04');
INSERT INTO employee_status VALUES (2, 1, 'status 2', '2000-06-05', '2000-07-05');
INSERT INTO employee_status VALUES (3, 1, 'status 3', '2000-07-06', null);
