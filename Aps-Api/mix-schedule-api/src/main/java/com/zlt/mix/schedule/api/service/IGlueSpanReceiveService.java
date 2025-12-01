package com.zlt.mix.schedule.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import io.swagger.annotations.ApiOperation;

import com.zlt.mix.schedule.api.domain.entity.GlueSpanReceive;

/**
 * 胶料跨区接收Service接口
 * @author chen
 * @date 2022-08-16
 */
@FeignClient(contextId = "IGlueSpanReceiveService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.schedule:mixSchedule}")
public interface IGlueSpanReceiveService {

    /**
     * 根据排程日期、被委托密炼区查询未被接收的跨区请求总数
     */
    @ApiOperation("根据排程日期、被委托密炼区查询未被接收的跨区请求总数")
    @PostMapping("/glueSpanReceive/selectUnReceiveCount")
    public Integer selectUnReceiveCount(@RequestBody GlueSpanReceive glueSpanReceive);


    /**
     * 根据id查询跨区接收信息
     * @param entity id
     * @return 查询到的记录
     */
    @PostMapping("/glueSpanReceive/getGlueSpanReceiveInfo")
    GlueSpanReceive getGlueSpanReceiveInfo(@RequestBody GlueSpanReceive entity);
}
