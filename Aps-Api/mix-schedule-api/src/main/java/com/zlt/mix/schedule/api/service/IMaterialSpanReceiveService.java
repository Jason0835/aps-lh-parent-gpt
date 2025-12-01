package com.zlt.mix.schedule.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.zlt.mix.schedule.api.domain.entity.GlueSpanReceive;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import io.swagger.annotations.ApiOperation;

import com.zlt.mix.schedule.api.domain.entity.MaterialSpanReceive;

/**
 * 硫磺辅料跨区接收Service接口
 * @author cxy
 * @date 2022-08-30
 */
@FeignClient(contextId = "IMaterialSpanReceiveService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.schedule:mixSchedule}")
public interface IMaterialSpanReceiveService {

    /**
     * 根据排程日期、被委托密炼区查询未被接收的跨区请求总数
     */
    @ApiOperation("根据排程日期、被委托密炼区查询未被接收的跨区请求总数")
    @PostMapping("/materialSpanReceive/selectUnReceiveCount")
    public Integer selectUnReceiveCount(@RequestBody MaterialSpanReceive materialSpanReceive);


    /**
     * 根据id查询跨区接收信息
     * @param entity id
     * @return 查询到的记录
     */
    @PostMapping("/materialSpanReceive/getMaterialSpanReceiveInfo")
    MaterialSpanReceive getMaterialSpanReceiveInfo(@RequestBody MaterialSpanReceive entity);
}
