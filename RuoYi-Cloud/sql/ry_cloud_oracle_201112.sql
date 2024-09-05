/*
Navicat MySQL Data Transfer

Source Server         : 192.168.100.126
Source Server Version : 50730
Source Host           : 192.168.100.126:3306
Source Database       : ry-cloud

Target Server Type    : ORACLE
Target Server Version : 110200
File Encoding         : 20936

Date: 2020-11-12 16:20:02
*/
-- ----------------------------
-- TABLE  structure forgen_TABLE  
-- ----------------------------
create sequence seq_gen_TABLE 
 increment by 1
 start with 100
 nomaxvalue
 nominvalue
 cache 20;
 
create TABLE  gen_TABLE  (
  TABLE_id           number(20)       not null,
  TABLE_name         varchar2(200)    default '',
  TABLE_comment      varchar2(500)    default '',
  class_name         varchar2(100)    default '',
  tpl_category       varchar2(200)    default 'crud',
  package_name       varchar2(100),
  module_name        varchar2(30),
  business_name      varchar2(30),
  function_name      varchar2(50),
  function_author    varchar2(50),
  gen_type           char(1)          default '0',
  gen_path           varchar2(200)    default '/',
  options            varchar2(1000),
  create_by          varchar2(64)     default '',
  create_time        date,
  update_by          varchar2(64)     default '',
  update_time        date,
  remark             varchar2(500)    default null
);
alter TABLE  gen_TABLE  add constraint pk_gen_TABLE  primary key (TABLE_id);


COMMENT ON TABLE gen_TABLE  IS '��������ҵ���';
COMMENT ON COLUMN gen_TABLE.TABLE_id IS '���';
COMMENT ON COLUMN gen_TABLE.TABLE_name IS '������';
COMMENT ON COLUMN gen_TABLE.TABLE_comment IS '������';
COMMENT ON COLUMN gen_TABLE.class_name IS 'ʵ��������';
COMMENT ON COLUMN gen_TABLE.tpl_category IS 'ʹ�õ�ģ�壨crud������� tree���������';
COMMENT ON COLUMN gen_TABLE.package_name IS '���ɰ�·��';
COMMENT ON COLUMN gen_TABLE.module_name IS '����ģ����';
COMMENT ON COLUMN gen_TABLE.business_name IS '����ҵ����';
COMMENT ON COLUMN gen_TABLE.function_name IS '���ɹ�����';
COMMENT ON COLUMN gen_TABLE.function_author IS '���ɹ�������';
COMMENT ON COLUMN gen_TABLE.gen_type IS '���ɴ��뷽ʽ��0zipѹ���� 1�Զ���·����';
COMMENT ON COLUMN gen_TABLE.gen_path IS '����·��������Ĭ����Ŀ·����';
COMMENT ON COLUMN gen_TABLE.options IS '��������ѡ��';
COMMENT ON COLUMN gen_TABLE.create_by IS '������';
COMMENT ON COLUMN gen_TABLE.create_time IS '����ʱ��';
COMMENT ON COLUMN gen_TABLE.update_by IS '������';
COMMENT ON COLUMN gen_TABLE.update_time IS '����ʱ��';
COMMENT ON COLUMN gen_TABLE.remark IS '��ע';

-- ----------------------------
-- Records of gen_TABLE 
-- ----------------------------

-- ----------------------------
-- TABLE  structure forgen_TABLE_COLUMN  
-- ----------------------------
create sequence seq_gen_TABLE_COLUMN 
 increment by 1
 start with 100
 nomaxvalue
 nominvalue
 cache 20;

create TABLE  gen_TABLE_COLUMN  (
  COLUMN_id          number(20)      not null,
  TABLE_id           varchar2(64),
  COLUMN_name        varchar2(200),
  COLUMN_comment     varchar2(500),
  COLUMN_type        varchar2(100),
  java_type          varchar2(500),
  java_field         varchar2(200),
  is_pk              char(1),
  is_increment       char(1),
  is_required        char(1),
  is_insert          char(1),
  is_edit            char(1),
  is_list            char(1),
  is_query           char(1),
  query_type         varchar(200)    default 'EQ',
  html_type          varchar(200),
  dict_type          varchar(200)    default '',
  sort               number(4),
  create_by          varchar(64)     default '',
  create_time        date ,
  update_by          varchar(64)     default '',
  update_time        date
);
alter TABLE  gen_TABLE_COLUMN  add constraint pk_gen_TABLE_COLUMN  primary key (COLUMN_id);

COMMENT ON TABLE gen_TABLE_COLUMN  IS '��������ҵ����ֶ�';
COMMENT ON COLUMN gen_TABLE_COLUMN.COLUMN_id IS '���';
COMMENT ON COLUMN gen_TABLE_COLUMN.TABLE_id IS '��������';
COMMENT ON COLUMN gen_TABLE_COLUMN.COLUMN_name IS '������';
COMMENT ON COLUMN gen_TABLE_COLUMN.COLUMN_comment IS '������';
COMMENT ON COLUMN gen_TABLE_COLUMN.COLUMN_type IS '������';
COMMENT ON COLUMN gen_TABLE_COLUMN.java_type IS 'JAVA����';
COMMENT ON COLUMN gen_TABLE_COLUMN.java_field IS 'JAVA�ֶ���';
COMMENT ON COLUMN gen_TABLE_COLUMN.is_pk IS '�Ƿ�������1�ǣ�';
COMMENT ON COLUMN gen_TABLE_COLUMN.is_increment IS '�Ƿ�������1�ǣ�';
COMMENT ON COLUMN gen_TABLE_COLUMN.is_required IS '�Ƿ���1�ǣ�';
COMMENT ON COLUMN gen_TABLE_COLUMN.is_insert IS '�Ƿ�Ϊ�����ֶΣ�1�ǣ�';
COMMENT ON COLUMN gen_TABLE_COLUMN.is_edit IS '�Ƿ�༭�ֶΣ�1�ǣ�';
COMMENT ON COLUMN gen_TABLE_COLUMN.is_list IS '�Ƿ��б��ֶΣ�1�ǣ�';
COMMENT ON COLUMN gen_TABLE_COLUMN.is_query IS '�Ƿ��ѯ�ֶΣ�1�ǣ�';
COMMENT ON COLUMN gen_TABLE_COLUMN.query_type IS '��ѯ��ʽ�����ڡ������ڡ����ڡ�С�ڡ���Χ��';
COMMENT ON COLUMN gen_TABLE_COLUMN.html_type IS '��ʾ���ͣ��ı����ı��������򡢸�ѡ�򡢵�ѡ�����ڿؼ���';
COMMENT ON COLUMN gen_TABLE_COLUMN.dict_type IS '�ֵ�����';
COMMENT ON COLUMN gen_TABLE_COLUMN.sort IS '����';
COMMENT ON COLUMN gen_TABLE_COLUMN.create_by IS '������';
COMMENT ON COLUMN gen_TABLE_COLUMN.create_time IS '����ʱ��';
COMMENT ON COLUMN gen_TABLE_COLUMN.update_by IS '������';
COMMENT ON COLUMN gen_TABLE_COLUMN.update_time IS '����ʱ��';

-- ----------------------------
-- Records of gen_TABLE_COLUMN 
-- ----------------------------

-- ----------------------------
-- TABLE  structure forQRTZ_blob_TRIGGERS 
-- ----------------------------
CREATE TABLE QRTZ_blob_TRIGGERS (
sched_name VARCHAR2(120),
trigger_name VARCHAR2(200),
trigger_group VARCHAR2(200),
blob_data blob
);
alter TABLE  QRTZ_blob_TRIGGERS add constraint pk_QRTZ_blob_TRIGGERS primary key (sched_name,trigger_name,trigger_group);

-- ----------------------------
-- Records of QRTZ_blob_TRIGGERS
-- ----------------------------

-- ----------------------------
-- TABLE  structure forQRTZ_CALENDARS 
-- ----------------------------
CREATE TABLE QRTZ_CALENDARS (
 sched_name VARCHAR2(120),
 calendar_name VARCHAR2(200),
 calendar blob
);
alter TABLE  QRTZ_CALENDARS add constraint pk_QRTZ_CALENDARS primary key (sched_name,calendar_name);

-- ----------------------------
-- Records of QRTZ_CALENDARS
-- ----------------------------

-- ----------------------------
-- TABLE  structure forQRTZ_CRON_TRIGGERS 
-- ----------------------------
CREATE TABLE QRTZ_CRON_TRIGGERS (
 sched_name VARCHAR2(120),
 trigger_name VARCHAR2(200),
 trigger_group VARCHAR2(200),
 cron_expression VARCHAR2(200),
 time_zone_id VARCHAR2(80)
);
alter TABLE  QRTZ_CRON_TRIGGERS add constraint pk_QRTZ_CRON_TRIGGERS primary key (sched_name,trigger_name,trigger_group);

-- ----------------------------
-- Records of QRTZ_CRON_TRIGGERS
-- ----------------------------
INSERT INTO QRTZ_CRON_TRIGGERS VALUES ('RuoyiScheduler', 'TASK_CLASS_NAME1', 'DEFAULT', '0/10 * * * * ?', 'Asia/Shanghai');
INSERT INTO QRTZ_CRON_TRIGGERS VALUES ('RuoyiScheduler', 'TASK_CLASS_NAME2', 'DEFAULT', '0/15 * * * * ?', 'Asia/Shanghai');
INSERT INTO QRTZ_CRON_TRIGGERS VALUES ('RuoyiScheduler', 'TASK_CLASS_NAME3', 'DEFAULT', '0/20 * * * * ?', 'Asia/Shanghai');

-- ----------------------------
-- TABLE  structure forQRTZ_FIRED_TRIGGERS 
-- ----------------------------
CREATE TABLE QRTZ_FIRED_TRIGGERS (
 sched_name VARCHAR2(120),
 entry_id VARCHAR2(95),
 trigger_name VARCHAR2(200),
 trigger_group VARCHAR2(200),
 instance_name VARCHAR2(200),
 fired_time NUMBER(20),
 sched_time NUMBER(20),
 priority NUMBER(11),
 state VARCHAR2(16),
 job_name VARCHAR2(200),
 job_group VARCHAR2(200),
 is_nonconcurrent VARCHAR2(1),
 requests_recovery VARCHAR2(1)
);
alter TABLE  QRTZ_FIRED_TRIGGERS add constraint pk_QRTZ_FIRED_TRIGGERS primary key (sched_name,entry_id);

-- ----------------------------
-- Records of QRTZ_FIRED_TRIGGERS
-- ----------------------------

-- ----------------------------
-- TABLE  structure forQRTZ_JOB_DETAILS 
-- ----------------------------
CREATE TABLE QRTZ_JOB_DETAILS (
 sched_name VARCHAR2(120),
 job_name VARCHAR2(200),
 job_group VARCHAR2(200),
 description VARCHAR2(250),
 job_class_name VARCHAR2(250),
 is_durable VARCHAR2(1),
 is_nonconcurrent VARCHAR2(1),
 is_update_data VARCHAR2(1),
 requests_recovery VARCHAR2(1),
 job_data blob
);
alter TABLE  QRTZ_JOB_DETAILS add constraint pk_QRTZ_JOB_DETAILS primary key (sched_name,job_name,job_group);

