package com.zlt.aps.xwyy.engine.mapper;

import com.zlt.aps.xwyy.api.domain.dto.XwyyReserveStockDto;
import com.zlt.aps.xwyy.api.domain.entity.XwyyBigRollRemind;
import com.zlt.aps.xwyy.engine.vo.XwyyBigRollVo;
import com.zlt.aps.xwyy.engine.vo.XwyyParamsVo;
import com.zlt.aps.xwyy.engine.vo.XwyyStockVo;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 纤维压延库存数据mapper
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-22 11:40:19
 * @Version 1.0
 */
public interface XwyyEngineStockMapper {
	/**
	 * 加载纤维压延库存信息
	 * 
	 * @param scheduleDate 排产日期
	 * @Param stockLossRate 库存损耗率
	 * @param breadth 幅宽
	 * @Param isProductStage 仅对投产阶段的规格排产
	 * @return
	 */
	List<XwyyStockVo> selectXwyyStock(@Param("scheduleDate") Date scheduleDate,
			@Param("stockLossRate") BigDecimal stockLossRate, @Param("breadth") Double breadth,
			@Param("isProductStage") boolean isProductStage);

	/**
	 * 加载纤维压延外厂需求的库存信息
	 * 
	 * @param scheduleDate 排产日期
	 * @Param stockLossRate 库存损耗率
	 * @param breadth 幅宽
	 * @Param isProductStage 仅对投产阶段的规格排产
	 * @return
	 */
	List<XwyyStockVo> selectXwyyAssistStock(@Param("scheduleDate") Date scheduleDate, @Param("breadth") Double breadth,
			@Param("isProductStage") boolean isProductStage);

	/**
	 * 加载纤维压延库存量
	 * 
	 * @param scheduleDate 排产日期
	 * @Param stockLossRate 库存损耗率
	 * @return
	 */
	List<XwyyStockVo> selectXwyyStockQty(@Param("scheduleDate") Date scheduleDate,
			@Param("stockLossRate") BigDecimal stockLossRate);

	/**
	 * 加载纤维压延外厂需求的库存量
	 * 
	 * @param scheduleDate 排产日期
	 * @return
	 */
	List<XwyyStockVo> selectXwyyAssistStockQty(@Param("scheduleDate") Date scheduleDate);

	/**
	 * 加载90度裁断库存信息
	 * 
	 * @param scheduleDate 排产日期
	 * @Param stockLossRate 库存损耗率
	 * @param breadth 幅宽
	 * @return
	 */
	List<XwyyStockVo> selectCd90Stock(@Param("scheduleDate") Date scheduleDate,
			@Param("stockLossRate") BigDecimal stockLossRate, @Param("breadth") Double breadth);

	/**
	 * 查询90度裁断的参数配置，用于处理90度裁断的库存
	 * 
	 * @param paramCode 参数编号
	 * @return
	 */
	List<XwyyParamsVo> listCd90Params(String paramCode);

	/**
	 * 获取大卷提醒设置
	 * 
	 * @return
	 */
	List<XwyyBigRollRemind> listBigRollRemind();

	/**
	 * 获取大卷长度配置
	 * 
	 * @return
	 */
	List<XwyyBigRollVo> listXwyyBigRoll();

	/**
	 * 查询当天的收尾规格
	 * 
	 * @return
	 */
	List<String> listCloseOutSpec(@Param("scheduleDate") Date scheduleDate,
			@Param("isProductStage") boolean isProductStage);


	/**
	 * 根据规格查询预生产库存倍数
	 * @param codeList 规格列表
	 * @return 结果
	 */
	List<XwyyReserveStockDto> listReserveStock(@Param("codeList") List<String> codeList);

    /**
     * 预估指定日期的库存，使用前一天的库存与当天的计划进行预估
     * @param stockDate
     * @return
     */
    int estimateStock(@Param("stockDate") Date stockDate);
}
