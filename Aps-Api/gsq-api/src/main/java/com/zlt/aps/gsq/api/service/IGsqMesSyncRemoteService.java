package com.zlt.aps.gsq.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.gsq.api.domain.entity.GsqDayFinishQty;
import com.zlt.aps.gsq.api.domain.entity.GsqScheFinishQty;
import com.zlt.aps.gsq.api.domain.entity.GsqStock;
import com.zlt.aps.gsq.api.domain.vo.GsqMesTwiningDiscSyncVO;
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

    /**
     * 逻辑删除并批量保存钢丝圈排程完成量（事务性操作）
     * 步骤1：逻辑删除指定分厂+排程日期的旧数据（IS_DELETE置为1）
     * 步骤2：批量插入MES最新排程完成量数据（新记录，IS_DELETE=0）
     *
     * @param factoryCode  分厂编号
     * @param scheduleDate 排程日期，格式：yyyy-MM-dd
     * @param updateBy     更新者
     * @param list         待插入的钢丝圈排程完成量列表
     * @return 结果
     */
    @ApiOperation("逻辑删除并批量保存钢丝圈排程完成量（事务性操作）")
    @PostMapping("/gsqMesSync/logicDeleteAndSaveScheFinishQty")
    AjaxResult logicDeleteAndSaveScheFinishQty(@RequestParam("factoryCode") String factoryCode,
                                                @RequestParam("scheduleDate") String scheduleDate,
                                                @RequestParam("updateBy") String updateBy,
                                                @RequestBody List<GsqScheFinishQty> list);

    /**
     * 钢丝圈排程完成量回写钢丝圈排程结果表各班次完成量
     * 根据完成量回报数据，按工厂+钢丝圈代码+工单号+排程日期汇总后，
     * 查询排程结果表（排程日期为D-1、D、D+1）并按6班制3天窗口班次映射关系回写完成量
     *
     * @param list 完成量回报数据列表
     * @return 回写结果
     */
    @ApiOperation("钢丝圈排程完成量回写钢丝圈排程结果表各班次完成量")
    @PostMapping("/gsqMesSync/writeBackScheduleResultFinishQty")
    AjaxResult writeBackScheduleResultFinishQty(@RequestBody List<GsqScheFinishQty> list);

    /**
     * 逻辑删除并批量保存钢丝圈库存（事务性操作）
     * 步骤1：逻辑删除指定库存日期的旧数据（IS_DELETE置为1）
     * 步骤2：批量插入MES最新钢丝圈库存数据（新记录，IS_DELETE=0）
     *
     * @param stockDate 库存日期，格式：yyyy-MM-dd
     * @param updateBy  更新者
     * @param list      待插入的钢丝圈库存列表
     * @return 结果
     */
    @ApiOperation("逻辑删除并批量保存钢丝圈库存（事务性操作）")
    @PostMapping("/gsqMesSync/logicDeleteAndSaveGsqStockByStockDate")
    AjaxResult logicDeleteAndSaveGsqStockByStockDate(@RequestParam("stockDate") String stockDate,
                                                      @RequestParam("updateBy") String updateBy,
                                                      @RequestBody List<GsqStock> list);

    /**
     * 同步MES钢丝圈缠绕盘三表数据（事务性操作）
     * 单事务处理缠绕盘清单/规格关系/机台关系，保证三表一致性：
     * 1. 主表UPSERT：存在则更新MES字段（保留名称/数量/备注等手工维护字段），不存在则新增；
     *    APS中MES来源但MES最新清单已不存在的缠绕盘逻辑删除并级联清理子表/机台关系
     * 2. 子表全量替换：MES规格关系涉及的缠绕盘，按主表ID逻辑删除旧子表后整体替换（钢丝圈名称反显）
     * 3. 机台关系UPSERT：同主表策略，MES来源已失效的组合逻辑删除
     *
     * @param updateBy 更新者（MES同步传"MES"）
     * @param syncVO   三表聚合数据
     * @return 结果
     */
    @ApiOperation("同步MES钢丝圈缠绕盘三表数据（事务性操作）")
    @PostMapping("/gsqMesSync/syncTwiningDisc")
    AjaxResult syncTwiningDisc(@RequestParam("updateBy") String updateBy,
                               @RequestBody GsqMesTwiningDiscSyncVO syncVO);
}
