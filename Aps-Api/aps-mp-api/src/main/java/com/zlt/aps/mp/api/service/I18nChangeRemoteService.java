package com.zlt.aps.mp.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.entity.I18nChange;
import com.zlt.aps.mp.api.domain.vo.I18nJsonVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 国际化变更RemoteService
 */
@FeignClient(contextId = "I18nChangeRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface I18nChangeRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/i18nChange/list")
    TableDataInfo list(@RequestBody I18nChange query);

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/i18nChange/selectRelList")
    List<I18nChange> selectRelList(@RequestBody I18nChange query);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/i18nChange/{id}")
    I18nChange getInfo(@PathVariable("id") Long id);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/i18nChange/save")
    AjaxResult save(@RequestBody I18nChange i18nChange);


    /**
     * 查询页面JSON
     */
    @ApiOperation("查询页面JSON")
    @PostMapping("/i18nChange/pageJson")
    AjaxResult pageJson(@RequestBody I18nJsonVo jsonVo);

    /**
     * 下载国际化
     */
    @GetMapping("/i18nChange/download")
    @ApiOperation("下载国际化")
    byte[] download();
}
