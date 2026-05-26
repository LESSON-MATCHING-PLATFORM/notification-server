create table event (
    id bigint not null auto_increment,
    received_at datetime(6),
    event_id varchar(255),
    payload TEXT,
    event_type enum ('PAYMENT','PAYMENT_BULK'),
    primary key (id)
) engine=InnoDB;

create table token_entity (
    id bigint not null auto_increment,
    token varchar(255),
    user_id varchar(255),
    primary key (id)
) engine=InnoDB;
