package com.zlt.aps.tc.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tc.api.domain.dto.TcParamsDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 胎侧参数对外暴露接口
 * @author 89875
 */
@FeignClient(contextId = "ITcParamsService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.tc:tc}")
public interface ITcParamsService
{

    /**
     * 查询胎侧参数信息列表
     */
    @PostMapping("/tc/params/list")
    public TableDataInfo list(@RequestBody TcParamsDto dto);

    /**
     * 获取胎侧参数信息详细信息
     */
    @GetMapping("/tc/params/{id}")
    public TcParamsDto getInfo(@PathVariable("id") Long id);

    /**
     * 修改胎侧参数信息
     */
    @PostMapping("/tc/params/edit")
    public AjaxResult edit(@RequestBody TcParamsDto dto);

    /**
     * 导出接口
     * @param dto
     */
    @PostMapping("/tc/params/exportData")
    List<TcParamsDto> exportData(@RequestBody TcParamsDto dto);
}
