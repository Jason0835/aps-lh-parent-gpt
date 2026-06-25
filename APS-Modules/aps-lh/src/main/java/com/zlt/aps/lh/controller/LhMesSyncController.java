package com.zlt.aps.lh.controller;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.common.core.enums.MouldFinishStatusEnum;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.lh.api.domain.entity.*;
import com.zlt.aps.lh.api.service.ILhMesSyncRemoteService;
import com.zlt.aps.lh.mapper.*;
import com.zlt.aps.lh.service.*;
import com.zlt.aps.utils.GenerageMapKeyUtils;
import com.zlt.core.dao.basedao.BaseDao;
import io.seata.common.util.CollectionUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Api(tags = "MES数据同步")
@RestController
@RequestMapping("/mesSync")
public class LhMesSyncController implements ILhMesSyncRemoteService {

    @Autowired
    private BaseDao baseDao;

    @Autowired
    private LhMachineOnlineInfoMapper lhMachineOnlineInfoMapper;

    @Autowired
    private LhRepairCapsuleMapper lhRepairCapsuleMapper;

    @Autowired
    private LhMouldCleanWarnMapper lhMouldCleanWarnMapper;

    @Autowired
    private LhScheFinishQtyMapper lhScheFinishQtyMapper;

    @Autowired
    private LhDayFinishQtyMapper lhDayFinishQtyMapper;

    @Autowired
    private LhMoldAlterPlanFinishMapper lhMoldAlterPlanFinishMapper;

    @Autowired
    private ILhScheFinishQtyService lhScheFinishQtyService;

    @Autowired
    private ILhMachineOnlineInfoService lhMachineOnlineInfoService;

    @Autowired
    private ILhRepairCapsuleService lhRepairCapsuleService;

    @Autowired
    private ILhDayFinishQtyService lhDayFinishQtyService;

    @Autowired
    private ILhParamsService lhParamsService;

    @Autowired
    private LhMouldChangePlanEntityMapper lhMouldChangePlanEntityMapper;

    @Autowired
    private ILhMouldCleanPlanService lhMouldCleanPlanService;

    @Autowired
    private ILhScheduleService lhScheduleService;

    @Resource
    private LhScheduleResultMapper scheduleResultMapper;

    @Override
    @ApiOperation("批量删除硫化在机信息")
    @PostMapping("/deleteMachineOnlineInfo")
    public AjaxResult deleteMachineOnlineInfo(@RequestParam("factoryCode") String factoryCode) {
        lhMachineOnlineInfoMapper.logicDeleteByFactoryCode(factoryCode, "MES", new Date());
        return AjaxResult.success();
    }

    @Override
    @ApiOperation("根据分厂编号逻辑删除硫化在机信息")
    @PostMapping("/logicDeleteMachineOnlineInfo")
    public AjaxResult logicDeleteMachineOnlineInfo(@RequestParam("factoryCode") String factoryCode, @RequestParam("updateBy") String updateBy) {
        lhMachineOnlineInfoMapper.logicDeleteByFactoryCode(factoryCode, updateBy, new Date());
        return AjaxResult.success();
    }

    @Override
    @ApiOperation("批量保存硫化在机信息")
    @PostMapping("/saveMachineOnlineInfoBatch")
    public AjaxResult saveMachineOnlineInfoBatch(@RequestBody List<LhMachineOnlineInfo> list) {
        if (list != null && !list.isEmpty()) {
            baseDao.insertBatch(list);
        }
        return AjaxResult.success();
    }

    @Override
    @ApiOperation("逻辑删除并批量保存硫化在机信息（事务性操作）")
    @PostMapping("/logicDeleteAndSaveMachineOnlineInfo")
    public AjaxResult logicDeleteAndSaveMachineOnlineInfo(@RequestParam("factoryCode") String factoryCode, @RequestParam("onlineDate") String onlineDateStr, @RequestParam("updateBy") String updateBy, @RequestBody List<LhMachineOnlineInfo> list) {
        Date onlineDate = DateUtil.parse(onlineDateStr);
        lhMachineOnlineInfoService.logicDeleteAndSaveBatch(factoryCode, onlineDate, updateBy, list);
        return AjaxResult.success();
    }

