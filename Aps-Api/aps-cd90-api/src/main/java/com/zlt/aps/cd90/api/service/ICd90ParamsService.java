package com.zlt.aps.cd90.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd90.api.domain.dto.Cd90ParamsDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 90度裁断参数对外暴露接口
 *
 * @author chenxueyuan
 */
@FeignClient(contextId = "ICd90ParamsService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd90:cd90}")
public interface ICd90ParamsService {

    /**
     * 查询90度裁断参数信息列表
     */
    @PostMapping("/cd90/params/list")
    public TableDataInfo list(@RequestBody Cd90ParamsDto dto);

    /**
     * 获取90度裁断参数信息详细信息
     */
    @GetMapping("/cd90/params/{id}")
    public Cd90ParamsDto getInfo(@PathVariable("id") Long id);

    /**
     * 修改90度裁断参数信息
     */
    @PostMapping("/cd90/params/edit")
    public AjaxResult edit(@RequestBody Cd90ParamsDto dto);

    /**
     * 导出接口
     *
     * @param dto 查询条件
     */
    @PostMapping("/cd90/params/exportData")
    List<Cd90ParamsDto> exportData(@RequestBody Cd90ParamsDto dto);
}
