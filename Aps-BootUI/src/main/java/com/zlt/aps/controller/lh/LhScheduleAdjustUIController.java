package com.zlt.aps.controller.lh;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.lh.api.domain.entity.LhCxLinkageConfirm;
import com.zlt.aps.lh.api.service.ILhScheduleAdjustRemoteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 硫化排程联动调整
 */
@Slf4j
@Api(tags = "硫化排程联动调整")
@Controller
@RequestMapping("/lh/lhScheduleAdjust")
public class LhScheduleAdjustUIController extends BaseUIController<LhCxLinkageConfirm> {

    @Autowired
    private ILhScheduleAdjustRemoteService iLhScheduleAdjustRemoteService;

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("lh:scheduleAdjust:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(LhCxLinkageConfirm entity) {
        return iLhScheduleAdjustRemoteService.list(entity);
    }

    /**
     * 硫化成型联动确认
     */
    @ApiOperation("硫化成型联动确认")
    @RequiresPermissions("lh:scheduleAdjust:confirmAdjust")
    @PostMapping("/confirmAdjust")
    @ResponseBody
    public AjaxResult confirmAdjust(LhCxLinkageConfirm entity) {
        return iLhScheduleAdjustRemoteService.confirmAdjust(entity);
    }
}
