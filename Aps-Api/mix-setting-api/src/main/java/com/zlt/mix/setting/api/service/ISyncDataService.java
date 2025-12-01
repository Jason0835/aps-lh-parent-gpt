package com.zlt.mix.setting.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.setting.api.domain.entity.AccessoriesMachine;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 硫磺辅料与机台对应Service接口
 *
 * @author Liam
 * @date 2022-04-18
 */
@FeignClient(contextId = "ISyncDataService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.syncData:mix-sync-data}")
public interface ISyncDataService {

    /**
     * 查询硫磺辅料与机台对应列表
     */
    @PostMapping("/request/mes/sync/syncBasMaterial")
    AjaxResult syncBasMaterial();

    /**
     * 查询硫磺辅料与机台对应列表
     */
    @PostMapping("/request/mes/sync/syncMesPmtRecipe")
    AjaxResult syncMesPmtRecipe();
}
