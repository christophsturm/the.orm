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
    id     bigint      not null default nextval('vegetables_id_seq') primary key,
    name   varchar(20) not null unique,
    weight decimal(5, 2)
);

-- multi repo test
create sequence pages_id_seq no maxvalue;
create table pages
(
    id          bigint        not null default nextval('pages_id_seq'),
    url         varchar(4096) not null unique,
    title       varchar(2048),
    description text,
    ld_json     text,
    author      varchar(2048)
);

alter table pages
    add primary key (id);

create sequence ingredients_id_seq no maxvalue;
create table INGREDIENTS
(
    ID   BIGINT default NEXTVAL('ingredients_id_seq') not null primary key,
    NAME VARCHAR(1024) unique
);

create sequence recipes_id_seq no maxvalue;
create table RECIPES
(
    ID          BIGINT default NEXTVAL('recipes_id_seq') not null primary key,
    PAGE_ID     bigint                                   not null,
    NAME        VARCHAR(4096)                            NOT NULL,
    DESCRIPTION text,
    CONSTRAINT FK_RECIPES_PAGES FOREIGN KEY (PAGE_ID) REFERENCES pages
);

create sequence recipe_ingredients_id_seq no maxvalue;
create table recipe_ingredients
(
    ID            BIGINT default NEXTVAL('recipe_ingredients_id_seq') not null primary key,
    RECIPE_ID     BIGINT                                              NOT NULL,
    INGREDIENT_ID BIGINT                                              NOT NULL,
    AMOUNT        VARCHAR(100)                                        NOT NULL,
    CONSTRAINT FK_RECIPE_INGREDIENTS_RECIPES FOREIGN KEY (RECIPE_ID) REFERENCES recipes,
    CONSTRAINT FK_RECIPE_INGREDIENTS_INGREDIENTS FOREIGN KEY (INGREDIENT_ID) REFERENCES INGREDIENTS
)
