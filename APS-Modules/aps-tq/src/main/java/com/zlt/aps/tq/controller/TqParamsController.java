package com.zlt.aps.tq.controller;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.tq.api.domain.dto.TqParamsDto;
import com.zlt.aps.tq.entity.TqParams;
import com.zlt.aps.tq.service.TqParamsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 胎圈参数信息Controller
 *
 * @author zlt
 * @date 2021-05-25
 */
@RestController
@RequestMapping("/tq/params")
@Api(tags = {"胎圈参数信息维护接口"})
public class TqParamsController extends BaseController {
    @Autowired
    private TqParamsService tqParamsService;

    /**
     * 查询胎圈参数信息列表
     *
     * @return
     */
    @ApiOperation("查询胎圈参数信息列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody TqParamsDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        TqParams params = new TqParams();
        BeanUtils.copyProperties(dto, params);
        List<TqParamsDto> list = tqParamsService.selectParamsList(params);
        return getDataTable(list);
    }

    /**
     * 获取胎圈参数信息详细信息
     *
     * @return 结果
     */
    @ApiOperation("获取胎圈参数信息详细信息")
    @GetMapping(value = "/{id}")
    public TqParams getInfo(@PathVariable("id") Long id) {
        return tqParamsService.selectParamsById(id);
    }

    /**
     * 修改胎圈参数信息
     *
     * @return 结果
     */
    @Log(title = "ui.data.column.tq.params.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改胎圈参数信息")
    @PostMapping("/edit")
    public AjaxResult edit(@Validated @RequestBody TqParamsDto dto) {
        TqParams params = new TqParams();
        BeanUtils.copyProperties(dto, params);
        if (UserConstants.NOT_UNIQUE.equals(tqParamsService.checkParamsCodeUnique(params))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.params.message.unique"));
        }
        return tqParamsService.updateParams(params);
    }

    /**
     * 导出胎圈参数信息
     *
     * @param dto 查询条件
     * @return 查询到的集合
     */
    @Log(title = "ui.data.column.tq.params.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出胎圈参数信息")
    @GetMapping("/exportData")
    public List<TqParamsDto> export(@SpringQueryMap TqParamsDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        TqParams params = new TqParams();
        BeanUtils.copyProperties(dto, params);
        return tqParamsService.selectParamsList(params);
    }
}
