-- ==========================================
-- 硫化精度计划参数配置脚本
-- ==========================================

-- 1. 预警天数配置
INSERT INTO T_SYS_CONFIG (CONFIG_ID, CONFIG_NAME, CONFIG_KEY, CONFIG_VALUE, CONFIG_TYPE, REMARK, CREATE_BY, CREATE_TIME)
VALUES (SEQ_SYS_CONFIG.NEXTVAL, '硫化精度预警天数', 'lh.precision.warning.days', '30', '1', '硫化精度计划提前预警天数', 'SYSTEM', SYSDATE);

-- 2. 年度计划自动生成配置
INSERT INTO T_SYS_CONFIG (CONFIG_ID, CONFIG_NAME, CONFIG_KEY, CONFIG_VALUE, CONFIG_TYPE, REMARK, CREATE_BY, CREATE_TIME)
VALUES (SEQ_SYS_CONFIG.NEXTVAL, '硫化精度年度计划自动生成', 'lh.precision.yearly.auto', 'true', '1', '是否自动生成年度硫化精度计划', 'SYSTEM', SYSDATE);

-- 3. MES同步频率配置（单位：小时）
INSERT INTO T_SYS_CONFIG (CONFIG_ID, CONFIG_NAME, CONFIG_KEY, CONFIG_VALUE, CONFIG_TYPE, REMARK, CREATE_BY, CREATE_TIME)
VALUES (SEQ_SYS_CONFIG.NEXTVAL, 'MES同步频率', 'lh.precision.mes.sync.interval', '24', '1', '从MES同步硫化精度数据的频率（小时）', 'SYSTEM', SYSDATE);

-- 4. 预警检查频率配置（单位：小时）
INSERT INTO T_SYS_CONFIG (CONFIG_ID, CONFIG_NAME, CONFIG_KEY, CONFIG_VALUE, CONFIG_TYPE, REMARK, CREATE_BY, CREATE_TIME)
VALUES (SEQ_SYS_CONFIG.NEXTVAL, '预警检查频率', 'lh.precision.warning.check.interval', '24', '1', '硫化精度预警检查频率（小时）', 'SYSTEM', SYSDATE);

-- 5. 精度类型配置
INSERT INTO T_SYS_CONFIG (CONFIG_ID, CONFIG_NAME, CONFIG_KEY, CONFIG_VALUE, CONFIG_TYPE, REMARK, CREATE_BY, CREATE_TIME)
VALUES (SEQ_SYS_CONFIG.NEXTVAL, '硫化精度类型', 'lh.precision.type', '硫化精度', '1', '硫化精度计划类型标识', 'SYSTEM', SYSDATE);

-- 6. 维保时长配置（单位：小时）
INSERT INTO T_SYS_CONFIG (CONFIG_ID, CONFIG_NAME, CONFIG_KEY, CONFIG_VALUE, CONFIG_TYPE, REMARK, CREATE_BY, CREATE_TIME)
VALUES (SEQ_SYS_CONFIG.NEXTVAL, '硫化精度维保时长', 'lh.precision.maintenance.hours', '7', '1', '硫化精度维保时长（小时）', 'SYSTEM', SYSDATE);

-- 7. 维保开始时间配置
INSERT INTO T_SYS_CONFIG (CONFIG_ID, CONFIG_NAME, CONFIG_KEY, CONFIG_VALUE, CONFIG_TYPE, REMARK, CREATE_BY, CREATE_TIME)
VALUES (SEQ_SYS_CONFIG.NEXTVAL, '硫化精度维保开始时间', 'lh.precision.maintenance.start', '08:00', '1', '硫化精度维保开始时间', 'SYSTEM', SYSDATE);

-- 8. 维保结束时间配置
INSERT INTO T_SYS_CONFIG (CONFIG_ID, CONFIG_NAME, CONFIG_KEY, CONFIG_VALUE, CONFIG_TYPE, REMARK, CREATE_BY, CREATE_TIME)
VALUES (SEQ_SYS_CONFIG.NEXTVAL, '硫化精度维保结束时间', 'lh.precision.maintenance.end', '15:00', '1', '硫化精度维保结束时间', 'SYSTEM', SYSDATE);

COMMIT;

-- ==========================================
-- 定时任务配置脚本
-- ==========================================

