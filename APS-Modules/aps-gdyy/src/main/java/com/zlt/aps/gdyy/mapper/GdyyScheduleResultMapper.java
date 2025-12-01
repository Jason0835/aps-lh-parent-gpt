package com.zlt.aps.gdyy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.common.core.domain.SchedulePublishRecord;
import com.zlt.aps.common.engine.domain.ScheduleSummaryVo;
import com.zlt.aps.gdyy.api.domain.dto.GdyyScheduleResultDto;
import com.zlt.aps.gdyy.engine.vo.GdyyBigRollVo;
import com.zlt.aps.gdyy.entity.GdyyScheduleResult;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 钢带压延排程结果Mapper接口
 *
 * @author chen
 * @date 2021-07-05
 */
public interface GdyyScheduleResultMapper extends BaseMapper<GdyyScheduleResult> {

    /**
     * 查询排程结果列表
     *
     * @param scheduleResult 查询条件
     * @return 查询到的集合
     */
    public List<GdyyScheduleResultDto> selectScheduleResultList(GdyyScheduleResult scheduleResult);

    /**
     * 发布指定日期的所有排程结果
     *
     * @param scheduleResult 日期条件
     */
    public void publishAll(GdyyScheduleResult scheduleResult);

    /**
     * 根据id查询排程结果信息
     *
     * @param id 要查询的排程结果id
     * @return 查询到的信息
     */
    public GdyyScheduleResultDto selectScheduleResultById(Long id);

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
    public int checkUnique(GdyyScheduleResult scheduleResult);


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
    public int changeReleaseStatus(GdyyScheduleResult scheduleResult);

    int checkGdyyCodeExist(GdyyScheduleResult scheduleResult);

    public int batchUpdate(@Param("list") List<Long> ids, @Param("status") String status);

    public int deleteByIds(List<Long> ids);

    /**
     * 根据id查询未发布记录的条数
     * @param ids id
     * @return 未发布的记录条数
     */
    public int isPublishByIds(long[] ids);

    /**
     * 根据排程日期和帘布代码查询记录
     * @return 查询到的记录
     */
    List<GdyyScheduleResult> selectByScheduleDateAndCode(GdyyScheduleResult scheduleResult);

    /**
     * 获取钢压大卷配置信息
     * @Author hakimryan
     * @Description
     * @Date 2021-7-19 10:01:43
     * @return
     */
    List<GdyyBigRollVo> listCd15BigRoll();

    /**
     * 更新发布日志状态
     *
     * @param dataVersion 数据版本
     * @param status      状态
     */
    public int updatePublishRecordVersion(@Param("dataVersion") String dataVersion, @Param("status") String status);

    /**
     * 把排程数据发布到中间库
     * @param dataVersion 接口发布版本号
     * @param scheduleDate 排程日期
     * @param ids  排程发布的ids
     * @param factoryCode 厂别
     * @param companyCode 分公司编号
     * @param createTime  数据同步时间
     */
    void deployGdyyScheduleToMid(@Param("dataVersion") String dataVersion, @Param("scheduleDate") Date scheduleDate, @Param("ids") long[] ids,
                                 @Param("factoryCode") String factoryCode, @Param("companyCode") String companyCode,
                                 @Param("createTime") Date createTime);

    /**
     * 获取排程结果统计信息
     *
     * @param scheduleResult 排程日期
     * @return 结果
     */
    ScheduleSummaryVo getSummaryVo(GdyyScheduleResultDto scheduleResult);

    /**
     * 获取昨日早班计划量
     *
     * @param scheduleResult 排程日期
     * @return 结果
     */
    ScheduleSummaryVo getLastDayPlanQty(GdyyScheduleResultDto scheduleResult);

    /**
     * 获取成型消耗量
     *
     * @param scheduleResult 排程日期
     * @return 结果
     */
    ScheduleSummaryVo getCxConsume(GdyyScheduleResultDto scheduleResult);
}
