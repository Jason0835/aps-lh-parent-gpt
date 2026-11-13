package com.zlt.aps.common.engine.mapper;

import com.zlt.aps.mdm.api.domain.entity.MdmMaterialCodeName;
import com.zlt.core.dao.basemapper.CommBaseMapper;

/**
 * 物料代号名称对照公共 Mapper（T_MDM_MATERIAL_CODE_NAME）。
 * <p>
 * 各工序「部件」下拉（内衬、垫胶等）统一通过该 Mapper 取数，
 * 数据源为物料代号名称对照表，按工厂 + 物料类型（{@code MaterialCodeTypeEnum}）过滤。
 * </p>
 */
public interface MdmMaterialCodeNameCommonMapper extends CommBaseMapper<MdmMaterialCodeName> {
}
