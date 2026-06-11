package com.zlt.aps.dj.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.dj.api.domain.entity.DjParams;
import com.zlt.aps.dj.service.DjParamsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 垫胶参数信息Controller
 *
 * @author zlt
 * @date 2026-06-11
 */
@Slf4j
@Api(tags = "垫胶参数信息维护")
@RestController
@RequestMapping("/dj/params")
public class DjParamsController extends BaseController<DjParams> {

    private final DjParamsService paramsService;

    public DjParamsController(DjParamsService paramsService) {
        this.paramsService = paramsService;
    }

    /**
     * 查询垫胶参数信息列表
     */
    @ApiOperation("查询垫胶参数信息列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody DjParams entity) {
        startPage("FACTORY_CODE,BUSINESS_GROUP,PARAM_CODE asc");
        List<DjParams> list = paramsService.selectParamsList(entity);
        return getDataTable(list);
    }

    /**
     * 获取垫胶参数信息详细信息
     */
    @ApiOperation("获取垫胶参数信息详细信息")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(paramsService.selectParamsById(id));
    }

    /**
     * 修改垫胶参数信息
     */
    @Log(title = "ui.dj.params.column.modalName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改垫胶参数信息")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody DjParams entity) {
        return paramsService.updateParams(entity);
    }

    /**
     * 删除垫胶参数信息
     */
    @Log(title = "ui.dj.params.column.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("删除垫胶参数信息")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@RequestBody List<Long> ids) {
        return toAjax(paramsService.removeByIds(ids) ? 1 : 0);
    }

    /**
     * 校验参数代码唯一性
     */
    @ApiOperation("校验参数代码唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody DjParams params) {
        return paramsService.checkParamsCodeUnique(params);
    }

    /**
     * 根据参数编码查询垫胶参数信息
     */
    @ApiOperation("根据参数编码查询垫胶参数信息")
    @PostMapping("/getByParamCode")
    public DjParams getByParamCode(@RequestBody DjParams entity) {
        return paramsService.getParamsByCondition(entity.getFactoryCode(), entity.getProductTypeCode(), entity.getParamCode());
    }
}