package com.zlt.aps.lh.controller;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.lh.api.domain.entity.LhDayFinishQty;
import com.zlt.aps.lh.service.ILhDayFinishQtyService;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 硫化排程日完成量Controller
 *
 * @author APS Team
 */
@Slf4j
@Api(tags = "硫化排程日完成量管理")
@RestController
@RequestMapping("/lhDayFinishQty")
public class LhDayFinishQtyController extends AbstractDocBizController<LhDayFinishQty> {

    @Autowired
    private ILhDayFinishQtyService lhDayFinishQtyService;

    /**
     * 查询硫化排程日完成量列表
     */
    @ApiOperation("查询硫化排程日完成量列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody LhDayFinishQty queryVO) {
        return super.list(queryVO);
    }

    /**
     * 获取详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{id}")
    @Override
    public LhDayFinishQty getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    /**
     * 保存
     */
    @Log(title = "ui.lh.day.finish.qty.model.name", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody LhDayFinishQty billVO) {
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.lh.day.finish.qty.model.name", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    /**
     * 导入数据
     */
    @Log(title = "ui.lh.day.finish.qty.model.name", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导入硫化排程日完成量数据（Feign接口）
     */
    @Log(title = "ui.lh.day.finish.qty.model.name", businessType = BusinessType.IMPORT)
    @ApiOperation("导入硫化排程日完成量数据")
    @PostMapping("/importDataFeign")
    public AjaxResult importDataFeign(@RequestBody List<LhDayFinishQty> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        return lhDayFinishQtyService.importData(list, updateSupport, importLogId);
    }

    @Override
    public AjaxResult doImportData(List list, boolean updateSupport, long importLogId) {
        return lhDayFinishQtyService.importData(list, updateSupport, importLogId);
    }

    /**
     * 导出列表
     */
    @Log(title = "硫化排程日完成量", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody LhDayFinishQty queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    /**
     * 导出硫化排程日完成量列表（Feign接口）
     */
    @ApiOperation("导出硫化排程日完成量列表")
    @PostMapping("/exportData")
    public List<LhDayFinishQty> exportDataList(@RequestBody LhDayFinishQty queryVO) {
        return lhDayFinishQtyService.selectList(queryVO);
    }

    @Override
    protected IDocService getDocService() {
        return lhDayFinishQtyService;
    }

    @Override
    protected String getTypeCode() {
        return "0";
    }

    @Override
    protected String getOrderBy() {
        return "FINISH_DATE desc, ID desc";
    }
}
