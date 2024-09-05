package com.zlt.aps.cx.mapper;

import com.zlt.aps.common.core.domain.SchedulePublishRecord;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.cx.api.domain.entity.CxScheduleSub;
import com.zlt.aps.cx.api.domain.entity.Gante;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 成型排程结果Mapper接口
 *
 * @author zlt
 * @date 2021-07-12
 */
public interface CxScheduleResultMapper {
    /**
     * 查询成型排程结果
     *
     * @param id 成型排程结果ID
     * @return 成型排程结果
     */
    public CxScheduleResult selectCxScheduleResultById(Long id);

    public CxScheduleResult selectCxScheduleResultByIdForQty(Long id);

    /**
     * 查询成型排程结果列表
     *
     * @param cxScheduleResult 成型排程结果
     * @return 成型排程结果集合
     */
    public List<CxScheduleResult> selectCxScheduleResultList(CxScheduleResult cxScheduleResult);

    public List<CxScheduleResult> finishedList(CxScheduleResult cxScheduleResult);
    /**
     * 硫化自动排程校验
     */
    public List<CxScheduleResult> getLhList(CxScheduleResult cxScheduleResult);

    /**
     * 查询非本id的且包含该硫化机的记录
     */
    public List<CxScheduleResult> getListByLhMachineCode(CxScheduleResult cxScheduleResult);


    public List<CxScheduleResult> selectCxScheduleResultListForExport(CxScheduleResult cxScheduleResult);

    /**
     * 新增成型排程结果
     *
     * @param cxScheduleResult 成型排程结果
     * @return 结果
     */
    public int insertCxScheduleResult(CxScheduleResult cxScheduleResult);

    /**
     * 修改成型排程结果
     *
     * @param cxScheduleResult 成型排程结果
     * @return 结果
     */
    public int updateCxScheduleResult(CxScheduleResult cxScheduleResult);

    /**
     * 修改施工版本
     * @param cxScheduleResult
     * @return
     */
    public int changeBomDataVersion(CxScheduleResult cxScheduleResult);

    /**
     * 调量更新
     */
    public int updateTmScheduleResult(CxScheduleSub cxScheduleSub);

    public int updateTcScheduleResult(CxScheduleSub cxScheduleSub);

    public int updateTqScheduleResult(CxScheduleSub cxScheduleSub);

    public int updateNcScheduleResult(CxScheduleSub cxScheduleSub);

    public int updateCd15ScheduleResult(CxScheduleSub cxScheduleSub);

    public int updateCd90cheduleResult(CxScheduleSub cxScheduleSub);

    public int updateGdyyScheduleResult(CxScheduleSub cxScheduleSub);

    public int updateXwyyScheduleResult(CxScheduleSub cxScheduleSub);

    public int updateGsqScheduleResult(CxScheduleSub cxScheduleSub);

    /**
     * 删除成型排程结果
     *
     * @param id 成型排程结果ID
     * @return 结果
     */
    public int deleteCxScheduleResultById(Long id);

    /**
     * 批量删除成型排程结果
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteCxScheduleResultByIds(Long[] ids);


    /**
     * 手工收尾
     */
    public int manualClose(Long[] ids);

    public int batchUpdate(@Param("array") long[] ids, @Param("status") String status);

    public List<CxScheduleResult> checkScheduleResultUnique(CxScheduleResult cxScheduleResult);

    /**
     * 保存发布日志
     *
     * @param schedulePublishRecord 要保存的发布日志
     * @return 结果
     */
    public int insertPublishRecord(SchedulePublishRecord schedulePublishRecord);

    /**
     * 查询指定日期的成型排程结果是否已经发布
     *
     * @param schedulePublishRecord 要查询的日期及工序参数
     * @return 查询到的记录条数
     */
    public int isPublish(SchedulePublishRecord schedulePublishRecord);

    /**
     * 把排程数据发布到中间库
     * @param dataVersion 接口发布版本号
     * @param ids  排程发布的ids
	 * @param factoryCode 厂别
	 * @param companyCode 分公司编号
     */
    public void deployScheduleToMes(@Param("dataVersion") String dataVersion, @Param("ids") long[] ids,
			@Param("factoryCode") String factoryCode, @Param("companyCode") String companyCode, @Param("language") String language);

    /**
     * 在产下发MPS
     */
    public List<CxScheduleResult> producingIssue(CxScheduleResult cxScheduleResult);

    /**
     * 单机自动排程校验
     */
    public List<CxScheduleResult> singleMachinAutoPlanValidate(CxScheduleResult cxScheduleResult);

    /**
     * 生成模具变动单校验
     */
    public int modelChangeValidate(CxScheduleResult entity);

    /**
     * 查询待删除列表校验需要校验
     * @param ids
     */
    public List<CxScheduleResult> selectRemoveList(@Param("ids") Long[] ids);

    /**
     * 根据排程日期查询当前日期发布状态为"发布中"或"超时失败"的记录（查询成型排程结果）
     * @param scheduleDate 排程日期
     * @return 查询到的记录数
     */
    public int isReleasingOrTimeoutByDate(Date scheduleDate);

    /**
     * 根据排程日期查询当前日期发布状态为"发布中"或"超时失败"的记录（查询硫化排程结果）
     *
     * @param scheduleDate 排程日期
     * @return 查询到的记录数
     */
    public int lhIsReleasingOrTimeoutByDate(Date scheduleDate);

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
    public int changeReleaseStatus(CxScheduleResult entity);

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
     * 查询成型排程最新排程日期
     * @return 最新排程日期
     */
	public Date selectMaxScheduleDate();

    /**
     * 根据id查询未发布记录的条数
     * @param ids id
     * @return 未发布的记录条数
     */
    public int isPublishByIds(Long[] ids);

    /**
     * 根据排程日期、胎胚代码、SAP、施工版本查询记录
     * @return 查询到的记录
     */
    List<CxScheduleResult> selectByScheduleDateAndCode(CxScheduleResult scheduleResult);

    /**
     * 检测施工版本为空的数据是否存在
     * @param scheduleDate
     * @return
     */
    int checkBomDataVersionEmpty(@Param("scheduleDate") Date scheduleDate);

    /**
     * 查询成型排程机台甘特图数据
     */
    public List<Gante> getCxGanteData(Gante gante);

    /**
     * 查询成型排程规格甘特图数据
     */
    public List<Gante> getCxSpecGanteData(Gante gante);



}
