create sequence users_id_seq no maxvalue;
create table users
(
    id             bigint       not null default nextval('users_id_seq'),
    name           varchar(100) not null,
    email          varchar(100),
    is_cool        boolean,
    bio            text,
    favorite_color varchar(10)
);

alter table users
    add primary key (id);

create sequence mismatchs_id_seq no maxvalue;

create table mismatchs
(
    id bigint not null default nextval('mismatchs_id_seq')
);
alter table mismatchs
    add primary key (id);
