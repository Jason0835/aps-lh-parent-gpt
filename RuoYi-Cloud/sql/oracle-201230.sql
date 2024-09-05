----------------------------------------------------------------------
-- Export file for user RYCLOUD@192.168.100.250:1521/ORCL.168.0.176 --
-- Created by lbn on 2020/12/30, 14:54:48 ----------------------------
----------------------------------------------------------------------

set define off
spool oracle-201230.log

prompt
prompt Creating table GEN_TABLE
prompt ========================
prompt
create table GEN_TABLE
(
  table_id          NUMBER(20) not null,
  table_name        VARCHAR2(200) default '',
  table_comment     VARCHAR2(500) default '',
  class_name        VARCHAR2(100) default '',
  tpl_category      VARCHAR2(200) default 'crud',
  package_name      VARCHAR2(100),
  module_name       VARCHAR2(30),
  business_name     VARCHAR2(30),
  function_name     VARCHAR2(50),
  function_author   VARCHAR2(50),
  gen_type          CHAR(1) default '0',
  gen_path          VARCHAR2(200) default '/',
  options           VARCHAR2(1000),
  create_by         VARCHAR2(64) default '',
  create_time       DATE,
  update_by         VARCHAR2(64) default '',
  update_time       DATE,
  remark            VARCHAR2(500),
  sub_table_name    VARCHAR2(64),
  sub_table_fk_name VARCHAR2(64)
);
-- Add comments to the table 
comment on table GEN_TABLE
  is '代码生成业务表';
-- Add comments to the columns 
comment on column GEN_TABLE.table_id
  is '编号';
comment on column GEN_TABLE.table_name
  is '表名称';
comment on column GEN_TABLE.table_comment
  is '表描述';
comment on column GEN_TABLE.class_name
  is '实体类名称';
comment on column GEN_TABLE.tpl_category
  is '使用的模板（crud单表操作 tree树表操作）';
comment on column GEN_TABLE.package_name
  is '生成包路径';
comment on column GEN_TABLE.module_name
  is '生成模块名';
comment on column GEN_TABLE.business_name
  is '生成业务名';
comment on column GEN_TABLE.function_name
  is '生成功能名';
comment on column GEN_TABLE.function_author
  is '生成功能作者';
comment on column GEN_TABLE.gen_type
  is '生成代码方式（0zip压缩包 1自定义路径）';
comment on column GEN_TABLE.gen_path
  is '生成路径（不填默认项目路径）';
comment on column GEN_TABLE.options
  is '其它生成选项';
comment on column GEN_TABLE.create_by
  is '创建者';
comment on column GEN_TABLE.create_time
  is '创建时间';
comment on column GEN_TABLE.update_by
  is '更新者';
comment on column GEN_TABLE.update_time
  is '更新时间';
comment on column GEN_TABLE.remark
  is '备注';
comment on column GEN_TABLE.sub_table_name
  is '关联子表的表名';
comment on column GEN_TABLE.sub_table_fk_name
  is '子表关联的外键名';
-- Create/Recreate primary, unique and foreign key constraints 
alter table GEN_TABLE
  add constraint PK_GEN_TABLE primary key (TABLE_ID);
prompt
prompt Creating table GEN_TABLE_COLUMN
prompt ===============================
prompt
create table GEN_TABLE_COLUMN
(
  column_id      NUMBER(20) not null,
  table_id       VARCHAR2(64),
  column_name    VARCHAR2(200),
  column_comment VARCHAR2(500),
  column_type    VARCHAR2(100),
  java_type      VARCHAR2(500),
  java_field     VARCHAR2(200),
  is_pk          CHAR(1),
  is_increment   CHAR(1),
  is_required    CHAR(1),
  is_insert      CHAR(1),
  is_edit        CHAR(1),
  is_list        CHAR(1),
  is_query       CHAR(1),
  query_type     VARCHAR2(200) default 'EQ',
  html_type      VARCHAR2(200),
  dict_type      VARCHAR2(200) default '',
  sort           NUMBER(4),
  create_by      VARCHAR2(64) default '',
  create_time    DATE,
  update_by      VARCHAR2(64) default '',
  update_time    DATE
)
;
comment on table GEN_TABLE_COLUMN
  is '代码生成业务表字段';
comment on column GEN_TABLE_COLUMN.column_id
  is '编号';
comment on column GEN_TABLE_COLUMN.table_id
  is '归属表编号';
comment on column GEN_TABLE_COLUMN.column_name
  is '列名称';
comment on column GEN_TABLE_COLUMN.column_comment
  is '列描述';
comment on column GEN_TABLE_COLUMN.column_type
  is '列类型';
comment on column GEN_TABLE_COLUMN.java_type
  is 'JAVA类型';
comment on column GEN_TABLE_COLUMN.java_field
  is 'JAVA字段名';
comment on column GEN_TABLE_COLUMN.is_pk
  is '是否主键（1是）';
comment on column GEN_TABLE_COLUMN.is_increment
  is '是否自增（1是）';
comment on column GEN_TABLE_COLUMN.is_required
  is '是否必填（1是）';
comment on column GEN_TABLE_COLUMN.is_insert
  is '是否为插入字段（1是）';
comment on column GEN_TABLE_COLUMN.is_edit
  is '是否编辑字段（1是）';
comment on column GEN_TABLE_COLUMN.is_list
  is '是否列表字段（1是）';
comment on column GEN_TABLE_COLUMN.is_query
  is '是否查询字段（1是）';
comment on column GEN_TABLE_COLUMN.query_type
  is '查询方式（等于、不等于、大于、小于、范围）';
comment on column GEN_TABLE_COLUMN.html_type
  is '显示类型（文本框、文本域、下拉框、复选框、单选框、日期控件）';
comment on column GEN_TABLE_COLUMN.dict_type
  is '字典类型';
comment on column GEN_TABLE_COLUMN.sort
  is '排序';
comment on column GEN_TABLE_COLUMN.create_by
  is '创建者';
comment on column GEN_TABLE_COLUMN.create_time
  is '创建时间';
comment on column GEN_TABLE_COLUMN.update_by
  is '更新者';
comment on column GEN_TABLE_COLUMN.update_time
  is '更新时间';
alter table GEN_TABLE_COLUMN
  add constraint PK_GEN_TABLE_COLUMN primary key (COLUMN_ID);

