package com.zlt.aps.tq.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tq.api.domain.dto.TqLossSettingDto;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 胎圈损耗率设定Service接口
 *
 * @author chen
 * @date 2021-07-13
 */
@FeignClient(contextId = "ITqLossSettingService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tq:tq}")
public interface ITqLossSettingService {

    /**
     * 查询胎圈损耗率设定列表
     */
    @PostMapping("/tq/loss/list")
    TableDataInfo list(@RequestBody TqLossSettingDto dto);

    /**
     * 新增胎圈损耗率设定
     */
    @PostMapping("/tq/loss/add")
    AjaxResult add(@RequestBody TqLossSettingDto dto);

    /**
     * 修改胎圈损耗率设定
     */
    @PostMapping("/tq/loss/edit")
    AjaxResult edit(@RequestBody TqLossSettingDto dto);

    /**
     * 删除胎圈损耗率设定
     */
    @DeleteMapping("/tq/loss/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/tq/loss/{id}")
    TqLossSettingDto getInfo(@PathVariable("id") Long id);

    /**
     * 校验胎圈损耗率设定唯一性
     */
    @PostMapping("/tq/loss/checkTqLossSettingUnique")
    String checkTqLossSettingUnique(@RequestBody TqLossSettingDto dto);

    /**
     * 导出胎圈损耗率设定列表
     */
    @PostMapping("/tq/loss/getList")
    List<TqLossSettingDto> getList(@RequestBody TqLossSettingDto dto);

    @PostMapping("/tq/loss/importData")
    @ApiOperation("导入胎圈损耗率信息")
    public AjaxResult importData(@RequestBody List<TqLossSettingDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

    /**
     * 删除全部(逻辑删)
     */
    @PostMapping("/tq/loss/deleteAll")
    AjaxResult deleteAll();
}
