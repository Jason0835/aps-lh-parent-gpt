package com.zlt.aps.tc.controller;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.tc.api.domain.dto.TcParamsDto;
import com.zlt.aps.tc.entity.TcParams;
import com.zlt.aps.tc.service.TcParamsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 胎侧参数信息Controller
 *
 * @author zlt
 * @date 2021-05-25
 */
@RestController
@RequestMapping("/tc/params")
@Api(tags = {"胎侧参数信息维护接口"})
public class TcParamsController extends BaseController {
    @Autowired
    private TcParamsService tcParamsService;

    /**
     * 查询胎侧参数信息列表
     *
     * @return
     */
    @ApiOperation("查询胎侧参数信息列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody TcParamsDto dto) {
        TcParams params = new TcParams();
        BeanUtils.copyProperties(dto, params);
        startPage();
        params.setOrderStr(orderStr());
        List<TcParamsDto> list = tcParamsService.selectParamsList(params);
        return getDataTable(list);
    }

    /**
     * 获取胎侧参数信息详细信息
     *
     * @return 结果
     */
    @ApiOperation("获取胎侧参数信息详细信息")
    @GetMapping(value = "/{id}")
    public TcParams getInfo(@PathVariable("id") Long id) {
        return tcParamsService.selectParamsById(id);
    }

    /**
     * 修改胎侧参数信息
     *
     * @return 结果
     */
    @Log(title = "ui.data.column.tc.params.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改胎侧参数信息")
    @PostMapping("/edit")
    public AjaxResult edit(@Validated @RequestBody TcParamsDto dto) {
        TcParams params = new TcParams();
        BeanUtils.copyProperties(dto, params);
        if (UserConstants.NOT_UNIQUE.equals(tcParamsService.checkParamsCodeUnique(params))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.params.message.unique"));
        }
        return tcParamsService.updateParams(params);
    }

    @Log(title = "ui.data.column.tc.params.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出胎侧参数信息")
    @GetMapping("/exportData")
    public List<TcParamsDto> export(@SpringQueryMap TcParamsDto dto) {
        TcParams params = new TcParams();
        BeanUtils.copyProperties(dto, params);
        startPage();
        params.setOrderStr(orderStr());
        return tcParamsService.selectParamsList(params);
    }
}
