-- 垫胶施工信息表增加垫胶胶料和垫胶长度字段
-- 对应实体：MdmConstructionInfo 新增字段 paddingRubber(PADDING_RUBBER)、paddingLength(PADDING_LENGTH)
-- 新增位置：在 PADDING_CODE 字段之后
-- 执行日期：2026-06-18

ALTER TABLE `t_mdm_construction_info` 
 ADD COLUMN `PADDING_RUBBER` varchar(300) NULL COMMENT '垫胶胶料' AFTER `PADDING_CODE`;

ALTER TABLE `t_mdm_construction_info` 
 ADD COLUMN `PADDING_LENGTH` decimal(18,2) NULL COMMENT '垫胶长度' AFTER `PADDING_RUBBER`;
