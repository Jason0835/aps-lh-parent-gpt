package com.zlt.aps.xwyy.controller;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.xwyy.api.domain.dto.XwyyParamsDto;
import com.zlt.aps.xwyy.entity.XwyyParams;
import com.zlt.aps.xwyy.service.XwyyParamsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 纤维压延参数信息Controller
 *
 * @author chenxueyuan
 * @date 2021-06-07
 */
@RestController
@RequestMapping("/xwyy/params")
@Api(tags = {"纤维压延参数信息维护接口"})
public class XwyyParamsController extends BaseController {
    @Autowired
    private XwyyParamsService xwyyParamsService;

    /**
     * 查询纤维压延参数信息列表
     *
     * @return
     */
    @ApiOperation("查询纤维压延参数信息列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody XwyyParamsDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        XwyyParams params = new XwyyParams();
        BeanUtils.copyProperties(dto, params);
        List<XwyyParamsDto> list = xwyyParamsService.selectParamsList(params);
        return getDataTable(list);
    }

    /**
     * 获取纤维压延参数信息详细信息
     *
     * @return 结果
     */
    @ApiOperation("获取纤维压延参数信息详细信息")
    @GetMapping(value = "/{id}")
    public XwyyParams getInfo(@PathVariable("id") Long id) {
        return xwyyParamsService.selectParamsById(id);
    }

    /**
     * 修改纤维压延参数信息
     *
     * @return 结果
     */
    @Log(title = "ui.data.column.xwyy.params.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改纤维压延参数信息")
    @PostMapping("/edit")
    public AjaxResult edit(@Validated @RequestBody XwyyParamsDto dto) {
        XwyyParams params = new XwyyParams();
        BeanUtils.copyProperties(dto, params);
        if (UserConstants.NOT_UNIQUE.equals(xwyyParamsService.checkParamsCodeUnique(params))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.params.message.unique"));
        }
        return xwyyParamsService.updateParams(params);
    }

    /**
     * 导出纤维压延参数信息
     *
     * @param dto 查询条件
     * @return 查询到的集合
     */
    @Log(title = "ui.data.column.xwyy.params.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出纤维压延参数信息")
    @PostMapping("/exportData")
    public List<XwyyParamsDto> export(@RequestBody XwyyParamsDto dto) {
        dto.setOrderStr(orderStr());
        XwyyParams params = new XwyyParams();
        BeanUtils.copyProperties(dto, params);
        return xwyyParamsService.selectParamsList(params);
    }
}
