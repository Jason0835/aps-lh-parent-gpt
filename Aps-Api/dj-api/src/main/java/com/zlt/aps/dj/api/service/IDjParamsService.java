package com.zlt.aps.dj.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.dj.api.domain.dto.DjParamsDto;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 垫胶参数对外暴露接口
 * @author 89875
 */
@FeignClient(contextId = "INcParamsService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.dj:nc}")
public interface IDjParamsService
{

    /**
     * 查询垫胶参数信息列表
     */
    @PostMapping("/dj/params/list")
    public TableDataInfo list(@RequestBody DjParamsDto dto);

    /**
     * 获取垫胶参数信息详细信息
     */
    @GetMapping("/dj/params/{id}")
    public DjParamsDto getInfo(@PathVariable("id") Long id);

    /**
     * 修改垫胶参数信息
     */
    @PostMapping("/dj/params/edit")
    public AjaxResult edit(@RequestBody DjParamsDto dto);

    /**
     * 导出接口
     * @param dto
     */
    @PostMapping("/dj/params/exportData")
    List<DjParamsDto> exportData(@RequestBody DjParamsDto dto);
}
