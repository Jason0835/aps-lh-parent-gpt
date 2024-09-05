package com.zlt.aps.cx.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cx.api.domain.entity.PlmConstructionInfo;
import com.zlt.aps.cx.service.PlmParamsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * PLM参数信息Controller
 */
@RestController
@RequestMapping("/cx/plm")
@Api(tags = {"PLM参数信息维护接口"})
public class PlmParamsController extends BaseController {

    @Resource
    private PlmParamsService plmParamsService;

    /**
     * 查询PLM参数信息列表
     *
     * @return
     */
    @ApiOperation("查询PLM参数信息列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody PlmConstructionInfo plmEntity) {
        startPage();
        plmEntity.setOrderStr(orderStr());
        List<PlmConstructionInfo> list = plmParamsService.selectParamsList(plmEntity);
        return getDataTable(list);
    }

    /**
     * 获取PLM参数信息详细信息
     *
     * @return 结果
     */
    @ApiOperation("获取PLM参数信息详细信息")
    @GetMapping(value = "/{id}")
    public PlmConstructionInfo getInfo(@PathVariable("id") Long id) {
        return plmParamsService.getById(id);
    }

    /**
     * 导出PLM参数信息
     *
     * @param plm 查询条件
     * @return 查询到的集合
     */
    @Log(title = "ui.data.column.plm.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出PLM参数信息")
    @GetMapping("/exportData")
    public List<PlmConstructionInfo> export(@SpringQueryMap PlmConstructionInfo plm) {
        startPage();
        plm.setOrderStr(orderStr());
        return plmParamsService.selectParamsList(plm);
    }
}
