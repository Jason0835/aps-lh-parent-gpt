package com.zlt.aps.tm.mapper;

import com.zlt.aps.common.core.domain.SchedulePublishRecord;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 胎面排程结果Mapper接口
 *
 * @author zlt
 * @date 2021-06-17
 */
public interface TmScheduleResultMapper {
    /**
     * 查询胎面排程结果
     *
     * @param id 胎面排程结果ID
     * @return 胎面排程结果
     */
    public TmScheduleResult selectTmScheduleResultById(Long id);

    /**
     * 查询胎面排程结果列表
     *
     * @param tmScheduleResult 胎面排程结果
     * @return 胎面排程结果集合
     */
    public List<TmScheduleResult> selectTmScheduleResultList(TmScheduleResult tmScheduleResult);

    /**
     * 唯一性校验
     */
    public List<TmScheduleResult> checkUnique(TmScheduleResult tmScheduleResult);

    /**
     * 新增胎面排程结果
     *
     * @param tmScheduleResult 胎面排程结果
     * @return 结果
     */
    public int insertTmScheduleResult(TmScheduleResult tmScheduleResult);

    /**
     * 修改胎面排程结果
     *
     * @param tmScheduleResult 胎面排程结果
     * @return 结果
     */
    public int updateTmScheduleResult(TmScheduleResult tmScheduleResult);

    /**
     * 删除胎面排程结果
     *
     * @param id 胎面排程结果ID
     * @return 结果
     */
    public int deleteTmScheduleResultById(Long id);

    /**
     * 批量删除胎面排程结果
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteTmScheduleResultByIds(Long[] ids);

    /**
     * 批量更新发布状态
     *
     * @param ids
     */
    public int batchUpdate(@Param("array") long[] ids, @Param("status") String status);

    /**
     * 保存发布日志
     * @param schedulePublishRecord 要保存的发布日志
     * @return 结果
     */
    public int insertPublishRecord(SchedulePublishRecord schedulePublishRecord);

    /**
     * 查询指定日期的排程结果是否已经发布
     * @param schedulePublishRecord 要查询的日期及工序参数
     * @return 查询到的记录条数
     */
    public int isPublish(SchedulePublishRecord schedulePublishRecord);

    /**
     * 根据排程日期查询当前日期发布状态为"发布中"或"超时失败"的记录
     * @param scheduleDate 排程日期
     * @return 查询到的记录数
     */
    public int isReleasingOrTimeoutByDate(Date scheduleDate);

    /**
     * 根据id查询当前日期发布状态为"发布中"或"超时失败"的记录
     * @param ids id
     * @return 查询到的记录数
     */
    public int isReleasingOrTimeoutByIds(Long[] ids);

    /**
     * 把排程数据发布到中间库
     * @param dataVersion 接口发布版本号
     * @param ids  排程发布的ids
	 * @param factoryCode 厂别
	 * @param companyCode 分公司编号
     */
    public void deployScheduleToMes(@Param("dataVersion") String dataVersion, @Param("ids") long[] ids,
			@Param("factoryCode") String factoryCode, @Param("companyCode") String companyCode);

    /**
     * 更改发布状态
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int changeReleaseStatus(TmScheduleResult entity);

    /**
     * 更新发布记录发布状态
     * @param schedulePublishRecord 发布记录
     * @return 影响行数
     */
    public int updatePublishRecord(SchedulePublishRecord schedulePublishRecord);
    
	/**
	 * 更新发布日志状态
	 *
	 * @param dataVersion 数据版本
	 * @param status      状态
	 */
	public int updatePublishRecordVersion(@Param("dataVersion") String dataVersion, @Param("status") String status);

    /**
     * 归并中夜班计划量，合并到同一个班次
     * @param map 要合并的id及合并的班次(type = 1合并到中班，2 合并到夜班)
     * @return 修改行数
     */
	public int combinationMiddleAndNight(Map<String, Object> map);

    int checkTmCodeExist(TmScheduleResult tmScheduleResult);

    /**
     * 根据id查询未发布记录的条数
     * @param ids id
     * @return 未发布的记录条数
     */
    public int isPublishByIds(Long[] ids);

    List<TmScheduleResult> selectByIds(@Param("list") List<Long> ids2);

    /**
     * 根据排程日期和帘布代码查询记录
     * @return 查询到的记录
     */
    List<TmScheduleResult> selectByScheduleDateAndCode(TmScheduleResult scheduleResult);
}
