CREATE TABLE locations(
    id          INT AUTO_INCREMENT,
    country     VARCHAR(40),
    city        VARCHAR(100),

    CONSTRAINT pk_locations PRIMARY KEY (id)
);

CREATE TABLE users (
    id              INT AUTO_INCREMENT,
    name            VARCHAR(100),
    email           VARCHAR(50),
    location_id     INT,
    firedAt         TIMESTAMP(6),

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT fk_users_locations foreign key (location_id) REFERENCES locations(id)
);

INSERT INTO locations VALUES (1, 'Ukraine', 'Kharkiv');
INSERT INTO locations VALUES (2, 'Ukraine', 'Lviv');
INSERT INTO locations VALUES (3, 'USA', 'USA');


INSERT INTO users VALUES (1, 'user1', 'u1@acmE.org', 1, '2000-01-02 03:04:05');
INSERT INTO users VALUES (2, 'user2', 'u2@acme.org', 2, null);
INSERT INTO users VALUES (3, 'user3', 'u3@emca.org', 3, null);
INSERT INTO users VALUES (4, 'user4', 'u4@acmE.org', 1, null);
INSERT INTO users VALUES (5, 'user5', 'u5@acme.org', 2, '2000-05-04 03:02:01');