prompt
prompt Creating table K_JOB_RECORD
prompt ===========================
prompt
create table K_JOB_RECORD
(
  id            NUMBER(11) not null,
  record_job_id NUMBER(11),
  start_time    DATE,
  stop_time     DATE,
  record_status NUMBER(11),
  log_file_path NVARCHAR2(100),
  add_time      DATE,
  add_user      NUMBER(11),
  edit_time     DATE,
  edit_user     NUMBER(11),
  del_flag      NUMBER(11)
)
;
comment on table K_JOB_RECORD
  is '作业执行记录表';
comment on column K_JOB_RECORD.id
  is '作业记录ID';
comment on column K_JOB_RECORD.record_job_id
  is '作业ID';
comment on column K_JOB_RECORD.start_time
  is '启动时间';
comment on column K_JOB_RECORD.stop_time
  is '停止时间';
comment on column K_JOB_RECORD.record_status
  is '任务执行结果（1：成功；0：失败）';
comment on column K_JOB_RECORD.log_file_path
  is '作业日志记录文件保存位置';
comment on column K_JOB_RECORD.add_time
  is '添加时间';
comment on column K_JOB_RECORD.add_user
  is '添加者';
comment on column K_JOB_RECORD.edit_time
  is '编辑时间';
comment on column K_JOB_RECORD.edit_user
  is '编辑者';
comment on column K_JOB_RECORD.del_flag
  is '是否删除（0：存在；1：删除）';
alter table K_JOB_RECORD
  add primary key (ID);

prompt
prompt Creating table K_TASK_INFO
prompt ==========================
prompt
create table K_TASK_INFO
(
  id           NUMBER(11) not null,
  category_id  NUMBER(11),
  task_name    VARCHAR2(100),
  task_type    CHAR(1),
  remote_url   VARCHAR2(1000),
  del_flag     CHAR(1) default '0',
  create_by    VARCHAR2(64) default '',
  create_time  DATE default SYSDATE,
  update_by    VARCHAR2(64) default '',
  update_time  DATE default SYSDATE,
  remark       VARCHAR2(500) default '',
  last_runtime DATE,
  record_count NUMBER(20)
)
;
comment on table K_TASK_INFO
  is 'KETTLE任务信息';
comment on column K_TASK_INFO.category_id
  is '分类ID，筛选本系统的key';
comment on column K_TASK_INFO.task_name
  is '任务名称';
comment on column K_TASK_INFO.task_type
  is '0JOB,1TRANS';
comment on column K_TASK_INFO.remote_url
  is '远程服务调用地址';
comment on column K_TASK_INFO.del_flag
  is '删除标志';
comment on column K_TASK_INFO.create_by
  is '创建者';
comment on column K_TASK_INFO.create_time
  is '创建时间';
comment on column K_TASK_INFO.update_by
  is '更新者';
comment on column K_TASK_INFO.update_time
  is '更新时间';
comment on column K_TASK_INFO.remark
  is '备注';
comment on column K_TASK_INFO.last_runtime
  is '最后执行时间-待开发';
comment on column K_TASK_INFO.record_count
  is '执行记录数-待开发';
alter table K_TASK_INFO
  add constraint K_K_TASK_INFO_ID primary key (ID);

prompt
prompt Creating table K_TRANS_RECORD
prompt =============================
prompt
create table K_TRANS_RECORD
(
  id              NUMBER(11) not null,
  record_trans_id NUMBER(11),
  start_time      DATE,
  stop_time       DATE,
  record_status   NUMBER(11),
  log_file_path   NVARCHAR2(100),
  add_time        DATE,
  add_user        NUMBER(11),
  edit_time       DATE,
  edit_user       NUMBER(11),
  del_flag        NUMBER(11),
  category_id     NUMBER(11)
)
;
comment on table K_TRANS_RECORD
  is '转换执行记录表';
comment on column K_TRANS_RECORD.id
  is '转换记录ID';
comment on column K_TRANS_RECORD.record_trans_id
  is '转换ID';
comment on column K_TRANS_RECORD.start_time
  is '启动时间';
comment on column K_TRANS_RECORD.stop_time
  is '停止时间';
comment on column K_TRANS_RECORD.record_status
  is '任务执行结果（1：成功；0：失败）';
comment on column K_TRANS_RECORD.log_file_path
  is '转换日志记录文件保存位置';
comment on column K_TRANS_RECORD.add_time
  is '添加时间';
comment on column K_TRANS_RECORD.add_user
  is '添加者';
comment on column K_TRANS_RECORD.edit_time
  is '编辑时间';
comment on column K_TRANS_RECORD.edit_user
  is '编辑者';
comment on column K_TRANS_RECORD.del_flag
  is '是否删除（0：存在；1：删除）';
alter table K_TRANS_RECORD
  add primary key (ID);

prompt
prompt Creating table MDM_SYSTEM_DATA
prompt ==============================
prompt
create table MDM_SYSTEM_DATA
(
  create_by   VARCHAR2(64) default '',
  create_time DATE default sysdate,
  update_by   VARCHAR2(64) default '',
  update_time DATE default sysdate,
  system_id   VARCHAR2(64) not null,
  del_flag    CHAR(1) default '0',
  status      CHAR(1) default '0',
  system_code VARCHAR2(64) default '' not null,
  lang_json   VARCHAR2(1000) default '[{"zh_CN":""},{"en_US":""}]',
  order_num   NUMBER(4) default 0,
  parent_id   NUMBER(20) default 0,
  url         VARCHAR2(1000) default '',
  icon        VARCHAR2(64) default '',
  target      VARCHAR2(64) default '#'
)
;
comment on table MDM_SYSTEM_DATA
  is '用于分配的系统清单';
comment on column MDM_SYSTEM_DATA.create_by
  is '创建者';
comment on column MDM_SYSTEM_DATA.create_time
  is '创建时间';
comment on column MDM_SYSTEM_DATA.update_by
  is '更新者';
comment on column MDM_SYSTEM_DATA.update_time
  is '更新时间';
comment on column MDM_SYSTEM_DATA.system_id
  is '系统记录ID';
comment on column MDM_SYSTEM_DATA.del_flag
  is '删除标志（0代表存在 2代表删除）';
comment on column MDM_SYSTEM_DATA.status
  is '系统状态（0正常 1停用）';
comment on column MDM_SYSTEM_DATA.system_code
  is '系统代码';
comment on column MDM_SYSTEM_DATA.lang_json
  is '语言包';
comment on column MDM_SYSTEM_DATA.order_num
  is '显示顺序';
