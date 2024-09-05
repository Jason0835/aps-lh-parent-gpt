package com.zlt.aps.nc.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.nc.api.domain.dto.NcLossSettingDto;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 内衬损耗率设定Service接口
 *
 * @author chen
 * @date 2021-07-13
 */
@FeignClient(contextId = "INcLossSettingService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.nc:nc}")
public interface INcLossSettingService {

    /**
     * 查询内衬损耗率设定列表
     */
    @PostMapping("/loss/list")
    TableDataInfo list(@RequestBody NcLossSettingDto dto);

    /**
     * 新增内衬损耗率设定
     */
    @PostMapping("/loss/add")
    AjaxResult add(@RequestBody NcLossSettingDto dto);

    /**
     * 修改内衬损耗率设定
     */
    @PostMapping("/loss/edit")
    AjaxResult edit(@RequestBody NcLossSettingDto dto);

    /**
     * 删除内衬损耗率设定
     */
    @DeleteMapping("/loss/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/loss/{id}")
    NcLossSettingDto getInfo(@PathVariable("id") Long id);

    /**
     * 校验内衬损耗率设定唯一性
     */
    @PostMapping("/loss/checkNcLossSettingUnique")
    String checkNcLossSettingUnique(@RequestBody NcLossSettingDto dto);

    /**
     * 导出内衬损耗率设定列表
     */
    @PostMapping("/loss/getList")
    List<NcLossSettingDto> getList(@RequestBody NcLossSettingDto dto);

    @PostMapping("/loss/importData")
    @ApiOperation("导入内衬损耗率信息")
    public AjaxResult importData(@RequestBody List<NcLossSettingDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);


    /**
     * 删除全部(逻辑删)
     */
    @PostMapping("/loss/deleteAll")
    AjaxResult deleteAll();
}
