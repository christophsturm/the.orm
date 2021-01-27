create sequence users_id_seq no maxvalue;
create table users
(
    id             bigint       not null default nextval('users_id_seq') primary key,
    name           varchar(100) not null,
    email          varchar(100) unique,
    is_cool        boolean,
    bio            text,
    favorite_color varchar(10),
    birthday       date,
    weight         decimal(5, 2),
    balance        decimal(5, 2)

);



create sequence serializable_users_id_seq no maxvalue;

create table serializable_users
(
    id    bigint       not null default nextval('serializable_users_id_seq'),
    name  varchar(100) not null,
    email varchar(100)
);

alter table serializable_users
    add primary key (id);

create sequence mismatchs_id_seq no maxvalue;

create table mismatchs
(
    id bigint not null default nextval('mismatchs_id_seq')
);
alter table mismatchs
    add primary key (id);


create sequence vegetables_id_seq no maxvalue;
create table vegetables
(
    id   bigint      not null default nextval('vegetables_id_seq') primary key,
    name varchar(20) not null
);
