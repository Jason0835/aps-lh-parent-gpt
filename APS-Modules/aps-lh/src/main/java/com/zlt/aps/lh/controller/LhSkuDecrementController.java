package com.zlt.aps.lh.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.RowStateEnum;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.lh.api.domain.entity.LhSkuDecrement;
import com.zlt.aps.lh.mapper.LhSkuDecrementMapper;
import com.zlt.aps.lh.service.ILhSkuDecrementService;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jodd.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * SKU减量清单控制器
 */
@Api(tags = "SKU减量清单")
@RestController
@RequestMapping("/lhSkuDecrement")
public class LhSkuDecrementController extends AbstractDocBizController<LhSkuDecrement> {

    @Autowired
    private ILhSkuDecrementService lhSkuDecrementService;

    @Resource
    private LhSkuDecrementMapper lhSkuDecrementMapper;

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody LhSkuDecrement queryVO) {
        lhSkuDecrementService.normalizeConfirmData(queryVO);
        return super.list(queryVO);
    }

    @ApiOperation("获取详情")
    @GetMapping("/{billId}")
    @Override
    public LhSkuDecrement getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }

    @Log(title = "ui.data.column.lhSkuDecrement.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("确认减量")
    @PostMapping("/confirm")
    public AjaxResult confirm(@RequestBody LhSkuDecrement billVO) {
        try {
            if (UserConstants.NOT_UNIQUE.equals(lhSkuDecrementService.checkUnique(billVO))) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.lhSkuDecrement.notUnique"));
            }
            billVO.setRowState(RowStateEnum.ADDED);
            AjaxResult ajaxResult = super.save(billVO);
            ajaxResult.put(AjaxResult.MSG_TAG, I18nUtil.getMessage("ui.data.alert.lhSkuDecrement.confirmSuccess"));
            return ajaxResult;
        } catch (IllegalArgumentException exception) {
            return AjaxResult.error(exception.getMessage());
        }
    }

    @Log(title = "ui.data.column.lhSkuDecrement.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    @Log(title = "ui.data.column.lhSkuDecrement.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody LhSkuDecrement queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        lhSkuDecrementService.normalizeConfirmData(queryVO);
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<LhSkuDecrement> listExportData(LhSkuDecrement obj) {
        QueryWrapper<LhSkuDecrement> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return lhSkuDecrementMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return lhSkuDecrementService;
    }

    @Override
    protected String[] getQueryFormulas() {
        return lhSkuDecrementService.getQueryFormulas();
    }

    @Override
    protected void builderCondition(QueryWrapper<LhSkuDecrement> queryWrapper, LhSkuDecrement queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("year")), "YEAR", queryVO.getFieldValueByFieldName("year"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month")), "MONTH", queryVO.getFieldValueByFieldName("month"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialCode")), "MATERIAL_CODE", queryVO.getFieldValueByFieldName("materialCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialDesc")), "MATERIAL_DESC", queryVO.getFieldValueByFieldName("materialDesc"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("embryoDesc")), "EMBRYO_DESC", queryVO.getFieldValueByFieldName("embryoDesc"));
        queryWrapper.eq(StringUtil.isNotBlank(queryVO.getProductStatus()), "PRODUCT_STATUS", queryVO.getProductStatus());
        queryWrapper.orderByDesc("YEAR");
        queryWrapper.orderByDesc("MONTH");
        queryWrapper.orderByDesc("CREATE_TIME");
    }

    @Override
    protected String getTypeCode() {
        return "0";
    }

    @Override
    protected String getOrderBy() {
        return "YEAR desc, MONTH desc, CREATE_TIME desc";
    }
}
