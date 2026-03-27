package com.zlt.aps.lh.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.exception.BusinessException;
import com.zlt.aps.lh.api.domain.dto.*;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhGanttVo;
import com.zlt.aps.lh.api.domain.vo.LhMachineInfoVo;
import com.zlt.aps.lh.api.domain.vo.LhScheduleResultVo;
import com.zlt.aps.mp.api.domain.entity.LhMonthPlanSurplusDetail;
import com.zlt.aps.mp.api.domain.vo.LhMonthFinishQtyVo;
import com.zlt.aps.mp.api.domain.vo.LhMonthFinishStatisticsDayQtyVo;
import com.zlt.aps.mp.api.domain.vo.SpecCodeAndProductCodeVO;
import com.zlt.bill.common.service.IDocService;

import java.util.Date;
import java.util.List;

/**
 * @author xh
 * @version 1.0
 * @Description
 * @date 2025/2/13
 */
public interface LhScheduleResultService extends IDocService<LhScheduleResult> {

    /**
     * 查询一段时间内，存在于硫化排程的规格列表
     * @param factoryCode
     * @param beginDate
     * @param endDate
     * @param checkSpecCodeList
     * @return
     */
    List<String> selectLhSpecCodeList(String factoryCode,Date beginDate,Date endDate,
                                             List<String> checkSpecCodeList);

    /**
     * 硫化自动排程
     * @param autoLhScheduleResultDTO
     * @return
     */
    void autoLhScheduleResult(AutoLhScheduleResultDTO autoLhScheduleResultDTO) throws BusinessException;

    /**
     * 查询List
     * @param lhScheduleResult
     * @return
     */
    List<LhScheduleResult> selectList(QueryWrapper<LhScheduleResult> lhScheduleResult);

    /**
     * 根据ID查询
     * @param id
     * @return
     */
    LhScheduleResult selectById(Long id);

    /**
     * 获取上一天的排程结果
     * @return
     */
    List<LhScheduleResultVo> getTDayLhScheduleResult(Date scheduleTime, String factoryCode);

    /**
     * 获取上一天的排程结果
     * @return
     */
    List<LhScheduleResultVo> getLastDayLhScheduleResult(Date scheduleTime, String factoryCode, Integer traceDays);

    /**
     * 导入数据
     * @param list 导入集合
     * @param importLogId 日志ID
     * @param scheduleDate 导入日期
     * @return 返回结果
     */
    AjaxResult importData2(List<LhScheduleResult> list, Long importLogId, Date scheduleDate);

    /**
     * title:按照日期获取硫化排程计划
     *
     * @param currentDay  排程日期
     * @param scheduleLog 日志
     * @return List<CxScheduleResult> 排程结果
     */
    public List<LhScheduleResult> getScheduleLhScheduleResults(Date currentDay, StringBuilder scheduleLog);


    /**
     * 查询排程当天可查询机台信息
     * @param insertParamDTO
     * @return
     */
    List<LhMachineInfoVo> getScheduleMachineInfo(LhOrderInsertParamDTO insertParamDTO);

    /**
     * 插单
     * @param dto
     * @return
     */
    void insertOrder(LhOrderInsertDTO dto);


    /**
     * 根据id查询当前日期发布状态为"发布中"或"超时失败"的记录
     * @param ids id
     * @return 查询到的记录数
     */
    int isReleasingOrTimeoutByIds(long[] ids);

    /**
     * 转机台
     * @param dto
     */
    void changeMachine(LhTransferDeskDTO dto);

    /**
     * 更新排程结果
     * @param dto
     */
    void updateScheduleResult(LhScheduleResultUpdateDTO dto);

    /**
     * 根据机台编号和排程时间查询排程结果
     * @param factoryCode
     * @param machineCode
     * @param scheduleDate
     * @return
     */
    LhScheduleResult getScheduleResultByMachineCodeAndScheduleDate(String factoryCode,String machineCode,Date scheduleDate);

    /**
     * 根据分厂编码查询最后一条排程数据的排程时间
     * @return
     */
    Date selectLastScheduleTime(String factoryCode,Date lastScheduleTime);

    /**
     * 根据分厂编码和排产日期查询批次号
     * @param scheduleDate
     * @param factoryCode
     * @return
     */
    String selectBatchNoByScheduleDateAndFactoryCode(Date scheduleDate,String factoryCode);

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
     * 更新指定相关数据记录的发布状态
     *
     * @param dataVersion 数据版本
     * @param ids         排程ID列表
     * @param status      更新的状态
     */
    void updateReleaseStatus(String dataVersion, long[] ids, String status);

    /**
     * 查询排程日期是否已发布
     *
     * @param scheduleDate 排程日期
     * @return 是否已经发布
     */
    Boolean isPublish(Date scheduleDate);

    /**
     * 查询硫化机台甘特图
     *
     * @param queryVO 查询参数
     * @return 结果
     */
    AjaxResult selectMachineGantt(LhGanttVo queryVO);

    List<LhScheduleResult> getScheduleResultList(Date scheduleDate);

    void splitLeftRightMold(LhScheduleResult docEntity, List<LhScheduleResult> splitList);

    void copyToDbScheduleResult(LhScheduleResult scheduleResult, LhScheduleResult dbScheduleResult);

    void updateConstructionInfo(String factoryCode, List<LhScheduleResult> importList);

    int saveBatchByImport(List<LhScheduleResult> importList);

    List<SpecCodeAndProductCodeVO> getConstructionList(String factoryCode, List<String> specCodes);

    List<LhMonthFinishQtyVo> monthFinishQtyList(LhMonthPlanSurplusDetail queryVO);

    LhMonthFinishStatisticsDayQtyVo getStatisticsDay(LhMonthPlanSurplusDetail param);
}