comment on column MDM_SYSTEM_DATA.parent_id
  is '父级系统显示';
comment on column MDM_SYSTEM_DATA.url
  is '系统首页URL';
comment on column MDM_SYSTEM_DATA.icon
  is '系统图标';
comment on column MDM_SYSTEM_DATA.target
  is '弹出新页面或跳转';

prompt
prompt Creating table MDM_SYS_AUTH
prompt ===========================
prompt
create table MDM_SYS_AUTH
(
  create_by   VARCHAR2(64) default '',
  create_time DATE default sysdate,
  update_by   VARCHAR2(64) default '',
  update_time DATE default sysdate,
  user_id     NUMBER(20) not null,
  system_id   NUMBER(20) not null
)
;
comment on table MDM_SYS_AUTH
  is '用户可以访问的系统ID';
comment on column MDM_SYS_AUTH.create_by
  is '创建者';
comment on column MDM_SYS_AUTH.create_time
  is '创建时间';
comment on column MDM_SYS_AUTH.update_by
  is '更新者';
comment on column MDM_SYS_AUTH.update_time
  is '更新时间';
comment on column MDM_SYS_AUTH.user_id
  is '用户ID';
comment on column MDM_SYS_AUTH.system_id
  is '系统ID';
create index IDX_MDM_SYS_AUTH_USER_ID on MDM_SYS_AUTH (USER_ID);

prompt
prompt Creating table QRTZ_BLOB_TRIGGERS
prompt =================================
prompt
create table QRTZ_BLOB_TRIGGERS
(
  sched_name    VARCHAR2(120) not null,
  trigger_name  VARCHAR2(200) not null,
  trigger_group VARCHAR2(200) not null,
  blob_data     BLOB
)
;
alter table QRTZ_BLOB_TRIGGERS
  add constraint PK_QRTZ_BLOB_TRIGGERS primary key (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP);

prompt
prompt Creating table QRTZ_CALENDARS
prompt =============================
prompt
create table QRTZ_CALENDARS
(
  sched_name    VARCHAR2(120) not null,
  calendar_name VARCHAR2(200) not null,
  calendar      BLOB
)
;
alter table QRTZ_CALENDARS
  add constraint PK_QRTZ_CALENDARS primary key (SCHED_NAME, CALENDAR_NAME);

prompt
prompt Creating table QRTZ_CRON_TRIGGERS
prompt =================================
prompt
create table QRTZ_CRON_TRIGGERS
(
  sched_name      VARCHAR2(120) not null,
  trigger_name    VARCHAR2(200) not null,
  trigger_group   VARCHAR2(200) not null,
  cron_expression VARCHAR2(200),
  time_zone_id    VARCHAR2(80)
)
;
alter table QRTZ_CRON_TRIGGERS
  add constraint PK_QRTZ_CRON_TRIGGERS primary key (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP);

prompt
prompt Creating table QRTZ_FIRED_TRIGGERS
prompt ==================================
prompt
create table QRTZ_FIRED_TRIGGERS
(
  sched_name        VARCHAR2(120) not null,
  entry_id          VARCHAR2(95) not null,
  trigger_name      VARCHAR2(200),
  trigger_group     VARCHAR2(200),
  instance_name     VARCHAR2(200),
  fired_time        NUMBER(20),
  sched_time        NUMBER(20),
  priority          NUMBER(11),
  state             VARCHAR2(16),
  job_name          VARCHAR2(200),
  job_group         VARCHAR2(200),
  is_nonconcurrent  VARCHAR2(1),
  requests_recovery VARCHAR2(1)
)
;
alter table QRTZ_FIRED_TRIGGERS
  add constraint PK_QRTZ_FIRED_TRIGGERS primary key (SCHED_NAME, ENTRY_ID);

prompt
prompt Creating table QRTZ_JOB_DETAILS
prompt ===============================
prompt
create table QRTZ_JOB_DETAILS
(
  sched_name        VARCHAR2(120) not null,
  job_name          VARCHAR2(200) not null,
  job_group         VARCHAR2(200) not null,
  description       VARCHAR2(250),
  job_class_name    VARCHAR2(250),
  is_durable        VARCHAR2(1),
  is_nonconcurrent  VARCHAR2(1),
  is_update_data    VARCHAR2(1),
  requests_recovery VARCHAR2(1),
  job_data          BLOB
)
;
alter table QRTZ_JOB_DETAILS
  add constraint PK_QRTZ_JOB_DETAILS primary key (SCHED_NAME, JOB_NAME, JOB_GROUP);

prompt
prompt Creating table QRTZ_LOCKS
prompt =========================
prompt
create table QRTZ_LOCKS
(
  sched_name VARCHAR2(120) not null,
  lock_name  VARCHAR2(40) not null
)
;
alter table QRTZ_LOCKS
  add constraint PK_QRTZ_LOCKS primary key (SCHED_NAME, LOCK_NAME);

prompt
prompt Creating table QRTZ_PAUSED_TRIGGER_GRPS
prompt =======================================
prompt
create table QRTZ_PAUSED_TRIGGER_GRPS
(
  sched_name    VARCHAR2(120) not null,
  trigger_group VARCHAR2(200) not null
)
;
alter table QRTZ_PAUSED_TRIGGER_GRPS
  add constraint PK_QRTZ_PAUSED_TRIGGER_GRPS primary key (SCHED_NAME, TRIGGER_GROUP);

prompt
prompt Creating table QRTZ_SCHEDULER_STATE
prompt ===================================
prompt
create table QRTZ_SCHEDULER_STATE
(
  sched_name        VARCHAR2(120) not null,
  instance_name     VARCHAR2(200) not null,
  last_checkin_time NUMBER(20),
  checkin_interval  NUMBER(20)
)
;
alter table QRTZ_SCHEDULER_STATE
  add constraint PK_QRTZ_SCHEDULER_STATE primary key (SCHED_NAME, INSTANCE_NAME);

prompt
prompt Creating table QRTZ_SIMPLE_TRIGGERS
prompt ===================================
prompt
create table QRTZ_SIMPLE_TRIGGERS
(
  sched_name      VARCHAR2(120) not null,
  trigger_name    VARCHAR2(200) not null,
  trigger_group   VARCHAR2(200) not null,
  repeat_count    NUMBER(20),
  repeat_interval NUMBER(20),
  times_triggered NUMBER(20)
)
;
alter table QRTZ_SIMPLE_TRIGGERS
  add constraint PK_QRTZ_SIMPLE_TRIGGERS primary key (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP);

