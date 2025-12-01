package com.zlt.aps.gdyy.controller;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.gdyy.api.domain.dto.GdyyParamsDto;
import com.zlt.aps.gdyy.entity.GdyyParams;
import com.zlt.aps.gdyy.service.GdyyParamsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 钢带压延参数信息Controller
 *
 * @author zlt
 * @date 2021-05-25
 */
@RestController
@RequestMapping("/gdyy/params")
@Api(tags = {"钢带压延参数信息维护接口"})
public class GdyyParamsController extends BaseController {
    @Autowired
    private GdyyParamsService gdyyParamsService;

    /**
     * 查询钢带压延参数信息列表
     *
     * @return
     */
    @ApiOperation("查询钢带压延参数信息列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody GdyyParamsDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        GdyyParams params = new GdyyParams();
        BeanUtils.copyProperties(dto, params);
        List<GdyyParamsDto> list = gdyyParamsService.selectParamsList(params);
        return getDataTable(list);
    }

    /**
     * 获取钢带压延参数信息详细信息
     *
     * @return 结果
     */
    @ApiOperation("获取钢带压延参数信息详细信息")
    @GetMapping(value = "/{id}")
    public GdyyParams getInfo(@PathVariable("id") Long id) {
        return gdyyParamsService.selectParamsById(id);
    }

    /**
     * 修改钢带压延参数信息
     *
     * @return 结果
     */
    @Log(title = "ui.data.column.gdyy.params.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改钢带压延参数信息")
    @PostMapping("/edit")
    public AjaxResult edit(@Validated @RequestBody GdyyParamsDto dto) {
        GdyyParams params = new GdyyParams();
        BeanUtils.copyProperties(dto, params);
        if (UserConstants.NOT_UNIQUE.equals(gdyyParamsService.checkParamsCodeUnique(params))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.params.message.unique"));
        }
        return gdyyParamsService.updateParams(params);
    }

    /**
     * 导出钢带压延参数信息
     *
     * @param dto 查询条件
     * @return 查询到的集合
     */
    @Log(title = "ui.data.column.gdyy.params.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出钢带压延参数信息")
    @PostMapping("/exportData")
    public List<GdyyParamsDto> export(@RequestBody GdyyParamsDto dto) {
        dto.setOrderStr(orderStr());
        GdyyParams params = new GdyyParams();
        BeanUtils.copyProperties(dto, params);
        return gdyyParamsService.selectParamsList(params);
    }
}
