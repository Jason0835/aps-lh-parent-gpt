package com.zlt.aps.common.engine.service;

import java.util.List;
import java.util.Map;

import com.zlt.aps.mdm.api.enums.MaterialCodeTypeEnum;

/**
 * 物料代号名称对照公共服务。
 * <p>
 * 各工序「部件」下拉（内衬、垫胶、胎侧等）统一走这里取数，避免每个工序各写一套查询：
 * 数据源为物料代号名称对照表 T_MDM_MATERIAL_CODE_NAME，
 * 按当前工厂 + 物料类型（{@link MaterialCodeTypeEnum}）过滤，按物料名称排序、按物料代号去重。
 * </p>
 */
public interface MaterialCodeNameService {

    /**
     * 获取指定物料类型的部件下拉列表（按当前工厂过滤，按物料名称排序，按物料代号去重）。
     * <p>
     * 返回元素为 {@code {value: 物料代号, label: 物料名称}}，与前端 el-select 的选项结构一致。
     * </p>
     *
     * @param materialType 物料类型枚举，必填；为空时返回空列表
     * @return 部件下拉选项列表
     */
    List<Map<String, String>> getMaterialDistList(MaterialCodeTypeEnum materialType);
}
