package com.zlt.aps.tq.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.tq.api.domain.entity.TqStock;
import com.zlt.aps.tq.mapper.TqStockMapper;
import com.zlt.aps.tq.service.ITqStockService;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Slf4j
@Api(tags = "胎圈库存信息")
@RestController
@RequestMapping("/tqStock")
public class TqStockController extends AbstractDocBizController<TqStock> {

    @Autowired
    private ITqStockService tqStockService;

    @Resource
    private TqStockMapper tqStockMapper;

    @ApiOperation("查询胎圈库存信息列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody TqStock queryVO) {
        return super.list(queryVO);
    }

    @Log(title = "胎圈库存信息", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody TqStock billVO) {
        return super.save(billVO);
    }

    @Log(title = "胎圈库存信息", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @PostMapping("/delete/{ids}")
    public AjaxResult deleteByIds(@PathVariable("ids") List<Long> ids) {
        return super.removeByIds(ids);
    }

    @ApiOperation("获取详细信息")
    @GetMapping("/{id}")
    @Override
    public TqStock getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    @Log(title = "胎圈库存信息", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    @Log(title = "胎圈库存信息", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody TqStock queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }



    @ApiOperation("校验库存唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody TqStock stock) {
        return tqStockService.checkUnique(stock);
    }

    @Override
    protected IDocService getDocService() {
        return tqStockService;
    }

    @Override
    protected String getTypeCode() {
        return "0";
    }

    @Override
    protected String getOrderBy() {
        return "STOCK_DATE desc";
    }

    @Override
    protected List<TqStock> listExportData(TqStock obj) {
        QueryWrapper<TqStock> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        wrapper.last("ORDER BY " + getOrderBy());
        return tqStockMapper.selectList(wrapper);
    }

    @Override
    protected void builderCondition(QueryWrapper<TqStock> queryWrapper, TqStock queryVO) {
        queryWrapper.eq("IS_DELETE", 0);
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getMaterialCode()), "MATERIAL_CODE", queryVO.getMaterialCode());
        queryWrapper.ge(queryVO.getStockDateStart() != null, "STOCK_DATE", queryVO.getStockDateStart());
        queryWrapper.le(queryVO.getStockDateEnd() != null, "STOCK_DATE", queryVO.getStockDateEnd());
    }
}
