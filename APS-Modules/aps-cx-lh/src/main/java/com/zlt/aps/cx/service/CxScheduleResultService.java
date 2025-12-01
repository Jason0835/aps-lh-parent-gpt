package com.zlt.aps.cx.service;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cxlh.cx.api.domain.dto.CxTransferDeskDTO;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxOnlineImport;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.cxlh.cx.api.domain.vo.CxGanttVo;
import com.zlt.aps.lh.api.domain.bo.ValidateResult;
import com.zlt.aps.monthplan.api.domain.vo.MdmProductConstructionVO;
import com.zlt.bill.common.service.IDocService;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 成型排程服务实现接口
 *
 * @author tlt Nick
 * time：2025-02-12
 */
public interface CxScheduleResultService extends IDocService<CxScheduleResult> {

    /**
     * 查询导出数据
     *
     * @param cxScheduleResult
     * @return List<CxScheduleResult>
     */
    List<CxScheduleResult> selectListExportData(QueryWrapper<CxScheduleResult> cxScheduleResult);

    /**
     * 依据日期查询成型工序排程记录
     *
     * @param scheduleDate 排程天数
     * @return List<CxScheduleResult> 成型排程结果
     */
    List<CxScheduleResult> selectListByDate(Date scheduleDate);

    /**
     * 导入期初数据
     *
     * @param list         导入集合
     * @param id           日志ID
     * @param scheduleDate 导入日期
     * @return 返回结果
     */
    AjaxResult importData2(List<CxScheduleResult> list, Long id, String scheduleDate);

    /**
     * 导入期初数据
     *
     * @param list         导入集合
     * @param id           日志ID
     * @param scheduleDate 导入日期
     * @return 返回结果
     */
    AjaxResult importData3(List<CxOnlineImport> list, Long id, String scheduleDate);


    /**
     * title：按照日期获取成型排程计划
     *
     * @param scheduleDate 排程日期
     * @param scheduleLog  日志
     * @param values 排程结果
     * @return List<CxScheduleResult> 排程结果
     */
    public List<CxScheduleResult> getScheduleCxScheduleResults(Date scheduleDate, StringBuilder scheduleLog, Collection<CxScheduleResult> values);

    /**
     * title: 生成最终排程
     *
     * @param cxScheduleResultContextMap 排程结果
     */
    void generateFinalSchedule(Map<String, CxScheduleResult> cxScheduleResultContextMap);

    /**
     * title: 生成最终排程过程日志
     *
     * @param scheduleLog 排程结果日志
     * @param cxBatchNo
     */
    void genScheduleLog(String scheduleLog, String cxBatchNo);

    /**
     * 根据id查询当前日期发布状态为"发布中"或"超时失败"的记录
     *
     * @param ids id
     * @return 查询到的记录数
     */
    Long isReleasingOrTimeoutByIds(Long[] ids);

    /**
     * title: 修改排程数量
     *
     * @param cxScheduleResult 调量结果
     * @return 调整结果
     */
    AjaxResult changeQty(CxScheduleResult cxScheduleResult);

    /**
     * title: 修改排程
     *
     * @param cxScheduleResult 修改结果
     * @return 修改结果
     */
    AjaxResult edit(CxScheduleResult cxScheduleResult);

    /**
     * 根据排程日期查询当前日期发布状态为"发布中"或"超时失败"的记录（查询成型排程结果）
     *
     * @param scheduleDate 排程日期
     * @return 查询到的记录数
     */
    public Long isReleasingOrTimeoutByDate(Date scheduleDate);

    /**
     * title: 新增排程
     *
     * @param cxScheduleResult 排程结果
     * @return 新增结果
     */
    AjaxResult add(CxScheduleResult cxScheduleResult);

    /**
     * 手动插单参数验证及其他相关数据验证
     *
     * @param cxScheduleResult
     * @return
     */
    public ValidateResult insertPreCheck(CxScheduleResult cxScheduleResult);

    /**
     * 根据id查询当前日期发布状态为"发布中"或"超时失败"的记录
     *
     * @param ids id
     * @return 查询到的记录数
     */
    int isReleasingOrTimeoutByIds(long[] ids);

    /**
     * 根据ID查询
     *
     * @param id
     * @return
     */
    CxScheduleResult selectById(Long id);

