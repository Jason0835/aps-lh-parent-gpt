-- =========================================================
-- 胎面参数设置 菜单权限脚本
-- 父级ID: 150（胎面排程模块）
-- 依赖：需要先存在 PARENT_ID=150 的父级菜单
-- MENU_ID 使用数据库自增，子菜单通过 @parentId 变量引用
-- =========================================================

-- 主菜单：胎面参数设置
INSERT INTO `sys_menu` (`MENU_NAME`, `PARENT_ID`, `ORDER_NUM`, `TARGET`, `PATH`, `BT_URL`, `COMPONENT`, `IS_FRAME`, `IS_CACHE`, `MENU_TYPE`, `VISIBLE`, `STATUS`, `PERMS`, `BT_PERMS`, `ICON`, `BT_ICON`, `CREATE_BY`, `CREATE_TIME`, `UPDATE_BY`, `UPDATE_TIME`, `REMARK`, `LANG_JSON`, `IS_REFRESH`, `QUERY`) VALUES
('胎面参数设置', 150, 10, 'menuItem', '/tm/tmParams', '#', 'tm/params/index', 1, 0, 'M', '0', '0', 'tm:tmParams:view', '', '#', '#', 'admin', '2025-12-12 10:00:00', 'admin', '2025-12-12 10:00:00', NULL, '[{\"zh_CN\":\"胎面参数设置\",\"en_US\":\"TM Parameter Setting\"}]', NULL, NULL);

SET @parentId = LAST_INSERT_ID();

-- 查询
INSERT INTO `sys_menu` (`MENU_NAME`, `PARENT_ID`, `ORDER_NUM`, `TARGET`, `PATH`, `BT_URL`, `COMPONENT`, `IS_FRAME`, `IS_CACHE`, `MENU_TYPE`, `VISIBLE`, `STATUS`, `PERMS`, `BT_PERMS`, `ICON`, `BT_ICON`, `CREATE_BY`, `CREATE_TIME`, `UPDATE_BY`, `UPDATE_TIME`, `REMARK`, `LANG_JSON`, `IS_REFRESH`, `QUERY`) VALUES
('查询列表', @parentId, 10, 'menuItem', '#', '#', NULL, 1, 0, 'F', '0', '0', 'tm:tmParams:query', 'tm:tmParams:list', '#', '#', 'admin', '2025-12-12 10:00:00', 'admin', '2025-12-12 10:00:00', NULL, '[{\"zh_CN\":\"查询\",\"en_US\":\"Query\"}]', NULL, NULL);

-- 新增
INSERT INTO `sys_menu` (`MENU_NAME`, `PARENT_ID`, `ORDER_NUM`, `TARGET`, `PATH`, `BT_URL`, `COMPONENT`, `IS_FRAME`, `IS_CACHE`, `MENU_TYPE`, `VISIBLE`, `STATUS`, `PERMS`, `BT_PERMS`, `ICON`, `BT_ICON`, `CREATE_BY`, `CREATE_TIME`, `UPDATE_BY`, `UPDATE_TIME`, `REMARK`, `LANG_JSON`, `IS_REFRESH`, `QUERY`) VALUES
('新增', @parentId, 20, 'menuItem', '#', '#', NULL, 1, 0, 'F', '0', '0', 'tm:tmParams:add', 'tm:tmParams:add', '#', '#', 'admin', '2025-12-12 10:00:00', 'admin', '2025-12-12 10:00:00', NULL, '[{\"zh_CN\":\"新增\",\"en_US\":\"Add\"}]', NULL, NULL);

-- 修改
INSERT INTO `sys_menu` (`MENU_NAME`, `PARENT_ID`, `ORDER_NUM`, `TARGET`, `PATH`, `BT_URL`, `COMPONENT`, `IS_FRAME`, `IS_CACHE`, `MENU_TYPE`, `VISIBLE`, `STATUS`, `PERMS`, `BT_PERMS`, `ICON`, `BT_ICON`, `CREATE_BY`, `CREATE_TIME`, `UPDATE_BY`, `UPDATE_TIME`, `REMARK`, `LANG_JSON`, `IS_REFRESH`, `QUERY`) VALUES
('修改', @parentId, 30, 'menuItem', '#', '#', NULL, 1, 0, 'F', '0', '0', 'tm:tmParams:edit', 'tm:tmParams:edit', '#', '#', 'admin', '2025-12-12 10:00:00', 'admin', '2025-12-12 10:00:00', NULL, '[{\"zh_CN\":\"修改\",\"en_US\":\"Edit\"}]', NULL, NULL);

-- 删除
INSERT INTO `sys_menu` (`MENU_NAME`, `PARENT_ID`, `ORDER_NUM`, `TARGET`, `PATH`, `BT_URL`, `COMPONENT`, `IS_FRAME`, `IS_CACHE`, `MENU_TYPE`, `VISIBLE`, `STATUS`, `PERMS`, `BT_PERMS`, `ICON`, `BT_ICON`, `CREATE_BY`, `CREATE_TIME`, `UPDATE_BY`, `UPDATE_TIME`, `REMARK`, `LANG_JSON`, `IS_REFRESH`, `QUERY`) VALUES
('删除', @parentId, 40, 'menuItem', '#', '#', NULL, 1, 0, 'F', '0', '0', 'tm:tmParams:remove', 'tm:tmParams:remove', '#', '#', 'admin', '2025-12-12 10:00:00', 'admin', '2025-12-12 10:00:00', NULL, '[{\"zh_CN\":\"删除\",\"en_US\":\"Delete\"}]', NULL, NULL);

-- 导出
INSERT INTO `sys_menu` (`MENU_NAME`, `PARENT_ID`, `ORDER_NUM`, `TARGET`, `PATH`, `BT_URL`, `COMPONENT`, `IS_FRAME`, `IS_CACHE`, `MENU_TYPE`, `VISIBLE`, `STATUS`, `PERMS`, `BT_PERMS`, `ICON`, `BT_ICON`, `CREATE_BY`, `CREATE_TIME`, `UPDATE_BY`, `UPDATE_TIME`, `REMARK`, `LANG_JSON`, `IS_REFRESH`, `QUERY`) VALUES
('导出', @parentId, 50, 'menuItem', '#', '#', NULL, 1, 0, 'F', '0', '0', 'tm:tmParams:export', 'tm:tmParams:export', '#', '#', 'admin', '2025-12-12 10:00:00', 'admin', '2025-12-12 10:00:00', NULL, '[{\"zh_CN\":\"导出\",\"en_US\":\"Export\"}]', NULL, NULL);

-- 导入
INSERT INTO `sys_menu` (`MENU_NAME`, `PARENT_ID`, `ORDER_NUM`, `TARGET`, `PATH`, `BT_URL`, `COMPONENT`, `IS_FRAME`, `IS_CACHE`, `MENU_TYPE`, `VISIBLE`, `STATUS`, `PERMS`, `BT_PERMS`, `ICON`, `BT_ICON`, `CREATE_BY`, `CREATE_TIME`, `UPDATE_BY`, `UPDATE_TIME`, `REMARK`, `LANG_JSON`, `IS_REFRESH`, `QUERY`) VALUES
('导入', @parentId, 60, 'menuItem', '#', '#', NULL, 1, 0, 'F', '0', '0', 'tm:tmParams:import', 'tm:tmParams:import', '#', '#', 'admin', '2025-12-12 10:00:00', 'admin', '2025-12-12 10:00:00', NULL, '[{\"zh_CN\":\"导入\",\"en_US\":\"Import\"}]', NULL, NULL);
