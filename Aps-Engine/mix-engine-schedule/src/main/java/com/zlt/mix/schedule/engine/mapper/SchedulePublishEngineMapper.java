package com.zlt.mix.schedule.engine.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.zlt.mix.schedule.engine.vo.GlueScheduleResultVo;
import com.zlt.mix.schedule.engine.vo.SchedulePublishLogVo;
import com.zlt.mix.schedule.engine.vo.SchedulePublishRecordVo;

/**
 * 排程发布引擎mapper
 */
public interface SchedulePublishEngineMapper {
	/**
	 * 生成排程发布记录ID
	 * 
	 * @return
	 */
	Long getPublishRecordId();

	/**
	 * 保存发布记录
	 * 
	 * @param record 待保存发布记录
	 * @return
	 */
	void savePublishRecord(@Param("record") SchedulePublishRecordVo record);

	/**
	 * 保存发布日志明细
	 * 
	 * @param record 待保存发布记录
	 * @return
	 */
	void savePublishLog(@Param("logList") List<SchedulePublishLogVo> logList);

	/**
	 * 更新排程记录发布状态
	 * 
	 * @param scheduleList
	 */
	void updateScheduleReleseState(@Param("scheduleList") List<GlueScheduleResultVo> scheduleList);

	/**
	 * 查询最后一次有效排程发布日志
	 * 
	 * @param scheduleDate
	 * @param mixArea
	 * @param scheduleType
	 * @param publishResult
	 * @return
	 */
	List<SchedulePublishLogVo> listLatestPublishLog(@Param("scheduleDate") String scheduleDate,
			@Param("mixArea") String mixArea, @Param("scheduleType") String scheduleType,
			@Param("publishResult") String publishResult);
	
    /**
     * 把排程数据发布到中间库
     * 
     * @param dataVersion 接口发布版本号
     * @param ids         排程发布的ids
     * @param factoryCode 厂别
     * @param companyCode 分公司编号
     */
    void publishToMes(@Param("dataVersion") String dataVersion, @Param("ids") List<Long> ids,
            @Param("factoryCode") String factoryCode, @Param("companyCode") String companyCode);
}
