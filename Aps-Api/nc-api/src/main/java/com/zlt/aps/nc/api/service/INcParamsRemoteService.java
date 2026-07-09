package com.zlt.aps.nc.api.service;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.nc.api.domain.entity.NcParams;


/**
 * 内衬参数信息Service接口
 *
 * @author zlt
 * @date 2026-06-11
 */
@FeignClient(contextId = "INcParamsRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.nc:nc}")
public interface INcParamsRemoteService {

    /**
     * 获取内衬参数信息列表
     *
     * @param params
     * @return
     */
    @PostMapping("/nc/params/list")
    TableDataInfo list(@RequestBody NcParams params);

    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/nc/params/{id}")
    AjaxResult getInfo(@PathVariable("id") Long id);

    /**
     * 修改内衬参数信息
     *
     * @param params
     * @return
     */
    @PostMapping("/nc/params/edit")
    AjaxResult edit(@Validated @RequestBody NcParams params);

    /**
     * 删除内衬参数信息
     *
     * @param ids
     * @return
     */
    @PostMapping("/nc/params/remove")
    AjaxResult remove(@RequestBody List<Long> ids);

    /**
     * 校验唯一性
     */
    @PostMapping("/nc/params/checkUnique")
    String checkUnique(@RequestBody NcParams params);

    /**
     * 根据参数编码查询内衬参数信息
     */
    @PostMapping("/nc/params/getByParamCode")
    NcParams getByParamCode(@RequestBody NcParams entity);
}
