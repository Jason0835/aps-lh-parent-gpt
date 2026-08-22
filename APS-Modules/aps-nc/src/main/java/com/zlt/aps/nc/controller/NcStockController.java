package com.zlt.aps.nc.controller;

import java.io.IOException;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.nc.api.domain.entity.NcStock;
import com.zlt.aps.nc.mapper.NcStockMapper;
import com.zlt.aps.nc.service.NcStockService;
import com.zlt.aps.utils.AppUtils;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/**
 * 内衬库存信息Controller
 *
 * @author zlt
 * @date 2026-05-31
 */
@RestController
@RequestMapping("/nc/stock")
@Api(tags = "内衬库存信息维护接口")
public class NcStockController extends AbstractDocBizController<NcStock> {
    @Autowired
    private NcStockService stockService;

    @Resource
    private NcStockMapper ncStockMapper;

    /**
     * 查询信息列表
     */
    @PostMapping("/list")
    @ApiOperation("根据条件列表信息")
    public TableDataInfo list(@RequestBody NcStock queryVO) {
        return super.list(queryVO);
    }
    
    @Override
    protected void builderCondition(QueryWrapper<NcStock> queryWrapper, NcStock queryVO) {
        super.builderCondition(queryWrapper, queryVO);
        queryWrapper.ge(queryVO.getStartTime() != null, "STOCK_DATE", queryVO.getStartTime());
        queryWrapper.le(queryVO.getEndTime() != null, "STOCK_DATE", queryVO.getEndTime());
    }

    /**
     * 新增信息
     */
    @Log(title = "ui.frame.page.stock.title", businessType = BusinessType.INSERT)
    @ApiOperation("新增信息（id不为空）")
    @Override
    public AjaxResult save(@RequestBody NcStock stock) {
        if (UserConstants.NOT_UNIQUE.equals(stockService.checkUnique(stock))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.ncStock.importUnique"));
        }
        return super.save(stock);
    }

    /**
     * 删除信息
     */
    @Log(title = "ui.frame.page.stock.title", businessType = BusinessType.DELETE)
    @ApiOperation("根据id批量删除信息")
    @ApiImplicitParams({ @ApiImplicitParam(name = "ids", dataType = "Long[]", value = "主键ids") })
    @PostMapping("/remove")
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    /**
     * 导出列表
     */
    @Log(title = "ui.frame.page.stock.title", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody NcStock queryVO, @PathVariable("fileName") String fileName,
            HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<NcStock> listExportData(NcStock obj) {
        QueryWrapper<NcStock> wrapper = new QueryWrapper<>();
        startPage(getOrderBy());
        this.builderCondition(wrapper, obj);
        List<NcStock> list = ncStockMapper.selectList(wrapper);
        AppUtils.formatData(list, getQueryFormulas());
        return list;
    }

    @Log(title = "ui.frame.page.stock.title", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入信息")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext,
            @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected IDocService getDocService() {
        return stockService;
    }

    @Override
    protected String getTypeCode() {
        return "0";
    }

    @Override
    protected String getOrderBy() {
        return "STOCK_DATE DESC, MATERIAL_CODE, ID";
    }
}