prompt
prompt Creating table QRTZ_SIMPROP_TRIGGERS
prompt ====================================
prompt
create table QRTZ_SIMPROP_TRIGGERS
(
  sched_name    VARCHAR2(120) not null,
  trigger_name  VARCHAR2(200) not null,
  trigger_group VARCHAR2(200) not null,
  str_prop_1    VARCHAR2(512),
  str_prop_2    VARCHAR2(512),
  str_prop_3    VARCHAR2(512),
  int_prop_1    NUMBER(11),
  int_prop_2    NUMBER(11),
  long_prop_1   NUMBER(20),
  long_prop_2   NUMBER(20),
  dec_prop_1    NUMBER,
  dec_prop_2    NUMBER,
  bool_prop_1   VARCHAR2(1),
  bool_prop_2   VARCHAR2(1)
)
;
alter table QRTZ_SIMPROP_TRIGGERS
  add constraint PK_QRTZ_SIMPROP_TRIGGERS primary key (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP);

prompt
prompt Creating table QRTZ_TRIGGERS
prompt ============================
prompt
create table QRTZ_TRIGGERS
(
  sched_name     VARCHAR2(120) not null,
  trigger_name   VARCHAR2(200) not null,
  trigger_group  VARCHAR2(200) not null,
  job_name       VARCHAR2(200),
  job_group      VARCHAR2(200),
  description    VARCHAR2(250),
  next_fire_time NUMBER(20),
  prev_fire_time NUMBER(20),
  priority       NUMBER(11),
  trigger_state  VARCHAR2(16),
  trigger_type   VARCHAR2(8),
  start_time     NUMBER(20),
  end_time       NUMBER(20),
  calendar_name  VARCHAR2(200),
  misfire_instr  NUMBER(6),
  job_data       BLOB
)
;
alter table QRTZ_TRIGGERS
  add constraint PK_QRTZ_TRIGGERS primary key (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP);

prompt
prompt Creating table SYS_CONFIG
prompt =========================
prompt
create table SYS_CONFIG
(
  config_id    NUMBER(20) not null,
  config_name  VARCHAR2(100) default '',
  config_key   VARCHAR2(100) default '',
  config_value VARCHAR2(100) default '',
  config_type  CHAR(1) default 'N',
  create_by    VARCHAR2(64) default '',
  create_time  DATE,
  update_by    VARCHAR2(64) default '',
  update_time  DATE,
  remark       VARCHAR2(500)
)
;
comment on table SYS_CONFIG
  is '参数配置表';
comment on column SYS_CONFIG.config_id
  is '参数主键';
comment on column SYS_CONFIG.config_name
  is '参数名称';
comment on column SYS_CONFIG.config_key
  is '参数键名';
comment on column SYS_CONFIG.config_value
  is '参数键值';
comment on column SYS_CONFIG.config_type
  is '系统内置（Y是 N否）';
comment on column SYS_CONFIG.create_by
  is '创建者';
comment on column SYS_CONFIG.create_time
  is '创建时间';
comment on column SYS_CONFIG.update_by
  is '更新者';
comment on column SYS_CONFIG.update_time
  is '更新时间';
comment on column SYS_CONFIG.remark
  is '备注';
create index IDX_CONFIG_CONFIG_KEY on SYS_CONFIG (CONFIG_KEY);
alter table SYS_CONFIG
  add constraint PK_SYS_CONFIG primary key (CONFIG_ID);

prompt
prompt Creating table SYS_DEPT
prompt =======================
prompt
create table SYS_DEPT
(
  dept_id     NUMBER(20) not null,
  parent_id   NUMBER(20) default 0,
  ancestors   VARCHAR2(50) default '',
  dept_name   VARCHAR2(30) default '',
  order_num   NUMBER(4) default 0,
  leader      VARCHAR2(220),
  phone       VARCHAR2(11),
  email       VARCHAR2(50),
  status      CHAR(1) default '0',
  del_flag    CHAR(1) default '0',
  create_by   VARCHAR2(64) default '',
  create_time DATE,
  update_by   VARCHAR2(64) default '',
  update_time DATE,
  lang_json   VARCHAR2(1000) default '[]',
  dept_code   VARCHAR2(64) default '',
  is_factory  CHAR(1) default '0'
)
;
comment on table SYS_DEPT
  is '部门表';
comment on column SYS_DEPT.dept_id
  is '部门id';
comment on column SYS_DEPT.parent_id
  is '父部门id';
comment on column SYS_DEPT.ancestors
  is '祖级列表';
comment on column SYS_DEPT.dept_name
  is '部门名称';
comment on column SYS_DEPT.order_num
  is '显示顺序';
comment on column SYS_DEPT.leader
  is '负责人';
comment on column SYS_DEPT.phone
  is '联系电话';
comment on column SYS_DEPT.email
  is '邮箱';
comment on column SYS_DEPT.status
  is '部门状态（0正常 1停用）';
comment on column SYS_DEPT.del_flag
  is '删除标志（0代表存在 2代表删除）';
comment on column SYS_DEPT.create_by
  is '创建者';
comment on column SYS_DEPT.create_time
  is '创建时间';
comment on column SYS_DEPT.update_by
  is '更新者';
comment on column SYS_DEPT.update_time
  is '更新时间';
comment on column SYS_DEPT.lang_json
  is '语言包';
comment on column SYS_DEPT.dept_code
  is '部门代码';
comment on column SYS_DEPT.is_factory
  is '是否工厂';
alter table SYS_DEPT
  add constraint PK_SYS_DEPT primary key (DEPT_ID);

prompt
prompt Creating table SYS_DICT_DATA
prompt ============================
prompt
create table SYS_DICT_DATA
(
  dict_code   NUMBER(20) not null,
  dict_sort   NUMBER(4) default 0,
  dict_label  VARCHAR2(100) default '',
  dict_value  VARCHAR2(100) default '',
  dict_type   VARCHAR2(100) default '',
  css_class   VARCHAR2(100),
  list_class  VARCHAR2(100),
  is_default  CHAR(1) default 'N',
  status      CHAR(1) default '0',
  create_by   VARCHAR2(64) default '',
  create_time DATE,
  update_by   VARCHAR2(64) default '',
  update_time DATE,
  remark      VARCHAR2(500),
  locale      VARCHAR2(10) default 'zh_CN'
)
;
comment on table SYS_DICT_DATA
  is '字典数据表';
comment on column SYS_DICT_DATA.dict_code
  is '字典编码';
