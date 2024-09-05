package com.zlt.aps.cd15.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd15.api.domain.dto.Cd15ParamsDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 15度裁断参数对外暴露接口
 * @author chenxueyuan
 */
@FeignClient(contextId = "ICd15ParamsService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.cd15:cd15}")
public interface ICd15ParamsService
{
    
    /**
     * 查询15度裁断参数信息列表
     */
    @PostMapping("/cd15/params/list")
    public TableDataInfo list(@RequestBody Cd15ParamsDto dto);

    /**
     * 获取15度裁断参数信息详细信息
     */
    @GetMapping("/cd15/params/{id}")
    public Cd15ParamsDto getInfo(@PathVariable("id") Long id);

    /**
     * 修改15度裁断参数信息
     */
    @PostMapping("/cd15/params/edit")
    public AjaxResult edit(@RequestBody Cd15ParamsDto dto);

    /**
     * 导出接口
     * @param dto 查询条件
     */
    @GetMapping("/cd15/params/exportData")
    List<Cd15ParamsDto> exportData(@SpringQueryMap Cd15ParamsDto dto);
}