-- ----------------------------
-- Records of QRTZ_JOB_DETAILS
-- ----------------------------
INSERT INTO QRTZ_JOB_DETAILS VALUES ('RuoyiScheduler', 'TASK_CLASS_NAME1', 'DEFAULT', null, 'com.ruoyi.job.util.QuartzDisallowConcurrentExecution', '0', '1', '0', '0', '?? sr org.quartz.JobDataMap???��??  xr &org.quartz.utils.StringKeyDirtyFlagMap?????]( Z allowsTransientDataxr org.quartz.utils.DirtyFlagMap?.?(v
? Z dirtyL mapt Ljava/util/Map;xpsr java.util.HashMap???`? F 
loadFactorI 	thresholdxp?@     w      t TASK_PROPERTIESsr com.ruoyi.job.domain.SysJob        L 
concurrentt Ljava/lang/String;L cronExpressionq ~ 	L invokeTargetq ~ 	L jobGroupq ~ 	L jobIdt Ljava/lang/Long;L jobNameq ~ 	L 
misfirePolicyq ~ 	L statusq ~ 	xr +com.ruoyi.common.core.web.domain.BaseEntity        	L 	beginTimeq ~ 	L createByq ~ 	L 
createTimet Ljava/util/Date;L endTimeq ~ 	L paramsq ~ L remarkq ~ 	L searchValueq ~ 	L updateByq ~ 	L 
updateTimeq ~ xppt adminsr java.util.Datehj?KYt  xpw  b,?)?xppt  pppt 1t 0/10 * * * * ?t ryTask.ryNoParamst DEFAULTsr java.lang.Long;???#? J valuexr java.lang.Number?????  xp       t ϵͳĬ�ϣ��޲Σ�t 3t 1x ');
INSERT INTO QRTZ_JOB_DETAILS VALUES ('RuoyiScheduler', 'TASK_CLASS_NAME2', 'DEFAULT', null, 'com.ruoyi.job.util.QuartzDisallowConcurrentExecution', '0', '1', '0', '0', '?? sr org.quartz.JobDataMap???��??  xr &org.quartz.utils.StringKeyDirtyFlagMap?????]( Z allowsTransientDataxr org.quartz.utils.DirtyFlagMap?.?(v
? Z dirtyL mapt Ljava/util/Map;xpsr java.util.HashMap???`? F 
loadFactorI 	thresholdxp?@     w      t TASK_PROPERTIESsr com.ruoyi.job.domain.SysJob        L 
concurrentt Ljava/lang/String;L cronExpressionq ~ 	L invokeTargetq ~ 	L jobGroupq ~ 	L jobIdt Ljava/lang/Long;L jobNameq ~ 	L 
misfirePolicyq ~ 	L statusq ~ 	xr +com.ruoyi.common.core.web.domain.BaseEntity        	L 	beginTimeq ~ 	L createByq ~ 	L 
createTimet Ljava/util/Date;L endTimeq ~ 	L paramsq ~ L remarkq ~ 	L searchValueq ~ 	L updateByq ~ 	L 
updateTimeq ~ xppt adminsr java.util.Datehj?KYt  xpw  b,?)?xppt  pppt 1t 0/15 * * * * ?t ryTask.ryParams('ry')t DEFAULTsr java.lang.Long;???#? J valuexr java.lang.Number?????  xp       t ϵͳĬ�ϣ��вΣ�t 3t 1x ');
INSERT INTO QRTZ_JOB_DETAILS VALUES ('RuoyiScheduler', 'TASK_CLASS_NAME3', 'DEFAULT', null, 'com.ruoyi.job.util.QuartzDisallowConcurrentExecution', '0', '1', '0', '0', '?? sr org.quartz.JobDataMap???��??  xr &org.quartz.utils.StringKeyDirtyFlagMap?????]( Z allowsTransientDataxr org.quartz.utils.DirtyFlagMap?.?(v
? Z dirtyL mapt Ljava/util/Map;xpsr java.util.HashMap???`? F 
loadFactorI 	thresholdxp?@     w      t TASK_PROPERTIESsr com.ruoyi.job.domain.SysJob        L 
concurrentt Ljava/lang/String;L cronExpressionq ~ 	L invokeTargetq ~ 	L jobGroupq ~ 	L jobIdt Ljava/lang/Long;L jobNameq ~ 	L 
misfirePolicyq ~ 	L statusq ~ 	xr +com.ruoyi.common.core.web.domain.BaseEntity        	L 	beginTimeq ~ 	L createByq ~ 	L 
createTimet Ljava/util/Date;L endTimeq ~ 	L paramsq ~ L remarkq ~ 	L searchValueq ~ 	L updateByq ~ 	L 
updateTimeq ~ xppt adminsr java.util.Datehj?KYt  xpw  b,?)?xppt  pppt 1t 0/20 * * * * ?t 8ryTask.ryMultipleParams('ry', true, 2000L, 316.50D, 100)t DEFAULTsr java.lang.Long;???#? J valuexr java.lang.Number?????  xp       t ϵͳĬ�ϣ���Σ�t 3t 1x ');

-- ----------------------------
-- TABLE  structure forQRTZ_LOCKS 
-- ----------------------------
CREATE TABLE QRTZ_LOCKS (
 sched_name VARCHAR2(120),
 lock_name VARCHAR2(40)
);
alter TABLE  QRTZ_LOCKS add constraint pk_QRTZ_LOCKS primary key (sched_name,lock_name);


-- ----------------------------
-- Records of QRTZ_LOCKS
-- ----------------------------
INSERT INTO QRTZ_LOCKS VALUES ('RuoyiScheduler', 'STATE_ACCESS');
INSERT INTO QRTZ_LOCKS VALUES ('RuoyiScheduler', 'TRIGGER_ACCESS');

-- ----------------------------
-- TABLE  structure forQRTZ_PAUSED_TRIGGER_GRPS 
-- ----------------------------
CREATE TABLE QRTZ_PAUSED_TRIGGER_GRPS (
 sched_name VARCHAR2(120),
 trigger_group VARCHAR2(200)
);
alter TABLE  QRTZ_PAUSED_TRIGGER_GRPS add constraint pk_QRTZ_PAUSED_TRIGGER_GRPS primary key (sched_name,trigger_group);


-- ----------------------------
-- Records of QRTZ_PAUSED_TRIGGER_GRPS
-- ----------------------------

-- ----------------------------
-- TABLE  structure forQRTZ_SCHEDULER_STATE 
-- ----------------------------
CREATE TABLE QRTZ_SCHEDULER_STATE (
 sched_name VARCHAR2(120),
 instance_name VARCHAR2(200),
 last_checkin_time NUMBER(20),
 checkin_interval NUMBER(20)
);
alter TABLE  QRTZ_SCHEDULER_STATE add constraint pk_QRTZ_SCHEDULER_STATE primary key (sched_name,instance_name);



-- ----------------------------
-- Records of QRTZ_SCHEDULER_STATE
-- ----------------------------
INSERT INTO QRTZ_SCHEDULER_STATE VALUES ('RuoyiScheduler', 'DESKTOP-DUUM01O1604299257481', '1604299628631', '15000');

-- ----------------------------
-- TABLE  structure forQRTZ_SIMPLE_TRIGGERS 
-- ----------------------------
CREATE TABLE QRTZ_SIMPLE_TRIGGERS (
 sched_name VARCHAR2(120),
 trigger_name VARCHAR2(200),
 trigger_group VARCHAR2(200),
 repeat_count NUMBER(20),
 repeat_interval NUMBER(20),
 times_triggered NUMBER(20)
);
alter TABLE  QRTZ_SIMPLE_TRIGGERS add constraint pk_QRTZ_SIMPLE_TRIGGERS primary key (sched_name,trigger_name,trigger_group);

-- ----------------------------
-- Records of QRTZ_SIMPLE_TRIGGERS
-- ----------------------------

-- ----------------------------
-- TABLE  structure forQRTZ_SIMPROP_TRIGGERS 
-- ----------------------------
CREATE TABLE QRTZ_SIMPROP_TRIGGERS (
 sched_name VARCHAR2(120),
 trigger_name VARCHAR2(200),
 trigger_group VARCHAR2(200),
 str_prop_1 VARCHAR2(512),
 str_prop_2 VARCHAR2(512),
 str_prop_3 VARCHAR2(512),
 int_prop_1 NUMBER(11),
 int_prop_2 NUMBER(11),
 long_prop_1 NUMBER(20),
 long_prop_2 NUMBER(20),
 dec_prop_1 NUMBER,
 dec_prop_2 NUMBER,
 bool_prop_1 VARCHAR2(1),
 bool_prop_2 VARCHAR2(1)
);
alter TABLE  QRTZ_SIMPROP_TRIGGERS add constraint pk_QRTZ_SIMPROP_TRIGGERS primary key (sched_name,trigger_name,trigger_group);


-- ----------------------------
-- Records of QRTZ_SIMPROP_TRIGGERS
-- ----------------------------

-- ----------------------------
-- TABLE  structure forQRTZ_TRIGGERS 
-- ----------------------------
CREATE TABLE QRTZ_TRIGGERS (
 sched_name VARCHAR2(120),
 trigger_name VARCHAR2(200),
 trigger_group VARCHAR2(200),
 job_name VARCHAR2(200),
 job_group VARCHAR2(200),
 description VARCHAR2(250),
 next_fire_time NUMBER(20),
 prev_fire_time NUMBER(20),
 priority NUMBER(11),
 trigger_state VARCHAR2(16),
 trigger_type VARCHAR2(8),
 start_time NUMBER(20),
 end_time NUMBER(20),
 calendar_name VARCHAR2(200),
 misfire_instr NUMBER(6),
 job_data blob
);
alter TABLE  QRTZ_TRIGGERS add constraint pk_QRTZ_TRIGGERS primary key (sched_name,trigger_name,trigger_group);


-- ----------------------------
-- Records of QRTZ_TRIGGERS
-- ----------------------------
INSERT INTO QRTZ_TRIGGERS VALUES ('RuoyiScheduler', 'TASK_CLASS_NAME1', 'DEFAULT', 'TASK_CLASS_NAME1', 'DEFAULT', null, '1604299260000', '-1', '5', 'PAUSED', 'CRON', '1604299259000', '0', null, '2', '');
INSERT INTO QRTZ_TRIGGERS VALUES ('RuoyiScheduler', 'TASK_CLASS_NAME2', 'DEFAULT', 'TASK_CLASS_NAME2', 'DEFAULT', null, '1604299260000', '-1', '5', 'PAUSED', 'CRON', '1604299259000', '0', null, '2', '');
INSERT INTO QRTZ_TRIGGERS VALUES ('RuoyiScheduler', 'TASK_CLASS_NAME3', 'DEFAULT', 'TASK_CLASS_NAME3', 'DEFAULT', null, '1604299260000', '-1', '5', 'PAUSED', 'CRON', '1604299260000', '0', null, '2', '');

-- ----------------------------
-- TABLE  structure forsys_config 
-- ----------------------------
create sequence seq_sys_config
 increment by 1
 start with 100
 nomaxvalue
 nominvalue
 cache 20;
 
create TABLE  sys_config (
  config_id         number(20)     not null,
  config_name       varchar2(100)  default '',
  config_key        varchar2(100)  default '',
  config_value      varchar2(100)  default '',
  config_type       char(1)        default 'N',
  create_by         varchar2(64)   default '',
  create_time       date,
  update_by         varchar2(64)   default '',
  update_time       date,
  remark            varchar2(500)  default null
);
alter TABLE  sys_config add constraint pk_sys_config primary key (config_id);

COMMENT ON TABLE sys_config IS '�������ñ�';
COMMENT ON COLUMN sys_config.config_id IS '��������';
COMMENT ON COLUMN sys_config.config_name IS '��������';
COMMENT ON COLUMN sys_config.config_key IS '��������';
COMMENT ON COLUMN sys_config.config_value IS '������ֵ';
COMMENT ON COLUMN sys_config.config_type IS 'ϵͳ���ã�Y�� N��';
COMMENT ON COLUMN sys_config.create_by IS '������';
COMMENT ON COLUMN sys_config.create_time IS '����ʱ��';
COMMENT ON COLUMN sys_config.update_by IS '������';
COMMENT ON COLUMN sys_config.update_time IS '����ʱ��';
COMMENT ON COLUMN sys_config.remark IS '��ע';

-- ----------------------------
-- Records of sys_config
-- ----------------------------
INSERT INTO sys_config VALUES ('1', '�����ҳ-Ĭ��Ƥ����ʽ����', 'sys.index.skinName', 'skin-blue', 'Y', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '��ɫ skin-blue����ɫ skin-green����ɫ skin-purple����ɫ skin-red����ɫ skin-yellow');
INSERT INTO sys_config VALUES ('2', '�û�����-�˺ų�ʼ����', 'sys.user.initPassword', '123456', 'Y', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '��ʼ������ 123456');
INSERT INTO sys_config VALUES ('3', '�����ҳ-���������', 'sys.index.sideTheme', 'theme-dark', 'Y', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '��ɫ����theme-dark��ǳɫ����theme-light');

-- ----------------------------
-- TABLE  structure forsys_dept 
-- ----------------------------
create sequence seq_sys_dept
 increment by 1
 start with 200
 nomaxvalue
 nominvalue
 cache 20;
 
create TABLE  sys_dept (
  dept_id           number(20)      not null,
  parent_id         number(20)      default 0,
  ancestors         varchar2(50)    default '',
  dept_name         varchar2(30)    default '',
  order_num         number(4)       default 0,
  leader            varchar2(220)    default null,
  phone             varchar2(11)    default null,
  email             varchar2(50)    default null,
  status            char(1)         default '0',
  del_flag          char(1)         default '0',
  create_by         varchar2(64)    default '',
  create_time 	    date,
  update_by         varchar2(64)    default '',
  update_time       date
);

alter TABLE  sys_dept add constraint pk_sys_dept primary key (dept_id);

COMMENT ON TABLE sys_dept IS '���ű�';
COMMENT ON COLUMN sys_dept.dept_id IS '����id';
COMMENT ON COLUMN sys_dept.parent_id IS '������id';
COMMENT ON COLUMN sys_dept.ancestors IS '�漶�б�';
COMMENT ON COLUMN sys_dept.dept_name IS '��������';
COMMENT ON COLUMN sys_dept.order_num IS '��ʾ˳��';
COMMENT ON COLUMN sys_dept.leader IS '������';
COMMENT ON COLUMN sys_dept.phone IS '��ϵ�绰';
COMMENT ON COLUMN sys_dept.email IS '����';
COMMENT ON COLUMN sys_dept.status IS '����״̬��0���� 1ͣ�ã�';
COMMENT ON COLUMN sys_dept.del_flag IS 'ɾ����־��0������� 2����ɾ����';
COMMENT ON COLUMN sys_dept.create_by IS '������';
COMMENT ON COLUMN sys_dept.create_time IS '����ʱ��';
COMMENT ON COLUMN sys_dept.update_by IS '������';
COMMENT ON COLUMN sys_dept.update_time IS '����ʱ��';

-- ----------------------------
-- Records of sys_dept
-- ----------------------------
INSERT INTO sys_dept VALUES ('100', '0', '0', '�����Ƽ�', '0', '����', '15888888888', 'ry@qq.com', '0', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'admin', TO_TIMESTAMP('2020-10-30 15:37:12', 'YYYY-MM-DD HH24:MI:SS'));
INSERT INTO sys_dept VALUES ('101', '100', '0,100', '�����ܹ�˾', '1', '����', '15888888888', 'ry@qq.com', '0', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'));
INSERT INTO sys_dept VALUES ('102', '100', '0,100', '��ɳ�ֹ�˾', '2', '����', '15888888888', 'ry@qq.com', '1', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'admin', TO_TIMESTAMP('2020-10-30 15:38:09', 'YYYY-MM-DD HH24:MI:SS'));
INSERT INTO sys_dept VALUES ('103', '101', '0,100,101', '�з�����', '1', '����', '15888888888', 'ry@qq.com', '0', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'));
INSERT INTO sys_dept VALUES ('104', '101', '0,100,101', '�г�����', '2', '����', '15888888888', 'ry@qq.com', '0', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'));
INSERT INTO sys_dept VALUES ('105', '101', '0,100,101', '���Բ���', '3', '����', '15888888888', 'ry@qq.com', '0', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'));
INSERT INTO sys_dept VALUES ('106', '101', '0,100,101', '������', '4', '����', '15888888888', 'ry@qq.com', '0', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'));
INSERT INTO sys_dept VALUES ('107', '101', '0,100,101', '��ά����', '5', '����', '15888888888', 'ry@qq.com', '0', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'));
INSERT INTO sys_dept VALUES ('108', '102', '0,100,102', '�г�����', '1', '����', '15888888888', 'ry@qq.com', '1', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'admin', TO_TIMESTAMP('2020-10-30 15:38:05', 'YYYY-MM-DD HH24:MI:SS'));
INSERT INTO sys_dept VALUES ('109', '102', '0,100,102', '������', '2', '����', '15888888888', 'ry@qq.com', '1', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'admin', TO_TIMESTAMP('2020-10-30 15:37:55', 'YYYY-MM-DD HH24:MI:SS'));
INSERT INTO sys_dept VALUES ('111', '100', '0,100', '���Բ���', '99', '�����˸����˸����˸����˸����˸����˸���', '15711111111', 'ABC@QQ.COM', '0', '0', 'admin', TO_TIMESTAMP('2020-10-29 17:09:24', 'YYYY-MM-DD HH24:MI:SS'), 'admin', TO_TIMESTAMP('2020-10-29 17:47:46', 'YYYY-MM-DD HH24:MI:SS'));
INSERT INTO sys_dept VALUES ('112', '100', '0,100', '�з�����', '88', null, null, null, '0', '0', 'admin', TO_TIMESTAMP('2020-10-29 17:22:55', 'YYYY-MM-DD HH24:MI:SS'), '', null);

-- ----------------------------
-- TABLE  structure forsys_dict_data 
-- ----------------------------
create sequence seq_sys_dict_data
 increment by 1
 start with 100
 nomaxvalue
 nominvalue
 cache 20;

create TABLE  sys_dict_data
(
  dict_code        number(20)      not null,
  dict_sort        number(4)       default 0,
  dict_label       varchar2(100)   default '',
  dict_value       varchar2(100)   default '',
  dict_type        varchar2(100)   default '',
  css_class        varchar2(100)   default null,
  list_class       varchar2(100)   default null,
  is_default       char(1)         default 'N',
  status           char(1)         default '0',
  create_by        varchar2(64)    default '',
  create_time      date,
  update_by        varchar2(64)    default '',
  update_time      date,
  remark           varchar2(500)   default null
);

alter TABLE  sys_dict_data add constraint pk_sys_dict_data primary key (dict_code);

COMMENT ON TABLE sys_dict_data IS '�ֵ����ݱ�';
COMMENT ON COLUMN sys_dict_data.dict_code IS '�ֵ����';
COMMENT ON COLUMN sys_dict_data.dict_sort IS '�ֵ�����';
COMMENT ON COLUMN sys_dict_data.dict_label IS '�ֵ��ǩ';
COMMENT ON COLUMN sys_dict_data.dict_value IS '�ֵ��ֵ';
COMMENT ON COLUMN sys_dict_data.dict_type IS '�ֵ�����';
COMMENT ON COLUMN sys_dict_data.css_class IS '��ʽ���ԣ�������ʽ��չ��';
COMMENT ON COLUMN sys_dict_data.list_class IS '��������ʽ';
COMMENT ON COLUMN sys_dict_data.is_default IS '�Ƿ�Ĭ�ϣ�Y�� N��';
COMMENT ON COLUMN sys_dict_data.status IS '״̬��0���� 1ͣ�ã�';
COMMENT ON COLUMN sys_dict_data.create_by IS '������';
COMMENT ON COLUMN sys_dict_data.create_time IS '����ʱ��';
COMMENT ON COLUMN sys_dict_data.update_by IS '������';
COMMENT ON COLUMN sys_dict_data.update_time IS '����ʱ��';
COMMENT ON COLUMN sys_dict_data.remark IS '��ע';

-- ----------------------------
-- Records of sys_dict_data
-- ----------------------------
INSERT INTO sys_dict_data VALUES ('1', '1', '��', '0', 'sys_user_sex', '', '', 'Y', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '�Ա���');
INSERT INTO sys_dict_data VALUES ('2', '2', 'Ů', '1', 'sys_user_sex', '', '', 'N', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '�Ա�Ů');
INSERT INTO sys_dict_data VALUES ('3', '3', 'δ֪', '2', 'sys_user_sex', '', '', 'N', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '�Ա�δ֪');
INSERT INTO sys_dict_data VALUES ('4', '1', '��ʾ', '0', 'sys_show_hide', '', 'primary', 'Y', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '��ʾ�˵�');
INSERT INTO sys_dict_data VALUES ('5', '2', '����', '1', 'sys_show_hide', '', 'danger', 'N', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '���ز˵�');
INSERT INTO sys_dict_data VALUES ('6', '1', '����', '0', 'sys_normal_disable', '', 'primary', 'Y', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '����״̬');
INSERT INTO sys_dict_data VALUES ('7', '2', 'ͣ��', '1', 'sys_normal_disable', '', 'danger', 'N', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ͣ��״̬');
INSERT INTO sys_dict_data VALUES ('8', '1', '����', '0', 'sys_job_status', '', 'primary', 'Y', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '����״̬');
INSERT INTO sys_dict_data VALUES ('9', '2', '��ͣ', '1', 'sys_job_status', '', 'danger', 'N', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ͣ��״̬');
INSERT INTO sys_dict_data VALUES ('10', '1', 'Ĭ��', 'DEFAULT', 'sys_job_group', '', '', 'Y', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'Ĭ�Ϸ���');
INSERT INTO sys_dict_data VALUES ('11', '2', 'ϵͳ', 'SYSTEM', 'sys_job_group', '', '', 'N', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ϵͳ����');
INSERT INTO sys_dict_data VALUES ('12', '1', '��', 'Y', 'sys_yes_no', '', 'primary', 'Y', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ϵͳĬ����');
INSERT INTO sys_dict_data VALUES ('13', '2', '��', 'N', 'sys_yes_no', '', 'danger', 'N', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ϵͳĬ�Ϸ�');
INSERT INTO sys_dict_data VALUES ('14', '1', '֪ͨ', '1', 'sys_notice_type', '', 'warning', 'Y', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '֪ͨ');
INSERT INTO sys_dict_data VALUES ('15', '2', '����', '2', 'sys_notice_type', '', 'success', 'N', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '����');
INSERT INTO sys_dict_data VALUES ('16', '1', '����', '0', 'sys_notice_status', '', 'primary', 'Y', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '����״̬');
INSERT INTO sys_dict_data VALUES ('17', '2', '�ر�', '1', 'sys_notice_status', '', 'danger', 'N', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '�ر�״̬');
INSERT INTO sys_dict_data VALUES ('18', '1', '����', '1', 'sys_oper_type', '', 'info', 'N', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '��������');
INSERT INTO sys_dict_data VALUES ('19', '2', '�޸�', '2', 'sys_oper_type', '', 'info', 'N', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '�޸Ĳ���');
INSERT INTO sys_dict_data VALUES ('20', '3', 'ɾ��', '3', 'sys_oper_type', '', 'danger', 'N', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ɾ������');
INSERT INTO sys_dict_data VALUES ('21', '4', '��Ȩ', '4', 'sys_oper_type', '', 'primary', 'N', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '��Ȩ����');
INSERT INTO sys_dict_data VALUES ('22', '5', '����', '5', 'sys_oper_type', '', 'warning', 'N', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '��������');
INSERT INTO sys_dict_data VALUES ('23', '6', '����', '6', 'sys_oper_type', '', 'warning', 'N', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '�������');
INSERT INTO sys_dict_data VALUES ('24', '7', 'ǿ��', '7', 'sys_oper_type', '', 'danger', 'N', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ǿ�˲���');
INSERT INTO sys_dict_data VALUES ('25', '8', '���ɴ���', '8', 'sys_oper_type', '', 'warning', 'N', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '���ɲ���');
INSERT INTO sys_dict_data VALUES ('26', '9', '�������', '9', 'sys_oper_type', '', 'danger', 'N', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '��ղ���');
INSERT INTO sys_dict_data VALUES ('27', '1', '�ɹ�', '0', 'sys_common_status', '', 'primary', 'N', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '����״̬');
INSERT INTO sys_dict_data VALUES ('28', '2', 'ʧ��', '1', 'sys_common_status', '', 'danger', 'N', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ͣ��״̬');
INSERT INTO sys_dict_data VALUES ('29', '1', '��Ȩ��ģʽ', 'authorization_code', 'sys_grant_type', '', '', 'N', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '��Ȩ��ģʽ');
INSERT INTO sys_dict_data VALUES ('30', '2', '����ģʽ', 'password', 'sys_grant_type', '', '', 'N', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '����ģʽ');
INSERT INTO sys_dict_data VALUES ('31', '3', '�ͻ���ģʽ', 'client_credentials', 'sys_grant_type', '', '', 'N', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '�ͻ���ģʽ');
INSERT INTO sys_dict_data VALUES ('32', '4', '��ģʽ', 'implicit', 'sys_grant_type', '', '', 'N', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '��ģʽ');
INSERT INTO sys_dict_data VALUES ('33', '5', 'ˢ��ģʽ', 'refresh_token', 'sys_grant_type', '', '', 'N', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ˢ��ģʽ');
INSERT INTO sys_dict_data VALUES ('36', '0', '1', '1', 'te', null, null, 'N', '0', 'admin', TO_TIMESTAMP('2020-10-26 10:04:34', 'YYYY-MM-DD HH24:MI:SS'), '', null, null);

-- ----------------------------
-- TABLE  structure forsys_dict_type 
-- ----------------------------
create sequence seq_sys_dict_type
 increment by 1
 start with 100
 nomaxvalue
 nominvalue
 cache 20;

create TABLE  sys_dict_type
(
  dict_id           number(20)      not null,
  dict_name         varchar2(100)   default '',
  dict_type         varchar2(100)   default '',
  status            char(1)         default '0',
  create_by         varchar2(64)    default '',
  create_time       date,
  update_by         varchar2(64)    default '',
  update_time       date,
  remark            varchar2(500)   default null
);

alter TABLE  sys_dict_type add constraint pk_sys_dict_type primary key (dict_id);
create unique index sys_dict_type_index1 on sys_dict_type (dict_type);

COMMENT ON TABLE sys_dict_type IS '�ֵ����ͱ�';
COMMENT ON COLUMN sys_dict_type.dict_id IS '�ֵ�����';
COMMENT ON COLUMN sys_dict_type.dict_name IS '�ֵ�����';
COMMENT ON COLUMN sys_dict_type.dict_type IS '�ֵ�����';
COMMENT ON COLUMN sys_dict_type.status IS '״̬��0���� 1ͣ�ã�';
COMMENT ON COLUMN sys_dict_type.create_by IS '������';
COMMENT ON COLUMN sys_dict_type.create_time IS '����ʱ��';
COMMENT ON COLUMN sys_dict_type.update_by IS '������';
COMMENT ON COLUMN sys_dict_type.update_time IS '����ʱ��';
COMMENT ON COLUMN sys_dict_type.remark IS '��ע';

-- ----------------------------
-- Records of sys_dict_type
-- ----------------------------
INSERT INTO sys_dict_type VALUES ('1', '�û��Ա�', 'sys_user_sex', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '�û��Ա��б�');
INSERT INTO sys_dict_type VALUES ('2', '�˵�״̬', 'sys_show_hide', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '�˵�״̬�б�');
INSERT INTO sys_dict_type VALUES ('3', 'ϵͳ����', 'sys_normal_disable', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ϵͳ�����б�');
INSERT INTO sys_dict_type VALUES ('4', '����״̬', 'sys_job_status', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '����״̬�б�');
INSERT INTO sys_dict_type VALUES ('5', '�������', 'sys_job_group', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '��������б�');
INSERT INTO sys_dict_type VALUES ('6', 'ϵͳ�Ƿ�', 'sys_yes_no', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ϵͳ�Ƿ��б�');
INSERT INTO sys_dict_type VALUES ('7', '֪ͨ����', 'sys_notice_type', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '֪ͨ�����б�');
INSERT INTO sys_dict_type VALUES ('8', '֪ͨ״̬', 'sys_notice_status', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '֪ͨ״̬�б�');
INSERT INTO sys_dict_type VALUES ('9', '��������', 'sys_oper_type', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '���������б�');
INSERT INTO sys_dict_type VALUES ('10', 'ϵͳ״̬', 'sys_common_status', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '��¼״̬�б�');
INSERT INTO sys_dict_type VALUES ('11', '��Ȩ����', 'sys_grant_type', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '��Ȩ�����б�');
INSERT INTO sys_dict_type VALUES ('13', 'te', 'te', '0', 'admin', TO_TIMESTAMP('2020-10-26 10:04:23', 'YYYY-MM-DD HH24:MI:SS'), '', null, null);

-- ----------------------------
-- TABLE  structure forsys_job 
-- ----------------------------
create sequence seq_sys_job
 increment by 1
 start with 100
 nomaxvalue
 nominvalue
 cache 20;

create TABLE  sys_job (
  job_id              number(20)     not null,
  job_name            varchar2(64)   default '',
  job_group           varchar2(64)   default '',
  invoke_target       varchar2(500)  not null ,
  cron_expression     varchar2(255)  default '',
  misfire_policy      varchar2(20)   default '3',
  concurrent          char(1)        default '1',
  status              char(1)        default '0',
  create_by           varchar2(64)   default '',
  create_time         date,
  update_by           varchar2(64)   default '',
  update_time         date,
  remark              varchar2(500)  default ''
);

alter TABLE  sys_job add constraint pk_sys_job primary key (job_id, job_name, job_group);

COMMENT ON TABLE sys_job IS '��ʱ������ȱ�';
COMMENT ON COLUMN sys_job.job_id IS '����ID';
COMMENT ON COLUMN sys_job.job_name IS '��������';
COMMENT ON COLUMN sys_job.job_group IS '��������';
COMMENT ON COLUMN sys_job.invoke_target IS '����Ŀ���ַ���';
COMMENT ON COLUMN sys_job.cron_expression IS 'cronִ�б��ʽ';
COMMENT ON COLUMN sys_job.misfire_policy IS '�ƻ�ִ�д�����ԣ�1����ִ�� 2ִ��һ�� 3����ִ�У�';
COMMENT ON COLUMN sys_job.concurrent IS '�Ƿ񲢷�ִ�У�0���� 1��ֹ��';
COMMENT ON COLUMN sys_job.status IS '״̬��0���� 1��ͣ��';
COMMENT ON COLUMN sys_job.create_by IS '������';
COMMENT ON COLUMN sys_job.create_time IS '����ʱ��';
COMMENT ON COLUMN sys_job.update_by IS '������';
COMMENT ON COLUMN sys_job.update_time IS '����ʱ��';
COMMENT ON COLUMN sys_job.remark IS '��ע��Ϣ';

-- ----------------------------
-- Records of sys_job
-- ----------------------------
INSERT INTO sys_job VALUES ('1', 'ϵͳĬ�ϣ��޲Σ�', 'DEFAULT', 'ryTask.ryNoParams', '0/10 * * * * ?', '3', '1', '1', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_job VALUES ('2', 'ϵͳĬ�ϣ��вΣ�', 'DEFAULT', 'ryTask.ryParams(''ry'')', '0/15 * * * * ?', '3', '1', '1', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_job VALUES ('3', 'ϵͳĬ�ϣ���Σ�', 'DEFAULT', 'ryTask.ryMultipleParams(''ry'', true, 2000L, 316.50D, 100)', '0/20 * * * * ?', '3', '1', '1', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');

-- ----------------------------
-- TABLE  structure forsys_job_log 
-- ----------------------------
create sequence seq_sys_job_log
 increment by 1
 start with 1
 nomaxvalue
 nominvalue
 cache 20;

create TABLE  sys_job_log (
  job_log_id          number(20)       not null,
  job_name            varchar2(64)     not null,
  job_group           varchar2(64)     not null,
  invoke_target       varchar2(500)    not null ,
  job_message         varchar2(500),
  status              char(1)          default '0',
  exception_info      varchar2(2000)   default '',
  create_time         date
);

alter TABLE  sys_job_log add constraint pk_sys_job_log primary key (job_log_id);

COMMENT ON TABLE sys_job_log IS '��ʱ���������־��';
COMMENT ON COLUMN sys_job_log.job_log_id IS '������־ID';
COMMENT ON COLUMN sys_job_log.job_name IS '��������';
COMMENT ON COLUMN sys_job_log.job_group IS '��������';
COMMENT ON COLUMN sys_job_log.invoke_target IS '����Ŀ���ַ���';
COMMENT ON COLUMN sys_job_log.job_message IS '��־��Ϣ';
COMMENT ON COLUMN sys_job_log.status IS 'ִ��״̬��0���� 1ʧ�ܣ�';
COMMENT ON COLUMN sys_job_log.exception_info IS '�쳣��Ϣ';
COMMENT ON COLUMN sys_job_log.create_time IS '����ʱ��';

-- ----------------------------
-- Records of sys_job_log
-- ----------------------------

-- ----------------------------
-- TABLE  structure forsys_logininfor 
-- ----------------------------
create sequence seq_sys_logininfor
 increment by 1
 start with 100
 nomaxvalue
 nominvalue
 cache 20;
 
create TABLE  sys_logininfor (
  info_id         number(20)     not null,
  user_name      varchar2(50)   default '',
  ipaddr          varchar2(50)   default '',
  status          char(1)        default '0',
  msg             varchar2(255)  default '',
  access_time      date
);

alter TABLE  sys_logininfor add constraint pk_sys_logininfor primary key (info_id);

COMMENT ON TABLE sys_logininfor IS 'ϵͳ���ʼ�¼';
COMMENT ON COLUMN sys_logininfor.info_id IS '����ID';
COMMENT ON COLUMN sys_logininfor.user_name IS '�û��˺�';
COMMENT ON COLUMN sys_logininfor.ipaddr IS '��¼IP��ַ';
COMMENT ON COLUMN sys_logininfor.status IS '��¼״̬��0�ɹ� 1ʧ�ܣ�';
COMMENT ON COLUMN sys_logininfor.msg IS '��ʾ��Ϣ';
COMMENT ON COLUMN sys_logininfor.access_time IS '����ʱ��';

-- ----------------------------
-- Records of sys_logininfor
-- ----------------------------
INSERT INTO sys_logininfor VALUES ('182', 'admin', '192.168.100.126', '0', '��¼�ɹ�', TO_TIMESTAMP('2020-11-12 12:42:46', 'YYYY-MM-DD HH24:MI:SS'));
INSERT INTO sys_logininfor VALUES ('183', 'admin', '192.168.100.126', '0', '��¼�ɹ�', TO_TIMESTAMP('2020-11-12 15:03:56', 'YYYY-MM-DD HH24:MI:SS'));

-- ----------------------------
-- TABLE  structure forsys_menu 
-- ----------------------------
create sequence seq_sys_menu
 increment by 1
 start with 2000
 nomaxvalue
 nominvalue
 cache 20;

create TABLE  sys_menu (
  menu_id           number(20)      not null,
  menu_name         varchar2(50)    not null,
  parent_id         number(20)      default 0,
  order_num         number(4)       default 0,
  target            varchar2(255)    default '',
  path							varchar2(255)   default '',
  bt_url            varchar2(255)   default '#',
  component   varchar2(255) default null,
  is_frame    number(1) default 1,
  is_cache    number(1) default 0,
  menu_type         char(1)         default '',
  visible           char(1)         default 0,
  status     char(1) default 0,
  perms             varchar2(100)   default null,
  bt_perms             varchar2(100)   default null,
  icon              varchar2(100)   default '#',
  bt_icon              varchar2(100)   default '#',
  create_by         varchar2(64)    default '',
  create_time       date,
  update_by         varchar2(64)    default '',
  update_time       date ,
  remark            varchar2(500)   default ''
);

alter TABLE  sys_menu add constraint pk_sys_menu primary key (menu_id);
COMMENT ON TABLE sys_menu IS '�˵�Ȩ�ޱ�';
COMMENT ON COLUMN sys_menu.menu_id IS '�˵�ID';
COMMENT ON COLUMN sys_menu.menu_name IS '�˵�����';
COMMENT ON COLUMN sys_menu.parent_id IS '���˵�ID';
COMMENT ON COLUMN sys_menu.order_num IS '��ʾ˳��';
COMMENT ON COLUMN sys_menu.target IS '�´��ڵ�URL';
COMMENT ON COLUMN sys_menu.path IS '·�ɵ�ַ';
COMMENT ON COLUMN sys_menu.bt_url IS 'BootUI �Ĳ˵�URL';
COMMENT ON COLUMN sys_menu.component IS '���·��';
COMMENT ON COLUMN sys_menu.is_frame IS '�Ƿ�Ϊ������0�� 1��';
COMMENT ON COLUMN sys_menu.is_cache IS '�Ƿ񻺴棨0���� 1�����棩';
COMMENT ON COLUMN sys_menu.menu_type IS '�˵����ͣ�MĿ¼ C�˵� F��ť��';
COMMENT ON COLUMN sys_menu.visible IS '�˵�״̬��0��ʾ 1���أ�';
COMMENT ON COLUMN sys_menu.status IS '�˵�״̬��0���� 1ͣ�ã�';
COMMENT ON COLUMN sys_menu.perms IS 'Ȩ�ޱ�ʶ';
COMMENT ON COLUMN sys_menu.bt_perms IS 'btȨ�ޱ�ʶ';
COMMENT ON COLUMN sys_menu.icon IS '�˵�ͼ��';
COMMENT ON COLUMN sys_menu.bt_icon IS 'bt�˵�ͼ��';
COMMENT ON COLUMN sys_menu.create_by IS '������';
COMMENT ON COLUMN sys_menu.create_time IS '����ʱ��';
COMMENT ON COLUMN sys_menu.update_by IS '������';
COMMENT ON COLUMN sys_menu.update_time IS '����ʱ��';
COMMENT ON COLUMN sys_menu.remark IS '��ע';

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
INSERT INTO sys_menu VALUES ('1', 'ϵͳ����', '0', '1', null, 'system', '#', null, '1', '0', 'M', '0', '0', '', '', 'system', 'fa fa-gear', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'admin', TO_TIMESTAMP('2020-10-29 16:33:50', 'YYYY-MM-DD HH24:MI:SS'), 'ϵͳ����Ŀ¼');
INSERT INTO sys_menu VALUES ('2', 'ϵͳ���', '0', '2', null, 'monitor', '#', null, '1', '0', 'M', '0', '0', '', '', 'monitor', 'fa fa-video-camera', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ϵͳ���Ŀ¼');
INSERT INTO sys_menu VALUES ('3', 'ϵͳ����', '0', '3', null, 'tool', '#', null, '1', '0', 'M', '0', '0', '', '', 'tool', 'fa fa-bars', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ϵͳ����Ŀ¼');
INSERT INTO sys_menu VALUES ('4', '��������', '0', '4', null, 'http://ruoyi.vip', '#', null, '0', '0', 'M', '0', '0', '', null, 'guide', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '����������ַ');
INSERT INTO sys_menu VALUES ('100', '�û�����', '1', '1', null, 'user', '/system/user', 'system/user/index', '1', '0', 'C', '0', '0', 'system:user:list', 'system:user:view', 'user', 'fa fa-user-o', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '�û�����˵�');
INSERT INTO sys_menu VALUES ('101', '��ɫ����', '1', '2', null, 'role', '/system/role', 'system/role/index', '1', '0', 'C', '0', '0', 'system:role:list', 'system:role:view', 'peoples', 'fa fa-user-secret', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '��ɫ����˵�');
INSERT INTO sys_menu VALUES ('102', '�˵�����', '1', '3', null, 'menu', '/system/menu', 'system/menu/index', '1', '0', 'C', '0', '0', 'system:menu:list', 'system:menu:view', 'tree-TABLE ', 'fa fa-th-list', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '�˵�����˵�');
INSERT INTO sys_menu VALUES ('103', '���Ź���', '1', '4', null, 'dept', '/system/dept', 'system/dept/index', '1', '0', 'C', '0', '0', 'system:dept:list', 'system:dept:view', 'tree', 'fa fa-outdent', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '���Ź���˵�');
INSERT INTO sys_menu VALUES ('104', '��λ����', '1', '5', null, 'post', '/system/post', 'system/post/index', '1', '0', 'C', '0', '0', 'system:post:list', 'system:post:view', 'post', 'fa fa-address-card-o', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '��λ����˵�');
INSERT INTO sys_menu VALUES ('105', '�ֵ����', '1', '6', null, 'dict', '/system/dict', 'system/dict/index', '1', '0', 'C', '0', '0', 'system:dict:list', 'system:dict:view', 'dict', 'fa fa-bookmark-o', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '�ֵ����˵�');
INSERT INTO sys_menu VALUES ('106', '��������', '1', '7', null, 'config', '/system/config', 'system/config/index', '1', '0', 'C', '0', '0', 'system:config:list', 'system:config:view', 'edit', 'fa fa-sun-o', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '�������ò˵�');
INSERT INTO sys_menu VALUES ('107', '֪ͨ����', '1', '9', null, 'notice', '/system/notice', 'system/notice/index', '1', '0', 'C', '0', '0', 'system:notice:list', 'system:notice:view', 'message', 'fa fa-bullhorn', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '֪ͨ����˵�');
INSERT INTO sys_menu VALUES ('108', '��־����', '1', '10', null, 'log', '#', 'system/log/index', '1', '0', 'M', '0', '0', '', '', 'log', 'fa fa-pencil-square-o', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '��־����˵�');
INSERT INTO sys_menu VALUES ('109', '�����û�', '2', '1', null, 'online', '/monitor/online', 'monitor/online/index', '1', '0', 'C', '0', '0', 'monitor:online:list', 'monitor:online:view', 'online', 'fa fa-user-circle', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '�����û��˵�');
INSERT INTO sys_menu VALUES ('110', '��ʱ����', '2', '2', null, 'job', '/monitor/job', 'monitor/job/index', '1', '0', 'C', '0', '0', 'monitor:job:list', 'monitor:job:view', 'job', 'fa fa-tasks', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '��ʱ����˵�');
INSERT INTO sys_menu VALUES ('111', 'Sentinel����̨', '2', '3', null, 'http://localhost:8718', '/monitor/data', '', '1', '0', 'C', '0', '0', 'monitor:sentinel:list', 'monitor:data:view', 'sentinel', 'fa fa-bug', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'admin', TO_TIMESTAMP('2020-10-26 10:59:46', 'YYYY-MM-DD HH24:MI:SS'), '�������Ʋ˵�');
INSERT INTO sys_menu VALUES ('112', 'Nacos����̨', '2', '4', null, 'http://192.168.2.93:8848/nacos', '/monitor/server', '', '1', '0', 'C', '0', '0', 'monitor:nacos:list', 'monitor:server:view', 'nacos', 'fa fa-server', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'admin', TO_TIMESTAMP('2020-10-13 13:51:27', 'YYYY-MM-DD HH24:MI:SS'), '��������˵�');
INSERT INTO sys_menu VALUES ('113', 'Admin����̨', '2', '5', null, 'http://localhost:9100/login', '/tool/build', '', '1', '0', 'C', '0', '0', 'monitor:server:list', 'tool:build:view', 'server', 'fa fa-wpforms', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '�����ز˵�');
INSERT INTO sys_menu VALUES ('114', '������', '3', '1', null, 'build', '/tool/build', 'tool/build/index', '1', '0', 'C', '0', '0', 'tool:build:list', 'tool:build:view', 'build', 'fa fa-wpforms', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '�������˵�');
INSERT INTO sys_menu VALUES ('115', '��������', '3', '2', null, 'gen', '/tool/gen', 'tool/gen/index', '1', '0', 'C', '0', '0', 'tool:gen:list', 'tool:gen:view', 'code', 'fa fa-code', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '�������ɲ˵�');
INSERT INTO sys_menu VALUES ('116', 'ϵͳ�ӿ�', '3', '3', null, 'http://192.168.100.126:8080/swagger-ui.html', '/tool/swagger', '', '1', '0', 'C', '0', '0', 'tool:swagger:list', 'tool:swagger:view', 'swagger', 'fa fa-gg', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'admin', TO_TIMESTAMP('2020-10-27 09:53:29', 'YYYY-MM-DD HH24:MI:SS'), 'ϵͳ�ӿڲ˵�');
INSERT INTO sys_menu VALUES ('500', '������־', '108', '1', null, 'operlog', '/monitor/operlog', 'system/operlog/index', '1', '0', 'C', '0', '0', 'system:operlog:list', 'monitor:operlog:view', 'form', 'fa fa-address-book', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '������־�˵�');
INSERT INTO sys_menu VALUES ('501', '��¼��־', '108', '2', null, 'logininfor', '/monitor/logininfor', 'system/logininfor/index', '1', '0', 'C', '0', '0', 'system:logininfor:list', 'monitor:logininfor:view', 'logininfor', 'fa fa-file-image-o', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '��¼��־�˵�');
INSERT INTO sys_menu VALUES ('1001', '�û���ѯ', '100', '1', null, '', '#', '', '1', '0', 'F', '0', '0', 'system:user:query', 'system:user:list', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1002', '�û�����', '100', '2', null, '', '#', '', '1', '0', 'F', '0', '0', 'system:user:add', 'system:user:add', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1003', '�û��޸�', '100', '3', null, '', '#', '', '1', '0', 'F', '0', '0', 'system:user:edit', 'system:user:edit', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1004', '�û�ɾ��', '100', '4', null, '', '#', '', '1', '0', 'F', '0', '0', 'system:user:remove', 'system:user:remove', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1005', '�û�����', '100', '5', null, '', '#', '', '1', '0', 'F', '0', '0', 'system:user:export', 'system:user:export', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1006', '�û�����', '100', '6', null, '', '#', '', '1', '0', 'F', '0', '0', 'system:user:import', 'system:user:import', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1007', '��������', '100', '7', null, '', '#', '', '1', '0', 'F', '0', '0', 'system:user:resetPwd', 'system:user:resetPwd', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1008', '��ɫ��ѯ', '101', '1', null, '', '#', '', '1', '0', 'F', '0', '0', 'system:role:query', 'system:role:list', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1009', '��ɫ����', '101', '2', null, '', '#', '', '1', '0', 'F', '0', '0', 'system:role:add', 'system:role:add', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1010', '��ɫ�޸�', '101', '3', null, '', '#', '', '1', '0', 'F', '0', '0', 'system:role:edit', 'system:role:edit', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1011', '��ɫɾ��', '101', '4', null, '', '#', '', '1', '0', 'F', '0', '0', 'system:role:remove', 'system:role:remove', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1012', '��ɫ����', '101', '5', null, '', '#', '', '1', '0', 'F', '0', '0', 'system:role:export', 'system:role:export', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1013', '�˵���ѯ', '102', '1', null, '', '#', '', '1', '0', 'F', '0', '0', 'system:menu:query', 'system:menu:list', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1014', '�˵�����', '102', '2', null, '', '#', '', '1', '0', 'F', '0', '0', 'system:menu:add', 'system:menu:add', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1015', '�˵��޸�', '102', '3', null, '', '#', '', '1', '0', 'F', '0', '0', 'system:menu:edit', 'system:menu:edit', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1016', '�˵�ɾ��', '102', '4', null, '', '#', '', '1', '0', 'F', '0', '0', 'system:menu:remove', 'system:menu:remove', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1017', '���Ų�ѯ', '103', '1', null, '', '#', '', '1', '0', 'F', '0', '0', 'system:dept:query', 'system:dept:list', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1018', '��������', '103', '2', null, '', '#', '', '1', '0', 'F', '0', '0', 'system:dept:add', 'system:dept:add', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1019', '�����޸�', '103', '3', null, '', '#', '', '1', '0', 'F', '0', '0', 'system:dept:edit', 'system:dept:edit', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1020', '����ɾ��', '103', '4', null, '', '#', '', '1', '0', 'F', '0', '0', 'system:dept:remove', 'system:dept:remove', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1021', '��λ��ѯ', '104', '1', null, '', '#', '', '1', '0', 'F', '0', '0', 'system:post:query', 'system:post:list', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1022', '��λ����', '104', '2', null, '', '#', '', '1', '0', 'F', '0', '0', 'system:post:add', 'system:post:add', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1023', '��λ�޸�', '104', '3', null, '', '#', '', '1', '0', 'F', '0', '0', 'system:post:edit', 'system:post:edit', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1024', '��λɾ��', '104', '4', null, '', '#', '', '1', '0', 'F', '0', '0', 'system:post:remove', 'system:post:remove', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1025', '��λ����', '104', '5', null, '', '#', '', '1', '0', 'F', '0', '0', 'system:post:export', 'system:post:export', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1026', '�ֵ��ѯ', '105', '1', null, '#', '#', '', '1', '0', 'F', '0', '0', 'system:dict:query', 'system:dict:list', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1027', '�ֵ�����', '105', '2', null, '#', '#', '', '1', '0', 'F', '0', '0', 'system:dict:add', 'system:dict:add', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1028', '�ֵ��޸�', '105', '3', null, '#', '#', '', '1', '0', 'F', '0', '0', 'system:dict:edit', 'system:dict:edit', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1029', '�ֵ�ɾ��', '105', '4', null, '#', '#', '', '1', '0', 'F', '0', '0', 'system:dict:remove', 'system:dict:remove', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1030', '�ֵ䵼��', '105', '5', null, '#', '#', '', '1', '0', 'F', '0', '0', 'system:dict:export', 'system:dict:export', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1031', '������ѯ', '106', '1', null, '#', '#', '', '1', '0', 'F', '0', '0', 'system:config:query', 'system:config:list', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1032', '��������', '106', '2', null, '#', '#', '', '1', '0', 'F', '0', '0', 'system:config:add', 'system:config:add', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1033', '�����޸�', '106', '3', null, '#', '#', '', '1', '0', 'F', '0', '0', 'system:config:edit', 'system:config:edit', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1034', '����ɾ��', '106', '4', null, '#', '#', '', '1', '0', 'F', '0', '0', 'system:config:remove', 'system:config:remove', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1035', '��������', '106', '5', null, '#', '#', '', '1', '0', 'F', '0', '0', 'system:config:export', 'system:config:export', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1041', '�����ѯ', '107', '1', null, '#', '#', '', '1', '0', 'F', '0', '0', 'system:notice:query', 'system:notice:list', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1042', '��������', '107', '2', null, '#', '#', '', '1', '0', 'F', '0', '0', 'system:notice:add', 'system:notice:add', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1043', '�����޸�', '107', '3', null, '#', '#', '', '1', '0', 'F', '0', '0', 'system:notice:edit', 'system:notice:edit', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1044', '����ɾ��', '107', '4', null, '#', '#', '', '1', '0', 'F', '0', '0', 'system:notice:remove', 'system:notice:remove', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1045', '������ѯ', '500', '1', null, '#', '#', '', '1', '0', 'F', '0', '0', 'system:operlog:query', 'monitor:operlog:list', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1046', '����ɾ��', '500', '2', null, '#', '#', '', '1', '0', 'F', '0', '0', 'system:operlog:remove', 'monitor:operlog:remove', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1047', '��־����', '500', '4', null, '#', '#', '', '1', '0', 'F', '0', '0', 'system:operlog:export', 'monitor:operlog:export', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1048', '��¼��ѯ', '501', '1', null, '#', '#', '', '1', '0', 'F', '0', '0', 'system:logininfor:query', 'monitor:logininfor:list', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1049', '��¼ɾ��', '501', '2', null, '#', '#', '', '1', '0', 'F', '0', '0', 'system:logininfor:remove', 'monitor:logininfor:remove', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1050', '��־����', '501', '3', null, '#', '#', '', '1', '0', 'F', '0', '0', 'system:logininfor:export', 'monitor:operlog:export', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1051', '���߲�ѯ', '109', '1', null, '#', '#', '', '1', '0', 'F', '0', '0', 'monitor:online:query', 'monitor:online:list', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1052', '����ǿ��', '109', '2', null, '#', '#', '', '1', '0', 'F', '0', '0', 'monitor:online:batchLogout', 'monitor:online:batchForceLogout', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1053', '����ǿ��', '109', '3', null, '#', '#', '', '1', '0', 'F', '0', '0', 'monitor:online:forceLogout', 'monitor:online:forceLogout', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1054', '�����ѯ', '110', '1', null, '#', '#', '', '1', '0', 'F', '0', '0', 'monitor:job:query', 'monitor:job:list', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1055', '��������', '110', '2', null, '#', '#', '', '1', '0', 'F', '0', '0', 'monitor:job:add', 'monitor:job:add', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1056', '�����޸�', '110', '3', null, '#', '#', '', '1', '0', 'F', '0', '0', 'monitor:job:edit', 'monitor:job:edit', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1057', '����ɾ��', '110', '4', null, '#', '#', '', '1', '0', 'F', '0', '0', 'monitor:job:remove', 'monitor:job:remove', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1058', '״̬�޸�', '110', '5', null, '#', '#', '', '1', '0', 'F', '0', '0', 'monitor:job:changeStatus', 'monitor:job:changeStatus', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1059', '���񵼳�', '110', '7', null, '#', '#', '', '1', '0', 'F', '0', '0', 'monitor:job:export', 'monitor:job:export', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1060', '���ɲ�ѯ', '115', '1', null, '#', '#', '', '1', '0', 'F', '0', '0', 'tool:gen:query', 'tool:gen:list', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1061', '�����޸�', '115', '2', null, '#', '#', '', '1', '0', 'F', '0', '0', 'tool:gen:edit', 'tool:gen:edit', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1062', '����ɾ��', '115', '3', null, '#', '#', '', '1', '0', 'F', '0', '0', 'tool:gen:remove', 'tool:gen:remove', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1063', '�������', '115', '2', null, '#', '#', '', '1', '0', 'F', '0', '0', 'tool:gen:import', null, '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1064', 'Ԥ������', '115', '4', null, '#', '#', '', '1', '0', 'F', '0', '0', 'tool:gen:preview', 'tool:gen:preview', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1065', '���ɴ���', '115', '5', null, '#', '#', '', '1', '0', 'F', '0', '0', 'tool:gen:code', 'tool:gen:code', '#', '#', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1066', '�û�����', '100', '8', null, '', '#', null, '1', '0', 'F', '0', '0', 'system:user:view', null, '#', '#', 'admin', TO_TIMESTAMP('2020-11-05 10:48:21', 'YYYY-MM-DD HH24:MI:SS'), 'admin', TO_TIMESTAMP('2020-11-05 10:55:01', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1067', '��ɫ����', '101', '6', null, '', '#', null, '1', '0', 'F', '0', '0', 'system:role:view', null, '#', '#', 'admin', TO_TIMESTAMP('2020-11-05 11:11:09', 'YYYY-MM-DD HH24:MI:SS'), '', null, '');
INSERT INTO sys_menu VALUES ('1068', '�˵�����', '102', '5', null, '', '#', null, '1', '0', 'F', '0', '0', 'system:menu:view', null, '#', '#', 'admin', TO_TIMESTAMP('2020-11-05 11:12:31', 'YYYY-MM-DD HH24:MI:SS'), '', null, '');
INSERT INTO sys_menu VALUES ('1069', '���Ž���', '103', '5', null, '', '#', null, '1', '0', 'F', '0', '0', 'system:dept:view', null, '#', '#', 'admin', TO_TIMESTAMP('2020-11-05 11:13:59', 'YYYY-MM-DD HH24:MI:SS'), '', null, '');
INSERT INTO sys_menu VALUES ('1070', '���Բ˵�', '0', '99', null, 'testmenu', '#', null, '1', '0', 'M', '0', '0', '', null, '#', '#', 'admin', TO_TIMESTAMP('2020-10-29 16:16:19', 'YYYY-MM-DD HH24:MI:SS'), 'admin', TO_TIMESTAMP('2020-10-29 16:26:22', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1071', '�����ѷ����Ӳ˵�', '1070', '100', null, 'test', '#', null, '1', '0', 'M', '0', '0', '', null, '#', '#', 'admin', TO_TIMESTAMP('2020-10-29 16:42:56', 'YYYY-MM-DD HH24:MI:SS'), 'admin', TO_TIMESTAMP('2020-10-29 16:48:51', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1076', '��λ����', '104', '6', null, '', '#', null, '1', '0', 'F', '0', '0', 'system:post:view', null, '#', '#', 'admin', TO_TIMESTAMP('2020-11-05 11:23:16', 'YYYY-MM-DD HH24:MI:SS'), '', null, '');
INSERT INTO sys_menu VALUES ('1077', '�ֵ����', '105', '6', null, '', '#', null, '1', '0', 'F', '0', '0', 'system:dict:view', null, '#', '#', 'admin', TO_TIMESTAMP('2020-11-05 11:26:07', 'YYYY-MM-DD HH24:MI:SS'), '', null, '');
INSERT INTO sys_menu VALUES ('1078', '��������', '106', '6', null, '', '#', null, '1', '0', 'F', '0', '0', 'system:config:view', null, '#', '#', 'admin', TO_TIMESTAMP('2020-11-05 11:27:30', 'YYYY-MM-DD HH24:MI:SS'), '', null, '');
INSERT INTO sys_menu VALUES ('1079', '֪ͨ�������', '107', '6', null, '', '#', null, '1', '0', 'F', '0', '0', 'system:notice:view', null, '#', '#', 'admin', TO_TIMESTAMP('2020-11-05 11:28:49', 'YYYY-MM-DD HH24:MI:SS'), 'admin', TO_TIMESTAMP('2020-11-05 13:46:46', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1080', '������־����', '500', '3', null, '', '#', null, '1', '0', 'F', '0', '0', 'monitor:operlog:view', null, '#', '#', 'admin', TO_TIMESTAMP('2020-11-05 13:47:10', 'YYYY-MM-DD HH24:MI:SS'), 'admin', TO_TIMESTAMP('2020-11-05 13:48:16', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO sys_menu VALUES ('1081', '��¼��־����', '501', '4', null, '', '#', null, '1', '0', 'F', '0', '0', 'monitor:logininfor:view', null, '#', '#', 'admin', TO_TIMESTAMP('2020-11-05 13:49:32', 'YYYY-MM-DD HH24:MI:SS'), '', null, '');
INSERT INTO sys_menu VALUES ('1082', '�����û�����', '109', '4', null, '', '#', null, '1', '0', 'F', '0', '0', 'monitor:online:view', null, '#', '#', 'admin', TO_TIMESTAMP('2020-11-05 13:50:17', 'YYYY-MM-DD HH24:MI:SS'), '', null, '');
INSERT INTO sys_menu VALUES ('1083', '��ʱ�������', '110', '8', null, '', '#', null, '1', '0', 'F', '0', '0', 'monitor:job:view', null, '#', '#', 'admin', TO_TIMESTAMP('2020-11-05 13:50:41', 'YYYY-MM-DD HH24:MI:SS'), '', null, '');

-- ----------------------------
-- TABLE  structure forsys_notice 
-- ----------------------------
create sequence seq_sys_notice
 increment by 1
 start with 100
 nomaxvalue
 nominvalue
 cache 20;

create TABLE  sys_notice (
  notice_id         number(20)      not null,
  notice_title      varchar2(500)    not null,
  notice_type       char(1)         not null,
  notice_content    varchar2(2000)  default null,
  status            char(1)         default '0',
  create_by         varchar2(64)    default '',
  create_time       date,
  update_by         varchar2(64)    default '',
  update_time       date,
  remark            varchar2(255)   default null
);

alter TABLE  sys_notice add constraint pk_sys_notice primary key (notice_id);

COMMENT ON TABLE sys_notice IS '֪ͨ�����';
COMMENT ON COLUMN sys_notice.notice_id IS '����ID';
COMMENT ON COLUMN sys_notice.notice_title IS '�������';
COMMENT ON COLUMN sys_notice.notice_type IS '�������ͣ�1֪ͨ 2���棩';
COMMENT ON COLUMN sys_notice.notice_content IS '��������';
COMMENT ON COLUMN sys_notice.status IS '����״̬��0���� 1�رգ�';
COMMENT ON COLUMN sys_notice.create_by IS '������';
COMMENT ON COLUMN sys_notice.create_time IS '����ʱ��';
COMMENT ON COLUMN sys_notice.update_by IS '������';
COMMENT ON COLUMN sys_notice.update_time IS '����ʱ��';
COMMENT ON COLUMN sys_notice.remark IS '��ע';

-- ----------------------------
-- Records of sys_notice
-- ----------------------------
INSERT INTO sys_notice VALUES ('1', '��ܰ���ѣ�2018-07-01 �����°汾������', '1', '<p>�°汾����</p>', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'admin', TO_TIMESTAMP('2020-11-02 14:39:17', 'YYYY-MM-DD HH24:MI:SS'), '����Ա');
INSERT INTO sys_notice VALUES ('2', 'ά��֪ͨ��2018-07-01 ����ϵͳ�賿ά��', '1', 'ά������', '1', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'tester2', TO_TIMESTAMP('2020-10-26 17:47:27', 'YYYY-MM-DD HH24:MI:SS'), '����Ա');

-- ----------------------------
-- TABLE  structure forsys_oper_log 
-- ----------------------------
create sequence seq_sys_oper_log
 increment by 1
 start with 100
 nomaxvalue
 nominvalue
 cache 20;
 
create TABLE  sys_oper_log (
  oper_id           number(20)      not null ,
  title             varchar2(50)    default '',
  business_type     number(2)       default 0,
  method            varchar2(100)   default '',
  request_method    varchar(10)     default '',
  operator_type     number(1)       default 0,
  oper_name         varchar2(50)    default '',
  dept_name         varchar2(50)    default '',
  oper_url          varchar2(255) 	default '',
  oper_ip           varchar2(50)    default '',
  oper_location     varchar2(255)   default '',
  oper_param        varchar2(2000)  default '',
  json_result       varchar2(2000)  default '',
  status            number(1)       default 0,
  error_msg         varchar2(2000)  default '' ,
  oper_time         date
);
alter TABLE  sys_oper_log add constraint pk_sys_oper_log primary key (oper_id);

COMMENT ON TABLE sys_oper_log IS '������־��¼';
COMMENT ON COLUMN sys_oper_log.oper_id IS '��־����';
COMMENT ON COLUMN sys_oper_log.title IS 'ģ�����';
COMMENT ON COLUMN sys_oper_log.business_type IS 'ҵ�����ͣ�0���� 1���� 2�޸� 3ɾ����';
COMMENT ON COLUMN sys_oper_log.method IS '��������';
COMMENT ON COLUMN sys_oper_log.request_method IS '����ʽ';
COMMENT ON COLUMN sys_oper_log.operator_type IS '�������0���� 1��̨�û� 2�ֻ����û���';
COMMENT ON COLUMN sys_oper_log.oper_name IS '������Ա';
COMMENT ON COLUMN sys_oper_log.dept_name IS '��������';
COMMENT ON COLUMN sys_oper_log.oper_url IS '����URL';
COMMENT ON COLUMN sys_oper_log.oper_ip IS '������ַ';
COMMENT ON COLUMN sys_oper_log.oper_location IS '�����ص�';
COMMENT ON COLUMN sys_oper_log.oper_param IS '�������';
COMMENT ON COLUMN sys_oper_log.json_result IS '���ز���';
COMMENT ON COLUMN sys_oper_log.status IS '����״̬��0���� 1�쳣��';
COMMENT ON COLUMN sys_oper_log.error_msg IS '������Ϣ';
COMMENT ON COLUMN sys_oper_log.oper_time IS '����ʱ��';

-- ----------------------------
-- Records of sys_oper_log
-- ----------------------------

-- ----------------------------
-- TABLE  structure forsys_post 
-- ----------------------------
create sequence seq_sys_post
 increment by 1
 start with 10
 nomaxvalue
 nominvalue
 cache 20;


create TABLE  sys_post
(
  post_id           number(20)      not null,
  post_code         varchar2(64)    not null,
  post_name         varchar2(50)    not null,
  post_sort         number(4)       not null,
  status            char(1)         not null,
  create_by         varchar2(64)    default '',
  create_time       date,
  update_by         varchar2(64)    default '',
  update_time       date,
  remark            varchar2(500)
);

alter TABLE  sys_post add constraint pk_sys_post primary key (post_id);

COMMENT ON TABLE sys_post IS '��λ��Ϣ��';
COMMENT ON COLUMN sys_post.post_id IS '��λID';
COMMENT ON COLUMN sys_post.post_code IS '��λ����';
COMMENT ON COLUMN sys_post.post_name IS '��λ����';
COMMENT ON COLUMN sys_post.post_sort IS '��ʾ˳��';
COMMENT ON COLUMN sys_post.status IS '״̬��0���� 1ͣ�ã�';
COMMENT ON COLUMN sys_post.create_by IS '������';
COMMENT ON COLUMN sys_post.create_time IS '����ʱ��';
COMMENT ON COLUMN sys_post.update_by IS '������';
COMMENT ON COLUMN sys_post.update_time IS '����ʱ��';
COMMENT ON COLUMN sys_post.remark IS '��ע';

-- ----------------------------
-- Records of sys_post
-- ----------------------------
INSERT INTO  sys_post values(1, 'ceo',  '���³�',    1, '0', 'admin', TO_DATE('2018-03-16 11-33-00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_DATE('2018-03-16 11-33-00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO  sys_post values(2, 'se',   '��Ŀ����',  2, '0', 'admin', TO_DATE('2018-03-16 11-33-00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_DATE('2018-03-16 11-33-00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO  sys_post values(3, 'hr',   '������Դ',  3, '0', 'admin', TO_DATE('2018-03-16 11-33-00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_DATE('2018-03-16 11-33-00', 'YYYY-MM-DD HH24:MI:SS'), '');
INSERT INTO  sys_post values(4, 'user', '��ͨԱ��',  4, '0', 'admin', TO_DATE('2018-03-16 11-33-00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_DATE('2018-03-16 11-33-00', 'YYYY-MM-DD HH24:MI:SS'), '');

-- ----------------------------
-- TABLE  structure forsys_role 
-- ----------------------------
create sequence seq_sys_role
 increment by 1
 start with 100
 nomaxvalue
 nominvalue
 cache 20;

create TABLE  sys_role (
  role_id           number(20)      not null,
  role_name         varchar2(30)    not null,
  role_key          varchar2(100)   not null,
  role_sort         number(4)       not null,
  data_scope        char(1)         default '1',
menu_check_strictly NUMBER(4) DEFAULT '1',
dept_check_strictly NUMBER(4) DEFAULT '1',  
  status            char(1)         not null,
  del_flag          char(1)         default '0',
  create_by         varchar2(64)    default '',
  create_time       date,
  update_by         varchar2(64)    default '',
  update_time       date,
  remark            varchar2(500)   default null
);

alter TABLE  sys_role add constraint pk_sys_role primary key (role_id);

COMMENT ON TABLE sys_role IS '��ɫ��Ϣ��';
COMMENT ON COLUMN sys_role.role_id IS '��ɫID';
COMMENT ON COLUMN sys_role.role_name IS '��ɫ����';
COMMENT ON COLUMN sys_role.role_key IS '��ɫȨ���ַ���';
COMMENT ON COLUMN sys_role.role_sort IS '��ʾ˳��';
COMMENT ON COLUMN sys_role.data_scope IS '���ݷ�Χ��1��ȫ������Ȩ�� 2���Զ�����Ȩ�� 3������������Ȩ�� 4�������ż���������Ȩ�ޣ�';
COMMENT ON COLUMN sys_role.menu_check_strictly IS '�˵���ѡ�����Ƿ������ʾ';
COMMENT ON COLUMN sys_role.dept_check_strictly IS '������ѡ�����Ƿ������ʾ';
COMMENT ON COLUMN sys_role.status IS '��ɫ״̬��0���� 1ͣ�ã�';
COMMENT ON COLUMN sys_role.del_flag IS 'ɾ����־��0������� 2����ɾ����';
COMMENT ON COLUMN sys_role.create_by IS '������';
COMMENT ON COLUMN sys_role.create_time IS '����ʱ��';
COMMENT ON COLUMN sys_role.update_by IS '������';
COMMENT ON COLUMN sys_role.update_time IS '����ʱ��';
COMMENT ON COLUMN sys_role.remark IS '��ע';

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO sys_role VALUES ('1', '��������Ա', 'admin', '1', '1', '1', '1', '0', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'ry', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), '��������Ա');
INSERT INTO sys_role VALUES ('2', '��ͨ��ɫ', 'common', '2', '2', '0', '0', '0', '0', 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'admin', TO_TIMESTAMP('2020-10-29 14:31:41', 'YYYY-MM-DD HH24:MI:SS'), '��ͨ��ɫ');
INSERT INTO sys_role VALUES ('26', '���Խ�ɫ1', 'testrole11', '99', '3', '0', '0', '1', '0', 'admin', TO_TIMESTAMP('2020-10-29 13:36:14', 'YYYY-MM-DD HH24:MI:SS'), 'admin', TO_TIMESTAMP('2020-10-29 16:48:14', 'YYYY-MM-DD HH24:MI:SS'), null);

-- ----------------------------
-- TABLE  structure forsys_role_dept 
-- ----------------------------
create TABLE  sys_role_dept (
  role_id 	number(20)  not null,
  dept_id 	number(20)  not null
);
alter TABLE  sys_role_dept add constraint pk_sys_role_dept primary key (role_id, dept_id);

COMMENT ON TABLE sys_role_dept IS '��ɫ�Ͳ��Ź�����';
COMMENT ON COLUMN sys_role_dept.role_id IS '��ɫID';
COMMENT ON COLUMN sys_role_dept.dept_id IS '����ID';

-- ----------------------------
-- Records of sys_role_dept
-- ----------------------------
INSERT INTO sys_role_dept VALUES ('2', '100');
INSERT INTO sys_role_dept VALUES ('2', '101');
INSERT INTO sys_role_dept VALUES ('2', '105');

-- ----------------------------
-- TABLE  structure forsys_role_menu 
-- ----------------------------
create TABLE  sys_role_menu (
  role_id 	number(20)  not null,
  menu_id 	number(20)  not null
);

alter TABLE  sys_role_menu add constraint pk_sys_role_menu primary key (role_id, menu_id);

COMMENT ON TABLE sys_role_menu IS '��ɫ�Ͳ˵�������';
COMMENT ON COLUMN sys_role_menu.role_id IS '��ɫID';
COMMENT ON COLUMN sys_role_menu.menu_id IS '�˵�ID';

-- ----------------------------
-- Records of sys_role_menu
-- ----------------------------
INSERT INTO sys_role_menu VALUES ('2', '1');
INSERT INTO sys_role_menu VALUES ('2', '2');
INSERT INTO sys_role_menu VALUES ('2', '3');
INSERT INTO sys_role_menu VALUES ('2', '4');
INSERT INTO sys_role_menu VALUES ('2', '100');
INSERT INTO sys_role_menu VALUES ('2', '101');
INSERT INTO sys_role_menu VALUES ('2', '102');
INSERT INTO sys_role_menu VALUES ('2', '103');
INSERT INTO sys_role_menu VALUES ('2', '104');
INSERT INTO sys_role_menu VALUES ('2', '105');
INSERT INTO sys_role_menu VALUES ('2', '106');
INSERT INTO sys_role_menu VALUES ('2', '107');
INSERT INTO sys_role_menu VALUES ('2', '108');
INSERT INTO sys_role_menu VALUES ('2', '109');
INSERT INTO sys_role_menu VALUES ('2', '110');
INSERT INTO sys_role_menu VALUES ('2', '111');
INSERT INTO sys_role_menu VALUES ('2', '112');
INSERT INTO sys_role_menu VALUES ('2', '113');
INSERT INTO sys_role_menu VALUES ('2', '114');
INSERT INTO sys_role_menu VALUES ('2', '115');
INSERT INTO sys_role_menu VALUES ('2', '116');
INSERT INTO sys_role_menu VALUES ('2', '500');
INSERT INTO sys_role_menu VALUES ('2', '501');
INSERT INTO sys_role_menu VALUES ('2', '1000');
INSERT INTO sys_role_menu VALUES ('2', '1001');
INSERT INTO sys_role_menu VALUES ('2', '1002');
INSERT INTO sys_role_menu VALUES ('2', '1003');
INSERT INTO sys_role_menu VALUES ('2', '1004');
INSERT INTO sys_role_menu VALUES ('2', '1005');
INSERT INTO sys_role_menu VALUES ('2', '1006');
INSERT INTO sys_role_menu VALUES ('2', '1007');
INSERT INTO sys_role_menu VALUES ('2', '1008');
INSERT INTO sys_role_menu VALUES ('2', '1009');
INSERT INTO sys_role_menu VALUES ('2', '1010');
INSERT INTO sys_role_menu VALUES ('2', '1011');
INSERT INTO sys_role_menu VALUES ('2', '1012');
INSERT INTO sys_role_menu VALUES ('2', '1013');
INSERT INTO sys_role_menu VALUES ('2', '1014');
INSERT INTO sys_role_menu VALUES ('2', '1015');
INSERT INTO sys_role_menu VALUES ('2', '1016');
INSERT INTO sys_role_menu VALUES ('2', '1017');
INSERT INTO sys_role_menu VALUES ('2', '1018');
INSERT INTO sys_role_menu VALUES ('2', '1019');
INSERT INTO sys_role_menu VALUES ('2', '1020');
INSERT INTO sys_role_menu VALUES ('2', '1021');
INSERT INTO sys_role_menu VALUES ('2', '1022');
INSERT INTO sys_role_menu VALUES ('2', '1023');
INSERT INTO sys_role_menu VALUES ('2', '1024');
INSERT INTO sys_role_menu VALUES ('2', '1025');
INSERT INTO sys_role_menu VALUES ('2', '1026');
INSERT INTO sys_role_menu VALUES ('2', '1027');
INSERT INTO sys_role_menu VALUES ('2', '1028');
INSERT INTO sys_role_menu VALUES ('2', '1029');
INSERT INTO sys_role_menu VALUES ('2', '1030');
INSERT INTO sys_role_menu VALUES ('2', '1031');
INSERT INTO sys_role_menu VALUES ('2', '1032');
INSERT INTO sys_role_menu VALUES ('2', '1033');
INSERT INTO sys_role_menu VALUES ('2', '1034');
INSERT INTO sys_role_menu VALUES ('2', '1035');
INSERT INTO sys_role_menu VALUES ('2', '1036');
INSERT INTO sys_role_menu VALUES ('2', '1037');
INSERT INTO sys_role_menu VALUES ('2', '1038');
INSERT INTO sys_role_menu VALUES ('2', '1039');
INSERT INTO sys_role_menu VALUES ('2', '1040');
INSERT INTO sys_role_menu VALUES ('2', '1041');
INSERT INTO sys_role_menu VALUES ('2', '1042');
INSERT INTO sys_role_menu VALUES ('2', '1043');
INSERT INTO sys_role_menu VALUES ('2', '1044');
INSERT INTO sys_role_menu VALUES ('2', '1045');
INSERT INTO sys_role_menu VALUES ('2', '1046');
INSERT INTO sys_role_menu VALUES ('2', '1047');
INSERT INTO sys_role_menu VALUES ('2', '1048');
INSERT INTO sys_role_menu VALUES ('2', '1049');
INSERT INTO sys_role_menu VALUES ('2', '1050');
INSERT INTO sys_role_menu VALUES ('2', '1051');
INSERT INTO sys_role_menu VALUES ('2', '1052');
INSERT INTO sys_role_menu VALUES ('2', '1053');
INSERT INTO sys_role_menu VALUES ('2', '1054');
INSERT INTO sys_role_menu VALUES ('2', '1055');
INSERT INTO sys_role_menu VALUES ('2', '1056');
INSERT INTO sys_role_menu VALUES ('2', '1057');
INSERT INTO sys_role_menu VALUES ('2', '1058');
INSERT INTO sys_role_menu VALUES ('2', '1059');
INSERT INTO sys_role_menu VALUES ('2', '1060');
INSERT INTO sys_role_menu VALUES ('2', '1061');
INSERT INTO sys_role_menu VALUES ('2', '1062');
INSERT INTO sys_role_menu VALUES ('2', '1063');
INSERT INTO sys_role_menu VALUES ('2', '1064');
INSERT INTO sys_role_menu VALUES ('2', '1065');
INSERT INTO sys_role_menu VALUES ('4', '1');
INSERT INTO sys_role_menu VALUES ('4', '2');
INSERT INTO sys_role_menu VALUES ('4', '100');
INSERT INTO sys_role_menu VALUES ('4', '101');
INSERT INTO sys_role_menu VALUES ('4', '102');
INSERT INTO sys_role_menu VALUES ('4', '103');
INSERT INTO sys_role_menu VALUES ('4', '104');
INSERT INTO sys_role_menu VALUES ('4', '105');
INSERT INTO sys_role_menu VALUES ('4', '106');
INSERT INTO sys_role_menu VALUES ('4', '107');
INSERT INTO sys_role_menu VALUES ('4', '108');
INSERT INTO sys_role_menu VALUES ('4', '109');
INSERT INTO sys_role_menu VALUES ('4', '500');
INSERT INTO sys_role_menu VALUES ('4', '501');
INSERT INTO sys_role_menu VALUES ('4', '1001');
INSERT INTO sys_role_menu VALUES ('4', '1002');
INSERT INTO sys_role_menu VALUES ('4', '1003');
INSERT INTO sys_role_menu VALUES ('4', '1004');
INSERT INTO sys_role_menu VALUES ('4', '1005');
INSERT INTO sys_role_menu VALUES ('4', '1006');
INSERT INTO sys_role_menu VALUES ('4', '1007');
INSERT INTO sys_role_menu VALUES ('4', '1008');
INSERT INTO sys_role_menu VALUES ('4', '1009');
INSERT INTO sys_role_menu VALUES ('4', '1010');
INSERT INTO sys_role_menu VALUES ('4', '1011');
INSERT INTO sys_role_menu VALUES ('4', '1012');
INSERT INTO sys_role_menu VALUES ('4', '1013');
INSERT INTO sys_role_menu VALUES ('4', '1014');
INSERT INTO sys_role_menu VALUES ('4', '1015');
INSERT INTO sys_role_menu VALUES ('4', '1016');
INSERT INTO sys_role_menu VALUES ('4', '1017');
INSERT INTO sys_role_menu VALUES ('4', '1018');
INSERT INTO sys_role_menu VALUES ('4', '1019');
INSERT INTO sys_role_menu VALUES ('4', '1020');
INSERT INTO sys_role_menu VALUES ('4', '1021');
INSERT INTO sys_role_menu VALUES ('4', '1022');
INSERT INTO sys_role_menu VALUES ('4', '1023');
INSERT INTO sys_role_menu VALUES ('4', '1024');
INSERT INTO sys_role_menu VALUES ('4', '1025');
INSERT INTO sys_role_menu VALUES ('4', '1026');
INSERT INTO sys_role_menu VALUES ('4', '1027');
INSERT INTO sys_role_menu VALUES ('4', '1028');
INSERT INTO sys_role_menu VALUES ('4', '1029');
INSERT INTO sys_role_menu VALUES ('4', '1030');
INSERT INTO sys_role_menu VALUES ('4', '1031');
INSERT INTO sys_role_menu VALUES ('4', '1032');
INSERT INTO sys_role_menu VALUES ('4', '1033');
INSERT INTO sys_role_menu VALUES ('4', '1034');
INSERT INTO sys_role_menu VALUES ('4', '1035');
INSERT INTO sys_role_menu VALUES ('4', '1041');
INSERT INTO sys_role_menu VALUES ('4', '1042');
INSERT INTO sys_role_menu VALUES ('4', '1043');
INSERT INTO sys_role_menu VALUES ('4', '1044');
INSERT INTO sys_role_menu VALUES ('4', '1045');
INSERT INTO sys_role_menu VALUES ('4', '1046');
INSERT INTO sys_role_menu VALUES ('4', '1047');
INSERT INTO sys_role_menu VALUES ('4', '1048');
INSERT INTO sys_role_menu VALUES ('4', '1049');
INSERT INTO sys_role_menu VALUES ('4', '1050');
INSERT INTO sys_role_menu VALUES ('4', '1051');
INSERT INTO sys_role_menu VALUES ('4', '1052');
INSERT INTO sys_role_menu VALUES ('4', '1053');
INSERT INTO sys_role_menu VALUES ('26', '1071');

-- ----------------------------
-- TABLE  structure forsys_user 
-- ----------------------------
create sequence seq_sys_user
 increment by 1
 start with 100
 nomaxvalue
 nominvalue
 cache 20;
create TABLE  sys_user (
  user_id           number(20)      not null,
  dept_id           number(20)      default null,
  
  user_name         varchar2(30)    default '',
  nick_name varchar2(30) not null,
  user_type         varchar2(2)     default '00',
  email             varchar2(50)    default '',
  phonenumber       varchar2(11)    default '',
  sex               char(1)         default '0',
  avatar            varchar2(100)   default '',
  password          varchar2(100)    default '',
  salt              varchar2(20)    default '',
  status            char(1)         default '0',
  del_flag          char(1)         default '0',
  login_ip          varchar2(50)    default '',
  login_date        date,
  create_by         varchar2(64),
  create_time 	    date,
  update_by         varchar2(64)    default '',
  update_time       date,
  remark            varchar2(500)   default ''
);

alter TABLE  sys_user add constraint pk_sys_user primary key (user_id);

COMMENT ON TABLE sys_user IS '�û���Ϣ��';
COMMENT ON COLUMN sys_user.user_id IS '�û�ID';
COMMENT ON COLUMN sys_user.dept_id IS '����ID';
COMMENT ON COLUMN sys_user.user_name IS '�û��˺�';
COMMENT ON COLUMN sys_user.nick_name IS '�û��ǳ�';
COMMENT ON COLUMN sys_user.user_type IS '�û����ͣ�00ϵͳ�û���';
COMMENT ON COLUMN sys_user.email IS '�û�����';
COMMENT ON COLUMN sys_user.phonenumber IS '�ֻ�����';
COMMENT ON COLUMN sys_user.sex IS '�û��Ա�0�� 1Ů 2δ֪��';
COMMENT ON COLUMN sys_user.avatar IS 'ͷ���ַ';
COMMENT ON COLUMN sys_user.password IS '����';
comment on COLUMN  sys_user.salt         is '�μ���';
COMMENT ON COLUMN sys_user.status IS '�ʺ�״̬��0���� 1ͣ�ã�';
COMMENT ON COLUMN sys_user.del_flag IS 'ɾ����־��0������� 2����ɾ����';
COMMENT ON COLUMN sys_user.login_ip IS '����¼IP';
COMMENT ON COLUMN sys_user.login_date IS '����¼ʱ��';
COMMENT ON COLUMN sys_user.create_by IS '������';
COMMENT ON COLUMN sys_user.create_time IS '����ʱ��';
COMMENT ON COLUMN sys_user.update_by IS '������';
COMMENT ON COLUMN sys_user.update_time IS '����ʱ��';
COMMENT ON COLUMN sys_user.remark IS '��ע';

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO sys_user VALUES ('1', '103', 'admin', '����', '00', 'ry@163.com', '15888888888', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', null, '0', '0', '127.0.0.1', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'admin', TO_TIMESTAMP('2020-11-12 15:05:26', 'YYYY-MM-DD HH24:MI:SS'), '����Ա');
INSERT INTO sys_user VALUES ('2', '105', 'ry', '3423423', '00', 'abc@qq.com', '15666633332', '1', '', '$2a$10$gA1fpBALTbmVgyfUZb08Fe8djTkH6Cob3kmCQQjjs9phGObPsuFFO', null, '0', '0', '127.0.0.1', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'admin', TO_TIMESTAMP('2018-03-16 11:33:00', 'YYYY-MM-DD HH24:MI:SS'), 'admin', TO_TIMESTAMP('2020-11-03 10:44:05', 'YYYY-MM-DD HH24:MI:SS'), '����Ա');
INSERT INTO sys_user VALUES ('3', '109', 'joran', 'joran', '00', '18620306152@qq.com', '18620306152', '1', '', '$2a$10$VtJlGJbkbA/0.iUWrv1bmOwtsz8/5mshi/sHn9vvUCwQUlX90xvfS', null, '1', '0', '', null, 'admin', TO_TIMESTAMP('2020-10-19 15:05:04', 'YYYY-MM-DD HH24:MI:SS'), 'admin', TO_TIMESTAMP('2020-10-29 14:10:02', 'YYYY-MM-DD HH24:MI:SS'), '111');
INSERT INTO sys_user VALUES ('6', '100', 'tester02', '����02', '00', 'test02@qq.com', '13100000002', '0', '', '$2a$10$THAZFuKZfHsuq45QaWzrhO9K6fmXpfEJDXmC/23Yd00HGnnYeqiCq', null, '0', '0', '', null, 'admin', TO_TIMESTAMP('2020-10-21 10:34:23', 'YYYY-MM-DD HH24:MI:SS'), 'admin', TO_TIMESTAMP('2020-11-02 17:49:11', 'YYYY-MM-DD HH24:MI:SS'), 'auto test');
INSERT INTO sys_user VALUES ('15', null, 'tester900', '����Ա900', '00', 'tester900@qq.com', '19999999999', '0', '', '$2a$10$DPbAVHBo39EElHsvkZXw/.9NxB85S6oDhunAUOCxf7sdpvKfHQXfS', null, '0', '0', '', null, 'admin', TO_TIMESTAMP('2020-11-02 16:25:37', 'YYYY-MM-DD HH24:MI:SS'), '', null, 'auto test');

-- ----------------------------
-- TABLE  structure forsys_user_post 
-- ----------------------------
create TABLE  sys_user_post
(
	user_id number(20)  not null,
	post_id number(20)  not null
);

alter TABLE  sys_user_post add constraint pk_sys_user_post primary key (user_id, post_id);

COMMENT ON TABLE sys_user_post IS '�û����λ������';
COMMENT ON COLUMN sys_user_post.user_id IS '�û�ID';
COMMENT ON COLUMN sys_user_post.post_id IS '��λID';

-- ----------------------------
-- Records of sys_user_post
-- ----------------------------
INSERT INTO sys_user_post VALUES ('1', '2');
INSERT INTO sys_user_post VALUES ('11', '4');
INSERT INTO sys_user_post VALUES ('13', '2');

-- ----------------------------
-- TABLE  structure forsys_user_role 
-- ----------------------------
create TABLE  sys_user_role (
  user_id 	number(20)  not null,
  role_id 	number(20)  not null
);

alter TABLE  sys_user_role add constraint pk_sys_user_role primary key (user_id, role_id);

COMMENT ON TABLE sys_user_role IS '�û��ͽ�ɫ������';
COMMENT ON COLUMN sys_user_role.user_id IS '�û�ID';
COMMENT ON COLUMN sys_user_role.role_id IS '��ɫID';

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
INSERT INTO sys_user_role VALUES ('2', '2');
INSERT INTO sys_user_role VALUES ('11', '4');
INSERT INTO sys_user_role VALUES ('12', '4');
INSERT INTO sys_user_role VALUES ('13', '2');


-- ----------------------------
-- ���� ������mysql��find_in_set
-- ���磺 select * from sys_dept where FIND_IN_SET (101,ancestors) <> 0
-- mysql�ɽ���0������number��Ϊwhere ������oracleֻ���ܱ��ʽ��Ϊwhere ����
-- ----------------------------
create or replace function find_in_set(arg1 in varchar2,arg2 in varchar)
return number is Result number;
begin
select instr(','||arg2||',' , ','||arg1||',') into Result from dual;
return(Result);
end find_in_set;