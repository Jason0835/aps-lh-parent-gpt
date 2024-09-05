package com.zlt.aps.nc.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.nc.api.domain.dto.NcParamsDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 内衬参数对外暴露接口
 * @author 89875
 */
@FeignClient(contextId = "INcParamsService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.nc:nc}")
public interface INcParamsService
{

    /**
     * 查询内衬参数信息列表
     */
    @PostMapping("/nc/params/list")
    public TableDataInfo list(@RequestBody NcParamsDto dto);

    /**
     * 获取内衬参数信息详细信息
     */
    @GetMapping("/nc/params/{id}")
    public NcParamsDto getInfo(@PathVariable("id") Long id);

    /**
     * 修改内衬参数信息
     */
    @PostMapping("/nc/params/edit")
    public AjaxResult edit(@RequestBody NcParamsDto dto);

    /**
     * 导出接口
     * @param dto
     */
    @GetMapping("/nc/params/exportData")
    List<NcParamsDto> exportData(@SpringQueryMap NcParamsDto dto);
}
