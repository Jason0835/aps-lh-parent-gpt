package com.zlt.aps.tq.controller;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tq.api.domain.dto.TqPostponeConfirmDTO;
import com.zlt.aps.tq.api.domain.dto.TqPostponeRequestDTO;
import com.zlt.aps.tq.api.domain.vo.TqPostponePreviewVO;
import com.zlt.aps.tq.engine.vo.RollingUpdateResult;
import com.zlt.aps.tq.service.ITqPostponeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 胎圈排程跨班次推迟Controller
 *
 * <p>提供预览、确认、取消三个接口，支持前端先预览推迟效果再确认执行。</p>
 *
 * @author APS
 */
@Slf4j
@Api(tags = "胎圈排程跨班次推迟")
@RestController
@RequestMapping("/tqPostpone")
public class TqPostponeController {

    @Autowired
    private ITqPostponeService tqPostponeService;

    /**
     * 预览推迟效果
     *
     * <p>不持久化任何变更，仅计算并返回前后对比信息。</p>
     */
    @ApiOperation("预览跨班次推迟效果")
    @PostMapping("/preview")
    public AjaxResult preview(@Valid @RequestBody TqPostponeRequestDTO request) {
        TqPostponePreviewVO preview = tqPostponeService.previewPostpone(request);
        return AjaxResult.success(preview);
    }

    /**
     * 确认执行推迟
     *
     * <p>根据预览批次号执行推迟，持久化变更并记录日志明细。</p>
     */
    @ApiOperation("确认执行跨班次推迟")
    @PostMapping("/confirm")
    public AjaxResult confirm(@Valid @RequestBody TqPostponeConfirmDTO confirmDTO) {
        RollingUpdateResult result = tqPostponeService.confirmPostpone(confirmDTO);
        if (result.isSuccess()) {
            return AjaxResult.success("推迟执行成功", result);
        }
        return AjaxResult.error(result.getErrorMsg());
    }

    /**
     * 取消推迟
     *
     * <p>根据预览批次号清除缓存，不执行任何变更。</p>
     */
    @ApiOperation("取消跨班次推迟")
    @PostMapping("/cancel")
    public AjaxResult cancel(String previewBatchNo) {
        tqPostponeService.cancelPostpone(previewBatchNo);
        return AjaxResult.success("已取消推迟");
    }
}
