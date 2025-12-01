package com.zlt.aps.gdyy.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gdyy.api.domain.dto.GdyyParamsDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 钢带压延参数对外暴露接口
 * @author chenxueyuan
 */
@FeignClient(contextId = "IGdyyParamsService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.gdyy:gdyy}")
public interface IGdyyParamsService
{
    /**
     * 查询钢带压延参数信息列表
     */
    @PostMapping("/gdyy/params/list")
    public TableDataInfo list(@RequestBody GdyyParamsDto dto);

    /**
     * 获取钢带压延参数信息详细信息
     */
    @GetMapping("/gdyy/params/{id}")
    public GdyyParamsDto getInfo(@PathVariable("id") Long id);

    /**
     * 修改钢带压延参数信息
     */
    @PostMapping("/gdyy/params/edit")
    public AjaxResult edit(@RequestBody GdyyParamsDto dto);

    /**
     * 导出接口
     * @param dto 查询条件
     */
    @PostMapping("/gdyy/params/exportData")
    List<GdyyParamsDto> exportData(@RequestBody GdyyParamsDto dto);
}
