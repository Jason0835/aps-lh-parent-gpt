package com.zlt.aps.itf.scm.service;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.itf.scm.vo.SyncOutFacScheduleVersionVo;
import com.zlt.aps.itf.scm.vo.SyncPlanedNotShipParamVo;

import io.swagger.annotations.ApiOperation;

/**
 * 分厂月度计划控制台业务
 *
 * @author ZLT
 * @date 20250213
 */
@FeignClient(contextId = "IScmItfService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.itf:/itf}")
public interface IScmItfService {
    /**
     * 同步已计划未发货数据
     *
     * @param planedNotShipParamVo 查询条件
     * @return 结果集合
     */
    @ApiOperation("同步已计划未发货数据")
    @PostMapping("/scm/syncPlanedNotShipList")
    AjaxResult syncPlanedNotShipList(@RequestBody SyncPlanedNotShipParamVo planedNotShipParamVo);
    
    /**
     * 发货明细表同步接口
     *
     * @param planedNotShipParamVo 查询条件
     * @return 结果集合
     */
    @ApiOperation("发货明细表同步接口")
    @PostMapping("/scm/syncOutShipDmdOrdList")
    AjaxResult syncOutShipDmdOrdList(@RequestBody SyncPlanedNotShipParamVo syncOutShipDmdOrdVo);

    /**
     * 月计划排程结果推送
     *
     * @param outFacScheduleVersionVo 
     * @return 结果集合
     */
    @ApiOperation("月计划排程结果推送")
    @PostMapping("/scm/publicFacScheduleVersion")
    AjaxResult publicFacScheduleVersion(@RequestBody List<SyncOutFacScheduleVersionVo> outFacScheduleVersionList);
}
