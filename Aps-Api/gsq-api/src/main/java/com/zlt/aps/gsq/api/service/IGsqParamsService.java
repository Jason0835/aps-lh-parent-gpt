package com.zlt.aps.gsq.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gsq.api.domain.dto.GsqParamsDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 钢丝圈参数对外暴露接口
 * @author 89875
 */
@FeignClient(contextId = "IGsqParamsService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.gsq:gsq}")
public interface IGsqParamsService
{

    /**
     * 查询钢丝圈参数信息列表
     */
    @PostMapping("/gsq/params/list")
    public TableDataInfo list(@RequestBody GsqParamsDto dto);

    /**
     * 获取钢丝圈参数信息详细信息
     */
    @GetMapping("/gsq/params/{id}")
    public GsqParamsDto getInfo(@PathVariable("id") Long id);

    /**
     * 修改钢丝圈参数信息
     */
    @PostMapping("/gsq/params/edit")
    public AjaxResult edit(@RequestBody GsqParamsDto dto);

    /**
     * 导出接口
     * @param dto
     */
    @PostMapping("/gsq/params/exportData")
    List<GsqParamsDto> exportData(@RequestBody GsqParamsDto dto);
}
