package com.zlt.aps.tm.controller;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.tm.api.domain.dto.TmParamsDto;
import com.zlt.aps.tm.entity.TmParams;
import com.zlt.aps.tm.service.TmParamsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 胎面参数信息Controller
 *
 * @author zlt
 * @date 2021-05-25
 */
@RestController
@RequestMapping("/tm/params")
@Api(tags = {"胎面参数信息维护接口"})
public class TmParamsController extends BaseController {
    @Autowired
    private TmParamsService tmParamsService;

    /**
     * 查询胎面参数信息列表
     *
     * @return 查询到的集合
     */
    @ApiOperation("查询胎面参数信息列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody TmParamsDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        TmParams tmParams = new TmParams();
        BeanUtils.copyProperties(dto, tmParams);
        List<TmParamsDto> list = tmParamsService.selectParamsList(tmParams);
        return getDataTable(list);
    }

    /**
     * 获取胎面参数信息详细信息
     *
     * @return 结果
     */
    @ApiOperation("获取胎面参数信息详细信息")
    @GetMapping(value = "/{id}")
    public TmParams getInfo(@PathVariable("id") Long id) {
        return tmParamsService.selectParamsById(id);
    }

    /**
     * 修改胎面参数信息
     *
     * @return 结果
     */
    @Log(title = "ui.data.column.tm.params.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改胎面参数信息")
    @PostMapping("/edit")
    public AjaxResult edit(@Validated @RequestBody TmParamsDto dto) {
        TmParams tmParams = new TmParams();
        BeanUtils.copyProperties(dto, tmParams);
        if (UserConstants.NOT_UNIQUE.equals(tmParamsService.checkParamsCodeUnique(tmParams))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.params.message.unique"));
        }
        return tmParamsService.updateParams(tmParams);
    }

    /**
     * 导出胎面参数信息
     *
     * @param dto 查询条件
     * @return 查询到的集合
     */
    @Log(title = "ui.data.column.tm.params.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出胎面参数信息")
    @GetMapping("/exportData")
    public List<TmParamsDto> export(@SpringQueryMap TmParamsDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        TmParams tmParams = new TmParams();
        BeanUtils.copyProperties(dto, tmParams);
        return tmParamsService.selectParamsList(tmParams);
    }
}
