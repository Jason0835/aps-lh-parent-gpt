package com.zlt.aps.cx.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.api.domain.dto.CxParamsDto;
import com.zlt.aps.cx.api.domain.dto.CxShowDeDto;
import com.zlt.aps.cx.api.domain.dto.LhShowDeDto;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 成型参数对外暴露接口
 *
 * @author Joran.Zhang
 */
@FeignClient(contextId = "ICxParamsService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:cx}")
public interface ICxParamsService {

    /**
     * 查询成型参数信息列表
     */
    @PostMapping("/cx/params/list")
    public TableDataInfo list(@RequestBody CxParamsDto dto);

    /**
     * 获取成型参数信息详细信息
     */
    @GetMapping("/cx/params/{id}")
    public CxParamsDto getInfo(@PathVariable("id") Long id);

    /**
     * 修改成型参数信息
     */
    @PostMapping("/cx/params/edit")
    public AjaxResult edit(@RequestBody CxParamsDto dto);

    /**
     * 导出接口
     *
     * @param dto
     */
    @GetMapping("/cx/params/exportData")
    List<CxParamsDto> exportData(@SpringQueryMap CxParamsDto dto);


    @ApiOperation("查询成型定额信息列表")
    @PostMapping("/cx/params/showDeList")
     TableDataInfo showDeList(@RequestBody CxShowDeDto dto);


    @ApiOperation("查询成型定额信息列表")
    @PostMapping("/cx/params/lhShowDeList")
     TableDataInfo lhShowDeList(@RequestBody LhShowDeDto dto);
}
