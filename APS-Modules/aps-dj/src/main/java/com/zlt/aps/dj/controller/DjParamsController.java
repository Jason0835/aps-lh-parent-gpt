package com.zlt.aps.dj.controller;

import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.dj.api.domain.dto.DjParamsDto;
import com.zlt.aps.dj.api.domain.entity.DjParams;
import com.zlt.aps.dj.service.DjParamsService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 垫胶参数信息Controller
 *
 * @author zlt
 * @date 2021-05-25
 */
@RestController
@RequestMapping("/dj/params")
@Api(tags = {"垫胶参数信息维护接口"})
public class DjParamsController extends BaseController {
    @Autowired
    private DjParamsService ncParamsService;

    /**
     * 查询垫胶参数信息列表
     *
     * @return
     */
    @ApiOperation("查询垫胶参数信息列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody DjParamsDto dto) {
        DjParams params = new DjParams();
        BeanUtils.copyProperties(dto, params);
        startPage();
        params.setOrderStr(orderStr());
        List<DjParamsDto> list = ncParamsService.selectParamsList(params);
        return getDataTable(list);
    }

    /**
     * 获取垫胶参数信息详细信息
     *
     * @return 结果
     */
    @ApiOperation("获取垫胶参数信息详细信息")
    @GetMapping(value = "/{id}")
    public DjParams getInfo(@PathVariable("id") Long id) {
        return ncParamsService.selectParamsById(id);
    }

    /**
     * 修改垫胶参数信息
     *
     * @return 结果
     */
    @Log(title = "ui.data.column.nc.params.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改垫胶参数信息")
    @PostMapping("/edit")
    public AjaxResult edit(@Validated @RequestBody DjParamsDto dto) {
        DjParams params = new DjParams();
        BeanUtils.copyProperties(dto, params);
        if (UserConstants.NOT_UNIQUE.equals(ncParamsService.checkParamsCodeUnique(params))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.params.message.unique"));
        }
        return ncParamsService.updateParams(params);
    }

    /**
     * 导出垫胶参数信息
     *
     * @param dto 查询条件
     * @return 查询到的集合
     */
    @Log(title = "ui.data.column.nc.params.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出垫胶参数信息")
    @PostMapping("/exportData")
    public List<DjParamsDto> export(@RequestBody DjParamsDto dto) {
        DjParams params = new DjParams();
        BeanUtils.copyProperties(dto, params);
        startPage();
        params.setOrderStr(orderStr());
        return ncParamsService.selectParamsList(params);
    }
}
