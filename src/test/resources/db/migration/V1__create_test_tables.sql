create sequence users_id_seq no maxvalue;
create table users
(
    ID     bigint       not null primary key default nextval('users_id_seq'),
    NAME   varchar(100) not null,
    EMAIL  varchar(100),
    ISCOOL boolean
);
