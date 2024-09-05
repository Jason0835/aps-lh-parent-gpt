package com.zlt.aps.cd15.engine.mapper;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.zlt.aps.cd15.api.domain.entity.Cd15LineSideStock;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineInfo;
import com.zlt.aps.cd15.engine.vo.Cd15ParamsVo;
import com.zlt.aps.cd15.engine.vo.Cd15StockVo;

/**
 * 15度裁断库存数据mapper
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-11 11:40:19
 * @Version 1.0
 */
public interface Cd15EngineStockMapper {
	/**
	 * 加载15度裁断库存信息
	 * 
	 * @param scheduleDate 排产日期
	 * @Param stockLossRate 库存损耗率，需要在扣减12点至16点的预计消耗前乘上损耗率
	 * @Param isProductStage 仅对投产阶段的规格排产
	 * @return
	 */
	List<Cd15StockVo> selectCd15Stock(@Param("scheduleDate") Date scheduleDate,
			@Param("stockLossRate") BigDecimal stockLossRate, @Param("isProductStage") boolean isProductStage);

	/**
	 * 查询当天的收尾规格
	 * 
	 * @return
	 */
	List<String> listCloseOutSpec(@Param("scheduleDate") Date scheduleDate,
			@Param("isProductStage") boolean isProductStage);
	
	/**
	 * 查询钢带压延的排产参数
	 * @return
	 */
	List<Cd15ParamsVo> listGdyyParams();
	
	/**
	 * 查询15度裁断线边库库存
	 * @param scheduleDate
	 * @return
	 */
	List<Cd15LineSideStock> listCd15LineSideStock(@Param("scheduleDate") Date scheduleDate);
	
	/**
	 * 加载机台信息
	 * @return
	 */
	List<Cd15MachineInfo> listCd15MachineInfo();
}
