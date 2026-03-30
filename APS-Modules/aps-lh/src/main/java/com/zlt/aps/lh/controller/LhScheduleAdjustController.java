package com.zlt.aps.lh.controller;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.lh.api.domain.entity.LhCxLinkageConfirm;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.service.LhScheduleAdjustService;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Api(tags = "硫化排程调整控制类")
@RestController
@RequestMapping("/lhScheduleAdjust")
public class LhScheduleAdjustController extends AbstractDocBizController<LhCxLinkageConfirm> {

    @Autowired
    private LhScheduleAdjustService lhScheduleAdjustService;
    /**
     * 查询硫化成型联动确认列表
     */
    @ApiOperation("查询硫化成型联动确认列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody LhCxLinkageConfirm entity) {
        try {
            startPage("adjust_batch_no desc");
            List<LhCxLinkageConfirm> list = lhScheduleAdjustService.selectList(entity);
            return getDataTable(list);
        } finally {
            this.clearPage();
        }
    }

    /**
     * 硫化成型联动确认
     */
    @PostMapping("/confirmAdjust")
    public AjaxResult confirmAdjust(@RequestBody LhCxLinkageConfirm entity) {
        return lhScheduleAdjustService.confirmAdjust(entity);
    }


    /**
     * 生成硫化排程调整信息
     */
    @ApiOperation(value = "生成硫化排程调整信息")
    @PostMapping("/generateLhScheduleAdjust")
    @Deprecated
    public AjaxResult generateLhScheduleAdjust(@RequestBody LhScheduleResult entity) {
        return lhScheduleAdjustService.generateLhScheduleAdjust(entity);
    }

    @Override
    protected String[] getQueryFormulas() {
        return new String[]{
                "createByName->getcolvalue(SYS_USER, nick_name, user_name, createBy)",
                "updateByName->getcolvalue(SYS_USER, nick_name, user_name, updateBy)",
        };
    }

    @Override
    protected IDocService getDocService() {
        return lhScheduleAdjustService;
    }

    @Override
    protected String getTypeCode() {
        return "LH2025416";
    }
}