    @Override
    @ApiOperation("批量删除胶囊已使用次数")
    @PostMapping("/deleteRepairCapsule")
    public AjaxResult deleteRepairCapsule(@RequestParam("factoryCode") String factoryCode) {
        lhRepairCapsuleMapper.logicDeleteByFactoryCode(factoryCode, "MES", new Date());
        return AjaxResult.success();
    }

    @Override
    @ApiOperation("根据分厂编号逻辑删除胶囊已使用次数")
    @PostMapping("/logicDeleteRepairCapsule")
    public AjaxResult logicDeleteRepairCapsule(@RequestParam("factoryCode") String factoryCode, @RequestParam("updateBy") String updateBy) {
        lhRepairCapsuleMapper.logicDeleteByFactoryCode(factoryCode, updateBy, new Date());
        return AjaxResult.success();
    }

    @Override
    @ApiOperation("批量保存胶囊已使用次数")
    @PostMapping("/saveRepairCapsuleBatch")
    public AjaxResult saveRepairCapsuleBatch(@RequestBody List<LhRepairCapsule> list) {
        if (list != null && !list.isEmpty()) {
            baseDao.insertBatch(list);
        }
        return AjaxResult.success();
    }

    @Override
    @ApiOperation("逻辑删除并批量保存胶囊已使用次数（事务性操作）")
    @PostMapping("/logicDeleteAndSaveRepairCapsule")
    public AjaxResult logicDeleteAndSaveRepairCapsule(@RequestParam("factoryCode") String factoryCode, @RequestParam("obtainTime") String obtainTimeStr, @RequestParam("updateBy") String updateBy, @RequestBody List<LhRepairCapsule> list) {
        Date obtainTime = DateUtil.parse(obtainTimeStr);
        lhRepairCapsuleService.logicDeleteAndSaveBatch(factoryCode, obtainTime, updateBy, list);
        return AjaxResult.success();
    }

    @Override
    @ApiOperation("批量保存模具清洗预警")
    @PostMapping("/saveMouldCleanWarnBatch")
    public AjaxResult saveMouldCleanWarnBatch(@RequestBody List<LhMouldCleanWarn> list) {
        if (list != null && !list.isEmpty()) {
            baseDao.saveBatch(list);
        }
        return AjaxResult.success();
    }

    @Override
    @ApiOperation("查询模具清洗预警已存在数据")
    @PostMapping("/selectMouldCleanWarnExists")
    public List<LhMouldCleanWarn> selectMouldCleanWarnExists(@RequestBody List<LhMouldCleanWarn> list) {
        return lhMouldCleanWarnMapper.selectByUniqueKeyList(list);
    }

    @Override
    @ApiOperation("批量保存硫化排程完成量")
    @PostMapping("/saveScheFinishQtyBatch")
    public AjaxResult saveScheFinishQtyBatch(@RequestBody List<LhScheFinishQty> list) {
        if (list != null && !list.isEmpty()) {
            baseDao.saveBatch(list);
        }
        return AjaxResult.success();
    }

    @Override
    @ApiOperation("查询硫化排程完成量已存在数据")
    @PostMapping("/selectScheFinishQtyExists")
    public List<LhScheFinishQty> selectScheFinishQtyExists(@RequestBody List<LhScheFinishQty> list) {
        return lhScheFinishQtyMapper.selectByUniqueKeyList(list);
    }

    @Override
    @ApiOperation("根据分厂编号逻辑删除硫化排程完成量数据")
    @PostMapping("/logicDeleteScheFinishQty")
    public AjaxResult logicDeleteScheFinishQty(@RequestParam("factoryCode") String factoryCode, @RequestParam("updateBy") String updateBy) {
        lhScheFinishQtyMapper.logicDeleteByFactoryCode(factoryCode, updateBy, new Date());
        return AjaxResult.success();
    }

    @Override
    @ApiOperation("逻辑删除并批量保存硫化排程完成量（事务性操作）")
    @PostMapping("/logicDeleteAndSaveScheFinishQty")
    public AjaxResult logicDeleteAndSaveScheFinishQty(@RequestParam("factoryCode") String factoryCode, @RequestParam("scheduleDate") String scheduleDateStr, @RequestParam("updateBy") String updateBy, @RequestBody List<LhScheFinishQty> list) {
        Date scheduleDate = DateUtil.parse(scheduleDateStr);
        lhScheFinishQtyService.logicDeleteAndSaveBatch(factoryCode, scheduleDate, updateBy, list);
        return AjaxResult.success();
    }

