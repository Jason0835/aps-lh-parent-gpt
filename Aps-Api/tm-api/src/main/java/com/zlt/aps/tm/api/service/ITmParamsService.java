package com.zlt.aps.tm.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tm.api.domain.dto.TmParamsDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.IOException;
import java.util.List;

/**
 * 胎面参数对外暴露接口
 * @author 89875
 */
@FeignClient(contextId = "ITmParamsService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.tm:tm}")
public interface ITmParamsService
{

    /**
     * 查询胎面参数信息列表
     */
    @PostMapping("/tm/params/list")
    public TableDataInfo list(@RequestBody TmParamsDto dto);

    /**
     * 导出胎面参数信息列表
     */
    @PostMapping("/tm/params/export")
    public void export(@RequestBody TmParamsDto tmParamsDto) throws IOException;

    /**
     * 获取胎面参数信息详细信息
     */
    @GetMapping("/tm/params/{id}")
    public TmParamsDto getInfo(@PathVariable("id") Long id);

    /**
     * 修改胎面参数信息
     */
    @PostMapping("/tm/params/edit")
    public AjaxResult edit(@RequestBody TmParamsDto tmParamsDto);

    /**
     * 导出接口
     * @param dto
     */
    @GetMapping("/tm/params/exportData")
    List<TmParamsDto> exportData(@SpringQueryMap TmParamsDto dto);
}
