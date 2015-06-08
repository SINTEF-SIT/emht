# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table AALCOMPONENT (
  COMPONENT_TYPE            varchar(31) not null,
  id                        bigint not null,
  patient_id                bigint,
  sensor_type               varchar(255),
  constraint pk_AALCOMPONENT primary key (id))
;

create table alarm (
  id                        bigint not null,
  type                      varchar(255),
  callee_id                 bigint,
  opening_time              timestamp,
  dispatching_time          timestamp,
  closing_time              timestamp,
  occurance_address         varchar(255),
  attendant_id              bigint,
  alarm_log                 clob,
  notes                     clob,
  patient_id                bigint,
  constraint pk_alarm primary key (id))
;

create table alarm_attendant (
  id                        bigint not null,
  username                  varchar(255),
  password                  varchar(255),
  constraint uq_alarm_attendant_username unique (username),
  constraint pk_alarm_attendant primary key (id))
;

create table callee (
  id                        bigint not null,
  name                      varchar(255),
  address                   varchar(255),
  phone_number              varchar(255),
  constraint uq_callee_phone_number unique (phone_number),
  constraint pk_callee primary key (id))
;

create table component_reading (
  id                        bigint not null,
  reading_type              varchar(255),
  date                      timestamp,
  value                     double,
  component_id              bigint,
  constraint pk_component_reading primary key (id))
;

create table patient (
  id                        bigint not null,
  name                      varchar(255),
  address                   varchar(255),
  age                       integer,
  phone_number              varchar(255),
  personal_number           varchar(255),
  obs                       varchar(255),
  constraint pk_patient primary key (id))
;

create sequence AALCOMPONENT_seq;

create sequence alarm_seq;

create sequence alarm_attendant_seq;

create sequence callee_seq;

create sequence component_reading_seq;

create sequence patient_seq;

alter table AALCOMPONENT add constraint fk_AALCOMPONENT_patient_1 foreign key (patient_id) references patient (id) on delete restrict on update restrict;
create index ix_AALCOMPONENT_patient_1 on AALCOMPONENT (patient_id);
alter table alarm add constraint fk_alarm_callee_2 foreign key (callee_id) references callee (id) on delete restrict on update restrict;
create index ix_alarm_callee_2 on alarm (callee_id);
alter table alarm add constraint fk_alarm_attendant_3 foreign key (attendant_id) references alarm_attendant (id) on delete restrict on update restrict;
create index ix_alarm_attendant_3 on alarm (attendant_id);
alter table alarm add constraint fk_alarm_patient_4 foreign key (patient_id) references patient (id) on delete restrict on update restrict;
create index ix_alarm_patient_4 on alarm (patient_id);
alter table component_reading add constraint fk_component_reading_component_5 foreign key (component_id) references AALCOMPONENT (id) on delete restrict on update restrict;
create index ix_component_reading_component_5 on component_reading (component_id);



# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

drop table if exists AALCOMPONENT;

drop table if exists alarm;

drop table if exists alarm_attendant;

drop table if exists callee;

drop table if exists component_reading;

drop table if exists patient;

SET REFERENTIAL_INTEGRITY TRUE;

drop sequence if exists AALCOMPONENT_seq;

drop sequence if exists alarm_seq;

drop sequence if exists alarm_attendant_seq;

drop sequence if exists callee_seq;

drop sequence if exists component_reading_seq;

drop sequence if exists patient_seq;

