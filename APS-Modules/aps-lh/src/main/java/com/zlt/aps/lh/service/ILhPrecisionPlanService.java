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
     * 从MES同步数据生成硫化精度初版计划（按版本号前缀过滤）
     * 只处理版本号前缀匹配的数据，如APS_MES_AH01
     *
     * @param versionPrefix 版本号前缀（如：APS_MES_AH01）
     * @param year 年份
     * @return 生成数量
     */
    int generatePlansFromMesByVersionPrefix(String versionPrefix, Integer year);

    /**
     * 从MES同步数据生成硫化精度初版计划（按版本号前缀过滤，不限最大版本号）
     * 与generatePlansFromMesByVersionPrefix的区别：不限制最大版本号，版本前缀匹配的所有版本数据都参与生成
     *
     * @param versionPrefix 版本号前缀（如：APS_MES_AH01）
     * @param year 年份
     * @return 生成数量
     */
    int generatePlansFromMesByVersionPrefixAllVersions(String versionPrefix, Integer year);

    /**
     * 临时任务：按计划时间所在年份过滤，从MES同步数据生成硫化精度计划
     * 取最新版本号+版本前缀匹配+计划时间在指定年份的硫化精度数据，生成目标年度的精度计划
     * 用于MES全量版本数据中只取特定年份的数据，避免跨年数据干扰
     *
     * @param versionPrefix 版本号前缀（如：APS_MES_AH01）
     * @param operYear 计划时间所在年份（如：2025，只取operTime在该年份的数据）
     * @param targetYear 要生成的目标年份（如：2026）
     * @return 生成数量
     */
    int generatePlansFromMesByOperYear(String versionPrefix, Integer operYear, Integer targetYear);

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

    /**
     * 按设备保养计划(MES同步数据)分发写入硫化精度计划表
     * 现逻辑：MES全权决定计划时间(OPER_TIME)和实际完成时间(FIRST_WASH_TIME)，
     * APS侧不再回填实际日期、不再生成下一次精度计划。
     * 本方法根据MES字段值直接计算派生字段(DUE_DATE/DAYS_TO_DUE/COMPLETION_STATUS等)并upsert到T_LH_PRECISION_PLAN。
     * 匹配键：MES_SOURCE_ID（=T_MDM_DEV_MAINTENANCE_PLAN.ID）
     *
     * @param maintenancePlanIds 设备保养计划ID列表（仅处理PRECISION_TYPE='硫化精度'的数据）
     * @return 分发写入的记录数
     */
    int dispatchFromMaintenancePlan(List<Long> maintenancePlanIds);
}
