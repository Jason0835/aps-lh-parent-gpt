package com.zlt.aps.dj.api.service;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.dj.api.domain.entity.DjParams;


/**
 * 垫胶参数信息Service接口
 *
 * @author zlt
 * @date 2026-06-11
 */
@FeignClient(contextId = "IDjParamsService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.dj:dj}")
public interface IDjParamsService {

    /**
     * 获取垫胶参数信息列表
     *
     * @param params
     * @return
     */
    @PostMapping("/dj/params/list")
    TableDataInfo list(@RequestBody DjParams params);

    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/dj/params/{id}")
    AjaxResult getInfo(@PathVariable("id") Long id);

    /**
     * 修改垫胶参数信息
     *
     * @param params
     * @return
     */
    @PostMapping("/dj/params/edit")
    AjaxResult edit(@Validated @RequestBody DjParams params);

    /**
     * 删除垫胶参数信息
     *
     * @param ids
     * @return
     */
    @DeleteMapping("/dj/params/{ids}")
    AjaxResult remove(@RequestBody List<Long> ids);

    /**
     * 校验唯一性
     */
    @PostMapping("/dj/params/checkUnique")
    String checkUnique(@RequestBody DjParams params);

    /**
     * 根据参数编码查询垫胶参数信息
     */
    @PostMapping("/dj/params/getByParamCode")
    DjParams getByParamCode(@RequestBody DjParams entity);
}