comment on column SYS_DICT_DATA.dict_sort
  is '字典排序';
comment on column SYS_DICT_DATA.dict_label
  is '字典标签';
comment on column SYS_DICT_DATA.dict_value
  is '字典键值';
comment on column SYS_DICT_DATA.dict_type
  is '字典类型';
comment on column SYS_DICT_DATA.css_class
  is '样式属性（其他样式扩展）';
comment on column SYS_DICT_DATA.list_class
  is '表格回显样式';
comment on column SYS_DICT_DATA.is_default
  is '是否默认（Y是 N否）';
comment on column SYS_DICT_DATA.status
  is '状态（0正常 1停用）';
comment on column SYS_DICT_DATA.create_by
  is '创建者';
comment on column SYS_DICT_DATA.create_time
  is '创建时间';
comment on column SYS_DICT_DATA.update_by
  is '更新者';
comment on column SYS_DICT_DATA.update_time
  is '更新时间';
comment on column SYS_DICT_DATA.remark
  is '备注';
comment on column SYS_DICT_DATA.locale
  is '语言包';
create index IDEX_DICT_DATA_LOCALE on SYS_DICT_DATA (LOCALE);
create index IDX_DICT_DATA_DICT_TYPE on SYS_DICT_DATA (DICT_TYPE)
  nologging;
alter table SYS_DICT_DATA
  add constraint PK_SYS_DICT_DATA primary key (DICT_CODE);
alter index PK_SYS_DICT_DATA nologging;

prompt
prompt Creating table SYS_DICT_TYPE
prompt ============================
prompt
create table SYS_DICT_TYPE
(
  dict_id     NUMBER(20) not null,
  dict_name   VARCHAR2(100) default '',
  dict_type   VARCHAR2(100) default '',
  status      CHAR(1) default '0',
  create_by   VARCHAR2(64) default '',
  create_time DATE,
  update_by   VARCHAR2(64) default '',
  update_time DATE,
  remark      VARCHAR2(500)
)
;
comment on table SYS_DICT_TYPE
  is '字典类型表';
comment on column SYS_DICT_TYPE.dict_id
  is '字典主键';
comment on column SYS_DICT_TYPE.dict_name
  is '字典名称';
comment on column SYS_DICT_TYPE.dict_type
  is '字典类型';
comment on column SYS_DICT_TYPE.status
  is '状态（0正常 1停用）';
comment on column SYS_DICT_TYPE.create_by
  is '创建者';
comment on column SYS_DICT_TYPE.create_time
  is '创建时间';
comment on column SYS_DICT_TYPE.update_by
  is '更新者';
comment on column SYS_DICT_TYPE.update_time
  is '更新时间';
comment on column SYS_DICT_TYPE.remark
  is '备注';
create unique index SYS_DICT_TYPE_INDEX1 on SYS_DICT_TYPE (DICT_TYPE);
alter table SYS_DICT_TYPE
  add constraint PK_SYS_DICT_TYPE primary key (DICT_ID);

prompt
prompt Creating table SYS_JOB
prompt ======================
prompt
create table SYS_JOB
(
  job_id          NUMBER(20) not null,
  job_name        VARCHAR2(64) default '' not null,
  job_group       VARCHAR2(64) default '' not null,
  invoke_target   VARCHAR2(500) not null,
  cron_expression VARCHAR2(255) default '',
  misfire_policy  VARCHAR2(20) default '3',
  concurrent      CHAR(1) default '1',
  status          CHAR(1) default '0',
  create_by       VARCHAR2(64) default '',
  create_time     DATE,
  update_by       VARCHAR2(64) default '',
  update_time     DATE,
  remark          VARCHAR2(500) default ''
)
;
comment on table SYS_JOB
  is '定时任务调度表';
comment on column SYS_JOB.job_id
  is '任务ID';
comment on column SYS_JOB.job_name
  is '任务名称';
comment on column SYS_JOB.job_group
  is '任务组名';
comment on column SYS_JOB.invoke_target
  is '调用目标字符串';
comment on column SYS_JOB.cron_expression
  is 'cron执行表达式';
comment on column SYS_JOB.misfire_policy
  is '计划执行错误策略（1立即执行 2执行一次 3放弃执行）';
comment on column SYS_JOB.concurrent
  is '是否并发执行（0允许 1禁止）';
comment on column SYS_JOB.status
  is '状态（0正常 1暂停）';
comment on column SYS_JOB.create_by
  is '创建者';
comment on column SYS_JOB.create_time
  is '创建时间';
comment on column SYS_JOB.update_by
  is '更新者';
comment on column SYS_JOB.update_time
  is '更新时间';
comment on column SYS_JOB.remark
  is '备注信息';
alter table SYS_JOB
  add constraint PK_SYS_JOB primary key (JOB_ID, JOB_NAME, JOB_GROUP);

prompt
prompt Creating table SYS_JOB_LOG
prompt ==========================
prompt
create table SYS_JOB_LOG
(
  job_log_id     NUMBER(20) not null,
  job_name       VARCHAR2(64) not null,
  job_group      VARCHAR2(64) not null,
  invoke_target  VARCHAR2(500) not null,
  job_message    VARCHAR2(500),
  status         CHAR(1) default '0',
  exception_info VARCHAR2(2000) default '',
  create_time    DATE
)
;
comment on table SYS_JOB_LOG
  is '定时任务调度日志表';
comment on column SYS_JOB_LOG.job_log_id
  is '任务日志ID';
comment on column SYS_JOB_LOG.job_name
  is '任务名称';
comment on column SYS_JOB_LOG.job_group
  is '任务组名';
comment on column SYS_JOB_LOG.invoke_target
  is '调用目标字符串';
comment on column SYS_JOB_LOG.job_message
  is '日志信息';
comment on column SYS_JOB_LOG.status
  is '执行状态（0正常 1失败）';
comment on column SYS_JOB_LOG.exception_info
  is '异常信息';
comment on column SYS_JOB_LOG.create_time
  is '创建时间';
alter table SYS_JOB_LOG
  add constraint PK_SYS_JOB_LOG primary key (JOB_LOG_ID);

prompt
prompt Creating table SYS_LOGININFOR
prompt =============================
prompt
create table SYS_LOGININFOR
(
  info_id     NUMBER(20) not null,
  user_name   VARCHAR2(50) default '',
  ipaddr      VARCHAR2(50) default '',
  status      CHAR(1) default '0',
  msg         VARCHAR2(255) default '',
  access_time DATE
)
;
comment on table SYS_LOGININFOR
  is '系统访问记录';
