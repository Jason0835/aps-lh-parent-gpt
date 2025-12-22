package com.zlt.aps.itf.mes.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.zlt.aps.monthplan.api.domain.entity.MdmBomInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmConstructionInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmSkuConstructionRef;
import com.zlt.sync.domain.AuxReqSyncDataLogs;

/**
 * MES接口Mapper-Bom相关
 *
 * @author zlt
 * @since 2025/12/19
 */
@Mapper
public interface MesBomItfMapper {

	/**
	 * 查询SKU与模具关系列表
	 *
	 * @param syncDataLogs 查询参数
	 * @return 结果
	 */
	List<MdmSkuConstructionRef> selectLhConstructionInfo(AuxReqSyncDataLogs syncDataLogs);

	/**
	 * 半部件BOM信息
	 *
	 * @param syncDataLogs 查询参数
	 * @return 列表
	 */
	List<MdmConstructionInfo> selectMesConstructionInfo(AuxReqSyncDataLogs syncDataLogs);

	/**
	 * 成型及半部件BOM施工信息同步
	 *
	 * @param syncDataLogs 查询参数
	 * @return 结果
	 */
	List<MdmBomInfo> selectMesBomInfo(AuxReqSyncDataLogs syncDataLogs);
}
