package com.zlt.aps.cx.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.cx.api.domain.entity.Gante;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;


/**
 * 成型排程结果Service接口
 *
 * @author zlt
 * @date 2021-07-12
 */
public interface CxScheduleResultService {
    /**
     * 查询成型排程结果
     *
     * @param id 成型排程结果ID
     * @return 成型排程结果
     */
    public CxScheduleResult selectCxScheduleResultById(Long id);

    public CxScheduleResult selectCxScheduleResultByIdForQty(Long id);

    /**
     * 硫化自动排程校验
     */
    public List<CxScheduleResult> getLhList(CxScheduleResult cxScheduleResult);

    /**
     * 查询非本id的且包含该硫化机的记录
     */
    public List<CxScheduleResult> getListByLhMachineCode(CxScheduleResult cxScheduleResult);

    /**
     * 查询成型排程结果列表
     *
     * @param cxScheduleResult 成型排程结果
     * @return 成型排程结果集合
     */
    public List<CxScheduleResult> selectCxScheduleResultList(CxScheduleResult cxScheduleResult);

    public List<CxScheduleResult> finishedList(CxScheduleResult cxScheduleResult);


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

    public int updateCxScheduleResultForMolds(CxScheduleResult scheduleResult,CxScheduleResult osEntity);

    public int modifyStatus(CxScheduleResult cxScheduleResult);

    /**
     * 调量更新
     */
    @Transactional(rollbackFor = Exception.class)
    public int updateCxScheduleResultForQty(CxScheduleResult cxScheduleResult);

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     * @param operType 操作类型：0--转机台、1--调量
     * @param newSchedule
     */
    void insetDispatcherLog(String operType, CxScheduleResult oldSchedule, CxScheduleResult newSchedule);

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     * @param operType 操作类型：0--转机台、1--调量、2--插单
     */
    void insetDispatcherLogInsertOrder(String operType, List<CxScheduleResult> scheduleResults, CxScheduleResult newSchedule);

    /**
     * 根据排程日期和代码查询排程结果
     * @param scheduleResult 排程日期、代码
     * @return 查询到的数据
     */
    List<CxScheduleResult> selectByScheduleDateAndCode(CxScheduleResult scheduleResult);

    public int changeBomDataVersion(CxScheduleResult cxScheduleResult);
    /**
     * 批量删除成型排程结果
     *
     * @param ids 需要删除的成型排程结果ID
     * @return 结果
     */
    public int deleteCxScheduleResultByIds(Long[] ids);

    /**
     * 手工收尾
     */
    public int manualClose(Long[] ids);

    /**
     * 删除成型排程结果信息
     *
     * @param id 成型排程结果ID
     * @return 结果
     */
    public int deleteCxScheduleResultById(Long id);


    public int batchUpdate(long[] ids);

    public List<CxScheduleResult> checkScheduleResultUnique(CxScheduleResult cxScheduleResult);

    /**
     * 查询成型排程结果列表
     *
     * @param cxScheduleResult 成型排程结果
     * @return 成型排程结果集合
     */
    public List<CxScheduleResult> selectCxScheduleResultListForExport(CxScheduleResult cxScheduleResult);

    /**
     * 查询排程日期是否已发布
     *
     * @param scheduleDate 排程日期
     * @return 是否已经发布
     */
    Boolean isCxPublish(Date scheduleDate);

    /**
     * 查询排程日期是否已发布
     *
     * @param scheduleDate 排程日期
     * @return 是否已经发布
     */
    Boolean isLhPublish(Date scheduleDate);

    /**
     * 排程发布批量更新状态
     *
     * @param ids
     * @param status	发布状态
     * @return
     */
    public int schedulePublish(long[] ids, String status);

    /**
     * 导入数据
     */
    AjaxResult importData(List<CxScheduleResult> list, Long importLogId, String scheduleDate);

    /**
     * 排程发布
     * @param scheduleDate  排程日期
     * @param dataVersion 接口数据版本
     * @param factoryCode 分厂代号
     * @param companyCode  分公司代号
     */
    public AjaxResult publish(long[] ids,Date scheduleDate,String dataVersion,String factoryCode,String companyCode);

	/**
	 * 更新指定相关数据记录的发布状态
	 *
	 * @param dataVersion 数据版本
	 * @param ids         排程ID列表
	 * @param status      更新的状态
	 */
	void updateRelaseStatus(String dataVersion, long[] ids, String status);

    /**
     * 获取-使用模数
     */
    public CxScheduleResult getMolds(CxScheduleResult cxScheduleResult);

    /**
     * 校验-使用模数
     */
    public AjaxResult modifyMoldsValidate(CxScheduleResult cxScheduleResult);

    /**
     * 修改-使用模数
     */
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult modifyMolds(CxScheduleResult cxScheduleResult);

    /**
     * 在产下发MPS
     */
    public AjaxResult producingIssue(CxScheduleResult entity);


    /**
     * 单机自动排程校验
     */
    public List<CxScheduleResult> singleMachinAutoPlanValidate(CxScheduleResult cxScheduleResult);

    /**
     * 生成模具变动单校验
     */
    public AjaxResult modelChangeValidate(CxScheduleResult entity);

    /**
     * 验证列表中如果存在施工版本为空给出错误提示
     * @param cxScheduleResultList
     * @return
     */
    String checkBomDataVersion(List<CxScheduleResult> cxScheduleResultList);

    /**
     * 删除排程规格校验
     * @param ids
     * @return
     */
    String removeResultCheck(Long[] ids,List<CxScheduleResult> removeList);

    /**
     * 删除成型排程结果
     * @param removeList
     * @return
     */
    int removeCxSecheduleResultByList(Long[] ids,List<CxScheduleResult> removeList);

    /**
     * 根据排程日期查询当前日期发布状态为"发布中"或"超时失败"的记录（查询成型排程结果）
     * @param scheduleDate 排程日期
     * @return 查询到的记录数
     */
    public int isReleasingOrTimeoutByDate(Date scheduleDate);

    /**
     * 根据排程日期查询当前日期发布状态为"发布中"或"超时失败"的记录（查询硫化排程结果）
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
    @Transactional(rollbackFor = Exception.class)
    public int changeReleaseStatus(CxScheduleResult entity);

    /**
     * 验证选中记录的施工信息
     * @param ids
     * @return
     */
    String validateConstructionByIds(Long[] ids);

    /**
     * 查询成型排程最新排程日期
     * @return 最新排程日期
     */
    public Date selectMaxScheduleDate();

    int isPublishByIds(Long[] ids);

    /**
     * 查询成型排程机台甘特图数据
     */
    public List<Gante> getCxGanteData(Gante gante);



}
