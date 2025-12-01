package com.zlt.aps.tc.controller;


import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.tc.api.domain.dto.TcCurlRollDto;
import com.zlt.aps.tc.api.domain.entity.TcCurlRoll;
import com.zlt.aps.tc.service.TcCurlRollService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Api(tags = {"胎侧卷曲信息接口"})
@RestController
@RequestMapping("/tc/curlRoll")
public class TcCurlRollController extends BaseController {

    @Resource
    private TcCurlRollService tcCurlRollService;

    @ApiOperation("根据条件查询胎侧卷曲信息列表")
    @PostMapping("/listCurlRoll")
    public TableDataInfo listCurlRoll(@RequestBody TcCurlRoll dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<TcCurlRoll> list = tcCurlRollService.listCurlRoll(dto);
        return getDataTable(list);
    }

    @ApiOperation("根据id查询胎侧卷曲信息信息")
    @GetMapping("/getCurlRoll/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public TcCurlRoll getCurlRoll(@PathVariable("id") Long id) {
        TcCurlRoll dto = new TcCurlRoll();
        BeanUtils.copyProperties(tcCurlRollService.getById(id), dto);
        return dto;
    }

    @Log(title = "ui.tc.curlRoll.column.modalName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存胎侧卷曲信息信息（id为空则新增，id不为空则修改）")
    @PostMapping("/saveCurlRoll")
    public AjaxResult saveCurlRoll(@RequestBody TcCurlRoll dto) {
        TcCurlRoll entity = new TcCurlRoll();
        BeanUtils.copyProperties(dto, entity);
        tcCurlRollService.saveCurlRoll(entity);
        return AjaxResult.success();
    }

    @ApiOperation("根据code判断胎侧卷曲代号是否已经存在")
    @PostMapping("/checkCurlRollCodeUnique")
    public String checkCurlRollCodeUnique(@RequestBody TcCurlRoll dto) {
        return tcCurlRollService.checkCurlRollCodeUnique(dto);
    }

    @Log(title = "ui.tc.curlRoll.column.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("批量删除胎侧卷曲信息信息(逻辑删)")
    @PostMapping("/deleteCurlRoll/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteCurlRoll(@PathVariable("ids") Long[] ids) {
        tcCurlRollService.deleteCurlRoll(ids);
        return AjaxResult.success();
    }

    @Log(title = "ui.tc.curlRoll.column.modalName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData")
    public List<TcCurlRoll> exportData(@RequestBody TcCurlRoll dto) {
        dto.setOrderStr(orderStr());
        List<TcCurlRoll> list = tcCurlRollService.listCurlRoll(dto);
        return list;
    }

    @Log(title = "ui.tc.curlRoll.column.modalName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<TcCurlRollDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return tcCurlRollService.importData(list, updateSupport, importLogId);
    }

    /**
     * 根据编号查询卷曲长度
     *
     * @param curlRoll 查询条件
     * @return 结果
     */
    @ApiOperation("根据编号查询卷曲长度")
    @PostMapping("/selectCurlLengthByCode")
    public AjaxResult selectCurlLengthByCode(@RequestBody TcCurlRoll curlRoll) {
        return tcCurlRollService.selectCurlLengthByCode(curlRoll);
    }
}
