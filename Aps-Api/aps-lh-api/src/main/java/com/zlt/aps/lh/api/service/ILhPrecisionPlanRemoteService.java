package com.zlt.aps.lh.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.lh.api.domain.entity.LhPrecisionPlan;
import com.zlt.aps.lh.api.domain.vo.LhPrecisionPlanExportVO;
import com.zlt.aps.lh.api.domain.vo.LhPrecisionPlanImportVO;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 硫化精度计划远程服务接口
 *
 * @author APS Team
 */
@FeignClient(contextId = "ILhPrecisionPlanRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:/lh}")
public interface ILhPrecisionPlanRemoteService {

    /**
     * 查询硫化精度计划列表
     */
    @ApiOperation("查询硫化精度计划列表")
    @PostMapping("/lhPrecisionPlan/list")
    TableDataInfo listLhPrecisionPlan(@RequestBody LhPrecisionPlan lhPrecisionPlan);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/lhPrecisionPlan/{id}")
    LhPrecisionPlan getLhPrecisionPlanInfo(@PathVariable("id") Long id);

    /**
     * 保存硫化精度计划信息（id为空则新增，id不为空则修改）
     */
    @ApiOperation("保存硫化精度计划信息")
    @PostMapping("/lhPrecisionPlan/save")
    AjaxResult saveLhPrecisionPlan(@RequestBody LhPrecisionPlan lhPrecisionPlan);

    /**
     * 批量删除硫化精度计划
     */
    @ApiOperation("批量删除硫化精度计划")
    @PostMapping("/lhPrecisionPlan/delete/{ids}")
    AjaxResult deleteLhPrecisionPlan(@PathVariable("ids") Long[] ids);

    /**
     * 导出硫化精度计划列表
     */
    @ApiOperation("导出硫化精度计划列表")
    @PostMapping("/lhPrecisionPlan/exportData")
    List<LhPrecisionPlanExportVO> exportData(@RequestBody LhPrecisionPlan lhPrecisionPlan);

