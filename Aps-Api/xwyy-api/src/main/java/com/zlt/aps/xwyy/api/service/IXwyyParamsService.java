package com.zlt.aps.xwyy.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.xwyy.api.domain.dto.XwyyParamsDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 纤维压延参数对外暴露接口
 *
 * @author chenxueyuan
 */
@FeignClient(contextId = "IXwyyParamsService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.xwyy:xwyy}")
public interface IXwyyParamsService {

    /**
     * 查询纤维压延参数信息列表
     */
    @PostMapping("/xwyy/params/list")
    public TableDataInfo list(@RequestBody XwyyParamsDto dto);

    /**
     * 获取纤维压延参数信息详细信息
     */
    @GetMapping("/xwyy/params/{id}")
    public XwyyParamsDto getInfo(@PathVariable("id") Long id);

    /**
     * 修改纤维压延参数信息
     */
    @PostMapping("/xwyy/params/edit")
    public AjaxResult edit(@RequestBody XwyyParamsDto dto);

    /**
     * 导出接口
     *
     * @param dto 查询条件
     */
    @GetMapping("/xwyy/params/exportData")
    List<XwyyParamsDto> exportData(@SpringQueryMap XwyyParamsDto dto);
}
