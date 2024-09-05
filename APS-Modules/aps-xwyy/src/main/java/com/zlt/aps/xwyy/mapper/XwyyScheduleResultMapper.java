package com.zlt.aps.xwyy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.common.core.domain.SchedulePublishRecord;
import com.zlt.aps.xwyy.api.domain.dto.XwyyScheduleResultDto;
import com.zlt.aps.xwyy.entity.XwyyScheduleResult;
import com.zlt.aps.xwyy.vo.XwyyScheduleOriginalSumPlanVo;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 纤维压延排程结果Mapper接口
 *
 * @author chen
 * @date 2021-07-06
 */
public interface XwyyScheduleResultMapper extends BaseMapper<XwyyScheduleResult> {

    /**
     * 查询排程结果列表
     *
     * @param scheduleResult 查询条件
     * @return 查询到的集合
     */
    public List<XwyyScheduleResultDto> selectScheduleResultList(XwyyScheduleResult scheduleResult);

    /**
     * 发布指定日期的所有排程结果
     *
     * @param scheduleResult 日期条件
     */
    public void publishAll(XwyyScheduleResult scheduleResult);

    /**
     * 根据id查询排程结果信息
     *
     * @param id 要查询的排程结果id
     * @return 查询到的信息
     */
    public XwyyScheduleResultDto selectScheduleResultById(Long id);

    /**
     * 保存发布日志
     *
     * @param schedulePublishRecord 要保存的发布日志
     * @return 结果
     */
    public int insertPublishRecord(SchedulePublishRecord schedulePublishRecord);

    /**
     * 查询指定日期的排程结果是否已经发布
     *
     * @param schedulePublishRecord 要查询的日期及工序参数
     * @return 查询到的记录条数
     */
    public int isPublish(SchedulePublishRecord schedulePublishRecord);

    /**
     * 根据排程日期、物料编号、机台id校验唯一性
     * @param scheduleResult 要校验记录
     * @return 查询到的记录数
     */
    public int checkUnique(XwyyScheduleResult scheduleResult);

    /**
     * 把排程数据发布到中间库
     * @param dataVersion 接口发布版本号
     * @param scheduleDate 排程日期
     * @param ids  排程发布的ids
	 * @param factoryCode 厂别
	 * @param companyCode 分公司编号
	 * @param createTime  数据同步时间
     */
    void deployXwyyScheduleToMid(@Param("dataVersion") String dataVersion, @Param("scheduleDate") Date scheduleDate, @Param("ids") long[] ids,
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
    public int isReleasingOrTimeoutByIds(long[] ids);


    /**
     * 更改发布状态
     * @param scheduleDate 排程日期
     * @return 结果
     */
    public int changeReleaseStatus(XwyyScheduleResult scheduleResult);

    /**
	 * 更新发布日志状态
	 *
	 * @param dataVersion 数据版本
	 * @param status      状态
	 */
	public int updatePublishRecordVersion(@Param("dataVersion") String dataVersion, @Param("status") String status);

    int checkXwyyCodeExist(XwyyScheduleResultDto dto);

    public int deleteByIds(Long[] ids);

    public int batchUpdate(@Param("array") long[] ids, @Param("status") String status);

    /**
     * 根据id查询未发布记录的条数
     * @param ids id
     * @return 未发布的记录条数
     */
    public int isPublishByIds(Long[] ids);

    List<XwyyScheduleResultDto> selectByIds(@Param("list") List<Long> ids2);

    /**
     * 根据排程日期、原线代码获取全部计划量的和
     * @param params 参数
     */
    public Double selectPlanSumByParams(Map<String, Object> params);

    /**
     * 根据排程日期、帘布大卷代号、原线代码更新原线卷数
     * @param params 参数
     */
    public int updateOriginalLineQtyNumByParams(Map<String, Object> params);

    /**
     * 根据排程日期、帘布大卷代号、原线代码、原线品牌更新原线品牌个数
     * @param params 参数
     */
    public int updateOriginalBrandNumByParams(Map<String, Object> params);

    /**
     * 根据id数组查询对应记录
     * @param ids id数组
     * @return 查询到的记录
     */
    public List<XwyyScheduleResult> selectListByIds(@Param("array") Long[] ids);

    /**
     * 根据排程日期、原线代码集合根据原线代码分组查询对应的计划量和最大帘布大卷代号
     * @param scheduleDate 排程日期
     * @param originalLineCodeList 原线代码集合
     * @return 查询到的集合
     */
    public List<XwyyScheduleOriginalSumPlanVo> selectSumPlanByOriginalLineCode(@Param("scheduleDate") Date scheduleDate, @Param("originalLineCodeList") Set<String> originalLineCodeList);

    /**
     * 根据排程日期、原线代码集合根据原线代码、原线品牌分组查询对应的计划量和最大帘布大卷代号
     * @param scheduleDate 排程日期
     * @param originalLineCodeList 原线代码集合
     * @return 查询到的集合
     */
    public List<XwyyScheduleOriginalSumPlanVo> selectSumPlanByOriginalBrand(@Param("scheduleDate") Date scheduleDate, @Param("originalLineCodeList") Set<String> originalLineCodeList);

    /**
     * 根据id更新胶料车数
     * @param scheduleResult 参数
     */
    void updateRubberCarNumber(XwyyScheduleResult scheduleResult);

    /**
     * 根据排程日期和帘布代码查询记录
     * @return 查询到的记录
     */
    List<XwyyScheduleResult> selectByScheduleDateAndCode(XwyyScheduleResult scheduleResult);

    /**
     * 归并中夜班计划量，合并到同一个班次
     * @param map 要合并的id及合并的班次(type = 1合并到中班，2 合并到夜班)
     * @return 修改行数
     */
    int combinationMiddleAndNight(Map<String, Object> map);
}
