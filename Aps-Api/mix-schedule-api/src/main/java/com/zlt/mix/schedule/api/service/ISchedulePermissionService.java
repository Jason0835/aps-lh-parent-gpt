package com.zlt.mix.schedule.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

/**
 * 用户资源权限Service接口
 *
 * @author Liam
 * @date 2022-07-12
 */
@FeignClient(contextId = "IPermissionService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.schedule:mixSchedule}")
public interface ISchedulePermissionService {
    /**
     * 获取有权限的密炼区code
     *
     * @return 密炼区code列表
     */
    @PostMapping("/permission/haveMixAreaPermission")
    List<String> haveMixAreaPermission();

}