    @Override
    @ApiOperation("批量保存硫化排程日完成量")
    @PostMapping("/saveDayFinishQtyBatch")
    public AjaxResult saveDayFinishQtyBatch(@RequestBody List<LhDayFinishQty> list) {
        if (list != null && !list.isEmpty()) {
            baseDao.saveBatch(list);
        }
        return AjaxResult.success();
    }

    @Override
    @ApiOperation("查询硫化排程日完成量已存在数据")
    @PostMapping("/selectDayFinishQtyExists")
    public List<LhDayFinishQty> selectDayFinishQtyExists(@RequestBody List<LhDayFinishQty> list) {
        return lhDayFinishQtyMapper.selectByUniqueKeyList(list);
    }

    @Override
    @ApiOperation("根据分厂编号逻辑删除硫化排程日完成量数据")
    @PostMapping("/logicDeleteDayFinishQty")
    public AjaxResult logicDeleteDayFinishQty(@RequestParam("factoryCode") String factoryCode, @RequestParam("updateBy") String updateBy) {
        lhDayFinishQtyMapper.logicDeleteByFactoryCode(factoryCode, updateBy, new Date());
        return AjaxResult.success();
    }

    @Override
    @ApiOperation("逻辑删除并批量保存硫化排程日完成量（事务性操作）")
    @PostMapping("/logicDeleteAndSaveDayFinishQty")
    public AjaxResult logicDeleteAndSaveDayFinishQty(@RequestParam("factoryCode") String factoryCode, @RequestParam("finishDate") String finishDateStr, @RequestParam("updateBy") String updateBy, @RequestBody List<LhDayFinishQty> list) {
        Date finishDate = DateUtil.parse(finishDateStr);
        lhDayFinishQtyService.logicDeleteAndSaveBatch(factoryCode, finishDate, updateBy, list);
        return AjaxResult.success();
    }

    @Override
    @ApiOperation("批量保存模具交替计划完成回报")
    @PostMapping("/saveMoldAlterPlanFinishBatch")
    @Deprecated
    public AjaxResult saveMoldAlterPlanFinishBatch(@RequestBody List<LhMoldAlterPlanFinish> list) {
        if (list != null && !list.isEmpty()) {
            baseDao.saveBatch(list);
        }
        return AjaxResult.success();
    }

