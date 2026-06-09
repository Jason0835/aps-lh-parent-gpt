-- =========================================================
-- 胎面胶料与机台关系 菜单权限脚本
-- 父级ID: 4
-- 依赖：需要先存在 PARENT_ID=4 的父级菜单
-- MENU_ID 使用数据库自增，子菜单通过 @parentId 变量引用
-- =========================================================

-- 主菜单：胎面胶料与机台关系
INSERT INTO `sys_menu` (`MENU_NAME`, `PARENT_ID`, `ORDER_NUM`, `TARGET`, `PATH`, `BT_URL`, `COMPONENT`, `IS_FRAME`, `IS_CACHE`, `MENU_TYPE`, `VISIBLE`, `STATUS`, `PERMS`, `BT_PERMS`, `ICON`, `BT_ICON`, `CREATE_BY`, `CREATE_TIME`, `UPDATE_BY`, `UPDATE_TIME`, `REMARK`, `LANG_JSON`, `IS_REFRESH`, `QUERY`) VALUES
('胎面胶料与机台关系', 4, 10, 'menuItem', '/tm/tmGlueMachineReal', '#', 'tm/glueMachineReal/index', 1, 0, 'M', '0', '0', 'tm:tmGlueMachineReal:view', '', '#', '#', 'admin', NOW(), 'admin', NOW(), NULL, '[{"zh_CN":"胎面胶料与机台关系","en_US":"Glue Machine Relationship"}]', NULL, NULL);

SET @parentId = LAST_INSERT_ID();

-- 查询
INSERT INTO `sys_menu` (`MENU_NAME`, `PARENT_ID`, `ORDER_NUM`, `TARGET`, `PATH`, `BT_URL`, `COMPONENT`, `IS_FRAME`, `IS_CACHE`, `MENU_TYPE`, `VISIBLE`, `STATUS`, `PERMS`, `BT_PERMS`, `ICON`, `BT_ICON`, `CREATE_BY`, `CREATE_TIME`, `UPDATE_BY`, `UPDATE_TIME`, `REMARK`, `LANG_JSON`, `IS_REFRESH`, `QUERY`) VALUES
('查询列表', @parentId, 10, 'menuItem', '#', '#', NULL, 1, 0, 'F', '0', '0', 'tm:tmGlueMachineReal:query', 'tm:tmGlueMachineReal:list', '#', '#', 'admin', NOW(), 'admin', NOW(), NULL, '[{"zh_CN":"查询","en_US":"Query"}]', NULL, NULL);

-- 新增
INSERT INTO `sys_menu` (`MENU_NAME`, `PARENT_ID`, `ORDER_NUM`, `TARGET`, `PATH`, `BT_URL`, `COMPONENT`, `IS_FRAME`, `IS_CACHE`, `MENU_TYPE`, `VISIBLE`, `STATUS`, `PERMS`, `BT_PERMS`, `ICON`, `BT_ICON`, `CREATE_BY`, `CREATE_TIME`, `UPDATE_BY`, `UPDATE_TIME`, `REMARK`, `LANG_JSON`, `IS_REFRESH`, `QUERY`) VALUES
('新增', @parentId, 20, 'menuItem', '#', '#', NULL, 1, 0, 'F', '0', '0', 'tm:tmGlueMachineReal:add', 'tm:tmGlueMachineReal:add', '#', '#', 'admin', NOW(), 'admin', NOW(), NULL, '[{"zh_CN":"新增","en_US":"Add"}]', NULL, NULL);

-- 修改
INSERT INTO `sys_menu` (`MENU_NAME`, `PARENT_ID`, `ORDER_NUM`, `TARGET`, `PATH`, `BT_URL`, `COMPONENT`, `IS_FRAME`, `IS_CACHE`, `MENU_TYPE`, `VISIBLE`, `STATUS`, `PERMS`, `BT_PERMS`, `ICON`, `BT_ICON`, `CREATE_BY`, `CREATE_TIME`, `UPDATE_BY`, `UPDATE_TIME`, `REMARK`, `LANG_JSON`, `IS_REFRESH`, `QUERY`) VALUES
('修改', @parentId, 30, 'menuItem', '#', '#', NULL, 1, 0, 'F', '0', '0', 'tm:tmGlueMachineReal:edit', 'tm:tmGlueMachineReal:edit', '#', '#', 'admin', NOW(), 'admin', NOW(), NULL, '[{"zh_CN":"修改","en_US":"Edit"}]', NULL, NULL);

-- 删除
INSERT INTO `sys_menu` (`MENU_NAME`, `PARENT_ID`, `ORDER_NUM`, `TARGET`, `PATH`, `BT_URL`, `COMPONENT`, `IS_FRAME`, `IS_CACHE`, `MENU_TYPE`, `VISIBLE`, `STATUS`, `PERMS`, `BT_PERMS`, `ICON`, `BT_ICON`, `CREATE_BY`, `CREATE_TIME`, `UPDATE_BY`, `UPDATE_TIME`, `REMARK`, `LANG_JSON`, `IS_REFRESH`, `QUERY`) VALUES
('删除', @parentId, 40, 'menuItem', '#', '#', NULL, 1, 0, 'F', '0', '0', 'tm:tmGlueMachineReal:remove', 'tm:tmGlueMachineReal:remove', '#', '#', 'admin', NOW(), 'admin', NOW(), NULL, '[{"zh_CN":"删除","en_US":"Delete"}]', NULL, NULL);

-- 导出
INSERT INTO `sys_menu` (`MENU_NAME`, `PARENT_ID`, `ORDER_NUM`, `TARGET`, `PATH`, `BT_URL`, `COMPONENT`, `IS_FRAME`, `IS_CACHE`, `MENU_TYPE`, `VISIBLE`, `STATUS`, `PERMS`, `BT_PERMS`, `ICON`, `BT_ICON`, `CREATE_BY`, `CREATE_TIME`, `UPDATE_BY`, `UPDATE_TIME`, `REMARK`, `LANG_JSON`, `IS_REFRESH`, `QUERY`) VALUES
('导出', @parentId, 50, 'menuItem', '#', '#', NULL, 1, 0, 'F', '0', '0', 'tm:tmGlueMachineReal:export', 'tm:tmGlueMachineReal:export', '#', '#', 'admin', NOW(), 'admin', NOW(), NULL, '[{"zh_CN":"导出","en_US":"Export"}]', NULL, NULL);

-- 导入
INSERT INTO `sys_menu` (`MENU_NAME`, `PARENT_ID`, `ORDER_NUM`, `TARGET`, `PATH`, `BT_URL`, `COMPONENT`, `IS_FRAME`, `IS_CACHE`, `MENU_TYPE`, `VISIBLE`, `STATUS`, `PERMS`, `BT_PERMS`, `ICON`, `BT_ICON`, `CREATE_BY`, `CREATE_TIME`, `UPDATE_BY`, `UPDATE_TIME`, `REMARK`, `LANG_JSON`, `IS_REFRESH`, `QUERY`) VALUES
('导入', @parentId, 60, 'menuItem', '#', '#', NULL, 1, 0, 'F', '0', '0', 'tm:tmGlueMachineReal:import', 'tm:tmGlueMachineReal:import', '#', '#', 'admin', NOW(), 'admin', NOW(), NULL, '[{"zh_CN":"导入","en_US":"Import"}]', NULL, NULL);
