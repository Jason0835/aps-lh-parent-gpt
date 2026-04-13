package com.zlt.aps.lh.controller;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.lh.api.domain.entity.LhDayFinishQty;
import com.zlt.aps.lh.api.domain.entity.LhMachineOnlineInfo;
import com.zlt.aps.lh.api.domain.entity.LhMoldAlterPlanFinish;
import com.zlt.aps.lh.api.domain.entity.LhMouldCleanWarn;
import com.zlt.aps.lh.api.domain.entity.LhRepairCapsule;
import com.zlt.aps.lh.api.domain.entity.LhScheFinishQty;
import com.zlt.aps.lh.api.service.ILhMesSyncRemoteService;
import com.zlt.aps.lh.mapper.LhDayFinishQtyMapper;
import com.zlt.aps.lh.mapper.LhMachineOnlineInfoMapper;
import com.zlt.aps.lh.mapper.LhMoldAlterPlanFinishMapper;
import com.zlt.aps.lh.mapper.LhMouldCleanWarnMapper;
import com.zlt.aps.lh.mapper.LhRepairCapsuleMapper;
import com.zlt.aps.lh.mapper.LhScheFinishQtyMapper;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Override
    @ApiOperation("批量删除硫化在机信息")
    @PostMapping("/deleteMachineOnlineInfo")
    public AjaxResult deleteMachineOnlineInfo(@RequestParam("factoryCode") String factoryCode) {
        Map<String, Object> map = new HashMap<>();
        map.put("FACTORY_CODE", factoryCode);
        baseDao.deleteByMap(LhMachineOnlineInfo.class, map);
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
    @ApiOperation("批量删除胶囊已使用次数")
    @PostMapping("/deleteRepairCapsule")
    public AjaxResult deleteRepairCapsule(@RequestParam("factoryCode") String factoryCode) {
        Map<String, Object> map = new HashMap<>();
        map.put("FACTORY_CODE", factoryCode);
        baseDao.deleteByMap(LhRepairCapsule.class, map);
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
}
