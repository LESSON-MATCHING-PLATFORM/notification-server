-- notification_setting_entity 생성

create table notification_setting_entity
(
    id                bigint       not null auto_increment,
    user_id           varchar(255) not null,
    notification_type varchar(255) not null,
    is_enabled        boolean      not null,
    primary key (id)
) engine=InnoDB;