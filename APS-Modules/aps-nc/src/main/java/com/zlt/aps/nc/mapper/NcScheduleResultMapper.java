package com.zlt.aps.nc.mapper;

import com.zlt.aps.common.core.domain.SchedulePublishRecord;
import com.zlt.aps.nc.api.domain.entity.NcScheduleResult;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 内衬胶排程结果Mapper接口
 *
 * @author zlt
 * @date 2021-06-24
 */
public interface NcScheduleResultMapper {
    /**
     * 查询内衬排程结果
     *
     * @param id 内衬排程结果ID
     * @return 内衬排程结果
     */
    public NcScheduleResult selectNcScheduleResultById(Long id);

    /**
     * 查询内衬排程结果列表
     *
     * @param tcScheduleResult 内衬排程结果
     * @return 内衬排程结果集合
     */
    public List<NcScheduleResult> selectNcScheduleResultList(NcScheduleResult tcScheduleResult);

    /**
     * 新增内衬排程结果
     *
     * @param tcScheduleResult 内衬排程结果
     * @return 结果
     */
    public int insertNcScheduleResult(NcScheduleResult tcScheduleResult);

    /**
     * 修改内衬排程结果
     *
     * @param tcScheduleResult 内衬排程结果
     * @return 结果
     */
    public int updateNcScheduleResult(NcScheduleResult tcScheduleResult);

    /**
     * 删除内衬排程结果
     *
     * @param id 内衬排程结果ID
     * @return 结果
     */
    public int deleteNcScheduleResultById(Long id);

    /**
     * 批量删除内衬排程结果
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteNcScheduleResultByIds(Long[] ids);

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
     * 唯一性校验
     */
    public List<NcScheduleResult> checkUnique(NcScheduleResult entity);

    /**
     * 把排程数据发布到中间库
     * @param dataVersion 接口发布版本号
     * @param ids  排程发布的ids
	 * @param factoryCode 厂别
	 * @param companyCode 分公司编号
     */
    void deployNcScheduleToMid(@Param("dataVersion") String dataVersion, @Param("ids") long[] ids,
			@Param("factoryCode") String factoryCode, @Param("companyCode") String companyCode);


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
     * 更改发布状态
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int changeReleaseStatus(NcScheduleResult entity);

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

    int checkNcCodeExist(NcScheduleResult ncScheduleResult);

    /**
     * 根据id查询未发布记录的条数
     * @param ids id
     * @return 未发布的记录条数
     */
    public int isPublishByIds(Long[] ids);

    List<NcScheduleResult> selectByIds(@Param("list") List<Long> ids);

    /**
     * 根据排程日期和帘布代码查询记录
     * @return 查询到的记录
     */
    List<NcScheduleResult> selectByScheduleDateAndCode(NcScheduleResult scheduleResult);
}
