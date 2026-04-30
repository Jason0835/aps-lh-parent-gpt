package com.zlt.aps.lh.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.lh.api.domain.entity.LhPrecisionPlan;
import com.zlt.aps.lh.api.domain.vo.LhPrecisionPlanImportVO;
import com.zlt.aps.lh.api.domain.vo.LhPrecisionPlanVo;
import com.zlt.aps.mp.api.domain.entity.MdmDevMaintenancePlan;
import com.zlt.bill.common.service.IDocService;

import java.util.List;
import java.util.Map;

/**
 * 硫化精度计划Service接口
 *
 * @author APS Team
 */
public interface ILhPrecisionPlanService extends IDocService<LhPrecisionPlan> {

    /**
     * 查询硫化精度计划列表
     *
     * @param vo 查询条件
     * @return 计划列表
     */
    List<LhPrecisionPlan> selectLhPrecisionPlanList(LhPrecisionPlanVo vo);

    /**
     * 校验唯一性
     *
     * @param entity 硫化精度计划实体
     * @return UserConstants.NOT_UNIQUE 不唯一，UserConstants.UNIQUE 唯一
     */
    String checkUnique(LhPrecisionPlan entity);

    /**
     * 从MES同步数据生成硫化精度初版计划
     *
     * @param year 年份
     * @return 生成数量
     */
    int generatePlansFromMes(Integer year);

    /**
     * 自动生成年度硫化精度计划
     *
     * @param year 年份
     * @return 生成数量
     */
    int autoGenerateYearlyPlans(Integer year);

    /**
     * 执行30天预警检查
     *
     * @return 预警数量
     */
    int checkWarning();

    /**
     * 批量更新到期天数
     *
     * @return 更新数量
     */
    int batchUpdateDaysToDue();

    /**
     * MES回传实际完成时间
     *
     * @param mesSourceId MES来源ID
     * @param actualDate 实际日期
     * @return 是否成功
     */
    boolean updateActualDate(Long mesSourceId, String actualDate);

    /**
     * 根据设备保养计划生成硫化精度计划
     *
     * @param maintenancePlans 设备保养计划列表
     * @return 生成数量
     */
    int generateFromMaintenancePlan(List<MdmDevMaintenancePlan> maintenancePlans);

    /**
     * 导入硫化精度计划数据
     *
     * @param list 导入数据列表
     * @param updateSupport 是否更新支持
     * @param importLogId 导入日志ID
     * @return 导入结果
     */
    AjaxResult importDataFeign(List<LhPrecisionPlanImportVO> list, boolean updateSupport, Long importLogId);

    /**
     * 硫化排程回填计划排程精度日期
     * 找到对应机台且实际执行日期还没值的数据回填计划排程精度日期
     *
     * @param machineCode 机台编号
     * @param factoryCode 分厂编码
     * @param scheduleDate 计划排程精度日期
     * @return 回填数量
     */
    int fillScheduleDate(String machineCode, String factoryCode, java.util.Date scheduleDate);

    /**
     * 批量硫化排程回填计划排程精度日期
     * 将循环内的逐条DB查询优化为外层批量查询+内存分组匹配，逐条update优化为批量操作
     *
     * @param fillList 回填数据列表，每项包含machineCode、factoryCode、scheduleDate
     * @return 成功回填的数量
     */
    int batchFillScheduleDate(List<java.util.Map<String, Object>> fillList);

    /**
     * MES回填实际精度执行日期
     * 匹配最接近的计划排程精度日期且实际执行时间为空的硫化精度计划
     * 回填后立马推算生成下一次硫化精度计划
     *
     * @param machineCode 机台编号
     * @param factoryCode 分厂编码
     * @param actualDate 实际执行日期
     * @return 是否成功
     */
    boolean fillActualDateAndGenerateNext(String machineCode, String factoryCode, java.util.Date actualDate);

    /**
     * 批量MES回填实际精度执行日期并生成下一次计划
     * 将循环内的逐条DB查询优化为外层批量查询+内存过滤，逐条insert/update优化为批量操作
     *
     * @param fillList 回填数据列表，每项包含machineCode、factoryCode、actualDate
     * @return 成功回填的数量
     */
    int batchFillActualDateAndGenerateNext(List<java.util.Map<String, Object>> fillList);

    /**
     * 查询待下发的硫化精度计划列表
     * 计划排程精度日期有值且实际执行日期为空且未下发的数据
     *
     * @return 待下发计划列表
     */
    List<LhPrecisionPlan> selectPendingIssuePlans();

    /**
     * 查询待下发的硫化精度计划列表（转换为下发实体）
     * 计划排程精度日期有值且实际执行日期为空的数据
     *
     * @param factoryCode 分厂编码
     * @return 待下发计划列表
     */
    List<com.zlt.aps.lh.api.domain.entity.LhPrecisionPlanIssue> listPendingIssuePlans(String factoryCode);
}