comment on column SYS_LOGININFOR.info_id
  is '访问ID';
comment on column SYS_LOGININFOR.user_name
  is '用户账号';
comment on column SYS_LOGININFOR.ipaddr
  is '登录IP地址';
comment on column SYS_LOGININFOR.status
  is '登录状态（0成功 1失败）';
comment on column SYS_LOGININFOR.msg
  is '提示信息';
comment on column SYS_LOGININFOR.access_time
  is '访问时间';
alter table SYS_LOGININFOR
  add constraint PK_SYS_LOGININFOR primary key (INFO_ID);

prompt
prompt Creating table SYS_MENU
prompt =======================
prompt
create table SYS_MENU
(
  menu_id     NUMBER(20) not null,
  menu_name   VARCHAR2(50) not null,
  parent_id   NUMBER(20) default 0,
  order_num   NUMBER(4) default 0,
  target      VARCHAR2(255) default '',
  path        VARCHAR2(255) default '',
  bt_url      VARCHAR2(255) default '#',
  component   VARCHAR2(255),
  is_frame    NUMBER(1) default 1,
  is_cache    NUMBER(1) default 0,
  menu_type   CHAR(1) default '',
  visible     CHAR(1) default 0,
  status      CHAR(1) default 0,
  perms       VARCHAR2(100),
  bt_perms    VARCHAR2(100),
  icon        VARCHAR2(100) default '#',
  bt_icon     VARCHAR2(100) default '#',
  create_by   VARCHAR2(64) default '',
  create_time DATE,
  update_by   VARCHAR2(64) default '',
  update_time DATE,
  remark      VARCHAR2(500) default '',
  lang_json   VARCHAR2(1000) default '[]'
)
;
comment on table SYS_MENU
  is '菜单权限表';
comment on column SYS_MENU.menu_id
  is '菜单ID';
comment on column SYS_MENU.menu_name
  is '菜单名称';
comment on column SYS_MENU.parent_id
  is '父菜单ID';
comment on column SYS_MENU.order_num
  is '显示顺序';
comment on column SYS_MENU.target
  is '新窗口的URL';
comment on column SYS_MENU.path
  is '路由地址';
comment on column SYS_MENU.bt_url
  is 'BootUI 的菜单URL';
comment on column SYS_MENU.component
  is '组件路径';
comment on column SYS_MENU.is_frame
  is '是否为外链（0是 1否）';
comment on column SYS_MENU.is_cache
  is '是否缓存（0缓存 1不缓存）';
comment on column SYS_MENU.menu_type
  is '菜单类型（M目录 C菜单 F按钮）';
comment on column SYS_MENU.visible
  is '菜单状态（0显示 1隐藏）';
comment on column SYS_MENU.status
  is '菜单状态（0正常 1停用）';
comment on column SYS_MENU.perms
  is '权限标识';
comment on column SYS_MENU.bt_perms
  is 'bt权限标识';
comment on column SYS_MENU.icon
  is '菜单图标';
comment on column SYS_MENU.bt_icon
  is 'bt菜单图标';
comment on column SYS_MENU.create_by
  is '创建者';
comment on column SYS_MENU.create_time
  is '创建时间';
comment on column SYS_MENU.update_by
  is '更新者';
comment on column SYS_MENU.update_time
  is '更新时间';
comment on column SYS_MENU.remark
  is '备注';
comment on column SYS_MENU.lang_json
  is '语言包的json';
create index IDX_MENU_MENU_TYPE on SYS_MENU (MENU_TYPE);
alter table SYS_MENU
  add constraint PK_SYS_MENU primary key (MENU_ID);

prompt
prompt Creating table SYS_NOTICE
prompt =========================
prompt
create table SYS_NOTICE
(
  notice_id      NUMBER(20) not null,
  notice_title   VARCHAR2(500) not null,
  notice_type    CHAR(1) not null,
  notice_content VARCHAR2(2000),
  status         CHAR(1) default '0',
  create_by      VARCHAR2(64) default '',
  create_time    DATE,
  update_by      VARCHAR2(64) default '',
  update_time    DATE,
  remark         VARCHAR2(255)
)
;
comment on table SYS_NOTICE
  is '通知公告表';
comment on column SYS_NOTICE.notice_id
  is '公告ID';
comment on column SYS_NOTICE.notice_title
  is '公告标题';
comment on column SYS_NOTICE.notice_type
  is '公告类型（1通知 2公告）';
comment on column SYS_NOTICE.notice_content
  is '公告内容';
comment on column SYS_NOTICE.status
  is '公告状态（0正常 1关闭）';
comment on column SYS_NOTICE.create_by
  is '创建者';
comment on column SYS_NOTICE.create_time
  is '创建时间';
comment on column SYS_NOTICE.update_by
  is '更新者';
comment on column SYS_NOTICE.update_time
  is '更新时间';
comment on column SYS_NOTICE.remark
  is '备注';
alter table SYS_NOTICE
  add constraint PK_SYS_NOTICE primary key (NOTICE_ID);

prompt
prompt Creating table SYS_OPER_LOG
prompt ===========================
prompt
create table SYS_OPER_LOG
(
  oper_id        NUMBER(20) not null,
  title          VARCHAR2(50) default '',
  business_type  NUMBER(2) default 0,
  method         VARCHAR2(100) default '',
  request_method VARCHAR2(10) default '',
  operator_type  NUMBER(1) default 0,
  oper_name      VARCHAR2(50) default '',
  dept_name      VARCHAR2(50) default '',
  oper_url       VARCHAR2(255) default '',
  oper_ip        VARCHAR2(50) default '',
  oper_location  VARCHAR2(255) default '',
  oper_param     VARCHAR2(2000) default '',
  json_result    VARCHAR2(2000) default '',
  status         NUMBER(1) default 0,
  error_msg      VARCHAR2(2000) default '',
  oper_time      DATE
)
;
comment on table SYS_OPER_LOG
  is '操作日志记录';
comment on column SYS_OPER_LOG.oper_id
  is '日志主键';
comment on column SYS_OPER_LOG.title
  is '模块标题';
comment on column SYS_OPER_LOG.business_type
  is '业务类型（0其它 1新增 2修改 3删除）';
comment on column SYS_OPER_LOG.method
  is '方法名称';
comment on column SYS_OPER_LOG.request_method
  is '请求方式';
