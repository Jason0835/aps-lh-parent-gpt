package com.zlt.aps.cd90.controller;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cd90.api.domain.dto.Cd90ParamsDto;
import com.zlt.aps.cd90.entity.Cd90Params;
import com.zlt.aps.cd90.service.Cd90ParamsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 90度裁断参数信息Controller
 *
 * @author chenxueyuan
 * @date 2021-06-07
 */
@RestController
@RequestMapping("/cd90/params")
@Api(tags = {"90度裁断参数信息维护接口"})
public class Cd90ParamsController extends BaseController {
    @Autowired
    private Cd90ParamsService cd90ParamsService;

    /**
     * 查询90度裁断参数信息列表
     *
     * @return
     */
    @ApiOperation("查询90度裁断参数信息列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody Cd90ParamsDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        Cd90Params params = new Cd90Params();
        BeanUtils.copyProperties(dto, params);
        List<Cd90ParamsDto> list = cd90ParamsService.selectParamsList(params);
        return getDataTable(list);
    }

    /**
     * 获取90度裁断参数信息详细信息
     *
     * @return 结果
     */
    @ApiOperation("获取90度裁断参数信息详细信息")
    @GetMapping(value = "/{id}")
    public Cd90Params getInfo(@PathVariable("id") Long id) {
        return cd90ParamsService.selectParamsById(id);
    }

    /**
     * 修改90度裁断参数信息
     *
     * @return 结果
     */
    @Log(title = "ui.data.column.cd90.params.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改90度裁断参数信息")
    @PostMapping("/edit")
    public AjaxResult edit(@Validated @RequestBody Cd90ParamsDto dto) {
        Cd90Params params = new Cd90Params();
        BeanUtils.copyProperties(dto, params);
        if (UserConstants.NOT_UNIQUE.equals(cd90ParamsService.checkParamsCodeUnique(params))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.params.message.unique"));
        }
        return cd90ParamsService.updateParams(params);
    }

    /**
     * 导出90度裁断参数信息
     *
     * @param dto 查询条件
     * @return 查询到的集合
     */
    @Log(title = "ui.data.column.cd90.params.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出90度裁断参数信息")
    @PostMapping("/exportData")
    public List<Cd90ParamsDto> export(@RequestBody Cd90ParamsDto dto) {
        dto.setOrderStr(orderStr());
        Cd90Params params = new Cd90Params();
        BeanUtils.copyProperties(dto, params);
        return cd90ParamsService.selectParamsList(params);
    }
}
