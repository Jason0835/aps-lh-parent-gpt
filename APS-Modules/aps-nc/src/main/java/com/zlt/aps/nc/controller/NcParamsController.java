package com.zlt.aps.nc.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.nc.api.domain.entity.NcParams;
import com.zlt.aps.nc.service.NcParamsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 内衬参数信息Controller
 *
 * @author zlt
 * @date 2026-06-11
 */
@Slf4j
@Api(tags = "内衬参数信息维护")
@RestController
@RequestMapping("/nc/params")
public class NcParamsController extends BaseController<NcParams> {

    private final NcParamsService paramsService;

    public NcParamsController(NcParamsService paramsService) {
        this.paramsService = paramsService;
    }

    /**
     * 查询内衬参数信息列表
     */
    @ApiOperation("查询内衬参数信息列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody NcParams entity) {
        startPage("FACTORY_CODE,BUSINESS_GROUP,PARAM_CODE asc");
        List<NcParams> list = paramsService.selectParamsList(entity);
        return getDataTable(list);
    }

    /**
     * 获取内衬参数信息详细信息
     */
    @ApiOperation("获取内衬参数信息详细信息")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(paramsService.selectParamsById(id));
    }

    /**
     * 修改内衬参数信息
     */
    @Log(title = "ui.nc.params.column.modalName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改内衬参数信息")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody NcParams entity) {
        return paramsService.updateParams(entity);
    }

    /**
     * 删除内衬参数信息
     */
    @Log(title = "ui.nc.params.column.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("删除内衬参数信息")
    @PostMapping("/remove")
    public AjaxResult remove(@RequestBody List<Long> ids) {
        return toAjax(paramsService.removeByIds(ids) ? 1 : 0);
    }

    /**
     * 校验参数代码唯一性
     */
    @ApiOperation("校验参数代码唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody NcParams params) {
        return paramsService.checkParamsCodeUnique(params);
    }

    /**
     * 根据参数编码查询内衬参数信息
     */
    @ApiOperation("根据参数编码查询内衬参数信息")
    @PostMapping("/getByParamCode")
    public NcParams getByParamCode(@RequestBody NcParams entity) {
        return paramsService.getParamsByCondition(entity.getFactoryCode(), entity.getProductTypeCode(), entity.getParamCode());
    }
}