    /**
     * 导入硫化精度计划数据
     */
    @ApiOperation("导入硫化精度计划数据")
    @PostMapping("/lhPrecisionPlan/importDataFeign")
    AjaxResult importData(@RequestBody List<LhPrecisionPlanImportVO> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

    /**
     * 从MES同步数据生成硫化精度初版计划
     *
     * @param year 年份
     * @return 生成数量
     */
    @ApiOperation("从MES同步数据生成硫化精度初版计划")
    @PostMapping("/lhPrecisionPlan/generateFromMes")
    AjaxResult generatePlansFromMes(@RequestParam("year") Integer year);

    /**
     * 从MES同步数据生成硫化精度初版计划（按版本号前缀过滤）
     *
     * @param versionPrefix 版本号前缀（如：APS_MES_AH01）
     * @param year 年份
     * @return 生成数量
     */
    @ApiOperation("从MES同步数据生成硫化精度初版计划（按版本号前缀过滤）")
    @PostMapping("/lhPrecisionPlan/generateFromMesByVersionPrefix")
    AjaxResult generatePlansFromMesByVersionPrefix(@RequestParam("versionPrefix") String versionPrefix,
                                                    @RequestParam("year") Integer year);

    /**
     * 从MES同步数据生成硫化精度初版计划（按版本号前缀过滤，不限最大版本号）
     *
     * @param versionPrefix 版本号前缀（如：APS_MES_AH01）
     * @param year 年份
     * @return 生成数量
     */
    @ApiOperation("从MES同步数据生成硫化精度初版计划（按版本号前缀过滤，不限最大版本号）")
    @PostMapping("/lhPrecisionPlan/generateFromMesByVersionPrefixAllVersions")
    AjaxResult generatePlansFromMesByVersionPrefixAllVersions(@RequestParam("versionPrefix") String versionPrefix,
                                                               @RequestParam("year") Integer year);

    /**
     * 临时任务：按计划时间所在年份过滤，从MES同步数据生成硫化精度计划
     *
     * @param versionPrefix 版本号前缀（如：APS_MES_AH01）
     * @param operYear 计划时间所在年份（如：2025）
     * @param targetYear 要生成的目标年份（如：2026）
     * @return 生成数量
     */
    @ApiOperation("临时任务-按计划时间年份过滤生成硫化精度计划")
    @PostMapping("/lhPrecisionPlan/generateFromMesByOperYear")
    AjaxResult generatePlansFromMesByOperYear(@RequestParam("versionPrefix") String versionPrefix,
                                               @RequestParam("operYear") Integer operYear,
                                               @RequestParam("targetYear") Integer targetYear);

    /**
     * 自动生成年度硫化精度计划
     *
     * @param year 年份
     * @return 生成数量
     */
    @ApiOperation("自动生成年度硫化精度计划")
    @PostMapping("/lhPrecisionPlan/autoGenerateYearly")
    AjaxResult autoGenerateYearlyPlans(@RequestParam("year") Integer year);

    /**
     * 执行30天预警检查
     *
     * @return 预警数量
     */
    @ApiOperation("执行30天预警检查")
    @PostMapping("/lhPrecisionPlan/checkWarning")
    AjaxResult checkWarning();

    /**
     * 批量更新到期天数
     *
     * @return 更新数量
     */
    @ApiOperation("批量更新到期天数")
    @PostMapping("/lhPrecisionPlan/batchUpdateDaysToDue")
    AjaxResult batchUpdateDaysToDue();

    /**
     * MES回传实际完成时间
     *
     * @param mesSourceId MES来源ID
     * @param actualDate  实际日期
     * @return 是否成功
     */
    @ApiOperation("MES回传实际完成时间")
    @PostMapping("/lhPrecisionPlan/updateActualDate")
    AjaxResult updateActualDate(@RequestParam("mesSourceId") Long mesSourceId,
                                @RequestParam("actualDate") String actualDate);

    /**
     * 自动推算硫化精度计划（年度）
     *
     * @param year 年度
     * @return 推算结果
     */
    @ApiOperation("自动推算硫化精度计划（年度）")
    @PostMapping("/lhPrecisionPlan/autoCalculateLh")
    AjaxResult autoCalculateLhPrecisionPlan(@RequestParam("year") Integer year);

    /**
     * 根据设备保养计划生成并推算硫化精度计划
     *
     * @param maintenancePlanIds 设备保养计划ID列表
     * @return 生成结果
     */
    @ApiOperation("根据设备保养计划生成并推算硫化精度计划")
    @PostMapping("/lhPrecisionPlan/generateFromMaintenance")
    AjaxResult generateFromMaintenancePlan(@RequestBody List<Long> maintenancePlanIds);

    /**
     * 校验唯一性
     *
     * @param lhPrecisionPlan 硫化精度计划实体
     * @return 校验结果
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/lhPrecisionPlan/checkUnique")
    String checkUnique(@RequestBody LhPrecisionPlan lhPrecisionPlan);

    /**
     * 硫化排程回填计划排程精度日期
     *
     * @param machineCode  机台编码
     * @param factoryCode  分厂编码
     * @param scheduleDate 计划排程精度日期
     * @return 回填数量
     */
    @ApiOperation("硫化排程回填计划排程精度日期")
    @PostMapping("/lhPrecisionPlan/fillScheduleDate")
    AjaxResult fillScheduleDate(@RequestParam("machineCode") String machineCode,
                                @RequestParam("factoryCode") String factoryCode,
                                @RequestParam("scheduleDate") String scheduleDate);

    @ApiOperation("批量硫化排程回填计划排程精度日期")
    @PostMapping("/lhPrecisionPlan/batchFillScheduleDate")
    public AjaxResult batchFillScheduleDate(@RequestBody List<Map<String, Object>> fillList);

    /**
     * MES回填实际执行日期并生成下一次精度计划（闭环）
     *
     * @param machineCode 机台编码
     * @param factoryCode 分厂编码
     * @param actualDate  实际执行日期
     * @return 回填结果
     */
    @ApiOperation("MES回填实际执行日期并生成下一次精度计划")
    @PostMapping("/lhPrecisionPlan/fillActualDateAndGenerateNext")
    AjaxResult fillActualDateAndGenerateNext(@RequestParam("machineCode") String machineCode,
                                             @RequestParam("factoryCode") String factoryCode,
                                             @RequestParam("actualDate") java.util.Date actualDate);

    /**
     * 批量MES回填实际执行日期并生成下一次精度计划（闭环）
     * 优化：将循环内逐条DB查询改为外层批量查询+内存过滤，逐条insert/update改为批量操作
     *
     * @param fillList 回填数据列表，每项包含machineCode、factoryCode、actualDate
     * @return 成功回填数量
     */
    @ApiOperation("批量MES回填实际执行日期并生成下一次精度计划")
    @PostMapping("/lhPrecisionPlan/batchFillActualDateAndGenerateNext")
    AjaxResult batchFillActualDateAndGenerateNext(@RequestBody List<java.util.Map<String, Object>> fillList);

    /**
     * 查询待下发的硫化精度计划列表（计划排程精度日期有值且实际执行日期为空）
     *
     * @param factoryCode 分厂编码
     * @return 待下发计划列表
     */
    @ApiOperation("查询待下发的硫化精度计划列表")
    @PostMapping("/lhPrecisionPlan/listPendingIssue")
    AjaxResult listPendingIssuePlans(@RequestParam("factoryCode") String factoryCode);
}
