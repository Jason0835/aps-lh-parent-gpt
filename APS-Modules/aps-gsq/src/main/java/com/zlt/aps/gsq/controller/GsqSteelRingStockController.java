package com.zlt.aps.gsq.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.gsq.api.domain.entity.GsqSteelRingStock;
import com.zlt.aps.gsq.mapper.GsqSteelRingStockMapper;
import com.zlt.aps.gsq.service.IGsqSteelRingStockService;
import com.zlt.aps.utils.AppUtils;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 钢丝圈库存管理控制层
 *
 * @author zlt
 * @date 2026-07-08
 */
@Api(tags = "钢丝圈库存管理")
@RestController
@RequestMapping("/gsq/steelRingStock")
public class GsqSteelRingStockController extends AbstractDocBizController<GsqSteelRingStock> {

    @Resource
    private IGsqSteelRingStockService gsqSteelRingStockService;

    @Resource
    private GsqSteelRingStockMapper gsqSteelRingStockMapper;

    /** 查询钢丝圈库存列表 */
    @ApiOperation("查询钢丝圈库存列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody GsqSteelRingStock queryVO) {
        return super.list(queryVO);
    }

    /** 新增钢丝圈库存 */
    @Log(title = "ui.data.column.gsq.steelRingStock.modalName", businessType = BusinessType.INSERT)
    @ApiOperation("新增钢丝圈库存")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody GsqSteelRingStock entity) {
        return super.save(entity);
    }

    /** 编辑钢丝圈库存 */
    @Log(title = "ui.data.column.gsq.steelRingStock.modalName", businessType = BusinessType.UPDATE)
    @ApiOperation("编辑钢丝圈库存")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody GsqSteelRingStock entity) {
        return super.save(entity);
    }

    /** 删除钢丝圈库存 */
    @Log(title = "ui.data.column.gsq.steelRingStock.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("删除钢丝圈库存")
    @PostMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    /** 获取钢丝圈库存详情 */
    @ApiOperation("获取钢丝圈库存详情")
    @GetMapping("/getInfo/{id}")
    @Override
    public GsqSteelRingStock getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    /** 校验钢丝圈库存唯一性 */
    @ApiOperation("校验钢丝圈库存唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody GsqSteelRingStock entity) {
        return gsqSteelRingStockService.checkUnique(entity);
    }

    /** 导入钢丝圈库存 */
    @Log(title = "ui.data.column.gsq.steelRingStock.modalName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入钢丝圈库存")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /** 导出钢丝圈库存 */
    @Log(title = "ui.data.column.gsq.steelRingStock.modalName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出钢丝圈库存")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody GsqSteelRingStock queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected IDocService getDocService() {
        return gsqSteelRingStockService;
    }

    @Override
    protected void builderCondition(QueryWrapper<GsqSteelRingStock> queryWrapper, GsqSteelRingStock queryVO) {
        queryWrapper.ge(queryVO.getStockDateBegin() != null, "STOCK_DATE", queryVO.getStockDateBegin());
        queryWrapper.le(queryVO.getStockDateEnd() != null, "STOCK_DATE", queryVO.getStockDateEnd());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getSteelRingCode()), "STEEL_RING_CODE", queryVO.getSteelRingCode());
    }

    @Override
    protected String getTypeCode() {
        return "GSQ_STEEL_RING_STOCK";
    }

    @Override
    protected String getOrderBy() {
        return "STOCK_DATE desc, STEEL_RING_CODE asc";
    }
}
