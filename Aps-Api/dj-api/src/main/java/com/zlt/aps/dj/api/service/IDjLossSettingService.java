package com.zlt.aps.dj.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.dj.api.domain.dto.DjLossSettingDto;

import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 垫胶损耗率设定Service接口
 *
 * @author chen
 * @date 2021-07-13
 */
@FeignClient(contextId = "INcLossSettingService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.dj:nc}")
public interface IDjLossSettingService {

    /**
     * 查询垫胶损耗率设定列表
     */
    @PostMapping("/dj/loss/list")
    TableDataInfo list(@RequestBody DjLossSettingDto dto);

    /**
     * 新增垫胶损耗率设定
     */
    @PostMapping("/dj/loss/add")
    AjaxResult add(@RequestBody DjLossSettingDto dto);

    /**
     * 修改垫胶损耗率设定
     */
    @PostMapping("/dj/loss/edit")
    AjaxResult edit(@RequestBody DjLossSettingDto dto);

    /**
     * 删除垫胶损耗率设定
     */
    @DeleteMapping("/dj/loss/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/dj/loss/{id}")
    DjLossSettingDto getInfo(@PathVariable("id") Long id);

    /**
     * 校验垫胶损耗率设定唯一性
     */
    @PostMapping("/dj/loss/checkNcLossSettingUnique")
    String checkNcLossSettingUnique(@RequestBody DjLossSettingDto dto);

    /**
     * 导出垫胶损耗率设定列表
     */
    @PostMapping("/dj/loss/getList")
    List<DjLossSettingDto> getList(@RequestBody DjLossSettingDto dto);

    @PostMapping("/dj/loss/importData")
    @ApiOperation("导入垫胶损耗率信息")
    public AjaxResult importData(@RequestBody List<DjLossSettingDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);


    /**
     * 删除全部(逻辑删)
     */
    @PostMapping("/dj/loss/deleteAll")
    AjaxResult deleteAll();
}
