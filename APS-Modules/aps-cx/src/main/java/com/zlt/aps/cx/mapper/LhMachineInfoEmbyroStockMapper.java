package com.zlt.aps.cx.mapper;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.zlt.aps.cx.api.domain.dto.LhMachineInfoDto;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.cx.api.domain.entity.CxStock;

/**
 * 硫化机-胎胚库存数据库接口
 * 
 * @Description
 * @Author hakimryan
 * @Date 2021-12-17 10:30:03
 */
public interface LhMachineInfoEmbyroStockMapper {
	/**
	 * 查询所有可用硫化机台
	 * 
	 * @param machineName 机台名称
	 * @return
	 */
	List<LhMachineInfoDto> selectAll(@Param("machineName") String machineName);

	/**
	 * 查询指定日期区间的成型排程记录
	 * 
	 * @param startDate   开始时间
	 * @param endDate     结束时间
	 * @param machineName 机台名称
	 * @return
	 */
	List<CxScheduleResult> selectCxScheduleResult(@Param("startDate") Date startDate, @Param("endDate") Date endDate,
			@Param("machineName") String machineName);

	/**
	 * 获取指定日期的库存信息，返回的库存数不分bom版本，同一胎胚合并一起
	 * 
	 * @param scheduleDate 排程日期
	 * @return
	 */
	List<CxStock> selectCxStock(@Param("scheduleDate") Date scheduleDate);
}
