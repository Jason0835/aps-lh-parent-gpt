package com.zlt.aps.nc.controller;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.nc.api.domain.dto.NcParamsDto;
import com.zlt.aps.nc.entity.NcParams;
import com.zlt.aps.nc.service.NcParamsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 内衬参数信息Controller
 *
 * @author zlt
 * @date 2021-05-25
 */
@RestController
@RequestMapping("/nc/params")
@Api(tags = {"内衬参数信息维护接口"})
public class NcParamsController extends BaseController {
    @Autowired
    private NcParamsService ncParamsService;

    /**
     * 查询内衬参数信息列表
     *
     * @return
     */
    @ApiOperation("查询内衬参数信息列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody NcParamsDto dto) {
        NcParams params = new NcParams();
        BeanUtils.copyProperties(dto, params);
        startPage();
        params.setOrderStr(orderStr());
        List<NcParamsDto> list = ncParamsService.selectParamsList(params);
        return getDataTable(list);
    }

    /**
     * 获取内衬参数信息详细信息
     *
     * @return 结果
     */
    @ApiOperation("获取内衬参数信息详细信息")
    @GetMapping(value = "/{id}")
    public NcParams getInfo(@PathVariable("id") Long id) {
        return ncParamsService.selectParamsById(id);
    }

    /**
     * 修改内衬参数信息
     *
     * @return 结果
     */
    @Log(title = "ui.data.column.nc.params.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改内衬参数信息")
    @PostMapping("/edit")
    public AjaxResult edit(@Validated @RequestBody NcParamsDto dto) {
        NcParams params = new NcParams();
        BeanUtils.copyProperties(dto, params);
        if (UserConstants.NOT_UNIQUE.equals(ncParamsService.checkParamsCodeUnique(params))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.params.message.unique"));
        }
        return ncParamsService.updateParams(params);
    }

    /**
     * 导出内衬参数信息
     *
     * @param dto 查询条件
     * @return 查询到的集合
     */
    @Log(title = "ui.data.column.nc.params.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出内衬参数信息")
    @GetMapping("/exportData")
    public List<NcParamsDto> export(@SpringQueryMap NcParamsDto dto) {
        NcParams params = new NcParams();
        BeanUtils.copyProperties(dto, params);
        startPage();
        params.setOrderStr(orderStr());
        return ncParamsService.selectParamsList(params);
    }
}
