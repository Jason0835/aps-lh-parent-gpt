package com.zlt.aps.lh.controller;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.lh.api.domain.dto.LhParamsDto;
import com.zlt.aps.lh.entity.LhParams;
import com.zlt.aps.lh.service.LhParamsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 硫化参数信息Controller
 */
@RestController
@RequestMapping("/lh/params")
@Api(tags = {"硫化参数信息维护接口"})
public class LhParamsController extends BaseController {
    @Autowired
    private LhParamsService paramsService;

    /**
     * 查询硫化参数信息列表
     *
     * @return 查询到的结果
     */
    @ApiOperation("查询硫化参数信息列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody LhParamsDto dto) {
        LhParams params = new LhParams();
        BeanUtils.copyProperties(dto, params);
        startPage();
        params.setOrderStr(orderStr());
        List<LhParamsDto> list = paramsService.selectParamsList(params);
        return getDataTable(list);
    }

    /**
     * 获取硫化参数信息详细信息
     *
     * @return 结果
     */
    @ApiOperation("获取硫化参数信息详细信息")
    @GetMapping(value = "/{id}")
    public LhParams getInfo(@PathVariable("id") Long id) {
        return paramsService.selectParamsById(id);
    }

    /**
     * 修改硫化参数信息
     *
     * @return 结果
     */
    @Log(title = "ui.data.column.lh.params.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改硫化参数信息")
    @PostMapping("/edit")
    public AjaxResult edit(@Validated @RequestBody LhParamsDto dto) {
        LhParams params = new LhParams();
        BeanUtils.copyProperties(dto, params);
        if (UserConstants.NOT_UNIQUE.equals(paramsService.checkParamsCodeUnique(params))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.params.message.unique"));
        }
        return paramsService.updateParams(params);
    }

    /**
     * 导出硫化参数信息
     *
     * @param dto 查询条件
     * @return 查询到的集合
     */
    @Log(title = "ui.data.column.lh.params.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出硫化参数信息")
    @GetMapping("/exportData")
    public List<LhParamsDto> export(@SpringQueryMap LhParamsDto dto) {
        LhParams params = new LhParams();
        BeanUtils.copyProperties(dto, params);
        startPage();
        params.setOrderStr(orderStr());
        return paramsService.selectParamsList(params);
    }
}
