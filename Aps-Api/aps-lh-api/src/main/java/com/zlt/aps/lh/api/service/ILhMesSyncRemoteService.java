package com.zlt.aps.lh.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.lh.api.domain.entity.LhDayFinishQty;
import com.zlt.aps.lh.api.domain.entity.LhMachineOnlineInfo;
import com.zlt.aps.lh.api.domain.entity.LhMoldAlterPlanFinish;
import com.zlt.aps.lh.api.domain.entity.LhMouldCleanWarn;
import com.zlt.aps.lh.api.domain.entity.LhParams;
import com.zlt.aps.lh.api.domain.entity.LhRepairCapsule;
import com.zlt.aps.lh.api.domain.entity.LhScheFinishQty;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(contextId = "ILhMesSyncRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:/lh}")
public interface ILhMesSyncRemoteService {

    @ApiOperation("批量删除硫化在机信息")
    @PostMapping("/mesSync/deleteMachineOnlineInfo")
    AjaxResult deleteMachineOnlineInfo(@RequestParam("factoryCode") String factoryCode);

    @ApiOperation("根据分厂编号逻辑删除硫化在机信息")
    @PostMapping("/mesSync/logicDeleteMachineOnlineInfo")
    AjaxResult logicDeleteMachineOnlineInfo(@RequestParam("factoryCode") String factoryCode, @RequestParam("updateBy") String updateBy);

    @ApiOperation("批量保存硫化在机信息")
    @PostMapping("/mesSync/saveMachineOnlineInfoBatch")
    AjaxResult saveMachineOnlineInfoBatch(@RequestBody List<LhMachineOnlineInfo> list);

    @ApiOperation("逻辑删除并批量保存硫化在机信息（事务性操作）")
    @PostMapping("/mesSync/logicDeleteAndSaveMachineOnlineInfo")
    AjaxResult logicDeleteAndSaveMachineOnlineInfo(@RequestParam("factoryCode") String factoryCode, @RequestParam("onlineDate") String onlineDate, @RequestParam("updateBy") String updateBy, @RequestBody List<LhMachineOnlineInfo> list);

    @ApiOperation("批量删除胶囊已使用次数")
    @PostMapping("/mesSync/deleteRepairCapsule")
    AjaxResult deleteRepairCapsule(@RequestParam("factoryCode") String factoryCode);

    @ApiOperation("根据分厂编号逻辑删除胶囊已使用次数")
    @PostMapping("/mesSync/logicDeleteRepairCapsule")
    AjaxResult logicDeleteRepairCapsule(@RequestParam("factoryCode") String factoryCode, @RequestParam("updateBy") String updateBy);

    @ApiOperation("批量保存胶囊已使用次数")
    @PostMapping("/mesSync/saveRepairCapsuleBatch")
    AjaxResult saveRepairCapsuleBatch(@RequestBody List<LhRepairCapsule> list);

    @ApiOperation("逻辑删除并批量保存胶囊已使用次数（事务性操作）")
    @PostMapping("/mesSync/logicDeleteAndSaveRepairCapsule")
    AjaxResult logicDeleteAndSaveRepairCapsule(@RequestParam("factoryCode") String factoryCode, @RequestParam("obtainTime") String obtainTime, @RequestParam("updateBy") String updateBy, @RequestBody List<LhRepairCapsule> list);

    @ApiOperation("批量保存模具清洗预警")
    @PostMapping("/mesSync/saveMouldCleanWarnBatch")
    AjaxResult saveMouldCleanWarnBatch(@RequestBody List<LhMouldCleanWarn> list);

    @ApiOperation("查询模具清洗预警已存在数据")
    @PostMapping("/mesSync/selectMouldCleanWarnExists")
    List<LhMouldCleanWarn> selectMouldCleanWarnExists(@RequestBody List<LhMouldCleanWarn> list);

    @ApiOperation("批量保存硫化排程完成量")
    @PostMapping("/mesSync/saveScheFinishQtyBatch")
    AjaxResult saveScheFinishQtyBatch(@RequestBody List<LhScheFinishQty> list);

    @ApiOperation("查询硫化排程完成量已存在数据")
    @PostMapping("/mesSync/selectScheFinishQtyExists")
    List<LhScheFinishQty> selectScheFinishQtyExists(@RequestBody List<LhScheFinishQty> list);

    @ApiOperation("根据分厂编号逻辑删除硫化排程完成量数据")
    @PostMapping("/mesSync/logicDeleteScheFinishQty")
    AjaxResult logicDeleteScheFinishQty(@RequestParam("factoryCode") String factoryCode, @RequestParam("updateBy") String updateBy);

    @ApiOperation("逻辑删除并批量保存硫化排程完成量（事务性操作）")
    @PostMapping("/mesSync/logicDeleteAndSaveScheFinishQty")
    AjaxResult logicDeleteAndSaveScheFinishQty(@RequestParam("factoryCode") String factoryCode, @RequestParam("scheduleDate") String scheduleDate, @RequestParam("updateBy") String updateBy, @RequestBody List<LhScheFinishQty> list);

    @ApiOperation("批量保存硫化排程日完成量")
    @PostMapping("/mesSync/saveDayFinishQtyBatch")
    AjaxResult saveDayFinishQtyBatch(@RequestBody List<LhDayFinishQty> list);