    @Override
    @ApiOperation("模具交替计划完成回报：批量插入或更新 + 回填模具交替计划完成状态")
    @PostMapping("/saveOrUpdateMoldAlterPlanFinishAndWriteBack")
    public AjaxResult saveOrUpdateMoldAlterPlanFinishAndWriteBack(@RequestBody List<LhMoldAlterPlanFinish> list) {
        if (list == null || list.isEmpty()) {
            return AjaxResult.success();
        }

        // ========== 步骤1：拆分为插入列表和更新列表 ==========
        List<LhMoldAlterPlanFinish> existsList = lhMoldAlterPlanFinishMapper.selectByUniqueKeyList(list);
        Map<String, LhMoldAlterPlanFinish> existsMap = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(existsList)) {
            existsMap = existsList.stream()
                    .collect(Collectors.toMap(
                            item -> GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), item.getLhBatchNo(), item.getOrderNo(), String.valueOf(item.getScheduleDate()), item.getLhMachineCode(), item.getLeftRightMold()),
                            item -> item,
                            (v1, v2) -> v1
                    ));
        }

        List<LhMoldAlterPlanFinish> insertList = new ArrayList<>();
        List<LhMoldAlterPlanFinish> updateList = new ArrayList<>();
        for (LhMoldAlterPlanFinish entity : list) {
            String mapKey = GenerageMapKeyUtils.createMapKey(entity.getFactoryCode(), entity.getLhBatchNo(), entity.getOrderNo(), String.valueOf(entity.getScheduleDate()), entity.getLhMachineCode(), entity.getLeftRightMold());
            if (existsMap.containsKey(mapKey)) {
                // 已存在：设置id，仅更新完成状态
                LhMoldAlterPlanFinish existsData = existsMap.get(mapKey);
                entity.setId(existsData.getId());
                entity.setUpdateTime(new Date());
                updateList.add(entity);
            } else {
                // 不存在：新增
                insertList.add(entity);
            }
        }

        // ========== 步骤2：批量操作 T_LH_MOLD_ALTER_PLAN_FINISH 表 ==========
        if (CollectionUtils.isNotEmpty(insertList)) {
            baseDao.insertBatch(insertList);
            log.info("模具交替计划完成回报-批量插入{}条", insertList.size());
        }
        if (CollectionUtils.isNotEmpty(updateList)) {
            lhMoldAlterPlanFinishMapper.batchUpdateFinishStatus(updateList);
            log.info("模具交替计划完成回报-批量更新{}条", updateList.size());
        }

        // ========== 步骤3：筛选已完成的数据，批量回填 t_lh_mould_change_plan ==========
        List<LhMoldAlterPlanFinish> completedList = list.stream()
                .filter(item -> Objects.nonNull(item) && MouldFinishStatusEnum.isCompleted(item.getFinishStatus())
                        && item.getIsDelete() != null && item.getIsDelete() == 0)
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(completedList)) {
            lhMouldChangePlanEntityMapper.batchUpdateMouldStatusByFinish(completedList);
            log.info("模具交替计划完成回报-批量回填模具交替计划完成状态{}条", completedList.size());
        }

        // ========== 步骤4：批量查询关联的 LhScheduleResult 并触发排程自动更新 ==========
        if (CollectionUtils.isNotEmpty(completedList)) {
            List<LhScheduleResult> allScheduleResults = new ArrayList<>();
            for (LhMoldAlterPlanFinish finishItem : completedList) {
                LambdaQueryWrapper<LhScheduleResult> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(LhScheduleResult::getFactoryCode, finishItem.getFactoryCode());
                queryWrapper.eq(LhScheduleResult::getScheduleDate, finishItem.getScheduleDate());
                queryWrapper.eq(LhScheduleResult::getLhMachineCode, finishItem.getLhMachineCode());
                queryWrapper.eq(LhScheduleResult::getIsDelete, YesOrNoEnum.NO.getCode());
                List<LhScheduleResult> lhScheduleResultList = scheduleResultMapper.selectList(queryWrapper);
                if (CollectionUtils.isNotEmpty(lhScheduleResultList)) {
                    allScheduleResults.add(lhScheduleResultList.get(0));
                }
            }
            if (CollectionUtils.isNotEmpty(allScheduleResults)) {
                lhScheduleService.batchIncreaseMouldStartPlan(allScheduleResults);
            }
        }

        return AjaxResult.success();
    }

    @Override
    @ApiOperation("查询模具交替计划完成回报已存在数据")
    @PostMapping("/selectMoldAlterPlanFinishExists")
    public List<LhMoldAlterPlanFinish> selectMoldAlterPlanFinishExists(@RequestBody List<LhMoldAlterPlanFinish> list) {
        return lhMoldAlterPlanFinishMapper.selectByUniqueKeyList(list);
    }

    @Override
    @ApiOperation("硫化排程完成量回写硫化排程结果表各班次完成量")
    @PostMapping("/writeBackScheduleResultFinishQty")
    public AjaxResult writeBackScheduleResultFinishQty(@RequestBody List<LhScheFinishQty> list) {
        return lhScheFinishQtyService.writeBackScheduleResultFinishQty(list);
    }

    @Override
    @ApiOperation("模具交替回报回填流程排程结果表的模具交替完成状态")
    @PostMapping("/writeBackMouldChangePlanFinishStatus")
    @Deprecated
    public AjaxResult writeBackMouldChangePlanFinishStatus(@RequestBody List<LhMoldAlterPlanFinish> list) {
        if (list == null || list.isEmpty()) {
            return AjaxResult.success();
        }
        List<LhMoldAlterPlanFinish> completedList = list.stream()
                .filter(item -> Objects.nonNull(item) && MouldFinishStatusEnum.isCompleted(item.getFinishStatus())
                        && item.getIsDelete() != null && item.getIsDelete() == 0)
                .collect(Collectors.toList());
        if (completedList.isEmpty()) {
            return AjaxResult.success();
        }
        // 批量更新模具交替计划完成状态
        for (LhMoldAlterPlanFinish finishItem : completedList) {
            LambdaUpdateWrapper<LhMouldChangePlan> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.set(LhMouldChangePlan::getMouldStatus, MouldFinishStatusEnum.COMPLETED.getCode());
            updateWrapper.eq(LhMouldChangePlan::getFactoryCode, finishItem.getFactoryCode());
            updateWrapper.eq(LhMouldChangePlan::getScheduleDate, finishItem.getScheduleDate());
            updateWrapper.eq(LhMouldChangePlan::getOrderNo, finishItem.getOrderNo());
            updateWrapper.eq(LhMouldChangePlan::getLhMachineCode, finishItem.getLhMachineCode());
            updateWrapper.eq(LhMouldChangePlan::getIsDelete, 0);
            if (StringUtils.isNotBlank(finishItem.getLeftRightMold())) {
                updateWrapper.eq(LhMouldChangePlan::getLeftRightMould, finishItem.getLeftRightMold());
            }
            lhMouldChangePlanEntityMapper.update(null, updateWrapper);
        }
        // 批量执行硫化排程自动更新
        List<LhScheduleResult> allScheduleResults = new ArrayList<>();
        for (LhMoldAlterPlanFinish finishItem : completedList) {
            LambdaQueryWrapper<LhScheduleResult> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(LhScheduleResult::getFactoryCode, finishItem.getFactoryCode());
            queryWrapper.eq(LhScheduleResult::getScheduleDate, finishItem.getScheduleDate());
            queryWrapper.eq(LhScheduleResult::getLhMachineCode, finishItem.getLhMachineCode());
            queryWrapper.eq(LhScheduleResult::getIsDelete, YesOrNoEnum.NO.getCode());
            List<LhScheduleResult> lhScheduleResultList = scheduleResultMapper.selectList(queryWrapper);
            if (CollectionUtils.isNotEmpty(lhScheduleResultList)) {
                allScheduleResults.add(lhScheduleResultList.get(0));
            }
        }
        if (CollectionUtils.isNotEmpty(allScheduleResults)) {
            lhScheduleService.batchIncreaseMouldStartPlan(allScheduleResults);
        }
        return AjaxResult.success();
    }

    @Override
    @ApiOperation("逻辑删除硫化在机今天及今天之前所有数据")
    @PostMapping("/logicDeleteLhMachineOnlineAllBeforeToday")
    public AjaxResult logicDeleteLhMachineOnlineAllBeforeToday() {
        log.info("开始逻辑删除硫化在机今天及今天之前所有数据...");
        int deleteCount = lhMachineOnlineInfoMapper.logicDeleteAllBeforeToday();
        log.info("逻辑删除硫化在机今天及今天之前所有数据完成，删除记录数={}", deleteCount);
        return AjaxResult.success("逻辑删除记录数：" + deleteCount);
    }

    @Override
    @ApiOperation("逻辑删除胶囊已使用次数今天及今天之前所有数据")
    @PostMapping("/logicDeleteLhRepairCapsuleAllBeforeToday")
    public AjaxResult logicDeleteLhRepairCapsuleAllBeforeToday() {
        log.info("开始逻辑删除胶囊已使用次数今天及今天之前所有数据...");
        int deleteCount = lhRepairCapsuleMapper.logicDeleteAllBeforeToday();
        log.info("逻辑删除胶囊已使用次数今天及今天之前所有数据完成，删除记录数={}", deleteCount);
        return AjaxResult.success("逻辑删除记录数：" + deleteCount);
    }

    @Override
    @ApiOperation("逻辑删除硫化排程完成量今天及今天之前所有数据")
    @PostMapping("/logicDeleteLhScheFinishQtyAllBeforeToday")
    public AjaxResult logicDeleteLhScheFinishQtyAllBeforeToday() {
        log.info("开始逻辑删除硫化排程完成量今天及今天之前所有数据...");
        int deleteCount = lhScheFinishQtyMapper.logicDeleteAllBeforeToday();
        log.info("逻辑删除硫化排程完成量今天及今天之前所有数据完成，删除记录数={}", deleteCount);
        return AjaxResult.success("逻辑删除记录数：" + deleteCount);
    }

    @Override
    @ApiOperation("逻辑删除硫化排程日完成量今天及今天之前所有数据")
    @PostMapping("/logicDeleteLhDayFinishQtyAllBeforeToday")
    public AjaxResult logicDeleteLhDayFinishQtyAllBeforeToday() {
        log.info("开始逻辑删除硫化排程日完成量今天及今天之前所有数据...");
        int deleteCount = lhDayFinishQtyMapper.logicDeleteAllBeforeToday();
        log.info("逻辑删除硫化排程日完成量今天及今天之前所有数据完成，删除记录数={}", deleteCount);
        return AjaxResult.success("逻辑删除记录数：" + deleteCount);
    }

    @Override
    @ApiOperation("从模具清洗预警同步生成清洗计划")
    @PostMapping("/syncMouldCleanPlanFromWarn")
    public AjaxResult syncMouldCleanPlanFromWarn() {
        try {
            int count = lhMouldCleanPlanService.syncFromMouldCleanWarn();
            return AjaxResult.success("操作成功，成功同步" + count + "条模具清洗计划");
        } catch (Exception e) {
            log.error("从模具清洗预警同步生成计划失败", e);
            return AjaxResult.error("操作失败：" + e.getMessage());
        }
    }

    @Override
    @ApiOperation("清空模具清洗预警和清洗计划表全部数据")
    @PostMapping("/cleanAllMouldCleanWarnAndPlan")
    public AjaxResult cleanAllMouldCleanWarnAndPlan() {
        try {
            lhMouldCleanPlanService.cleanAllWarnAndPlan();
            return AjaxResult.success("操作成功，已清空模具清洗预警和清洗计划表全部数据");
        } catch (Exception e) {
            log.error("清空模具清洗预警和清洗计划表数据失败", e);
            return AjaxResult.error("操作失败：" + e.getMessage());
        }
    }

    @Override
    @ApiOperation("基于全部预警数据全量生成清洗计划（不限制版本号）")
    @PostMapping("/syncAllMouldCleanPlanFromWarn")
    public AjaxResult syncAllMouldCleanPlanFromWarn() {
        try {
            int count = lhMouldCleanPlanService.syncAllFromMouldCleanWarn();
            return AjaxResult.success("操作成功，成功全量生成" + count + "条模具清洗计划");
        } catch (Exception e) {
            log.error("基于全部预警数据全量生成清洗计划失败", e);
            return AjaxResult.error("操作失败：" + e.getMessage());
        }
    }

    @Override
    @ApiOperation("查询每天最新版本的硫化排程日完成量数据")
    @PostMapping("/queryLatestDayFinishQty")
    public List<LhDayFinishQty> queryLatestDayFinishQty() {
        LambdaQueryWrapper<LhDayFinishQty> wrapper = new LambdaQueryWrapper<>();
        List<LhDayFinishQty> allList = lhDayFinishQtyMapper.selectList(wrapper);
        if (allList == null || allList.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Optional<LhDayFinishQty>> latestByVersion = allList.stream()
                .collect(Collectors.groupingBy(
                        item -> buildGroupKey(item.getFactoryCode(), item.getFinishDate()),
                        Collectors.maxBy(Comparator.comparing(
                                item -> item.getDataVersion() != null ? item.getDataVersion() : "",
                                Comparator.nullsFirst(Comparator.naturalOrder())
                        ))
                ));
        return latestByVersion.values().stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private String buildGroupKey(String factoryCode, Date finishDate) {
        String dateStr = finishDate != null ? DateUtil.formatDate(finishDate) : "null";
        return factoryCode + "|" + dateStr;
    }

    @Override
    @ApiOperation("根据参数编码和分厂编码查询硫化参数配置")
    @PostMapping("/selectLhParamsByCode")
    public LhParams selectLhParamsByCode(@RequestParam("paramCode") String paramCode, @RequestParam("factoryCode") String factoryCode) {
        return lhParamsService.selectOneByParamCode(paramCode, factoryCode);
    }

    @Override
    @ApiOperation("根据参数编码查询所有分厂的硫化参数配置")
    @PostMapping("/selectLhParamsListByParamCode")
    public List<LhParams> selectLhParamsListByParamCode(@RequestParam("paramCode") String paramCode) {
        return lhParamsService.selectListByParamCode(paramCode);
    }
}
