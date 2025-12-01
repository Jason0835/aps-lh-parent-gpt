package com.zlt.aps.tq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.common.core.domain.SchedulePublishRecord;
import com.zlt.aps.common.engine.domain.ScheduleSummaryVo;
import com.zlt.aps.tq.api.domain.dto.TqScheduleResultDto;
import com.zlt.aps.tq.entity.TqScheduleResult;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 胎圈排程结果Mapper接口
 *
 * @author chen
 * @date 2021-06-24
 */
public interface TqScheduleResultMapper extends BaseMapper<TqScheduleResult> {

    /**
     * 查询排程结果列表
     *
     * @param scheduleResult 查询条件
     * @return 查询到的集合
     */
    public List<TqScheduleResultDto> selectScheduleResultList(TqScheduleResult scheduleResult);

    /**
     * 发布指定日期的所有排程结果
     *
     * @param scheduleResult 日期条件
     */
    public void publishAll(TqScheduleResult scheduleResult);

    /**
     * 根据id查询排程结果信息
     *
     * @param id 要查询的排程结果id
     * @return 查询到的信息
     */
    public TqScheduleResultDto selectScheduleResultById(Long id);

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
     * 根据排程日期、物料编号、机台id校验唯一性
     * @param scheduleResult 要校验记录
     * @return 查询到的记录数
     */
    public int checkUnique(TqScheduleResult scheduleResult);

    /**
     * 把排程数据发布到中间库
     * @param dataVersion 接口发布版本号
     * @param ids  排程发布的ids
	 * @param factoryCode 厂别
	 * @param companyCode 分公司编号
     */
    void deployTqScheduleToMid(@Param("dataVersion") String dataVersion, @Param("ids") long[] ids,
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
    public int isReleasingOrTimeoutByIds(long[] ids);

    /**
     * 更改发布状态
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int changeReleaseStatus(TqScheduleResult entity);

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

    int checkTqCodeExist(TqScheduleResultDto dto);

    public int batchUpdate(@Param("list") List<Long> ids, @Param("status") String status);

    public int deleteByIds(List<Long> ids);

    /**
     * 根据id查询未发布记录的条数
     * @param ids id
     * @return 未发布的记录条数
     */
    public int isPublishByIds(long[] ids);

    List<TqScheduleResultDto> selectByIds(@Param("list") List<Long> ids2);

    /**
     * 根据排程日期和帘布代码查询记录
     * @return 查询到的记录
     */
    List<TqScheduleResult> selectByScheduleDateAndCode(TqScheduleResult scheduleResult);

    /**
     * 获取排程结果统计信息
     *
     * @param scheduleResult 排程日期
     * @return 结果
     */
    ScheduleSummaryVo getSummaryVo(TqScheduleResultDto scheduleResult);

    /**
     * 获取昨日早班计划量
     *
     * @param scheduleResult 排程日期
     * @return 结果
     */
    ScheduleSummaryVo getLastDayPlanQty(TqScheduleResultDto scheduleResult);

    /**
     * 获取成型消耗量
     *
     * @param scheduleResult 排程日期
     * @return 结果
     */
    ScheduleSummaryVo getCxConsume(TqScheduleResultDto scheduleResult);

    /**
     * 获取昨日早班计划量-具体到每个规格
     *
     * @param scheduleResult 排程日期
     * @return 结果
     */
    List<TqScheduleResultDto> getLastDayPlanQty4List(TqScheduleResult scheduleResult);

    /**
     * 获取成型消耗量-具体到每个规格
     *
     * @param scheduleResult 排程日期
     * @return 结果
     */
    List<TqScheduleResultDto> getCxConsume4List(TqScheduleResult scheduleResult);
}