-- 1. 从MES同步数据生成硫化精度计划（每天凌晨2点执行）
INSERT INTO T_SYS_JOB (JOB_ID, JOB_NAME, JOB_GROUP, INVOKE_TARGET, CRON_EXPRESSION, MISFIRE_POLICY, CONCURRENT, STATUS, CREATE_BY, CREATE_TIME, REMARK)
VALUES (SEQ_SYS_JOB.NEXTVAL, '从MES同步硫化精度计划', 'LH_PRECISION', 'lhPrecisionPlanTask.generateFromMes()', '0 0 2 * * ?', '1', '1', '0', 'SYSTEM', SYSDATE, '每天凌晨2点从MES同步硫化精度计划数据');

-- 2. 自动生成年度硫化精度计划（每年1月1日凌晨3点执行）
INSERT INTO T_SYS_JOB (JOB_ID, JOB_NAME, JOB_GROUP, INVOKE_TARGET, CRON_EXPRESSION, MISFIRE_POLICY, CONCURRENT, STATUS, CREATE_BY, CREATE_TIME, REMARK)
VALUES (SEQ_SYS_JOB.NEXTVAL, '自动生成年度硫化精度计划', 'LH_PRECISION', 'lhPrecisionPlanTask.autoGenerateYearly(''2026'')', '0 0 3 1 1 ?', '1', '1', '0', 'SYSTEM', SYSDATE, '每年1月1日凌晨3点自动生成年度硫化精度计划');

-- 3. 执行30天预警检查（每天凌晨4点执行）
INSERT INTO T_SYS_JOB (JOB_ID, JOB_NAME, JOB_GROUP, INVOKE_TARGET, CRON_EXPRESSION, MISFIRE_POLICY, CONCURRENT, STATUS, CREATE_BY, CREATE_TIME, REMARK)
VALUES (SEQ_SYS_JOB.NEXTVAL, '硫化精度30天预警检查', 'LH_PRECISION', 'lhPrecisionPlanTask.checkWarning()', '0 0 4 * * ?', '1', '1', '0', 'SYSTEM', SYSDATE, '每天凌晨4点执行硫化精度30天预警检查');

-- 4. 批量更新到期天数（每天凌晨1点执行）
INSERT INTO T_SYS_JOB (JOB_ID, JOB_NAME, JOB_GROUP, INVOKE_TARGET, CRON_EXPRESSION, MISFIRE_POLICY, CONCURRENT, STATUS, CREATE_BY, CREATE_TIME, REMARK)
VALUES (SEQ_SYS_JOB.NEXTVAL, '批量更新硫化精度到期天数', 'LH_PRECISION', 'lhPrecisionPlanTask.batchUpdateDaysToDue()', '0 0 1 * * ?', '1', '1', '0', 'SYSTEM', SYSDATE, '每天凌晨1点批量更新硫化精度计划到期天数');

COMMIT;

-- ==========================================
-- 说明
-- ==========================================
-- 1. 参数配置表：T_SYS_CONFIG
--    - CONFIG_KEY: 参数键名
--    - CONFIG_VALUE: 参数键值
--    - CONFIG_TYPE: 系统内置（1：是，0：否）
--
-- 2. 定时任务表：T_SYS_JOB
--    - INVOKE_TARGET: 调用目标字符串（格式：bean名称.方法名(参数)）
--    - CRON_EXPRESSION: cron执行表达式
--    - MISFIRE_POLICY: 计划执行错误策略（1：立即执行，2：执行一次，3：放弃执行）
--    - CONCURRENT: 是否并发执行（0：允许，1：禁止）
--    - STATUS: 状态（0：正常，1：暂停）
--
-- 3. Cron表达式说明：
--    - 0 0 2 * * ?: 每天凌晨2点执行
--    - 0 0 3 1 1 ?: 每年1月1日凌晨3点执行
--    - 0 0 4 * * ?: 每天凌晨4点执行
--    - 0 0 1 * * ?: 每天凌晨1点执行
--
-- 4. 业务流程：
--    - MES同步 → 生成初版计划 → 年度推算 → 30天预警
--    - 定时任务执行顺序：
--      1. 批量更新到期天数（1:00）
--      2. MES同步生成计划（2:00）
--      3. 年度计划生成（3:00，每年1月1日）
--      4. 预警检查（4:00）
