package com.zlt.aps.lh.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.lh.api.domain.dto.LhParamsDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 硫化参数对外暴露接口
 *
 * @author Joran.Zhang
 */
@FeignClient(contextId = "ILhParamsService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:lh}")
public interface ILhParamsService {

    /**
     * 查询硫化参数信息列表
     */
    @PostMapping("/lh/params/list")
    public TableDataInfo list(@RequestBody LhParamsDto dto);

    /**
     * 获取硫化参数信息详细信息
     */
    @GetMapping("/lh/params/{id}")
    public LhParamsDto getInfo(@PathVariable("id") Long id);

    /**
     * 修改硫化参数信息
     */
    @PostMapping("/lh/params/edit")
    public AjaxResult edit(@RequestBody LhParamsDto dto);

    /**
     * 导出接口
     *
     * @param dto
     */
    @GetMapping("/lh/params/exportData")
    List<LhParamsDto> exportData(@SpringQueryMap LhParamsDto dto);
}
