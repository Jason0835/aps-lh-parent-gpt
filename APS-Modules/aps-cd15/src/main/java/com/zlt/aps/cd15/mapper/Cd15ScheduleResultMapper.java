package com.zlt.aps.cd15.mapper;

import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.common.core.domain.SchedulePublishRecord;
import com.zlt.aps.common.engine.domain.EngineConstructionInfo;
import com.zlt.aps.common.engine.domain.ScheduleSummaryVo;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 15度裁断排程结果Mapper接口
 *
 * @author zlt
 * @date 2021-07-05
 */
public interface Cd15ScheduleResultMapper {
    /**
     * 查询15度裁断排程结果
     *
     * @param id 15度裁断排程结果ID
     * @return 15度裁断排程结果
     */
    public Cd15ScheduleResult selectCd15ScheduleResultById(Long id);

    /**
     * 查询15度裁断排程结果列表
     *
     * @param cd15ScheduleResult 15度裁断排程结果
     * @return 15度裁断排程结果集合
     */
    public List<Cd15ScheduleResult> selectCd15ScheduleResultList(Cd15ScheduleResult cd15ScheduleResult);

    /**
     * 新增15度裁断排程结果
     *
     * @param cd15ScheduleResult 15度裁断排程结果
     * @return 结果
     */
    public int insertCd15ScheduleResult(Cd15ScheduleResult cd15ScheduleResult);

    /**
     * 修改15度裁断排程结果
     *
     * @param cd15ScheduleResult 15度裁断排程结果
     * @return 结果
     */
    public int updateCd15ScheduleResult(Cd15ScheduleResult cd15ScheduleResult);

    /**
     * 删除15度裁断排程结果
     *
     * @param id 15度裁断排程结果ID
     * @return 结果
     */
    public int deleteCd15ScheduleResultById(Long id);

    /**
     * 批量删除15度裁断排程结果
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteCd15ScheduleResultByIds(Long[] ids);

    public int batchUpdate(@Param("list") List<Long> ids, @Param("status") String status);

    public List<Cd15ScheduleResult> checkScheduleResultUnique(Cd15ScheduleResult cd15ScheduleResult);

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
	 * 把排程数据发布到中间库
     *
	 * @param dataVersion 接口发布版本号
	 * @param ids         排程发布的ids
	 * @param factoryCode 厂别
	 * @param companyCode 分公司编号
	 * @param createTime  数据同步时间
	 */
	void deployCd15ScheduleToMid(@Param("dataVersion") String dataVersion, @Param("ids") long[] ids,
			@Param("factoryCode") String factoryCode, @Param("companyCode") String companyCode,
			@Param("createTime") Date createTime);


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
    public int changeReleaseStatus(Cd15ScheduleResult scheduleResult);

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

    int checkCd15CodeExist(Cd15ScheduleResult cd15ScheduleResult);

    /**
     * 根据id查询未发布记录的条数
     * @param ids id
     * @return 未发布的记录条数
     */
    public int isPublishByIds(Long[] ids);

    List<Cd15ScheduleResult> selectCd15ScheduleResultByIds(@Param("list") List<Long> ids);

    /**
     * 根据排程日期和钢带代码查询排程结果
     * @param cd15ScheduleResult 排程日期、钢带代码
     * @return 查询到的数据
     */
    List<Cd15ScheduleResult> selectByScheduleDateAndBigRollCode(Cd15ScheduleResult cd15ScheduleResult);

    /**
     * 查询出对应的施工信息字段
     *
     * @param embryoCodeList  施工代码
     * @param productionStage 仅投产阶段规格排产标识
     * @return 结果
     */
    List<EngineConstructionInfo> listConstruction(@Param("embryoCodeList") List<String> embryoCodeList, @Param("productionStage") String productionStage);

    /**
     * 获取排程结果统计信息
     *
     * @param scheduleResult 排程日期
     * @return 结果
     */
    ScheduleSummaryVo getSummaryVo(Cd15ScheduleResult scheduleResult);

    /**
     * 获取昨日早班计划量
     *
     * @param scheduleResult 排程日期
     * @return 结果
     */
    ScheduleSummaryVo getLastDayPlanQty(Cd15ScheduleResult scheduleResult);

    /**
     * 获取昨日早班计划量-具体到每个规格
     *
     * @param scheduleResult 排程日期
     * @return 结果
     */
    List<Cd15ScheduleResult> getLastDayPlanQty4List1(Cd15ScheduleResult scheduleResult);

    /**
     * 获取昨日早班计划量-具体到每个规格
     *
     * @param scheduleResult 排程日期
     * @return 结果
     */
    List<Cd15ScheduleResult> getLastDayPlanQty4List2(Cd15ScheduleResult scheduleResult);

    /**
     * 获取成型消耗量
     *
     * @param scheduleResult 排程日期
     * @return 结果
     */
    ScheduleSummaryVo getCxConsume(Cd15ScheduleResult scheduleResult);

    /**
     * 获取成型消耗量-具体到每个规格
     *
     * @param cd15ScheduleResult 查询条件
     * @return 结果
     */
    List<Cd15ScheduleResult> getCxConsume4List(Cd15ScheduleResult cd15ScheduleResult);
}
