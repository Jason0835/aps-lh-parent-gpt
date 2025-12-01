package com.zlt.mix.schedule.engine.mapper;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.zlt.mix.schedule.engine.vo.DailyReturnGlueStockVo;
import com.zlt.mix.schedule.engine.vo.GlueFinishVo;
import com.zlt.mix.schedule.engine.vo.GlueStockVo;
import com.zlt.mix.schedule.engine.vo.GlueUnclaimed;

/**
 * 胶料库存mapper
 * 
 * @author hakimryan
 *
 */
public interface GlueScheduleStockPlatMapper {
	/**
	 * 查询终炼胶库存
	 * 
	 * @param stockDate 库存日
	 * @param mixArea   密炼区
	 * @return
	 */
	List<GlueStockVo> listGlueStock(@Param("stockDate") Date stockDate, @Param("mixArea") String mixArea);

	/**
	 * 查询终炼胶库存
	 * 
	 * @param stockDate 库存日
	 * @param mixArea   密炼区
	 * @return
	 */
	List<GlueStockVo> listMlGlueStock(@Param("stockDate") Date stockDate, @Param("mixArea") String mixArea);

	/**
	 * 查询终炼胶库存
	 * 
	 * @param stockDate 库存日
	 * @param mixArea   密炼区
	 * @return
	 */
	List<GlueStockVo> listFhGlueStock(@Param("stockDate") Date stockDate, @Param("mixArea") String mixArea);

	/**
	 * 查询终炼胶库存
	 * 
	 * @param stockDate 库存日
	 * @param mixArea   密炼区
	 * @return
	 */
	List<GlueStockVo> listBhgGlueStock(@Param("stockDate") Date stockDate, @Param("mixArea") String mixArea);

	/**
	 * 查询安全库存
	 * 
	 * @param mixArea 密炼区
	 * @return
	 */
	List<GlueStockVo> listSafeStock(@Param("mixArea") String mixArea);

	/**
	 * 8点到12点完成量
	 * 
	 * @param scheduleDate 排产日期
	 * @param mixArea      密炼区
	 * @return
	 */
	List<GlueFinishVo> list12AmFinishQty(@Param("scheduleDate") Date scheduleDate, @Param("mixArea") String mixArea);

	/**
	 * 胶料待支领量
	 * 
	 * @param scheduleDate 排产日期
	 * @param mixArea      密炼区
	 * @return
	 */
	List<GlueUnclaimed> listGlueUnclaimed(@Param("scheduleDate") Date scheduleDate, @Param("mixArea") String mixArea);
	
	/**
	 * 预计返回胶料量
	 * 
	 * @param scheduleDate 排产日期
	 * @param mixArea      密炼区
	 * @return
	 */
	List<DailyReturnGlueStockVo> listDailyReturnGlueStock(@Param("scheduleDate") Date scheduleDate, @Param("mixArea") String mixArea);

}