    /**
     * 根据机台编号和排程时间查询排程结果
     *
     * @param factoryCode
     * @param machineCode
     * @param scheduleDate
     * @return
     */
    CxScheduleResult getScheduleResultByMachineCodeAndScheduleDate(String factoryCode, String machineCode, Date scheduleDate);

    /**
     * 转机台
     *
     * @param dto
     */
    void changeMachine(CxTransferDeskDTO dto);

    /**
     * 调量进行班次计划量定额校验
     *
     * @param cxScheduleResult
     * @return
     */
    public ValidateResult changePlanQtyPreCheck(CxScheduleResult cxScheduleResult);

    /**
     * 校验排程结果是否唯一
     * @param cxScheduleResult 校验对象
     * @return 校验结果
     */
    public List<CxScheduleResult> checkScheduleResultUnique(CxScheduleResult cxScheduleResult);

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     * @param operType 操作类型：0--转机台、1--调量、2--插单
     */
    void insetDispatcherLogInsertOrder(String operType, List<CxScheduleResult> scheduleResults, CxScheduleResult newSchedule);


    /**
     * 依据物料号，胎胚代码，规格代码获取Bom信息，和成型法
     * @param cxScheduleResult  查询对象
     * @return Bom信息
     */
    MdmProductConstructionVO getBomData(CxScheduleResult cxScheduleResult);

    /**
     * 加载关联任务列表
     */
    List<CxScheduleResult> loadRelatedTasks(CxScheduleResult cxScheduleResult);


    /**
     * 任务重排核心方法 - 根据欠胎时间和班次产能重新分配生产任务
     */
    List<CxScheduleResult> cxScheduleResultListReSort(List<CxScheduleResult> allTasks, Date scheduleDate);

    /**
     * 批量更新任务状态
     */
    void updateTasksStatus(List<CxScheduleResult> resortedTasks);

    /**
     * 依据ID获取未发布的数据条数
     * @param ids ids
     * @return 未发布的数据条数
     */
    Long isPublishByIds(Long[] ids);


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
     * 更改发布状态
     * @param entity 排程日期
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    public int changeReleaseStatus(CxScheduleResult entity);



    /**
     * 更新指定相关数据记录的发布状态
     *
     * @param ids         排程ID列表
     * @param status      更新的状态
     */
    int updateRelaseStatus(long[] ids, String status);

    /**
     * 验证选中记录的施工信息
     *
     * @param ids
     * @return
     */
    String validateConstructionByIds(Long[] ids);

    /**
     * 验证列表中如果存在施工版本为空给出错误提示
     *
     * @param cxScheduleResultList
     * @return
     */
    String checkBomDataVersion(List<CxScheduleResult> cxScheduleResultList);

    /**
     * 排程发布
     *
     * @param scheduleDate 排程日期
     * @param dataVersion  接口数据版本
     * @param factoryCode  分厂代号
     * @param companyCode  分公司代号
     */
    public AjaxResult publish(long[] ids, Date scheduleDate, String dataVersion, String factoryCode, String companyCode);

    /**
     * 将现场数据变成排程数据处理逻辑
     * @param cxScheduleResult
     * @return
     */
    AjaxResult parseCxScheduleResult(CxScheduleResult cxScheduleResult);


    /**
     * 生产计划员可用的导出
     */
    public List<CxOnlineImport> genXcScheduleResult(CxScheduleResult cxScheduleResult);

    /**
     * 查询成型机台甘特图
     *
     * @param queryVO 查询参数
     * @return 结果
     */
    AjaxResult selectMachineGantt(CxGanttVo queryVO);


    /**
     * 成型触发反向修改硫化计划
     *
     * @param cxScheduleResult 查询参数
     * @return 结果
     */
    AjaxResult updateLhScheduleResult(CxScheduleResult cxScheduleResult);

    /**
     * 校验施工切换版本是否还有剩余旧版本半部件的库存
     * @param embryoCode 施工代号
     * @param oldVersion 旧版本
     * @param newVersion 新版本
     * @param scheduleDate 排程日期
     * @return 结果
     */
    AjaxResult checkConsOldVersionStock(String embryoCode, String oldVersion, String newVersion, Date scheduleDate);
}


