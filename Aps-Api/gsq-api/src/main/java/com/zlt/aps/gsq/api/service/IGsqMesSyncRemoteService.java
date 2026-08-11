package com.zlt.aps.gsq.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.gsq.api.domain.entity.GsqDayFinishQty;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 钢丝圈MES同步远程服务接口
 *
 * <p>供 aps-itf 模块通过 Gateway + Feign 调用钢丝圈微服务的内部接口，
 * 与 {@code ITqMesSyncRemoteService} 对齐。</p>
 *
 * @author APS Team
 * @since 2026/08/11
 */
@FeignClient(contextId = "IGsqMesSyncRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.gsq:gsq}")
public interface IGsqMesSyncRemoteService {

    /**
     * 逻辑删除并批量保存钢丝圈排程日完成量（事务性操作）
     * 步骤1：逻辑删除指定分厂+排程日期的旧数据（IS_DELETE置为1）
     * 步骤2：批量插入MES最新排程日完成量数据（新记录，IS_DELETE=0）
     *
     * @param factoryCode  分厂编号
     * @param scheduleDate 排程日期，格式：yyyy-MM-dd
     * @param updateBy     更新者
     * @param list         待插入的钢丝圈排程日完成量列表
     * @return 结果
     */
    @ApiOperation("逻辑删除并批量保存钢丝圈排程日完成量（事务性操作）")
    @PostMapping("/gsqMesSync/logicDeleteAndSaveDayFinishQty")
    AjaxResult logicDeleteAndSaveDayFinishQty(@RequestParam("factoryCode") String factoryCode,
                                               @RequestParam("scheduleDate") String scheduleDate,
                                               @RequestParam("updateBy") String updateBy,
                                               @RequestBody List<GsqDayFinishQty> list);
}
