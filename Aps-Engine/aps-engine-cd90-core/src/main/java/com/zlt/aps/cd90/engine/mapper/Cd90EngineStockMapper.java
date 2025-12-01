package com.zlt.aps.cd90.engine.mapper;

import com.zlt.aps.cd90.api.domain.entity.Cd90LineSideStock;
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineInfo;
import com.zlt.aps.cd90.engine.vo.Cd90ParamsVo;
import com.zlt.aps.cd90.engine.vo.Cd90StockConsumeVo;
import com.zlt.aps.cd90.engine.vo.Cd90StockVo;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 90度裁断库存数据mapper
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-14 11:40:19
 * @Version 1.0
 */
public interface Cd90EngineStockMapper {
	/**
	 * 加载90度裁断库存信息
	 * 
	 * @param scheduleDate 排产日期
	 * @Param stockLossRate 库存损耗率
	 * @Param isProductStage 仅对投产阶段的规格排产
	 * @return
	 */
	List<Cd90StockVo> selectCd90Stock(@Param("scheduleDate") Date scheduleDate,
			@Param("stockLossRate") BigDecimal stockLossRate, @Param("isProductStage") boolean isProductStage);

	/**
	 * 加载90度裁断库存信息
	 * 
	 * @param scheduleDate 排产日期
	 * @return
	 */
	List<Cd90StockVo> selectCd90StockQty(@Param("scheduleDate") Date scheduleDate);

	/**
	 * 查询当天的收尾规格
	 * 
	 * @return
	 */
	List<String> listCloseOutSpec(@Param("scheduleDate") Date scheduleDate,
			@Param("isProductStage") boolean isProductStage);
	
	/**
	 * 查询压纤维压延延的排产参数
	 * @return
	 */
	List<Cd90ParamsVo> listXwyyParams();
	
	/**
	 * 查询90度裁断线边库库存
	 * @param scheduleDate
	 * @return
	 */
	List<Cd90LineSideStock> listCd90LineSideStock(@Param("scheduleDate") Date scheduleDate);
	
	/**
	 * 加载机台信息
	 * @return
	 */
	List<Cd90MachineInfo> listCd90MachineInfo();

	/**
	 * 查询压90度的排产参数
	 * @return
	 */
	List<Cd90ParamsVo> listCd90Params();

    /**
     * 查询昨日早班计划量
     *
     * @param scheduleDate
     * @return
     */
    List<Cd90StockConsumeVo> listLastDayMidPlan(@Param("scheduleDate") Date scheduleDate);
}
