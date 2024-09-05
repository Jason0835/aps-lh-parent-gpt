prompt PL/SQL Developer import file
prompt Created on 2020年11月26日 by lbn
set feedback off
set define off
prompt Disabling triggers for SYS_CONFIG...
alter table SYS_CONFIG disable all triggers;
prompt Disabling triggers for SYS_DEPT...
alter table SYS_DEPT disable all triggers;
prompt Disabling triggers for SYS_DICT_DATA...
alter table SYS_DICT_DATA disable all triggers;
prompt Disabling triggers for SYS_DICT_TYPE...
alter table SYS_DICT_TYPE disable all triggers;
prompt Disabling triggers for SYS_MENU...
alter table SYS_MENU disable all triggers;
prompt Disabling triggers for SYS_NOTICE...
alter table SYS_NOTICE disable all triggers;
prompt Disabling triggers for SYS_POST...
alter table SYS_POST disable all triggers;
prompt Disabling triggers for SYS_ROLE...
alter table SYS_ROLE disable all triggers;
prompt Disabling triggers for SYS_ROLE_DEPT...
alter table SYS_ROLE_DEPT disable all triggers;
prompt Disabling triggers for SYS_ROLE_MENU...
alter table SYS_ROLE_MENU disable all triggers;
prompt Disabling triggers for SYS_USER...
alter table SYS_USER disable all triggers;
prompt Disabling triggers for SYS_USER_POST...
alter table SYS_USER_POST disable all triggers;
prompt Disabling triggers for SYS_USER_ROLE...
alter table SYS_USER_ROLE disable all triggers;
prompt Deleting SYS_USER_ROLE...
delete from SYS_USER_ROLE;
commit;
prompt Deleting SYS_USER_POST...
delete from SYS_USER_POST;
commit;
prompt Deleting SYS_USER...
delete from SYS_USER;
commit;
prompt Deleting SYS_ROLE_MENU...
delete from SYS_ROLE_MENU;
commit;
prompt Deleting SYS_ROLE_DEPT...
delete from SYS_ROLE_DEPT;
commit;
prompt Deleting SYS_ROLE...
delete from SYS_ROLE;
commit;
prompt Deleting SYS_POST...
delete from SYS_POST;
commit;
prompt Deleting SYS_NOTICE...
delete from SYS_NOTICE;
commit;
prompt Deleting SYS_MENU...
delete from SYS_MENU;
commit;
prompt Deleting SYS_DICT_TYPE...
delete from SYS_DICT_TYPE;
commit;
prompt Deleting SYS_DICT_DATA...
delete from SYS_DICT_DATA;
commit;
prompt Deleting SYS_DEPT...
delete from SYS_DEPT;
commit;
prompt Deleting SYS_CONFIG...
delete from SYS_CONFIG;
commit;
prompt Loading SYS_CONFIG...
insert into SYS_CONFIG (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
values (1, '主框架页-默认皮肤样式名称', 'sys.index.skinName', 'skin-blue', 'Y', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '蓝色 skin-blue、绿色 skin-green、紫色 skin-purple、红色 skin-red、黄色 skin-yellow');
insert into SYS_CONFIG (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
values (2, '用户管理-账号初始密码', 'sys.user.initPassword', '123456', 'Y', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '初始化密码 123456');
insert into SYS_CONFIG (config_id, config_name, config_key, config_value, config_type, create_by, create_time, update_by, update_time, remark)
values (3, '主框架页-侧边栏主题', 'sys.index.sideTheme', 'theme-dark', 'Y', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '深色主题theme-dark，浅色主题theme-light');
commit;
prompt 3 records loaded
prompt Loading SYS_DEPT...
insert into SYS_DEPT (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time, update_by, update_time, lang_json)
values (200, 100, '0,100', 'ora', 123, '11', '18620306152', '18620306155@qq.com', '0', '0', 'admin', to_date('13-11-2020 14:20:05', 'dd-mm-yyyy hh24:mi:ss'), 'admin', to_date('13-11-2020 14:20:12', 'dd-mm-yyyy hh24:mi:ss'), '[{"zh_CN":"ora","en_US":"en_USora"}]');
insert into SYS_DEPT (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time, update_by, update_time, lang_json)
values (100, 0, '0', '若依科技', 0, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'admin', to_date('13-11-2020 14:20:12', 'dd-mm-yyyy hh24:mi:ss'), '[{"zh_CN":"若依科技","en_US":"en_US若依科技"}]');
insert into SYS_DEPT (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time, update_by, update_time, lang_json)
values (101, 100, '0,100', '深圳总公司', 1, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'admin', to_date('13-11-2020 11:05:55', 'dd-mm-yyyy hh24:mi:ss'), '[{"zh_CN":"深圳总公司","en_US":"en_US深圳总公司"}]');
insert into SYS_DEPT (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time, update_by, update_time, lang_json)
values (102, 100, '0,100', '长沙分公司', 2, '若依', '15888888888', 'ry@qq.com', '1', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'admin', to_date('30-10-2020 15:38:09', 'dd-mm-yyyy hh24:mi:ss'), '[{"zh_CN":"长沙分公司","en_US":"en_US长沙分公司"}]');
insert into SYS_DEPT (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time, update_by, update_time, lang_json)
values (103, 101, '0,100,101', '研发部门2', 1, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'admin', to_date('13-11-2020 11:05:55', 'dd-mm-yyyy hh24:mi:ss'), '[{"zh_CN":"研发部门2","en_US":"en_US研发部门2"}]');
insert into SYS_DEPT (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time, update_by, update_time, lang_json)
values (104, 101, '0,100,101', '市场部门', 2, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '[{"zh_CN":"市场部门","en_US":"en_US市场部门"}]');
insert into SYS_DEPT (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time, update_by, update_time, lang_json)
values (105, 101, '0,100,101', '测试部门', 3, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '[{"zh_CN":"测试部门","en_US":"en_US测试部门"}]');
insert into SYS_DEPT (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time, update_by, update_time, lang_json)
values (106, 101, '0,100,101', '财务部门', 4, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '[{"zh_CN":"财务部门","en_US":"en_US财务部门"}]');
insert into SYS_DEPT (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time, update_by, update_time, lang_json)
values (107, 101, '0,100,101', '运维部门', 5, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '[{"zh_CN":"运维部门","en_US":"en_US运维部门"}]');
insert into SYS_DEPT (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time, update_by, update_time, lang_json)
values (108, 102, '0,100,102', '市场部门', 1, '若依', '15888888888', 'ry@qq.com', '1', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'admin', to_date('30-10-2020 15:38:05', 'dd-mm-yyyy hh24:mi:ss'), '[{"zh_CN":"市场部门","en_US":"en_US市场部门"}]');
insert into SYS_DEPT (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time, update_by, update_time, lang_json)
values (109, 102, '0,100,102', '财务部门', 2, '若依', '15888888888', 'ry@qq.com', '1', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'admin', to_date('30-10-2020 15:37:55', 'dd-mm-yyyy hh24:mi:ss'), '[{"zh_CN":"财务部门","en_US":"en_US财务部门"}]');
insert into SYS_DEPT (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time, update_by, update_time, lang_json)
values (111, 100, '0,100', '测试部门', 99, '负责人负责人负责人负责人负责人负责人负责', '15711111111', 'ABC@QQ.COM', '0', '0', 'admin', to_date('29-10-2020 17:09:24', 'dd-mm-yyyy hh24:mi:ss'), 'admin', to_date('29-10-2020 17:47:46', 'dd-mm-yyyy hh24:mi:ss'), '[{"zh_CN":"测试部门","en_US":"en_US测试部门"}]');
insert into SYS_DEPT (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time, update_by, update_time, lang_json)
values (112, 100, '0,100', '研发部门', 88, null, null, null, '0', '0', 'admin', to_date('29-10-2020 17:22:55', 'dd-mm-yyyy hh24:mi:ss'), null, null, '[{"zh_CN":"研发部门","en_US":"en_US研发部门"}]');
insert into SYS_DEPT (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time, update_by, update_time, lang_json)
values (201, 200, '0,100,200', 'ora-test', 123, '111', '13232321321', '18620306155@qq.com', '0', '2', 'admin', to_date('13-11-2020 14:20:35', 'dd-mm-yyyy hh24:mi:ss'), null, null, '[{"zh_CN":"ora-test","en_US":"en_USora-test"}]');
commit;
prompt 14 records loaded
prompt Loading SYS_DICT_DATA...
insert into SYS_DICT_DATA (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, locale)
values (123, 1, 'Add', '1', 'sys_oper_type', null, 'info', 'Y', '0', 'admin', to_date('20-11-2020 12:30:45', 'dd-mm-yyyy hh24:mi:ss'), 'admin', to_date('20-11-2020 12:31:01', 'dd-mm-yyyy hh24:mi:ss'), null, 'en_US');
insert into SYS_DICT_DATA (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, locale)
values (1, 1, '男', '0', 'sys_user_sex', null, null, 'Y', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '性别男', 'zh_CN');
insert into SYS_DICT_DATA (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, locale)
values (2, 2, '女', '1', 'sys_user_sex', null, null, 'N', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '性别女', 'zh_CN');
insert into SYS_DICT_DATA (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, locale)
values (3, 3, '未知', '2', 'sys_user_sex', null, null, 'N', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '性别未知', 'zh_CN');
insert into SYS_DICT_DATA (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, locale)
values (4, 1, '显示', '0', 'sys_show_hide', null, 'primary', 'Y', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '显示菜单', 'zh_CN');
insert into SYS_DICT_DATA (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, locale)
values (5, 2, '隐藏', '1', 'sys_show_hide', null, 'danger', 'N', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '隐藏菜单', 'zh_CN');
insert into SYS_DICT_DATA (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, locale)
values (6, 1, '正常', '0', 'sys_normal_disable', null, 'primary', 'Y', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '正常状态', 'zh_CN');
insert into SYS_DICT_DATA (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, locale)
values (7, 2, '停用', '1', 'sys_normal_disable', null, 'danger', 'N', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '停用状态', 'zh_CN');
insert into SYS_DICT_DATA (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, locale)
values (8, 1, '正常', '0', 'sys_job_status', null, 'primary', 'Y', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '正常状态', 'zh_CN');
insert into SYS_DICT_DATA (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, locale)
values (9, 2, '暂停', '1', 'sys_job_status', null, 'danger', 'N', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '停用状态', 'zh_CN');
insert into SYS_DICT_DATA (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, locale)
values (10, 1, '默认', 'DEFAULT', 'sys_job_group', null, null, 'Y', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '默认分组', 'zh_CN');
insert into SYS_DICT_DATA (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, locale)
values (11, 2, '系统', 'SYSTEM', 'sys_job_group', null, null, 'N', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '系统分组', 'zh_CN');
insert into SYS_DICT_DATA (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, locale)
values (12, 1, '是', 'Y', 'sys_yes_no', null, 'primary', 'Y', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '系统默认是', 'zh_CN');
insert into SYS_DICT_DATA (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, locale)
values (13, 2, '否', 'N', 'sys_yes_no', null, 'danger', 'N', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '系统默认否', 'zh_CN');
insert into SYS_DICT_DATA (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, locale)
values (14, 1, '通知', '1', 'sys_notice_type', null, 'warning', 'Y', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '通知', 'zh_CN');
insert into SYS_DICT_DATA (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, locale)
values (15, 2, '公告', '2', 'sys_notice_type', null, 'success', 'N', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '公告', 'zh_CN');
insert into SYS_DICT_DATA (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, locale)
values (16, 1, '正常', '0', 'sys_notice_status', null, 'primary', 'Y', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '正常状态', 'zh_CN');
insert into SYS_DICT_DATA (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, locale)
values (17, 2, '关闭', '1', 'sys_notice_status', null, 'danger', 'N', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '关闭状态', 'zh_CN');
insert into SYS_DICT_DATA (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, locale)
values (18, 1, '新增', '1', 'sys_oper_type', null, 'info', 'N', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '新增操作', 'zh_CN');
insert into SYS_DICT_DATA (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, locale)
values (19, 2, '修改', '2', 'sys_oper_type', null, 'info', 'N', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '修改操作', 'zh_CN');
insert into SYS_DICT_DATA (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, locale)
values (20, 3, '删除', '3', 'sys_oper_type', null, 'danger', 'N', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '删除操作', 'zh_CN');
insert into SYS_DICT_DATA (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, locale)
values (21, 4, '授权', '4', 'sys_oper_type', null, 'primary', 'N', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '授权操作', 'zh_CN');
insert into SYS_DICT_DATA (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, locale)
values (22, 5, '导出', '5', 'sys_oper_type', null, 'warning', 'N', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '导出操作', 'zh_CN');
insert into SYS_DICT_DATA (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, locale)
values (23, 6, '导入', '6', 'sys_oper_type', null, 'warning', 'N', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '导入操作', 'zh_CN');
insert into SYS_DICT_DATA (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, locale)
values (24, 7, '强退', '7', 'sys_oper_type', null, 'danger', 'N', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '强退操作', 'zh_CN');
insert into SYS_DICT_DATA (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, locale)
values (25, 8, '生成代码', '8', 'sys_oper_type', null, 'warning', 'N', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '生成操作', 'zh_CN');
insert into SYS_DICT_DATA (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, locale)
values (26, 9, '清空数据', '9', 'sys_oper_type', null, 'danger', 'N', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '清空操作', 'zh_CN');
insert into SYS_DICT_DATA (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, locale)
values (27, 1, '成功', '0', 'sys_common_status', null, 'primary', 'N', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '正常状态', 'zh_CN');
insert into SYS_DICT_DATA (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, locale)
values (28, 2, '失败', '1', 'sys_common_status', null, 'danger', 'N', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '停用状态', 'zh_CN');
insert into SYS_DICT_DATA (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, locale)
values (29, 1, '授权码模式', 'authorization_code', 'sys_grant_type', null, null, 'N', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '授权码模式', 'zh_CN');
insert into SYS_DICT_DATA (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, locale)
values (30, 2, '密码模式', 'password', 'sys_grant_type', null, null, 'N', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '密码模式', 'zh_CN');
insert into SYS_DICT_DATA (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, locale)
values (31, 3, '客户端模式', 'client_credentials', 'sys_grant_type', null, null, 'N', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '客户端模式', 'zh_CN');
insert into SYS_DICT_DATA (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, locale)
values (32, 4, '简化模式', 'implicit', 'sys_grant_type', null, null, 'N', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '简化模式', 'zh_CN');
insert into SYS_DICT_DATA (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, locale)
values (33, 5, '刷新模式', 'refresh_token', 'sys_grant_type', null, null, 'N', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '刷新模式', 'zh_CN');
insert into SYS_DICT_DATA (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, locale)
values (36, 0, '1', '1', 'te', null, null, 'N', '0', 'admin', to_date('26-10-2020 10:04:34', 'dd-mm-yyyy hh24:mi:ss'), null, null, null, 'zh_CN');
insert into SYS_DICT_DATA (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, locale)
values (37, 1, 'Success', '0', 'sys_common_status', null, 'primary', 'N', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'admin', to_date('18-11-2020 17:44:11', 'dd-mm-yyyy hh24:mi:ss'), 'Succ', 'en_US');
insert into SYS_DICT_DATA (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, locale)
values (120, 0, 'male', '0', 'sys_user_sex', null, null, 'Y', '0', 'admin', to_date('19-11-2020 15:24:17', 'dd-mm-yyyy hh24:mi:ss'), 'admin', to_date('19-11-2020 15:25:52', 'dd-mm-yyyy hh24:mi:ss'), null, 'en_US');
insert into SYS_DICT_DATA (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, locale)
values (121, 2, 'female', '1', 'sys_user_sex', null, null, 'Y', '0', 'admin', to_date('19-11-2020 15:25:41', 'dd-mm-yyyy hh24:mi:ss'), 'admin', to_date('19-11-2020 16:33:32', 'dd-mm-yyyy hh24:mi:ss'), null, 'en_US');
insert into SYS_DICT_DATA (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, locale)
values (122, 3, 'unknown', '2', 'sys_user_sex', null, null, 'Y', '0', 'admin', to_date('19-11-2020 15:26:20', 'dd-mm-yyyy hh24:mi:ss'), 'admin', to_date('19-11-2020 16:33:36', 'dd-mm-yyyy hh24:mi:ss'), null, 'en_US');
insert into SYS_DICT_DATA (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, locale)
values (39, 2, 'Disable', '1', 'sys_normal_disable', null, 'danger', 'N', '0', null, null, null, null, null, 'en_US');
insert into SYS_DICT_DATA (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, locale)
values (38, 1, 'Enable', '0', 'sys_normal_disable', null, 'primary', 'Y', '0', null, null, null, null, null, 'en_US');
insert into SYS_DICT_DATA (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, locale)
values (100, 1, '成功', '1', 'test-dict-type', '1', 'primary', 'Y', '0', 'admin', to_date('13-11-2020 14:41:07', 'dd-mm-yyyy hh24:mi:ss'), null, null, '11', 'zh_CN');
insert into SYS_DICT_DATA (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, update_by, update_time, remark, locale)
values (101, 2, '失败', '2', 'test-dict-type', '22', 'info', 'Y', '0', 'admin', to_date('13-11-2020 14:41:22', 'dd-mm-yyyy hh24:mi:ss'), 'admin', to_date('13-11-2020 14:41:28', 'dd-mm-yyyy hh24:mi:ss'), '22', 'zh_CN');
commit;
prompt 43 records loaded
prompt Loading SYS_DICT_TYPE...
insert into SYS_DICT_TYPE (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
values (100, '测试字典', 'test-dict-type', '0', 'admin', to_date('13-11-2020 14:24:18', 'dd-mm-yyyy hh24:mi:ss'), null, null, '1');
insert into SYS_DICT_TYPE (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
values (1, '用户性别', 'sys_user_sex', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '用户性别列表');
insert into SYS_DICT_TYPE (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
values (2, '菜单状态', 'sys_show_hide', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '菜单状态列表');
insert into SYS_DICT_TYPE (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
values (3, '系统开关', 'sys_normal_disable', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '系统开关列表');
insert into SYS_DICT_TYPE (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
values (4, '任务状态', 'sys_job_status', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '任务状态列表');
insert into SYS_DICT_TYPE (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
values (5, '任务分组', 'sys_job_group', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '任务分组列表');
insert into SYS_DICT_TYPE (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
values (6, '系统是否', 'sys_yes_no', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '系统是否列表');
insert into SYS_DICT_TYPE (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
values (7, '通知类型', 'sys_notice_type', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '通知类型列表');
insert into SYS_DICT_TYPE (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
values (8, '通知状态', 'sys_notice_status', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '通知状态列表');
insert into SYS_DICT_TYPE (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
values (9, '操作类型', 'sys_oper_type', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '操作类型列表');
insert into SYS_DICT_TYPE (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
values (10, '系统状态', 'sys_common_status', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '登录状态列表');
insert into SYS_DICT_TYPE (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
values (11, '授权类型', 'sys_grant_type', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '授权类型列表');
insert into SYS_DICT_TYPE (dict_id, dict_name, dict_type, status, create_by, create_time, update_by, update_time, remark)
values (13, 'te', 'te', '0', 'admin', to_date('26-10-2020 10:04:23', 'dd-mm-yyyy hh24:mi:ss'), null, null, null);
commit;
prompt 13 records loaded
prompt Loading SYS_MENU...
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (2010, 'Sentinel', 2, 5, null, null, 'http://192.168.100.126:8718', null, 1, 0, 'C', '0', '0', null, null, '#', '#', 'admin', to_date('17-11-2020 14:50:27', 'dd-mm-yyyy hh24:mi:ss'), 'admin', to_date('17-11-2020 14:53:21', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"Sentinel","en_US":"en_USSentinel"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (2009, 'nacos', 2, 4, 'menuBlank', null, 'http://192.168.2.93:8848/nacos', null, 1, 0, 'C', '0', '0', null, null, '#', '#', 'admin', to_date('17-11-2020 14:49:48', 'dd-mm-yyyy hh24:mi:ss'), 'admin', to_date('17-11-2020 15:28:34', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"nacos","en_US":"en_USnacos"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (2, '系统监控', 0, 2, null, 'monitor', '#', null, 1, 0, 'M', '0', '0', null, 'monitor', 'monitor', 'fa fa-video-camera', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '系统监控目录', '[{"zh_CN":"系统监控","en_US":"en_US系统监控"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (3, '系统工具', 0, 3, null, 'tool', '#', null, 1, 0, 'M', '0', '0', null, 'tool', 'tool', 'fa fa-bars', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '系统工具目录', '[{"zh_CN":"系统工具","en_US":"en_US系统工具"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (100, '用户管理', 1, 1, null, 'user', '/system/user', 'system/user/index', 1, 0, 'C', '0', '0', 'system:user:list', 'system:user:view', 'user', 'fa fa-user-o', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '用户管理菜单', '[{"zh_CN":"用户管理","en_US":"en_US用户管理"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (101, '角色管理', 1, 2, null, 'role', '/system/role', 'system/role/index', 1, 0, 'C', '0', '0', 'system:role:list', 'system:role:view', 'peoples', 'fa fa-user-secret', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '角色管理菜单', '[{"zh_CN":"角色管理","en_US":"en_US角色管理"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (102, '菜单管理', 1, 3, null, 'menu', '/system/menu', 'system/menu/index', 1, 0, 'C', '0', '0', 'system:menu:list', 'system:menu:view', 'tree-TABLE ', 'fa fa-th-list', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '菜单管理菜单', '[{"zh_CN":"菜单管理","en_US":"en_US菜单管理"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (104, '岗位管理', 1, 5, null, 'post', '/system/post', 'system/post/index', 1, 0, 'C', '0', '0', 'system:post:list', 'system:post:view', 'post', 'fa fa-address-card-o', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '岗位管理菜单', '[{"zh_CN":"岗位管理","en_US":"en_US岗位管理"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (105, '字典管理', 1, 6, null, 'dict', '/system/dict', 'system/dict/index', 1, 0, 'C', '0', '0', 'system:dict:list', 'system:dict:view', 'dict', 'fa fa-bookmark-o', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '字典管理菜单', '[{"zh_CN":"字典管理","en_US":"en_US字典管理"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (106, '参数设置', 1, 7, null, 'config', '/system/config', 'system/config/index', 1, 0, 'C', '0', '0', 'system:config:list', 'system:config:view', 'edit', 'fa fa-sun-o', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '参数设置菜单', '[{"zh_CN":"参数设置","en_US":"en_US参数设置"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (107, '通知公告', 1, 9, null, 'notice', '/system/notice', 'system/notice/index', 1, 0, 'C', '0', '0', 'system:notice:list', 'system:notice:view', 'message', 'fa fa-bullhorn', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '通知公告菜单', '[{"zh_CN":"通知公告","en_US":"en_US通知公告"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (108, '日志管理', 1, 10, null, 'log', '#', 'system/log/index', 1, 0, 'M', '0', '0', null, 'log', 'log', 'fa fa-pencil-square-o', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '日志管理菜单', '[{"zh_CN":"日志管理","en_US":"en_US日志管理"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (109, '在线用户', 2, 1, null, 'online', '/monitor/online', 'monitor/online/index', 1, 0, 'C', '0', '0', 'monitor:online:list', 'monitor:online:view', 'online', 'fa fa-user-circle', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '在线用户菜单', '[{"zh_CN":"在线用户","en_US":"en_US在线用户"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (110, '定时任务', 2, 2, null, 'job', '/monitor/job', 'monitor/job/index', 1, 0, 'C', '0', '0', 'monitor:job:list', 'monitor:job:view', 'job', 'fa fa-tasks', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '定时任务菜单', '[{"zh_CN":"定时任务","en_US":"en_US定时任务"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (112, '前端服务器监控', 2, 4, null, 'http://192.168.2.93:8848/nacos', '/monitor/server', null, 1, 0, 'C', '0', '0', 'monitor:nacos:list', 'monitor:server:view', 'nacos', 'fa fa-server', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'admin', to_date('17-11-2020 14:47:29', 'dd-mm-yyyy hh24:mi:ss'), '服务治理菜单', '[{"zh_CN":"前端服务器监控","en_US":"en_US前端服务器监控"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (113, 'Admin控制台', 2, 5, null, 'http://localhost:9100/login', 'http://localhost:9100/login', null, 1, 0, 'C', '0', '0', 'monitor:server:list', 'tool:build:view', 'server', 'fa fa-wpforms', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'admin', to_date('17-11-2020 14:52:36', 'dd-mm-yyyy hh24:mi:ss'), '服务监控菜单', '[{"zh_CN":"Admin控制台","en_US":"en_USAdmin控制台"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (114, '表单构建', 3, 1, null, 'build', '/tool/build', 'tool/build/index', 1, 0, 'C', '0', '0', 'tool:build:list', 'tool:build:view', 'build', 'fa fa-wpforms', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '表单构建菜单', '[{"zh_CN":"表单构建","en_US":"en_US表单构建"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (115, '代码生成', 3, 2, null, 'gen', '/tool/gen', 'tool/gen/index', 1, 0, 'C', '0', '0', 'tool:gen:list', 'tool:gen:view', 'code', 'fa fa-code', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '代码生成菜单', '[{"zh_CN":"代码生成","en_US":"en_US代码生成"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (116, '系统接口', 3, 3, null, 'http://192.168.100.126:8080/swagger-ui.html', '/tool/swagger', null, 1, 0, 'C', '0', '0', 'tool:swagger:list', 'tool:swagger:view', 'swagger', 'fa fa-gg', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'admin', to_date('27-10-2020 09:53:29', 'dd-mm-yyyy hh24:mi:ss'), '系统接口菜单', '[{"zh_CN":"系统接口","en_US":"en_US系统接口"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (500, '操作日志', 108, 1, null, 'operlog', '/monitor/operlog', 'system/operlog/index', 1, 0, 'C', '0', '0', 'system:operlog:list', 'monitor:operlog:view', 'form', 'fa fa-address-book', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '操作日志菜单', '[{"zh_CN":"操作日志","en_US":"en_US操作日志"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (501, '登录日志', 108, 2, null, 'logininfor', '/monitor/logininfor', 'system/logininfor/index', 1, 0, 'C', '0', '0', 'system:logininfor:list', 'monitor:logininfor:view', 'logininfor', 'fa fa-file-image-o', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '登录日志菜单', '[{"zh_CN":"登录日志","en_US":"en_US登录日志"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1002, '用户新增', 100, 2, null, null, '#', null, 1, 0, 'F', '0', '0', 'system:user:add', 'system:user:add', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"用户新增","en_US":"en_US用户新增"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1003, '用户修改', 100, 3, null, null, '#', null, 1, 0, 'F', '0', '0', 'system:user:edit', 'system:user:edit', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"用户修改","en_US":"en_US用户修改"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1004, '用户删除', 100, 4, null, null, '#', null, 1, 0, 'F', '0', '0', 'system:user:remove', 'system:user:remove', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"用户删除","en_US":"en_US用户删除"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1005, '用户导出', 100, 5, null, null, '#', null, 1, 0, 'F', '0', '0', 'system:user:export', 'system:user:export', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"用户导出","en_US":"en_US用户导出"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1007, '重置密码', 100, 7, null, null, '#', null, 1, 0, 'F', '0', '0', 'system:user:resetPwd', 'system:user:resetPwd', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"重置密码","en_US":"en_US重置密码"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1008, '角色查询', 101, 1, null, null, '#', null, 1, 0, 'F', '0', '0', 'system:role:query', 'system:role:list', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"角色查询","en_US":"en_US角色查询"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1009, '角色新增', 101, 2, null, null, '#', null, 1, 0, 'F', '0', '0', 'system:role:add', 'system:role:add', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"角色新增","en_US":"en_US角色新增"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1011, '角色删除', 101, 4, null, null, '#', null, 1, 0, 'F', '0', '0', 'system:role:remove', 'system:role:remove', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"角色删除","en_US":"en_US角色删除"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1012, '角色导出', 101, 5, null, null, '#', null, 1, 0, 'F', '0', '0', 'system:role:export', 'system:role:export', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"角色导出","en_US":"en_US角色导出"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1014, '菜单新增', 102, 2, null, null, '#', null, 1, 0, 'F', '0', '0', 'system:menu:add', 'system:menu:add', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"菜单新增","en_US":"en_US菜单新增"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1016, '菜单删除', 102, 4, null, null, '#', null, 1, 0, 'F', '0', '0', 'system:menu:remove', 'system:menu:remove', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"菜单删除","en_US":"en_US菜单删除"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1017, '部门查询', 103, 1, null, null, '#', null, 1, 0, 'F', '0', '0', 'system:dept:query', 'system:dept:list', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"部门查询","en_US":"en_US部门查询"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1018, '部门新增', 103, 2, null, null, '#', null, 1, 0, 'F', '0', '0', 'system:dept:add', 'system:dept:add', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"部门新增","en_US":"en_US部门新增"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1020, '部门删除', 103, 4, null, null, '#', null, 1, 0, 'F', '0', '0', 'system:dept:remove', 'system:dept:remove', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"部门删除","en_US":"en_US部门删除"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1022, '岗位新增', 104, 2, null, null, '#', null, 1, 0, 'F', '0', '0', 'system:post:add', 'system:post:add', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"岗位新增","en_US":"en_US岗位新增"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1023, '岗位修改', 104, 3, null, null, '#', null, 1, 0, 'F', '0', '0', 'system:post:edit', 'system:post:edit', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"岗位修改","en_US":"en_US岗位修改"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1024, '岗位删除', 104, 4, null, null, '#', null, 1, 0, 'F', '0', '0', 'system:post:remove', 'system:post:remove', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"岗位删除","en_US":"en_US岗位删除"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1025, '岗位导出', 104, 5, null, null, '#', null, 1, 0, 'F', '0', '0', 'system:post:export', 'system:post:export', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"岗位导出","en_US":"en_US岗位导出"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1028, '字典修改', 105, 3, null, '#', '#', null, 1, 0, 'F', '0', '0', 'system:dict:edit', 'system:dict:edit', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"字典修改","en_US":"en_US字典修改"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1029, '字典删除', 105, 4, null, '#', '#', null, 1, 0, 'F', '0', '0', 'system:dict:remove', 'system:dict:remove', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"字典删除","en_US":"en_US字典删除"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1030, '字典导出', 105, 5, null, '#', '#', null, 1, 0, 'F', '0', '0', 'system:dict:export', 'system:dict:export', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"字典导出","en_US":"en_US字典导出"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1031, '参数查询', 106, 1, null, '#', '#', null, 1, 0, 'F', '0', '0', 'system:config:query', 'system:config:list', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"参数查询","en_US":"en_US参数查询"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1032, '参数新增', 106, 2, null, '#', '#', null, 1, 0, 'F', '0', '0', 'system:config:add', 'system:config:add', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"参数新增","en_US":"en_US参数新增"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1033, '参数修改', 106, 3, null, '#', '#', null, 1, 0, 'F', '0', '0', 'system:config:edit', 'system:config:edit', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"参数修改","en_US":"en_US参数修改"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1034, '参数删除', 106, 4, null, '#', '#', null, 1, 0, 'F', '0', '0', 'system:config:remove', 'system:config:remove', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"参数删除","en_US":"en_US参数删除"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1035, '参数导出', 106, 5, null, '#', '#', null, 1, 0, 'F', '0', '0', 'system:config:export', 'system:config:export', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"参数导出","en_US":"en_US参数导出"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1041, '公告查询', 107, 1, null, '#', '#', null, 1, 0, 'F', '0', '0', 'system:notice:query', 'system:notice:list', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"公告查询","en_US":"en_US公告查询"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1042, '公告新增', 107, 2, null, '#', '#', null, 1, 0, 'F', '0', '0', 'system:notice:add', 'system:notice:add', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"公告新增","en_US":"en_US公告新增"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1043, '公告修改', 107, 3, null, '#', '#', null, 1, 0, 'F', '0', '0', 'system:notice:edit', 'system:notice:edit', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"公告修改","en_US":"en_US公告修改"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1044, '公告删除', 107, 4, null, '#', '#', null, 1, 0, 'F', '0', '0', 'system:notice:remove', 'system:notice:remove', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"公告删除","en_US":"en_US公告删除"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1045, '操作查询', 500, 1, null, '#', '#', null, 1, 0, 'F', '0', '0', 'system:operlog:query', 'monitor:operlog:list', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"操作查询","en_US":"en_US操作查询"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1046, '操作删除', 500, 2, null, '#', '#', null, 1, 0, 'F', '0', '0', 'system:operlog:remove', 'monitor:operlog:remove', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"操作删除","en_US":"en_US操作删除"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1047, '日志导出', 500, 4, null, '#', '#', null, 1, 0, 'F', '0', '0', 'system:operlog:export', 'monitor:operlog:export', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"日志导出","en_US":"en_US日志导出"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1048, '登录查询', 501, 1, null, '#', '#', null, 1, 0, 'F', '0', '0', 'system:logininfor:query', 'monitor:logininfor:list', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"登录查询","en_US":"en_US登录查询"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1049, '登录删除', 501, 2, null, '#', '#', null, 1, 0, 'F', '0', '0', 'system:logininfor:remove', 'monitor:logininfor:remove', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"登录删除","en_US":"en_US登录删除"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1050, '日志导出', 501, 3, null, '#', '#', null, 1, 0, 'F', '0', '0', 'system:logininfor:export', 'monitor:operlog:export', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"日志导出","en_US":"en_US日志导出"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1051, '在线查询', 109, 1, null, '#', '#', null, 1, 0, 'F', '0', '0', 'monitor:online:query', 'monitor:online:list', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"在线查询","en_US":"en_US在线查询"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1052, '批量强退', 109, 2, null, '#', '#', null, 1, 0, 'F', '0', '0', 'monitor:online:batchLogout', 'monitor:online:batchForceLogout', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"批量强退","en_US":"en_US批量强退"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1053, '单条强退', 109, 3, null, '#', '#', null, 1, 0, 'F', '0', '0', 'monitor:online:forceLogout', 'monitor:online:forceLogout', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"单条强退","en_US":"en_US单条强退"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1054, '任务查询', 110, 1, null, '#', '#', null, 1, 0, 'F', '0', '0', 'monitor:job:query', 'monitor:job:list', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"任务查询","en_US":"en_US任务查询"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1055, '任务新增', 110, 2, null, '#', '#', null, 1, 0, 'F', '0', '0', 'monitor:job:add', 'monitor:job:add', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"任务新增","en_US":"en_US任务新增"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1056, '任务修改', 110, 3, null, '#', '#', null, 1, 0, 'F', '0', '0', 'monitor:job:edit', 'monitor:job:edit', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"任务修改","en_US":"en_US任务修改"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1057, '任务删除', 110, 4, null, '#', '#', null, 1, 0, 'F', '0', '0', 'monitor:job:remove', 'monitor:job:remove', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"任务删除","en_US":"en_US任务删除"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1058, '状态修改', 110, 5, null, '#', '#', null, 1, 0, 'F', '0', '0', 'monitor:job:changeStatus', 'monitor:job:changeStatus', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"状态修改","en_US":"en_US状态修改"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1059, '任务导出', 110, 7, null, '#', '#', null, 1, 0, 'F', '0', '0', 'monitor:job:export', 'monitor:job:export', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"任务导出","en_US":"en_US任务导出"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1060, '生成查询', 115, 1, null, '#', '#', null, 1, 0, 'F', '0', '0', 'tool:gen:query', 'tool:gen:list', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"生成查询","en_US":"en_US生成查询"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1061, '生成修改', 115, 2, null, '#', '#', null, 1, 0, 'F', '0', '0', 'tool:gen:edit', 'tool:gen:edit', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"生成修改","en_US":"en_US生成修改"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1062, '生成删除', 115, 3, null, '#', '#', null, 1, 0, 'F', '0', '0', 'tool:gen:remove', 'tool:gen:remove', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"生成删除","en_US":"en_US生成删除"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1063, '导入代码', 115, 2, null, '#', '#', null, 1, 0, 'F', '0', '0', 'tool:gen:import', ' ', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"导入代码","en_US":"en_US导入代码"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1064, '预览代码', 115, 4, null, '#', '#', null, 1, 0, 'F', '0', '0', 'tool:gen:preview', 'tool:gen:preview', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"预览代码","en_US":"en_US预览代码"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1065, '生成代码', 115, 5, null, '#', '#', null, 1, 0, 'F', '0', '0', 'tool:gen:code', 'tool:gen:code', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"生成代码","en_US":"en_US生成代码"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1010, '角色修改', 101, 3, null, null, '#', null, 1, 0, 'F', '0', '0', 'system:role:edit', 'system:role:edit', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"角色修改","en_US":"en_US角色修改"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1015, '菜单修改', 102, 3, null, null, '#', null, 1, 0, 'F', '0', '0', 'system:menu:edit', 'system:menu:edit', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"菜单修改","en_US":"en_US菜单修改"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1021, '岗位查询', 104, 1, null, null, '#', null, 1, 0, 'F', '0', '0', 'system:post:query', 'system:post:list', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"岗位查询","en_US":"en_US岗位查询"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1026, '字典查询', 105, 1, null, '#', '#', null, 1, 0, 'F', '0', '0', 'system:dict:query', 'system:dict:list', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"字典查询","en_US":"en_US字典查询"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1070, '测试菜单', 0, 99, null, 'testmenu', '#', null, 1, 0, 'M', '0', '0', null, 'test', '#', '#', 'admin', to_date('29-10-2020 16:16:19', 'dd-mm-yyyy hh24:mi:ss'), 'admin', to_date('29-10-2020 16:26:22', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"测试菜单","en_US":"en_US测试菜单"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1071, '测试已分配子菜单', 1070, 100, null, 'test', '#', null, 1, 0, 'M', '0', '0', null, null, '#', '#', 'admin', to_date('29-10-2020 16:42:56', 'dd-mm-yyyy hh24:mi:ss'), 'admin', to_date('29-10-2020 16:48:51', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"测试已分配子菜单","en_US":"en_US测试已分配子菜单"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1, '系统管理', 0, 1, null, 'system', '#', null, 1, 0, 'M', '0', '0', null, 'system', 'system', 'fa fa-gear', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'admin', to_date('29-10-2020 16:33:50', 'dd-mm-yyyy hh24:mi:ss'), '系统管理目录', '[{"zh_CN":"系统管理","en_US":"en_US系统管理"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (103, '部门管理', 1, 4, null, 'dept', '/system/dept', 'system/dept/index', 1, 0, 'C', '0', '0', 'system:dept:list', 'system:dept:view', 'tree', 'fa fa-outdent', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '部门管理菜单', '[{"zh_CN":"部门管理","en_US":"en_US部门管理"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (111, '数据库监控', 2, 3, null, 'http://localhost:8718', '/monitor/data', null, 1, 0, 'C', '0', '0', 'monitor:sentinel:list', 'monitor:data:view', 'sentinel', 'fa fa-bug', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'admin', to_date('17-11-2020 14:50:54', 'dd-mm-yyyy hh24:mi:ss'), '流量控制菜单', '[{"zh_CN":"数据库监控","en_US":"en_US数据库监控"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1001, '用户查询', 100, 1, null, null, '#', null, 1, 0, 'F', '0', '0', 'system:user:query', 'system:user:list', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"用户查询","en_US":"en_US用户查询"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1006, '用户导入', 100, 6, null, null, '#', null, 1, 0, 'F', '0', '0', 'system:user:import', 'system:user:import', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"用户导入","en_US":"en_US用户导入"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1013, '菜单查询', 102, 1, null, null, '#', null, 1, 0, 'F', '0', '0', 'system:menu:query', 'system:menu:list', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"菜单查询","en_US":"en_US菜单查询"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1019, '部门修改', 103, 3, null, null, '#', null, 1, 0, 'F', '0', '0', 'system:dept:edit', 'system:dept:edit', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"部门修改","en_US":"en_US部门修改"}]');
insert into SYS_MENU (menu_id, menu_name, parent_id, order_num, target, path, bt_url, component, is_frame, is_cache, menu_type, visible, status, perms, bt_perms, icon, bt_icon, create_by, create_time, update_by, update_time, remark, lang_json)
values (1027, '字典新增', 105, 2, null, '#', '#', null, 1, 0, 'F', '0', '0', 'system:dict:add', 'system:dict:add', '#', '#', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null, '[{"zh_CN":"字典新增","en_US":"en_US字典新增"}]');
commit;
prompt 86 records loaded
prompt Loading SYS_NOTICE...
insert into SYS_NOTICE (notice_id, notice_title, notice_type, notice_content, status, create_by, create_time, update_by, update_time, remark)
values (1, '温馨提醒：2018-07-01 ttt下新版本发布啦', '1', '<p>新版本内容</p>', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'admin', to_date('16-11-2020 14:52:43', 'dd-mm-yyyy hh24:mi:ss'), '管理员');
insert into SYS_NOTICE (notice_id, notice_title, notice_type, notice_content, status, create_by, create_time, update_by, update_time, remark)
values (2, '维护通知：2018-07-01 1234系统凌晨维护', '1', '维护内容dsfdsf', '1', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'admin', to_date('16-11-2020 10:11:07', 'dd-mm-yyyy hh24:mi:ss'), '管理员');
commit;
prompt 2 records loaded
prompt Loading SYS_POST...
insert into SYS_POST (post_id, post_code, post_name, post_sort, status, create_by, create_time, update_by, update_time, remark)
values (1, 'ceo', '董事长', 1, '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null);
insert into SYS_POST (post_id, post_code, post_name, post_sort, status, create_by, create_time, update_by, update_time, remark)
values (2, 'se', '项目经理', 2, '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null);
insert into SYS_POST (post_id, post_code, post_name, post_sort, status, create_by, create_time, update_by, update_time, remark)
values (3, 'hr', '人力资源', 3, '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null);
insert into SYS_POST (post_id, post_code, post_name, post_sort, status, create_by, create_time, update_by, update_time, remark)
values (4, 'user', '普通员工', 4, '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), null);
commit;
prompt 4 records loaded
prompt Loading SYS_ROLE...
insert into SYS_ROLE (role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, update_by, update_time, remark)
values (1, '超级管理员', 'admin', 1, '1', 1, 1, '0', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'ry', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), '超级管理员');
insert into SYS_ROLE (role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, update_by, update_time, remark)
values (2, '普通角色', 'common', 2, '2', 0, 0, '0', '0', 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'admin', to_date('13-11-2020 14:18:16', 'dd-mm-yyyy hh24:mi:ss'), '普通角色');
insert into SYS_ROLE (role_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, update_by, update_time, remark)
values (26, '测试角色1', 'testrole11', 99, '3', 0, 0, '1', '0', 'admin', to_date('29-10-2020 13:36:14', 'dd-mm-yyyy hh24:mi:ss'), 'admin', to_date('13-11-2020 14:18:43', 'dd-mm-yyyy hh24:mi:ss'), null);
commit;
prompt 3 records loaded
prompt Loading SYS_ROLE_DEPT...
insert into SYS_ROLE_DEPT (role_id, dept_id)
values (2, 100);
insert into SYS_ROLE_DEPT (role_id, dept_id)
values (2, 101);
insert into SYS_ROLE_DEPT (role_id, dept_id)
values (2, 105);
commit;
prompt 3 records loaded
prompt Loading SYS_ROLE_MENU...
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 2);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 3);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 100);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 101);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 102);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 103);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 104);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 105);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 106);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 107);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 108);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 109);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 110);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 111);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 112);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 113);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 114);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 115);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 116);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 500);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 501);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1001);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1002);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1003);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1004);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1005);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1006);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1007);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1008);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1009);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1010);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1011);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1012);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1013);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1014);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1015);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1016);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1017);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1018);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1019);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1020);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1021);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1022);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1023);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1024);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1025);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1026);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1027);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1028);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1029);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1030);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1031);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1032);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1033);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1034);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1035);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1041);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1042);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1043);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1044);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1045);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1046);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1047);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1048);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1049);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1050);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1051);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1052);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1053);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1054);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1055);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1056);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1057);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1058);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1059);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1060);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1061);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1062);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1063);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1064);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1065);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1070);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (2, 1071);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 2);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 100);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 101);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 102);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 103);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 104);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 105);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 106);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 107);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 108);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 109);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 500);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 501);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1001);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1002);
commit;
prompt 100 records committed...
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1003);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1004);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1005);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1006);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1007);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1008);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1009);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1010);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1011);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1012);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1013);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1014);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1015);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1016);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1017);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1018);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1019);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1020);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1021);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1022);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1023);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1024);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1025);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1026);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1027);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1028);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1029);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1030);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1031);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1032);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1033);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1034);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1035);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1041);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1042);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1043);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1044);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1045);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1046);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1047);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1048);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1049);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1050);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1051);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1052);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (4, 1053);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (26, 3);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (26, 114);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (26, 115);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (26, 116);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (26, 1060);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (26, 1061);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (26, 1062);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (26, 1063);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (26, 1064);
insert into SYS_ROLE_MENU (role_id, menu_id)
values (26, 1065);
commit;
prompt 156 records loaded
prompt Loading SYS_USER...
insert into SYS_USER (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, salt, status, del_flag, login_ip, login_date, create_by, create_time, update_by, update_time, remark)
values (126, 105, 'zhangjf', 'zhangjf', '00', '18620306155@qq.com', '18620306155', '0', null, '$2a$10$/ekc3F2tYeF9b.nl4gOWSeABsqea3LJR/0dahsaKqYOYBFjYHGVCG', null, '0', '0', null, null, 'admin', to_date('13-11-2020 11:25:58', 'dd-mm-yyyy hh24:mi:ss'), 'admin', to_date('13-11-2020 13:40:26', 'dd-mm-yyyy hh24:mi:ss'), 'zdsdddsdad');
insert into SYS_USER (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, salt, status, del_flag, login_ip, login_date, create_by, create_time, update_by, update_time, remark)
values (1, 103, 'admin', '若依', '00', 'ry@163.com', '15888888888', '0', null, '$2a$10$U6xH2wHTfAkZPQQbrp3zsODdYUXLi1xm1g8xpLUzd21fk2Cc8US3S', null, '0', '0', '127.0.0.1', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'admin', to_date('12-11-2020 15:05:26', 'dd-mm-yyyy hh24:mi:ss'), '管理员');
insert into SYS_USER (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, salt, status, del_flag, login_ip, login_date, create_by, create_time, update_by, update_time, remark)
values (2, 105, 'ry', '3423423', '00', 'abc@qq.com', '15666633332', '1', null, '$2a$10$gA1fpBALTbmVgyfUZb08Fe8djTkH6Cob3kmCQQjjs9phGObPsuFFO', null, '0', '0', '127.0.0.1', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'admin', to_date('16-03-2018 11:33:00', 'dd-mm-yyyy hh24:mi:ss'), 'admin', to_date('03-11-2020 10:44:05', 'dd-mm-yyyy hh24:mi:ss'), '测试员');
insert into SYS_USER (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, salt, status, del_flag, login_ip, login_date, create_by, create_time, update_by, update_time, remark)
values (3, 109, 'joran', 'joran', '00', '18620306152@qq.com', '18620306152', '1', null, '$2a$10$VtJlGJbkbA/0.iUWrv1bmOwtsz8/5mshi/sHn9vvUCwQUlX90xvfS', null, '1', '2', null, null, 'admin', to_date('19-10-2020 15:05:04', 'dd-mm-yyyy hh24:mi:ss'), 'admin', to_date('29-10-2020 14:10:02', 'dd-mm-yyyy hh24:mi:ss'), '111');
insert into SYS_USER (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, salt, status, del_flag, login_ip, login_date, create_by, create_time, update_by, update_time, remark)
values (6, 100, 'tester02', '测试02', '00', 'test02@qq.com', '13100000002', '0', null, '$2a$10$THAZFuKZfHsuq45QaWzrhO9K6fmXpfEJDXmC/23Yd00HGnnYeqiCq', null, '0', '0', null, null, 'admin', to_date('21-10-2020 10:34:23', 'dd-mm-yyyy hh24:mi:ss'), 'admin', to_date('02-11-2020 17:49:11', 'dd-mm-yyyy hh24:mi:ss'), 'auto test');
insert into SYS_USER (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, salt, status, del_flag, login_ip, login_date, create_by, create_time, update_by, update_time, remark)
values (15, null, 'tester900', '测试员900', '00', 'tester900@qq.com', '19999999999', '0', null, '$2a$10$wHg84yjxLDzobt6Jr.2sV.cWV1hvdTjs1yPPfI6HGUl9ldNYcylA2', null, '0', '0', null, null, 'admin', to_date('02-11-2020 16:25:37', 'dd-mm-yyyy hh24:mi:ss'), 'admin', to_date('13-11-2020 14:03:03', 'dd-mm-yyyy hh24:mi:ss'), 'auto test');
commit;
prompt 6 records loaded
prompt Loading SYS_USER_POST...
insert into SYS_USER_POST (user_id, post_id)
values (1, 2);
insert into SYS_USER_POST (user_id, post_id)
values (11, 4);
insert into SYS_USER_POST (user_id, post_id)
values (13, 2);
insert into SYS_USER_POST (user_id, post_id)
values (126, 3);
commit;
prompt 4 records loaded
prompt Loading SYS_USER_ROLE...
insert into SYS_USER_ROLE (user_id, role_id)
values (1, 1);
insert into SYS_USER_ROLE (user_id, role_id)
values (2, 1);
insert into SYS_USER_ROLE (user_id, role_id)
values (2, 2);
insert into SYS_USER_ROLE (user_id, role_id)
values (6, 1);
insert into SYS_USER_ROLE (user_id, role_id)
values (6, 2);
insert into SYS_USER_ROLE (user_id, role_id)
values (11, 4);
insert into SYS_USER_ROLE (user_id, role_id)
values (12, 4);
insert into SYS_USER_ROLE (user_id, role_id)
values (13, 2);
insert into SYS_USER_ROLE (user_id, role_id)
values (15, 2);
insert into SYS_USER_ROLE (user_id, role_id)
values (126, 2);
insert into SYS_USER_ROLE (user_id, role_id)
values (126, 26);
commit;
prompt 11 records loaded
prompt Enabling triggers for SYS_CONFIG...
alter table SYS_CONFIG enable all triggers;
prompt Enabling triggers for SYS_DEPT...
alter table SYS_DEPT enable all triggers;
prompt Enabling triggers for SYS_DICT_DATA...
alter table SYS_DICT_DATA enable all triggers;
prompt Enabling triggers for SYS_DICT_TYPE...
alter table SYS_DICT_TYPE enable all triggers;
prompt Enabling triggers for SYS_MENU...
alter table SYS_MENU enable all triggers;
prompt Enabling triggers for SYS_NOTICE...
alter table SYS_NOTICE enable all triggers;
prompt Enabling triggers for SYS_POST...
alter table SYS_POST enable all triggers;
prompt Enabling triggers for SYS_ROLE...
alter table SYS_ROLE enable all triggers;
prompt Enabling triggers for SYS_ROLE_DEPT...
alter table SYS_ROLE_DEPT enable all triggers;
prompt Enabling triggers for SYS_ROLE_MENU...
alter table SYS_ROLE_MENU enable all triggers;
prompt Enabling triggers for SYS_USER...
alter table SYS_USER enable all triggers;
prompt Enabling triggers for SYS_USER_POST...
alter table SYS_USER_POST enable all triggers;
prompt Enabling triggers for SYS_USER_ROLE...
alter table SYS_USER_ROLE enable all triggers;
set feedback on
set define on
prompt Done.
