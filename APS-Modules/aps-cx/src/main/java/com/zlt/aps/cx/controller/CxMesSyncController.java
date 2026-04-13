package com.zlt.aps.cx.controller;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cx.api.domain.entity.CxDayFinishQty;
import com.zlt.aps.cx.api.domain.entity.CxMachineOnlineInfo;
import com.zlt.aps.cx.api.domain.entity.CxMesStock;
import com.zlt.aps.cx.api.domain.entity.CxScheFinishQty;
import com.zlt.aps.cx.api.domain.entity.CxStructureTreadConfig;
import com.zlt.aps.cx.api.service.ICxMesSyncRemoteService;
import com.zlt.aps.cx.mapper.CxDayFinishQtyMapper;
import com.zlt.aps.cx.mapper.CxMachineOnlineInfoMapper;
import com.zlt.aps.cx.mapper.CxMesStockMapper;
import com.zlt.aps.cx.mapper.CxScheFinishQtyMapper;
import com.zlt.aps.cx.mapper.CxStructureTreadConfigMapper;
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
public class CxMesSyncController implements ICxMesSyncRemoteService {

    @Autowired
    private BaseDao baseDao;

    @Autowired
    private CxMachineOnlineInfoMapper cxMachineOnlineInfoMapper;

    @Autowired
    private CxStructureTreadConfigMapper cxStructureTreadConfigMapper;

    @Autowired
    private CxMesStockMapper cxMesStockMapper;

    @Autowired
    private CxScheFinishQtyMapper cxScheFinishQtyMapper;

    @Autowired
    private CxDayFinishQtyMapper cxDayFinishQtyMapper;

    @Override
    @ApiOperation("批量删除成型在机信息")
    @PostMapping("/deleteMachineOnlineInfo")
    public AjaxResult deleteMachineOnlineInfo(@RequestParam("factoryCode") String factoryCode) {
        Map<String, Object> map = new HashMap<>();
        map.put("FACTORY_CODE", factoryCode);
        baseDao.deleteByMap(CxMachineOnlineInfo.class, map);
        return AjaxResult.success();
    }

    @Override
    @ApiOperation("批量保存成型在机信息")
    @PostMapping("/saveMachineOnlineInfoBatch")
    public AjaxResult saveMachineOnlineInfoBatch(@RequestBody List<CxMachineOnlineInfo> list) {
        if (list != null && !list.isEmpty()) {
            baseDao.insertBatch(list);
        }
        return AjaxResult.success();
    }

    @Override
    @ApiOperation("批量保存结构整车胎面配置")
    @PostMapping("/saveStructureTreadConfigBatch")
    public AjaxResult saveStructureTreadConfigBatch(@RequestBody List<CxStructureTreadConfig> list) {
        if (list != null && !list.isEmpty()) {
            baseDao.saveBatch(list);
        }
        return AjaxResult.success();
    }

    @Override
    @ApiOperation("查询结构整车胎面配置已存在数据")
    @PostMapping("/selectStructureTreadConfigExists")
    public List<CxStructureTreadConfig> selectStructureTreadConfigExists(@RequestBody List<CxStructureTreadConfig> list) {
        return cxStructureTreadConfigMapper.selectByUniqueKeyList(list);
    }

    @Override
    @ApiOperation("批量删除生胎库存")
    @PostMapping("/deleteMesStock")
    public AjaxResult deleteMesStock(@RequestParam("factoryCode") String factoryCode) {
        Map<String, Object> map = new HashMap<>();
        map.put("FACTORY_CODE", factoryCode);
        baseDao.deleteByMap(CxMesStock.class, map);
        return AjaxResult.success();
    }

    @Override
    @ApiOperation("批量保存生胎库存")
    @PostMapping("/saveMesStockBatch")
    public AjaxResult saveMesStockBatch(@RequestBody List<CxMesStock> list) {
        if (list != null && !list.isEmpty()) {
            baseDao.insertBatch(list);
        }
        return AjaxResult.success();
    }

    @Override
    @ApiOperation("批量保存成型排程完成量")
    @PostMapping("/saveScheFinishQtyBatch")
    public AjaxResult saveScheFinishQtyBatch(@RequestBody List<CxScheFinishQty> list) {
        if (list != null && !list.isEmpty()) {
            baseDao.saveBatch(list);
        }
        return AjaxResult.success();
    }

    @Override
    @ApiOperation("查询成型排程完成量已存在数据")
    @PostMapping("/selectScheFinishQtyExists")
    public List<CxScheFinishQty> selectScheFinishQtyExists(@RequestBody List<CxScheFinishQty> list) {
        return cxScheFinishQtyMapper.selectByUniqueKeyList(list);
    }

    @Override
    @ApiOperation("批量保存成型排程日完成量")
    @PostMapping("/saveDayFinishQtyBatch")
    public AjaxResult saveDayFinishQtyBatch(@RequestBody List<CxDayFinishQty> list) {
        if (list != null && !list.isEmpty()) {
            baseDao.saveBatch(list);
        }
        return AjaxResult.success();
    }

    @Override
    @ApiOperation("查询成型排程日完成量已存在数据")
    @PostMapping("/selectDayFinishQtyExists")
    public List<CxDayFinishQty> selectDayFinishQtyExists(@RequestBody List<CxDayFinishQty> list) {
        return cxDayFinishQtyMapper.selectByUniqueKeyList(list);
    }
}
