package com.zlt.aps.cx.controller;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cx.service.CxParamsService;
import com.zlt.aps.cxlh.cx.api.domain.dto.CxParamsDto;
import com.zlt.aps.cxlh.cx.api.domain.dto.CxShowDeDto;
import com.zlt.aps.cxlh.cx.api.domain.dto.LhShowDeDto;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxParams;
import com.zlt.aps.maindata.service.impl.MdmMoldingMachineServiceImpl;
import com.zlt.aps.monthplan.api.domain.entity.MdmMoldingMachineB;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 成型参数信息Controller
 */
@RestController
@RequestMapping("/cx/params")
@Api(tags = {"成型参数信息维护接口"})
public class CxParamsController extends BaseController {
    @Autowired
    private CxParamsService cxParamsService;

    @Autowired
    private MdmMoldingMachineServiceImpl mdmMoldingMachineService;

    /**
     * 查询成型参数信息列表
     *
     * @return
     */
    @ApiOperation("查询成型参数信息列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody CxParamsDto dto) {
        CxParams params = new CxParams();
        BeanUtils.copyProperties(dto, params);
        startPage();
        dto.setOrderStr(orderStr());
        List<CxParamsDto> list = cxParamsService.selectParamsList(params);
        return getDataTable(list);
    }

    /**
     * 查询成型参数信息列表
     *
     * @return
     */
    @ApiOperation("查询成型定额信息列表")
    @PostMapping("/showDeList")
    public TableDataInfo showDeList(@RequestBody CxShowDeDto dto) {
        CxShowDeDto showDeDto = new CxShowDeDto();
        BeanUtils.copyProperties(dto, showDeDto);
        startPage();
        List<CxShowDeDto> list = cxParamsService.selectCxShowDeDtoList(showDeDto);
        return getDataTable(list);
    }


    /**
     * 查询硫化定额信息列表
     *
     * @return
     */
    @ApiOperation("查询成型定额信息列表")
    @PostMapping("/lhShowDeList")
    public TableDataInfo lhShowDeList(@RequestBody LhShowDeDto dto) {
        LhShowDeDto showDeDto = new LhShowDeDto();
        BeanUtils.copyProperties(dto, showDeDto);
        startPage();
        List<LhShowDeDto> list = cxParamsService.selectLhShowDeDtoList(showDeDto);
        return getDataTable(list);
    }


    /**
     * 获取成型参数信息详细信息
     *
     * @return 结果
     */
    @ApiOperation("获取成型参数信息详细信息")
    @GetMapping(value = "/{id}")
    public CxParams getInfo(@PathVariable("id") Long id) {
        return cxParamsService.selectParamsById(id);
    }

    /**
     * 修改成型参数信息
     *
     * @return 结果
     */
    @Log(title = "ui.data.column.cx.params.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改成型参数信息")
    @PostMapping("/edit")
    public AjaxResult edit(@Validated @RequestBody CxParamsDto dto) {
        CxParams params = new CxParams();
        BeanUtils.copyProperties(dto, params);
        if (UserConstants.NOT_UNIQUE.equals(cxParamsService.checkParamsCodeUnique(params))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.params.message.unique"));
        }
        return cxParamsService.updateParams(params);
    }

    /**
     * 导出成型参数信息
     *
     * @param dto 查询条件
     * @return 查询到的集合
     */
    @Log(title = "ui.data.column.cx.params.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出成型参数信息")
    @PostMapping("/exportData")
    public List<CxParamsDto> export(@RequestBody CxParamsDto dto) {
        CxParams params = new CxParams();
        BeanUtils.copyProperties(dto, params);
        startPage();
        dto.setOrderStr(orderStr());
        return cxParamsService.selectParamsList(params);
    }


    @ApiOperation("测试接口")
    @PostMapping("/test")
    public void test() {
        mdmMoldingMachineService.getMoldingMachineProNum("AH01");
    }

}
