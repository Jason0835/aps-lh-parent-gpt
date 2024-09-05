package com.zlt.aps.cx.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cx.api.domain.dto.CxScheduleLimitDto;
import com.zlt.aps.cx.entity.CxScheduleLimit;
import com.zlt.aps.cx.service.CxScheduleLimitService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 成型排产限制Service接口
 *
 * @author chen
 * @date 2021-06-16
 */
@RestController
@RequestMapping("/cx/limit")
@Api(tags = "成型排产限制信息维护接口")
public class CxScheduleLimitController extends BaseController {
    @Autowired
    private CxScheduleLimitService cxScheduleLimitService;

    /**
     * 查询成型排产限制列表
     *
     * @param dto 成型排产限制
     * @return 成型排产限制集合
     */
    @PostMapping("/list")
    @ApiOperation("查询成型排产限制信息维护列表")
    public TableDataInfo list(@RequestBody CxScheduleLimitDto dto) {
        CxScheduleLimit limit = new CxScheduleLimit();
        BeanUtils.copyProperties(dto, limit);
        startPage();
        limit.setOrderStr(orderStr());
        List<CxScheduleLimitDto> list = cxScheduleLimitService.selectLimitList(limit);
        return getDataTable(list);
    }

    /**
     * 查询成型排产限制
     *
     * @param id 成型排产限制ID
     * @return 成型排产限制
     */
    @GetMapping("/{id}")
    @ApiOperation("查询成型排产限制信息维护信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public CxScheduleLimitDto getInfo(@PathVariable("id") Long id) {
        CxScheduleLimit limit = cxScheduleLimitService.selectLimitById(id);
        CxScheduleLimitDto dto = new CxScheduleLimitDto();
        BeanUtils.copyProperties(limit, dto);
        return dto;
    }

    /**
     * 修改成型排产限制
     *
     * @param dto 成型排产限制
     * @return 结果
     */
    @Log(title = "ui.data.column.cx.limit.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @PostMapping("/edit")
    @ApiOperation("修改成型排产限制（id为空则新增，id不为空则修改）")
    public AjaxResult edit(@RequestBody CxScheduleLimitDto dto) {
        CxScheduleLimit limit = new CxScheduleLimit();
        BeanUtils.copyProperties(dto, limit);
        cxScheduleLimitService.saveLimit(limit);
        return AjaxResult.success();
    }

    /**
     * 删除成型排产限制
     *
     * @param ids 需要删除的成型排产限制ID
     * @return 结果
     */
    @Log(title = "ui.data.column.cx.limit.modelName", businessType = BusinessType.DELETE)
    @PostMapping("/{ids}")
    @ApiOperation("删除成型排产限制信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id数组", paramType = "query")
    })
    public AjaxResult remove(@PathVariable("ids") Long[] ids) {
        cxScheduleLimitService.deleteLimitByIds(ids);
        return AjaxResult.success();
    }

    /**
     * 导出成型排产限制信息
     */
    @Log(title = "ui.data.column.cx.limit.modelName", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @ApiOperation("导出成型排产限制信息")
    public List<CxScheduleLimitDto> exportData(@SpringQueryMap CxScheduleLimitDto dto) {
        CxScheduleLimit limit = new CxScheduleLimit();
        BeanUtils.copyProperties(dto, limit);
        startPage();
        limit.setOrderStr(orderStr());
        return cxScheduleLimitService.selectLimitList(limit);
    }

    @Log(title = "ui.data.column.cx.limit.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<CxScheduleLimitDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return cxScheduleLimitService.importData(list, updateSupport, importLogId);
    }
}