comment on column SYS_OPER_LOG.operator_type
  is '操作类别（0其它 1后台用户 2手机端用户）';
comment on column SYS_OPER_LOG.oper_name
  is '操作人员';
comment on column SYS_OPER_LOG.dept_name
  is '部门名称';
comment on column SYS_OPER_LOG.oper_url
  is '请求URL';
comment on column SYS_OPER_LOG.oper_ip
  is '主机地址';
comment on column SYS_OPER_LOG.oper_location
  is '操作地点';
comment on column SYS_OPER_LOG.oper_param
  is '请求参数';
comment on column SYS_OPER_LOG.json_result
  is '返回参数';
comment on column SYS_OPER_LOG.status
  is '操作状态（0正常 1异常）';
comment on column SYS_OPER_LOG.error_msg
  is '错误消息';
comment on column SYS_OPER_LOG.oper_time
  is '操作时间';
alter table SYS_OPER_LOG
  add constraint PK_SYS_OPER_LOG primary key (OPER_ID);

prompt
prompt Creating table SYS_POST
prompt =======================
prompt
create table SYS_POST
(
  post_id     NUMBER(20) not null,
  post_code   VARCHAR2(64) not null,
  post_name   VARCHAR2(50) not null,
  post_sort   NUMBER(4) not null,
  status      CHAR(1) not null,
  create_by   VARCHAR2(64) default '',
  create_time DATE,
  update_by   VARCHAR2(64) default '',
  update_time DATE,
  remark      VARCHAR2(500)
)
;
comment on table SYS_POST
  is '岗位信息表';
comment on column SYS_POST.post_id
  is '岗位ID';
comment on column SYS_POST.post_code
  is '岗位编码';
comment on column SYS_POST.post_name
  is '岗位名称';
comment on column SYS_POST.post_sort
  is '显示顺序';
comment on column SYS_POST.status
  is '状态（0正常 1停用）';
comment on column SYS_POST.create_by
  is '创建者';
comment on column SYS_POST.create_time
  is '创建时间';
comment on column SYS_POST.update_by
  is '更新者';
comment on column SYS_POST.update_time
  is '更新时间';
comment on column SYS_POST.remark
  is '备注';
alter table SYS_POST
  add constraint PK_SYS_POST primary key (POST_ID);

prompt
prompt Creating table SYS_ROLE
prompt =======================
prompt
create table SYS_ROLE
(
  role_id             NUMBER(20) not null,
  role_name           VARCHAR2(30) not null,
  role_key            VARCHAR2(100) not null,
  role_sort           NUMBER(4) not null,
  data_scope          CHAR(1) default '1',
  menu_check_strictly NUMBER(4) default '1',
  dept_check_strictly NUMBER(4) default '1',
  status              CHAR(1) not null,
  del_flag            CHAR(1) default '0',
  create_by           VARCHAR2(64) default '',
  create_time         DATE,
  update_by           VARCHAR2(64) default '',
  update_time         DATE,
  remark              VARCHAR2(500)
)
;
comment on table SYS_ROLE
  is '角色信息表';
comment on column SYS_ROLE.role_id
  is '角色ID';
comment on column SYS_ROLE.role_name
  is '角色名称';
comment on column SYS_ROLE.role_key
  is '角色权限字符串';
comment on column SYS_ROLE.role_sort
  is '显示顺序';
comment on column SYS_ROLE.data_scope
  is '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限）';
comment on column SYS_ROLE.menu_check_strictly
  is '菜单树选择项是否关联显示';
comment on column SYS_ROLE.dept_check_strictly
  is '部门树选择项是否关联显示';
comment on column SYS_ROLE.status
  is '角色状态（0正常 1停用）';
comment on column SYS_ROLE.del_flag
  is '删除标志（0代表存在 2代表删除）';
comment on column SYS_ROLE.create_by
  is '创建者';
comment on column SYS_ROLE.create_time
  is '创建时间';
comment on column SYS_ROLE.update_by
  is '更新者';
comment on column SYS_ROLE.update_time
  is '更新时间';
comment on column SYS_ROLE.remark
  is '备注';
alter table SYS_ROLE
  add constraint PK_SYS_ROLE primary key (ROLE_ID);

prompt
prompt Creating table SYS_ROLE_DEPT
prompt ============================
prompt
create table SYS_ROLE_DEPT
(
  role_id NUMBER(20) not null,
  dept_id NUMBER(20) not null
)
;
comment on table SYS_ROLE_DEPT
  is '角色和部门关联表';
comment on column SYS_ROLE_DEPT.role_id
  is '角色ID';
comment on column SYS_ROLE_DEPT.dept_id
  is '部门ID';
alter table SYS_ROLE_DEPT
  add constraint PK_SYS_ROLE_DEPT primary key (ROLE_ID, DEPT_ID);

prompt
prompt Creating table SYS_ROLE_MENU
prompt ============================
prompt
create table SYS_ROLE_MENU
(
  role_id NUMBER(20) not null,
  menu_id NUMBER(20) not null
)
;
comment on table SYS_ROLE_MENU
  is '角色和菜单关联表';
comment on column SYS_ROLE_MENU.role_id
  is '角色ID';
comment on column SYS_ROLE_MENU.menu_id
  is '菜单ID';
alter table SYS_ROLE_MENU
  add constraint PK_SYS_ROLE_MENU primary key (ROLE_ID, MENU_ID);

prompt
prompt Creating table SYS_USER
prompt =======================
prompt
create table SYS_USER
(
  user_id     NUMBER(20) not null,
  dept_id     NUMBER(20),
  user_name   VARCHAR2(30) default '',
  nick_name   VARCHAR2(30) not null,
  user_type   VARCHAR2(2) default '00',
  email       VARCHAR2(50) default '',
  phonenumber VARCHAR2(11) default '',
  sex         CHAR(1) default '0',
  avatar      VARCHAR2(100) default '',
  password    VARCHAR2(100) default '',
  salt        VARCHAR2(20) default '',
  status      CHAR(1) default '0',
  del_flag    CHAR(1) default '0',
  login_ip    VARCHAR2(50) default '',
  login_date  DATE,
  create_by   VARCHAR2(64),
  create_time DATE,
  update_by   VARCHAR2(64) default '',
  update_time DATE,
  remark      VARCHAR2(500) default ''
)
;
comment on table SYS_USER
  is '用户信息表';
comment on column SYS_USER.user_id
  is '用户ID';
comment on column SYS_USER.dept_id
  is '部门ID';
