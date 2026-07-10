package com.zlt.aps.nc.controller;

import java.io.IOException;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.nc.api.domain.entity.NcMouthPlate;
import com.zlt.aps.nc.mapper.NcMouthPlateMapper;
import com.zlt.aps.nc.service.NcMouthPlateService;
import com.zlt.aps.utils.AppUtils;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/**
 * <p>
 * 内衬口型板信息维护 前端控制器
 * </p>
 *
 * @author zlt
 * @since 2026-06-27
 */
@RestController
@RequestMapping("/nc/mouthPlate")
public class NcMouthPlateController extends AbstractDocBizController<NcMouthPlate> {

    @Autowired
    private NcMouthPlateService ncMouthPlateService;

    @Resource
    private NcMouthPlateMapper ncMouthPlateMapper;

    /**
     * 查询内衬口型板信息维护列表
     */
    @PostMapping("/list")
    @ApiOperation("查询内衬口型板信息维护列表")
    public TableDataInfo list(@RequestBody NcMouthPlate queryVO) {
        return super.list(queryVO);
    }

    /**
     * 新增信息
     */
    @Log(title = "ui.data.column.nc.mouthPlate.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增信息（id不为空）")
    @PostMapping
    public AjaxResult save(@RequestBody NcMouthPlate mouthPlate) {
        if (UserConstants.NOT_UNIQUE.equals(ncMouthPlateService.checkUnique(mouthPlate))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.error.message.quota.unique"));
        }
        return super.save(mouthPlate);
    }

    /**
     * 删除信息
     */
    @Log(title = "ui.data.column.nc.mouthPlate.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("根据id批量删除信息")
    @ApiImplicitParams({ @ApiImplicitParam(name = "ids", dataType = "Long[]", value = "主键ids") })
    @PostMapping("/remove")
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    /**
     * 导出列表
     */
    @Log(title = "ui.nc.specifyMachine.column.modalName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody NcMouthPlate queryVO, @PathVariable("fileName") String fileName,
            HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<NcMouthPlate> listExportData(NcMouthPlate obj) {
        QueryWrapper<NcMouthPlate> wrapper = new QueryWrapper<>();
        startPage("update_time desc");
        this.builderCondition(wrapper, obj);
        List<NcMouthPlate> list = ncMouthPlateMapper.selectList(wrapper);
        AppUtils.formatData(list, getQueryFormulas());
        return list;
    }

    @Log(title = "ui.nc.specifyMachine.column.modalName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入信息")
    public AjaxResult importData(@RequestBody List<NcMouthPlate> list,
            @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return ncMouthPlateService.importData(list, updateSupport, importLogId);
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected IDocService getDocService() {
        return ncMouthPlateService;
    }

    @Override
    protected String getTypeCode() {
        return "0";
    }

    @Override
    protected String getOrderBy() {
        return "MACHINE_CODE, MOUTH_PLATE_CODE";
    }
}