    @ApiOperation("查询硫化排程日完成量已存在数据")
    @PostMapping("/mesSync/selectDayFinishQtyExists")
    List<LhDayFinishQty> selectDayFinishQtyExists(@RequestBody List<LhDayFinishQty> list);

    @ApiOperation("根据分厂编号逻辑删除硫化排程日完成量数据")
    @PostMapping("/mesSync/logicDeleteDayFinishQty")
    AjaxResult logicDeleteDayFinishQty(@RequestParam("factoryCode") String factoryCode, @RequestParam("updateBy") String updateBy);

    @ApiOperation("逻辑删除并批量保存硫化排程日完成量（事务性操作）")
    @PostMapping("/mesSync/logicDeleteAndSaveDayFinishQty")
    AjaxResult logicDeleteAndSaveDayFinishQty(@RequestParam("factoryCode") String factoryCode, @RequestParam("finishDate") String finishDate, @RequestParam("updateBy") String updateBy, @RequestBody List<LhDayFinishQty> list);

    @ApiOperation("批量保存模具交替计划完成回报")
    @PostMapping("/mesSync/saveMoldAlterPlanFinishBatch")
    @Deprecated
    AjaxResult saveMoldAlterPlanFinishBatch(@RequestBody List<LhMoldAlterPlanFinish> list);

    @ApiOperation("查询模具交替计划完成回报已存在数据")
    @PostMapping("/mesSync/selectMoldAlterPlanFinishExists")
    List<LhMoldAlterPlanFinish> selectMoldAlterPlanFinishExists(@RequestBody List<LhMoldAlterPlanFinish> list);

    /**
     * 模具交替计划完成回报：批量插入或更新 + 回填模具交替计划完成状态（同一事务）
     * 不存在则插入，存在则更新完成状态；同时回填t_lh_mould_change_plan的mould_status
     *
     * @param list MES同步过来的模具交替计划完成回报数据（finishStatus已转为数值码）
     * @return 结果
     */
    @ApiOperation("模具交替计划完成回报：批量插入或更新 + 回填模具交替计划完成状态")
    @PostMapping("/mesSync/saveOrUpdateMoldAlterPlanFinishAndWriteBack")
    AjaxResult saveOrUpdateMoldAlterPlanFinishAndWriteBack(@RequestBody List<LhMoldAlterPlanFinish> list);

    @ApiOperation("硫化排程完成量回写硫化排程结果表各班次完成量")
    @PostMapping("/mesSync/writeBackScheduleResultFinishQty")
    AjaxResult writeBackScheduleResultFinishQty(@RequestBody List<LhScheFinishQty> list);

    @ApiOperation("模具交替回报回填流程排程结果表的模具交替完成状态")
    @PostMapping("/mesSync/writeBackMouldChangePlanFinishStatus")
    @Deprecated
    AjaxResult writeBackMouldChangePlanFinishStatus(@RequestBody List<LhMoldAlterPlanFinish> list);

    @ApiOperation("逻辑删除硫化在机今天及今天之前所有数据")
    @PostMapping("/mesSync/logicDeleteLhMachineOnlineAllBeforeToday")
    AjaxResult logicDeleteLhMachineOnlineAllBeforeToday();

    @ApiOperation("逻辑删除胶囊已使用次数今天及今天之前所有数据")
    @PostMapping("/mesSync/logicDeleteLhRepairCapsuleAllBeforeToday")
    AjaxResult logicDeleteLhRepairCapsuleAllBeforeToday();

    @ApiOperation("逻辑删除硫化排程完成量今天及今天之前所有数据")
    @PostMapping("/mesSync/logicDeleteLhScheFinishQtyAllBeforeToday")
    AjaxResult logicDeleteLhScheFinishQtyAllBeforeToday();

    @ApiOperation("逻辑删除硫化排程日完成量今天及今天之前所有数据")
    @PostMapping("/mesSync/logicDeleteLhDayFinishQtyAllBeforeToday")
    AjaxResult logicDeleteLhDayFinishQtyAllBeforeToday();

    @ApiOperation("从模具清洗预警同步生成清洗计划")
    @PostMapping("/mesSync/syncMouldCleanPlanFromWarn")
    AjaxResult syncMouldCleanPlanFromWarn();

    @ApiOperation("清空模具清洗预警和清洗计划表全部数据")
    @PostMapping("/mesSync/cleanAllMouldCleanWarnAndPlan")
    AjaxResult cleanAllMouldCleanWarnAndPlan();

    @ApiOperation("基于全部预警数据全量生成清洗计划（不限制版本号）")
    @PostMapping("/mesSync/syncAllMouldCleanPlanFromWarn")
    AjaxResult syncAllMouldCleanPlanFromWarn();

    @ApiOperation("查询每天最新版本的硫化排程日完成量数据")
    @PostMapping("/mesSync/queryLatestDayFinishQty")
    List<LhDayFinishQty> queryLatestDayFinishQty();

    @ApiOperation("根据参数编码和分厂编码查询硫化参数配置")
    @PostMapping("/mesSync/selectLhParamsByCode")
    LhParams selectLhParamsByCode(@RequestParam("paramCode") String paramCode, @RequestParam("factoryCode") String factoryCode);

    @ApiOperation("根据参数编码查询所有分厂的硫化参数配置")
    @PostMapping("/mesSync/selectLhParamsListByParamCode")
    List<LhParams> selectLhParamsListByParamCode(@RequestParam("paramCode") String paramCode);
}
