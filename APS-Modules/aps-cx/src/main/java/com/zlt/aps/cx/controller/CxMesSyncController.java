package com.zlt.aps.cx.controller;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cx.api.domain.entity.CxDayFinishQty;
import com.zlt.aps.cx.api.domain.entity.CxMachineOnlineInfo;
import com.zlt.aps.cx.api.domain.entity.CxMesStock;
import com.zlt.aps.cx.api.domain.entity.CxScheFinishQty;
import com.zlt.aps.cx.api.domain.entity.CxStock;
import com.zlt.aps.cx.api.domain.entity.CxStructureTreadConfig;
import com.zlt.aps.cx.api.service.ICxMesSyncRemoteService;
import com.zlt.aps.cx.service.CxStockService;
import com.zlt.aps.cx.service.ICxMachineOnlineInfoService;
import com.zlt.aps.cx.mapper.CxDayFinishQtyMapper;
import com.zlt.aps.cx.mapper.CxMachineOnlineInfoMapper;
import com.zlt.aps.cx.mapper.CxMesStockMapper;
import com.zlt.aps.cx.mapper.CxScheFinishQtyMapper;
import com.zlt.aps.cx.mapper.CxStockMapper;
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

import java.util.Date;
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
    private ICxMachineOnlineInfoService cxMachineOnlineInfoService;

    @Autowired
    private CxStructureTreadConfigMapper cxStructureTreadConfigMapper;

    @Autowired
    private CxMesStockMapper cxMesStockMapper;

    @Autowired
    private CxScheFinishQtyMapper cxScheFinishQtyMapper;

    @Autowired
    private CxDayFinishQtyMapper cxDayFinishQtyMapper;

    @Autowired
    private CxStockMapper cxStockMapper;

    @Autowired
    private CxStockService cxStockService;

    @Override
    @ApiOperation("批量删除成型在机信息")
    @PostMapping("/deleteMachineOnlineInfo")
    public AjaxResult deleteMachineOnlineInfo(@RequestParam("factoryCode") String factoryCode) {
        cxMachineOnlineInfoMapper.logicDeleteByFactoryCode(factoryCode, "MES", new Date());
        return AjaxResult.success();
    }

    @Override
    @ApiOperation("根据分厂编号逻辑删除成型在机信息")
    @PostMapping("/logicDeleteMachineOnlineInfo")
    public AjaxResult logicDeleteMachineOnlineInfo(@RequestParam("factoryCode") String factoryCode, @RequestParam("updateBy") String updateBy) {
        cxMachineOnlineInfoMapper.logicDeleteByFactoryCode(factoryCode, updateBy, new Date());
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
    @ApiOperation("逻辑删除并批量保存成型在机信息（事务性操作）")
    @PostMapping("/logicDeleteAndSaveMachineOnlineInfo")
    public AjaxResult logicDeleteAndSaveMachineOnlineInfo(@RequestParam("factoryCode") String factoryCode, @RequestParam("onlineDate") Date onlineDate, @RequestParam("updateBy") String updateBy, @RequestBody List<CxMachineOnlineInfo> list) {
        cxMachineOnlineInfoService.logicDeleteAndSaveBatch(factoryCode, onlineDate, updateBy, list);
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

    @Override
    @ApiOperation("查询成型库存已存在数据（按唯一键，仅未删除）")
    @PostMapping("/selectCxStockExists")
    public List<CxStock> selectCxStockExists(@RequestBody List<CxStock> list) {
        return cxStockMapper.selectByUniqueKeyList(list);
    }

    @Override
    @ApiOperation("批量保存或更新成型库存（UPSERT）")
    @PostMapping("/saveCxStockBatch")
    public AjaxResult saveCxStockBatch(@RequestBody List<CxStock> list) {
        if (list != null && !list.isEmpty()) {
            baseDao.saveBatch(list);
        }
        return AjaxResult.success();
    }

    @Override
    @ApiOperation("根据分厂编号和数据来源逻辑删除成型库存")
    @PostMapping("/logicDeleteCxStockByDataSource")
    public AjaxResult logicDeleteCxStockByDataSource(@RequestParam("factoryCode") String factoryCode,
                                                      @RequestParam("dataSource") String dataSource,
                                                      @RequestParam("updateBy") String updateBy) {
        cxStockMapper.logicDeleteByFactoryCodeAndDataSource(factoryCode, dataSource, updateBy, new Date());
        return AjaxResult.success();
    }

    @Override
    @ApiOperation("逻辑删除并批量保存生胎库存（事务性操作）")
    @PostMapping("/logicDeleteAndSaveCxStockByDataSource")
    public AjaxResult logicDeleteAndSaveCxStockByDataSource(@RequestParam("factoryCode") String factoryCode,
                                                             @RequestParam("dataSource") String dataSource,
                                                             @RequestParam("stockDate") Date stockDate,
                                                             @RequestParam("updateBy") String updateBy,
                                                             @RequestBody List<CxStock> list) {
        cxStockService.logicDeleteAndSaveBatch(factoryCode, dataSource, stockDate, updateBy, list);
        return AjaxResult.success();
    }

    @Override
    @ApiOperation("根据分厂编号和数据来源删除成型库存")
    @PostMapping("/deleteCxStockByDataSource")
    public AjaxResult deleteCxStockByDataSource(@RequestParam("factoryCode") String factoryCode, @RequestParam("dataSource") String dataSource) {
        cxStockMapper.physicalDeleteByFactoryCodeAndDataSource(factoryCode, dataSource);
        return AjaxResult.success();
    }

    @Override
    @ApiOperation("根据分厂编号和数据来源查询成型库存（仅未删除）")
    @PostMapping("/selectCxStockByDataSource")
    public List<CxStock> selectCxStockByDataSource(@RequestParam("factoryCode") String factoryCode, @RequestParam("dataSource") String dataSource) {
        return cxStockMapper.selectByFactoryCodeAndDataSource(factoryCode, dataSource);
    }

    @Override
    @ApiOperation("根据分厂编号和数据来源查询全部成型库存（包含已删除）")
    @PostMapping("/selectAllCxStockByDataSource")
    public List<CxStock> selectAllCxStockByDataSource(@RequestParam("factoryCode") String factoryCode, @RequestParam("dataSource") String dataSource) {
        return cxStockMapper.selectAllByFactoryCodeAndDataSource(factoryCode, dataSource);
    }

    @Override
    @ApiOperation("根据ID列表批量逻辑删除成型库存")
    @PostMapping("/logicDeleteCxStockByIds")
    public AjaxResult logicDeleteCxStockByIds(@RequestBody List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            cxStockMapper.logicDeleteByIds(ids, "MES", new Date());
        }
        return AjaxResult.success();
    }

    @Override
    @ApiOperation("根据ID列表批量删除成型库存")
    @PostMapping("/deleteCxStockByIds")
    public AjaxResult deleteCxStockByIds(@RequestBody List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            cxStockMapper.logicDeleteByIds(ids, "MES", new Date());
        }
        return AjaxResult.success();
    }

    @Override
    @ApiOperation("根据唯一键恢复已逻辑删除的成型库存")
    @PostMapping("/recoverCxStockByUniqueKey")
    public AjaxResult recoverCxStockByUniqueKey(@RequestBody List<CxStock> list,
                                                @RequestParam("dataSource") String dataSource,
                                                @RequestParam("updateBy") String updateBy) {
        if (list != null && !list.isEmpty()) {
            cxStockMapper.recoverByUniqueKeyList(list, dataSource, updateBy, new Date());
        }
        return AjaxResult.success();
    }

    @Override
    @ApiOperation("清理成型在机历史重复数据，保留每个历史日期最新版本")
    @PostMapping("/cleanCxMachineOnlineHistoryDuplicate")
    public AjaxResult cleanCxMachineOnlineHistoryDuplicate() {
        log.info("开始清理成型在机历史重复数据...");
        int count = cxMachineOnlineInfoMapper.cleanHistoryDuplicateData();
        log.info("清理成型在机历史重复数据完成，删除记录数={}", count);
        return AjaxResult.success("删除记录数：" + count);
    }

    @Override
    @ApiOperation("清理生胎库存历史重复数据，保留每个历史日期最新版本")
    @PostMapping("/cleanCxStockHistoryDuplicate")
    public AjaxResult cleanCxStockHistoryDuplicate() {
        log.info("开始清理生胎库存历史重复数据...");
        int count = cxStockMapper.cleanHistoryDuplicateData();
        log.info("清理生胎库存历史重复数据完成，删除记录数={}", count);
        return AjaxResult.success("删除记录数：" + count);
    }
}
