# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table AALComponent (
  type                      varchar(31) not null,
  id                        bigint not null,
  patient_id                bigint,
  sensor_type               varchar(255),
  constraint pk_AALComponent primary key (id))
;

create table alarm (
  id                        bigint not null,
  type                      varchar(255),
  callee_id                 bigint,
  opening_time              timestamp,
  dispatching_time          timestamp,
  closing_time              timestamp,
  finished                  boolean,
  occurance_address         varchar(255),
  latitude                  double,
  longitude                 double,
  expired                   boolean,
  attendant_id              bigint,
  mobile_care_taker_id      bigint,
  assessment_id             bigint,
  field_assessment_id       bigint,
  notes                     clob,
  patient_id                bigint,
  constraint pk_alarm primary key (id))
;

create table alarm_attendant (
  id                        bigint not null,
  username                  varchar(255),
  password                  varchar(255) not null,
  role                      integer not null,
  gcm_reg_id                varchar(255),
  constraint uq_alarm_attendant_username unique (username),
  constraint uq_alarm_attendant_gcm_reg_id unique (gcm_reg_id),
  constraint pk_alarm_attendant primary key (id))
;

create table api_key (
  id                        bigint not null,
  user_id                   bigint,
  key                       varchar(255),
  constraint pk_api_key primary key (id))
;

create table assessment (
  id                        bigint not null,
  nmi_id                    bigint,
  sensors_checked           boolean,
  patient_information_checked boolean,
  constraint pk_assessment primary key (id))
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

create table field_operator_location (
  id                        bigint not null,
  field_operator_id         bigint,
  timestamp                 timestamp,
  latitude                  double,
  longitude                 double,
  constraint pk_field_operator_location primary key (id))
;

create table nmi (
  id                        bigint not null,
  conscious                 boolean,
  breathing                 boolean,
  movement                  boolean,
  standing                  boolean,
  talking                   boolean,
  constraint pk_nmi primary key (id))
;

create table patient (
  id                        bigint not null,
  name                      varchar(255),
  address                   varchar(255),
  latitude                  double,
  longitude                 double,
  age                       integer,
  phone_number              varchar(255),
  personal_number           varchar(255),
  obs                       varchar(255),
  constraint pk_patient primary key (id))
;

create sequence AALComponent_seq;

create sequence alarm_seq;

create sequence alarm_attendant_seq;

create sequence api_key_seq;

create sequence assessment_seq;

create sequence callee_seq;

create sequence component_reading_seq;

create sequence field_operator_location_seq;

create sequence nmi_seq;

create sequence patient_seq;

alter table AALComponent add constraint fk_AALComponent_patient_1 foreign key (patient_id) references patient (id) on delete restrict on update restrict;
create index ix_AALComponent_patient_1 on AALComponent (patient_id);
alter table alarm add constraint fk_alarm_callee_2 foreign key (callee_id) references callee (id) on delete restrict on update restrict;
create index ix_alarm_callee_2 on alarm (callee_id);
alter table alarm add constraint fk_alarm_attendant_3 foreign key (attendant_id) references alarm_attendant (id) on delete restrict on update restrict;
create index ix_alarm_attendant_3 on alarm (attendant_id);
alter table alarm add constraint fk_alarm_mobileCareTaker_4 foreign key (mobile_care_taker_id) references alarm_attendant (id) on delete restrict on update restrict;
create index ix_alarm_mobileCareTaker_4 on alarm (mobile_care_taker_id);
alter table alarm add constraint fk_alarm_assessment_5 foreign key (assessment_id) references assessment (id) on delete restrict on update restrict;
create index ix_alarm_assessment_5 on alarm (assessment_id);
alter table alarm add constraint fk_alarm_fieldAssessment_6 foreign key (field_assessment_id) references assessment (id) on delete restrict on update restrict;
create index ix_alarm_fieldAssessment_6 on alarm (field_assessment_id);
alter table alarm add constraint fk_alarm_patient_7 foreign key (patient_id) references patient (id) on delete restrict on update restrict;
create index ix_alarm_patient_7 on alarm (patient_id);
alter table api_key add constraint fk_api_key_user_8 foreign key (user_id) references alarm_attendant (id) on delete restrict on update restrict;
create index ix_api_key_user_8 on api_key (user_id);
alter table assessment add constraint fk_assessment_nmi_9 foreign key (nmi_id) references nmi (id) on delete restrict on update restrict;
create index ix_assessment_nmi_9 on assessment (nmi_id);
alter table component_reading add constraint fk_component_reading_componen_10 foreign key (component_id) references AALComponent (id) on delete restrict on update restrict;
create index ix_component_reading_componen_10 on component_reading (component_id);
alter table field_operator_location add constraint fk_field_operator_location_fi_11 foreign key (field_operator_id) references alarm_attendant (id) on delete restrict on update restrict;
create index ix_field_operator_location_fi_11 on field_operator_location (field_operator_id);



# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

drop table if exists AALComponent;

drop table if exists alarm;

drop table if exists alarm_attendant;

drop table if exists api_key;

drop table if exists assessment;

drop table if exists callee;

drop table if exists component_reading;

drop table if exists field_operator_location;

drop table if exists nmi;

drop table if exists patient;

SET REFERENTIAL_INTEGRITY TRUE;

drop sequence if exists AALComponent_seq;

drop sequence if exists alarm_seq;

drop sequence if exists alarm_attendant_seq;

drop sequence if exists api_key_seq;

drop sequence if exists assessment_seq;

drop sequence if exists callee_seq;

drop sequence if exists component_reading_seq;

drop sequence if exists field_operator_location_seq;

drop sequence if exists nmi_seq;

drop sequence if exists patient_seq;

