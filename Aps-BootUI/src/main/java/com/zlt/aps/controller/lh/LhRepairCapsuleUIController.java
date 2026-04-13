package com.zlt.aps.controller.lh;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.lh.api.service.ILhRepairCapsuleRemoteService;
import com.zlt.aps.lh.api.domain.entity.LhRepairCapsule;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Api(tags = "APS胶囊已使用次数")
@Controller
@RequestMapping("/lh/lhRepairCapsule")
public class LhRepairCapsuleUIController {

    @Autowired
    private ILhRepairCapsuleRemoteService iLhRepairCapsuleService;

    @ApiOperation("根据条件查询数据")
    @RequiresPermissions("lh:lhRepairCapsule:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(LhRepairCapsule query) {
        return iLhRepairCapsuleService.list(query);
    }

    @ApiOperation("获取详细信息")
    @RequiresPermissions("lh:lhRepairCapsule:list")
    @GetMapping("/getInfo/{id}")
    @ResponseBody
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(iLhRepairCapsuleService.getInfo(id));
    }
}
