package com.zlt.aps.cd15.controller;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cd15.api.domain.dto.Cd15ParamsDto;
import com.zlt.aps.cd15.entity.Cd15Params;
import com.zlt.aps.cd15.service.Cd15ParamsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 15度裁断参数信息Controller
 *
 * @author chenxueyuan
 * @date 2021-06-07
 */
@RestController
@RequestMapping("/cd15/params")
@Api(tags = {"15度裁断参数信息维护接口"})
public class Cd15ParamsController extends BaseController {
    @Autowired
    private Cd15ParamsService cd15ParamsService;

    /**
     * 查询15度裁断参数信息列表
     *
     * @return
     */
    @ApiOperation("查询15度裁断参数信息列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody Cd15ParamsDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        Cd15Params params = new Cd15Params();
        BeanUtils.copyProperties(dto, params);
        List<Cd15ParamsDto> list = cd15ParamsService.selectParamsList(params);
        return getDataTable(list);
    }

    /**
     * 获取15度裁断参数信息详细信息
     *
     * @return 结果
     */
    @ApiOperation("获取15度裁断参数信息详细信息")
    @GetMapping(value = "/{id}")
    public Cd15Params getInfo(@PathVariable("id") Long id) {
        return cd15ParamsService.selectParamsById(id);
    }

    /**
     * 修改15度裁断参数信息
     *
     * @return 结果
     */
    @Log(title = "ui.data.column.cd15.params.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改15度裁断参数信息")
    @PostMapping("/edit")
    public AjaxResult edit(@Validated @RequestBody Cd15ParamsDto dto) {
        Cd15Params params = new Cd15Params();
        BeanUtils.copyProperties(dto, params);
        if (UserConstants.NOT_UNIQUE.equals(cd15ParamsService.checkParamsCodeUnique(params))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.params.message.unique"));
        }
        return cd15ParamsService.updateParams(params);
    }

    /**
     * 导出15度裁断参数信息
     *
     * @param dto 查询条件
     * @return 查询到的集合
     */
    @Log(title = "ui.data.column.cd15.params.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出15度裁断参数信息")
    @PostMapping("/exportData")
    public List<Cd15ParamsDto> export(@RequestBody Cd15ParamsDto dto) {
        dto.setOrderStr(orderStr());
        Cd15Params params = new Cd15Params();
        BeanUtils.copyProperties(dto, params);
        return cd15ParamsService.selectParamsList(params);
    }
}
