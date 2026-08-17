package com.zlt.aps.cd15.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import cn.hutool.core.date.DateUtil;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cd15.api.domain.entity.Cd15Stock;
import com.zlt.aps.cd15.mapper.Cd15StockMapper;
import com.zlt.aps.cd15.service.ICd15StockService;
import com.zlt.aps.utils.AppUtils;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 斜裁库存管理控制层。
 */
@Api(tags = "斜裁库存管理")
@RestController
@RequestMapping("/cd15Stock")
public class Cd15StockController extends AbstractDocBizController<Cd15Stock> {

    @Resource
    private ICd15StockService cd15StockService;

    @Resource
    private Cd15StockMapper cd15StockMapper;

    /** 查询斜裁库存列表 */
    @ApiOperation("查询斜裁库存列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody Cd15Stock queryVO) {
        return super.list(queryVO);
    }

    /** 新增斜裁库存 */
    @Log(title = "ui.data.column.cd15Stock.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增斜裁库存")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody Cd15Stock entity) {
        String errorKey = cd15StockService.validateBusiness(entity);
        if (StringUtils.isNotBlank(errorKey)) {
            return AjaxResult.error(I18nUtil.getMessage(errorKey));
        }
        return super.save(entity);
    }

    /** 编辑斜裁库存 */
    @Log(title = "ui.data.column.cd15Stock.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("编辑斜裁库存")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody Cd15Stock entity) {
        String errorKey = cd15StockService.validateBusiness(entity);
        if (StringUtils.isNotBlank(errorKey)) {
            return AjaxResult.error(I18nUtil.getMessage(errorKey));
        }
        return super.save(entity);
    }

    /** 删除斜裁库存 */
    @Log(title = "ui.data.column.cd15Stock.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除斜裁库存")
    @PostMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    /** 获取斜裁库存详情 */
    @ApiOperation("获取斜裁库存详情")
    @GetMapping("/getInfo/{id}")
    @Override
    public Cd15Stock getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    /** 校验斜裁库存唯一性 */
    @ApiOperation("校验斜裁库存唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody Cd15Stock entity) {
        return cd15StockService.checkUnique(entity);
    }

    /** 导入斜裁库存 */
    @Log(title = "ui.data.column.cd15Stock.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入斜裁库存")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /** 替换斜裁MES库存快照。 */
    @ApiOperation("替换斜裁MES库存快照")
    @PostMapping("/logicDeleteAndSaveMesBatch")
    public AjaxResult logicDeleteAndSaveMesBatch(@RequestParam("factoryCode") String factoryCode,
                                                  @RequestParam("stockDate") String stockDate,
                                                  @RequestParam("shiftCode") String shiftCode,
                                                  @RequestParam("updateBy") String updateBy,
                                                  @RequestBody List<Cd15Stock> stockList) {
        this.cd15StockService.logicDeleteAndSaveBatch(factoryCode,
                DateUtil.parseDate(stockDate), shiftCode, updateBy, stockList);
        return AjaxResult.success();
    }

    /** 导出斜裁库存 */
    @Log(title = "ui.data.column.cd15Stock.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出斜裁库存")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody Cd15Stock queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<Cd15Stock> listExportData(Cd15Stock obj) {
        QueryWrapper<Cd15Stock> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        List<Cd15Stock> list = cd15StockMapper.selectList(wrapper);
        AppUtils.formatData(list, getQueryFormulas());
        return list;
    }

    @Override
    protected IDocService getDocService() {
        return cd15StockService;
    }

    @Override
    protected void builderCondition(QueryWrapper<Cd15Stock> queryWrapper, Cd15Stock queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFactoryCode()), "FACTORY_CODE", queryVO.getFactoryCode());
        queryWrapper.eq(queryVO.getStockDate() != null, "STOCK_DATE", queryVO.getStockDate());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getShiftCode()), "SHIFT_CODE", queryVO.getShiftCode());
        queryWrapper.ge(queryVO.getStockDateStart() != null, "STOCK_DATE", queryVO.getStockDateStart());
        queryWrapper.le(queryVO.getStockDateEnd() != null, "STOCK_DATE", queryVO.getStockDateEnd());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getMaterialCode()), "MATERIAL_CODE", queryVO.getMaterialCode());
    }

    @Override
    protected String getTypeCode() {
        return "CD15_STOCK";
    }

    @Override
    protected String getOrderBy() {
        return "STOCK_DATE desc, SHIFT_CODE asc, MATERIAL_CODE asc";
    }
}
