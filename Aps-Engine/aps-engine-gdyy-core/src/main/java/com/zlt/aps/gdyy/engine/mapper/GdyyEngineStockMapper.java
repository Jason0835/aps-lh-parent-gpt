package com.zlt.aps.gdyy.engine.mapper;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.zlt.aps.gdyy.engine.vo.GdyyParamsVo;
import com.zlt.aps.gdyy.engine.vo.GdyyStockVo;

/**
 * 钢带裁断库存数据mapper
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-16 11:40:19
 * @Version 1.0
 */
public interface GdyyEngineStockMapper {
	/**
	 * 加载钢带压延库存信息
	 * 
	 * @param scheduleDate  排产日期
	 * @param stockLossRate 库存损耗率
	 * @return
	 */
	List<GdyyStockVo> selectGdyyStockQty(@Param("scheduleDate") Date scheduleDate,
			@Param("stockLossRate") BigDecimal stockLossRate);

	/**
	 * 加载15度裁断库存信息
	 * 
	 * @param scheduleDate  排产日期
	 * @param stockLossRate 库存损耗率
	 * @param breadth       幅宽
	 * @Param isProductStage 仅对投产阶段的规格排产
	 * @return
	 */
	List<GdyyStockVo> selectCd15Stock(@Param("scheduleDate") Date scheduleDate,
			@Param("stockLossRate") BigDecimal stockLossRate, @Param("breadth") Double breadth,
			@Param("isProductStage") boolean isProductStage);

	/**
	 * 查询15度裁断的参数配置，用于处理15度裁断的库存
	 * 
	 * @param paramCode 参数编号
	 * @return
	 */
	List<GdyyParamsVo> listCd15Params(String paramCode);

	/**
	 * 查询当天的收尾规格
	 * 
	 * @return
	 */
	List<String> listCloseOutSpec(@Param("scheduleDate") Date scheduleDate,
			@Param("isProductStage") boolean isProductStage);
}
