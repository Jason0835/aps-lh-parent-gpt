package com.zlt.aps.cd90.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cd90.api.domain.entity.Cd90LineSideStock;
import com.zlt.aps.cd90.service.Cd90LineSideStockService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 90°裁断库存信息Controller
 *
 * @author hak
 * @date 2021-05-31
 */
@RestController
@RequestMapping("/cd90/lineSideStock")
@Api(tags = "90°裁断库存信息维护接口")
public class Cd90LineSideStockController extends BaseController {
    @Autowired
    private Cd90LineSideStockService stockService;

    /**
     * 查询90°裁断线边库存信息列表
     */
    @PostMapping("/list")
    @ApiOperation("根据条件查询库存列表信息")
    public TableDataInfo list(@RequestBody Cd90LineSideStock stock) {
        startPage();
        stock.setOrderStr(orderStr());
        List<Cd90LineSideStock> list = stockService.selectStockList(stock);
        return getDataTable(list);
    }

    /**
     * 查询90°裁断线边库存列表
     */
    @Log(title = "ui.frame.page.stock.title", businessType = BusinessType.EXPORT)
    @PostMapping("/exportList")
    public List<Cd90LineSideStock> exportList(@RequestBody Cd90LineSideStock stock) {
        stock.setOrderStr(orderStr());
        List<Cd90LineSideStock> list = stockService.selectStockList(stock);
        return list;
    }
}