comment on column SYS_USER.user_name
  is '用户账号';
comment on column SYS_USER.nick_name
  is '用户昵称';
comment on column SYS_USER.user_type
  is '用户类型（00系统用户）';
comment on column SYS_USER.email
  is '用户邮箱';
comment on column SYS_USER.phonenumber
  is '手机号码';
comment on column SYS_USER.sex
  is '用户性别（0男 1女 2未知）';
comment on column SYS_USER.avatar
  is '头像地址';
comment on column SYS_USER.password
  is '密码';
comment on column SYS_USER.salt
  is '盐加密';
comment on column SYS_USER.status
  is '帐号状态（0正常 1停用）';
comment on column SYS_USER.del_flag
  is '删除标志（0代表存在 2代表删除）';
comment on column SYS_USER.login_ip
  is '最后登录IP';
comment on column SYS_USER.login_date
  is '最后登录时间';
comment on column SYS_USER.create_by
  is '创建者';
comment on column SYS_USER.create_time
  is '创建时间';
comment on column SYS_USER.update_by
  is '更新者';
comment on column SYS_USER.update_time
  is '更新时间';
comment on column SYS_USER.remark
  is '备注';
alter table SYS_USER
  add constraint PK_SYS_USER primary key (USER_ID);

prompt
prompt Creating table SYS_USER_POST
prompt ============================
prompt
create table SYS_USER_POST
(
  user_id NUMBER(20) not null,
  post_id NUMBER(20) not null
)
;
comment on table SYS_USER_POST
  is '用户与岗位关联表';
comment on column SYS_USER_POST.user_id
  is '用户ID';
comment on column SYS_USER_POST.post_id
  is '岗位ID';
alter table SYS_USER_POST
  add constraint PK_SYS_USER_POST primary key (USER_ID, POST_ID);

prompt
prompt Creating table SYS_USER_ROLE
prompt ============================
prompt
create table SYS_USER_ROLE
(
  user_id NUMBER(20) not null,
  role_id NUMBER(20) not null
)
;
comment on table SYS_USER_ROLE
  is '用户和角色关联表';
comment on column SYS_USER_ROLE.user_id
  is '用户ID';
comment on column SYS_USER_ROLE.role_id
  is '角色ID';
alter table SYS_USER_ROLE
  add constraint PK_SYS_USER_ROLE primary key (USER_ID, ROLE_ID);

prompt
prompt Creating sequence SEQ_GEN_TABLE
prompt ===============================
prompt
create sequence SEQ_GEN_TABLE
minvalue 1
maxvalue 9999999999999999999999999999
start with 100
increment by 1
cache 20;

prompt
prompt Creating sequence SEQ_GEN_TABLE_COLUMN
prompt ======================================
prompt
create sequence SEQ_GEN_TABLE_COLUMN
minvalue 1
maxvalue 9999999999999999999999999999
start with 100
increment by 1
cache 20;

prompt
prompt Creating sequence SEQ_SYS_CONFIG
prompt ================================
prompt
create sequence SEQ_SYS_CONFIG
minvalue 1
maxvalue 9999999999999999999999999999
start with 180
increment by 1
cache 20;

prompt
prompt Creating sequence SEQ_SYS_DEPT
prompt ==============================
prompt
create sequence SEQ_SYS_DEPT
minvalue 1
maxvalue 9999999999999999999999999999
start with 280
increment by 1
cache 20;

prompt
prompt Creating sequence SEQ_SYS_DICT_DATA
prompt ===================================
prompt
create sequence SEQ_SYS_DICT_DATA
minvalue 1
maxvalue 9999999999999999999999999999
start with 220
increment by 1
cache 20;

prompt
prompt Creating sequence SEQ_SYS_DICT_TYPE
prompt ===================================
prompt
create sequence SEQ_SYS_DICT_TYPE
minvalue 1
maxvalue 9999999999999999999999999999
start with 200
increment by 1
cache 20;

prompt
prompt Creating sequence SEQ_SYS_JOB
prompt =============================
prompt
create sequence SEQ_SYS_JOB
minvalue 1
maxvalue 9999999999999999999999999999
start with 120
increment by 1
cache 20;

prompt
prompt Creating sequence SEQ_SYS_JOB_LOG
prompt =================================
prompt
create sequence SEQ_SYS_JOB_LOG
minvalue 1
maxvalue 9999999999999999999999999999
start with 41
increment by 1
cache 20;

prompt
prompt Creating sequence SEQ_SYS_LOGININFOR
prompt ====================================
prompt
create sequence SEQ_SYS_LOGININFOR
minvalue 1
maxvalue 9999999999999999999999999999
start with 1280
increment by 1
cache 20;

prompt
prompt Creating sequence SEQ_SYS_MENU
prompt ==============================
prompt
create sequence SEQ_SYS_MENU
minvalue 1
maxvalue 9999999999999999999999999999
start with 2100
increment by 1
cache 20;

prompt
prompt Creating sequence SEQ_SYS_NOTICE
prompt ================================
prompt
create sequence SEQ_SYS_NOTICE
minvalue 1
maxvalue 9999999999999999999999999999
start with 140
increment by 1
cache 20;

prompt
prompt Creating sequence SEQ_SYS_OPER_LOG
prompt ==================================
prompt
create sequence SEQ_SYS_OPER_LOG
minvalue 1
maxvalue 9999999999999999999999999999
start with 2820
increment by 1
cache 20;

prompt
prompt Creating sequence SEQ_SYS_POST
prompt ==============================
prompt
create sequence SEQ_SYS_POST
minvalue 1
maxvalue 9999999999999999999999999999
start with 90
increment by 1
cache 20;

prompt
prompt Creating sequence SEQ_SYS_ROLE
prompt ==============================
prompt
create sequence SEQ_SYS_ROLE
minvalue 1
maxvalue 9999999999999999999999999999
start with 140
increment by 1
cache 20;

prompt
prompt Creating sequence SEQ_SYS_USER
prompt ==============================
prompt
create sequence SEQ_SYS_USER
minvalue 1
maxvalue 9999999999999999999999999999
start with 220
increment by 1
cache 20;

prompt
prompt Creating function FIND_IN_SET
prompt =============================
prompt
create or replace function find_in_set(arg1 in varchar2,arg2 in varchar)
return number is Result number;
begin
select instr(','||arg2||',' , ','||arg1||',') into Result from dual;
return(Result);
end find_in_set;
/


spool off
