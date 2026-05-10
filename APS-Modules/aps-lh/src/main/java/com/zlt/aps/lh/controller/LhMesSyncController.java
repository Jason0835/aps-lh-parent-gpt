package com.zlt.aps.lh.controller;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.lh.api.domain.entity.LhDayFinishQty;
import com.zlt.aps.lh.api.domain.entity.LhMachineOnlineInfo;
import com.zlt.aps.lh.api.domain.entity.LhMoldAlterPlanFinish;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.api.domain.entity.LhMouldCleanWarn;
import com.zlt.aps.lh.api.domain.entity.LhRepairCapsule;
import com.zlt.aps.lh.api.domain.entity.LhScheFinishQty;
import com.zlt.aps.lh.api.service.ILhMesSyncRemoteService;
import com.zlt.aps.lh.mapper.LhDayFinishQtyMapper;
import com.zlt.aps.lh.mapper.LhMachineOnlineInfoMapper;
import com.zlt.aps.lh.mapper.LhMoldAlterPlanFinishMapper;
import com.zlt.aps.lh.mapper.LhMouldChangePlanEntityMapper;
import com.zlt.aps.lh.mapper.LhMouldCleanWarnMapper;
import com.zlt.aps.lh.mapper.LhRepairCapsuleMapper;
import com.zlt.aps.lh.mapper.LhScheFinishQtyMapper;
import com.zlt.aps.lh.service.ILhDayFinishQtyService;
import com.zlt.aps.lh.service.ILhMachineOnlineInfoService;
import com.zlt.aps.lh.service.ILhRepairCapsuleService;
import com.zlt.aps.lh.service.ILhScheFinishQtyService;
import com.zlt.core.dao.basedao.BaseDao;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.commons.lang.StringUtils;

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
    private LhMouldChangePlanEntityMapper lhMouldChangePlanEntityMapper;

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
    public AjaxResult logicDeleteAndSaveMachineOnlineInfo(@RequestParam("factoryCode") String factoryCode, @RequestParam("updateBy") String updateBy, @RequestBody List<LhMachineOnlineInfo> list) {
        lhMachineOnlineInfoService.logicDeleteAndSaveBatch(factoryCode, updateBy, list);
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
    public AjaxResult logicDeleteAndSaveRepairCapsule(@RequestParam("factoryCode") String factoryCode, @RequestParam("updateBy") String updateBy, @RequestBody List<LhRepairCapsule> list) {
        lhRepairCapsuleService.logicDeleteAndSaveBatch(factoryCode, updateBy, list);
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
    public AjaxResult logicDeleteAndSaveScheFinishQty(@RequestParam("factoryCode") String factoryCode, @RequestParam("updateBy") String updateBy, @RequestBody List<LhScheFinishQty> list) {
        lhScheFinishQtyService.logicDeleteAndSaveBatch(factoryCode, updateBy, list);
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
    public AjaxResult logicDeleteAndSaveDayFinishQty(@RequestParam("factoryCode") String factoryCode, @RequestParam("updateBy") String updateBy, @RequestBody List<LhDayFinishQty> list) {
        lhDayFinishQtyService.logicDeleteAndSaveBatch(factoryCode, updateBy, list);
        return AjaxResult.success();
    }

    @Override
    @ApiOperation("批量保存模具交替计划完成回报")
    @PostMapping("/saveMoldAlterPlanFinishBatch")
    public AjaxResult saveMoldAlterPlanFinishBatch(@RequestBody List<LhMoldAlterPlanFinish> list) {
        if (list != null && !list.isEmpty()) {
            baseDao.saveBatch(list);
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
    public AjaxResult writeBackMouldChangePlanFinishStatus(@RequestBody List<LhMoldAlterPlanFinish> list) {
        if (list == null || list.isEmpty()) {
            return AjaxResult.success();
        }
        List<LhMoldAlterPlanFinish> completedList = list.stream()
                .filter(item -> Objects.nonNull(item) && "1".equals(item.getFinishStatus())
                        && item.getIsDelete() != null && item.getIsDelete() == 0)
                .collect(Collectors.toList());
        if (completedList.isEmpty()) {
            return AjaxResult.success();
        }
        for (LhMoldAlterPlanFinish finishItem : completedList) {
            LambdaUpdateWrapper<LhMouldChangePlan> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.set(LhMouldChangePlan::getMouldStatus, "1");
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
        return AjaxResult.success();
    }
}
