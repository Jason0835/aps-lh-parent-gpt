package com.zlt.aps.tq.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tq.api.domain.dto.TqParamsDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 胎圈参数对外暴露接口
 * @author Joran.Zhang
 */
@FeignClient(contextId = "ITqParamsService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.tq:tq}")
public interface ITqParamsService
{
    
    /**
     * 查询胎圈参数信息列表
     */
    @PostMapping("/tq/params/list")
    public TableDataInfo list(@RequestBody TqParamsDto dto);

    /**
     * 获取胎圈参数信息详细信息
     */
    @GetMapping("/tq/params/{id}")
    public TqParamsDto getInfo(@PathVariable("id") Long id);

    /**
     * 修改胎圈参数信息
     */
    @PostMapping("/tq/params/edit")
    public AjaxResult edit(@RequestBody TqParamsDto dto);

    /**
     * 导出接口
     * @param dto 查询条件
     */
    @GetMapping("/tq/params/exportData")
    List<TqParamsDto> exportData(@SpringQueryMap TqParamsDto dto);
}
