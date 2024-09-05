package com.zlt.aps.gsq.controller;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.gsq.api.domain.dto.GsqParamsDto;
import com.zlt.aps.gsq.entity.GsqParams;
import com.zlt.aps.gsq.service.GsqParamsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 钢丝圈参数信息Controller
 *
 * @author zlt
 * @date 2021-05-25
 */
@RestController
@RequestMapping("/gsq/params")
@Api(tags = {"钢丝圈参数信息维护接口"})
public class GsqParamsController extends BaseController {
    @Autowired
    private GsqParamsService gsqParamsService;

    /**
     * 查询钢丝圈参数信息列表
     *
     * @return
     */
    @ApiOperation("查询钢丝圈参数信息列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody GsqParamsDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        GsqParams params = new GsqParams();
        BeanUtils.copyProperties(dto, params);
        List<GsqParamsDto> list = gsqParamsService.selectParamsList(params);
        return getDataTable(list);
    }

    /**
     * 获取钢丝圈参数信息详细信息
     *
     * @return 结果
     */
    @ApiOperation("获取钢丝圈参数信息详细信息")
    @GetMapping(value = "/{id}")
    public GsqParams getInfo(@PathVariable("id") Long id) {
        return gsqParamsService.selectParamsById(id);
    }

    /**
     * 修改钢丝圈参数信息
     *
     * @return 结果
     */
    @Log(title = "ui.data.column.gsq.params.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改钢丝圈参数信息")
    @PostMapping("/edit")
    public AjaxResult edit(@Validated @RequestBody GsqParamsDto dto) {
        GsqParams params = new GsqParams();
        BeanUtils.copyProperties(dto, params);
        if (UserConstants.NOT_UNIQUE.equals(gsqParamsService.checkParamsCodeUnique(params))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.params.message.unique"));
        }
        return gsqParamsService.updateParams(params);
    }

    /**
     * 导出钢丝圈参数信息
     *
     * @param dto 查询条件
     * @return 查询到的集合
     */
    @Log(title = "ui.data.column.gsq.params.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出钢丝圈参数信息")
    @GetMapping("/exportData")
    public List<GsqParamsDto> export(@SpringQueryMap GsqParamsDto dto) {
        dto.setOrderStr(orderStr());
        GsqParams params = new GsqParams();
        BeanUtils.copyProperties(dto, params);
        return gsqParamsService.selectParamsList(params);
    }
}
