package com.zlt.aps.tm.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tm.api.domain.dto.TmLossSettingDto;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 胎面损耗率设定Service接口
 * @author chen
 * @date 2021-07-12
 */
@FeignClient(contextId = "ITmLossSettingService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tm:tm}")
public interface ITmLossSettingService {

    /**
     * 查询胎面损耗率设定列表
     */
    @PostMapping("/tm/loss/list")
    TableDataInfo list(@RequestBody TmLossSettingDto dto);

    /**
    * 新增胎面损耗率设定
    */
    @PostMapping("/tm/loss/add")
    AjaxResult add(@RequestBody TmLossSettingDto dto);

    /**
     * 修改胎面损耗率设定
     */
    @PostMapping("/tm/loss/edit")
    AjaxResult edit(@RequestBody TmLossSettingDto dto);

    /**
     * 删除胎面损耗率设定
     */
    @DeleteMapping("/tm/loss/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/tm/loss/{id}")
    TmLossSettingDto getInfo(@PathVariable("id") Long id);

    /**
     * 校验胎面损耗率设定唯一性
     */
    @PostMapping("/tm/loss/checkTmLossSettingUnique")
    String checkTmLossSettingUnique(@RequestBody TmLossSettingDto dto);

    /**
     * 导出胎面损耗率设定列表
     */
    @PostMapping("/tm/loss/getList")
    List<TmLossSettingDto> getList(@RequestBody TmLossSettingDto dto);

    @PostMapping("/tm/loss/importData")
    @ApiOperation("导入胎面损耗率信息")
    public AjaxResult importData(@RequestBody List<TmLossSettingDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

    /**
     * 删除全部(逻辑删)
     */
    @PostMapping("/tm/loss/deleteAll")
    AjaxResult deleteAll();
}
