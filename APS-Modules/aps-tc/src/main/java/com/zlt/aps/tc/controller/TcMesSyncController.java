package com.zlt.aps.tc.controller;

import cn.hutool.core.date.DateUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tc.api.domain.entity.TcDayFinishQty;
import com.zlt.aps.tc.api.domain.entity.TcScheFinishQty;
import com.zlt.aps.tc.api.domain.entity.TcStock;
import com.zlt.aps.tc.api.domain.vo.TcReleaseFeedbackVo;
import com.zlt.aps.tc.api.service.ITcMesSyncRemoteService;
import com.zlt.aps.tc.service.ITcDayFinishQtyService;
import com.zlt.aps.tc.service.ITcScheFinishQtyService;
import com.zlt.aps.tc.service.ITcStockService;
import com.zlt.aps.tc.service.TcReleaseFeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 胎侧MES数据同步远程接口实现。
 */
@RestController
@RequiredArgsConstructor
public class TcMesSyncController implements ITcMesSyncRemoteService {

    private final ITcStockService stockService;
    private final ITcScheFinishQtyService scheFinishQtyService;
    private final ITcDayFinishQtyService dayFinishQtyService;
    private final TcReleaseFeedbackService releaseFeedbackService;

    /**
     * 失效并保存库存快照。
     *
     * @param factoryCode 工厂编码
     * @param stockDate 库存日期
     * @param updateBy 更新人
     * @param stockList 库存列表
     * @return 保存结果
     */
    @Override
    public AjaxResult logicDeleteAndSaveStock(String factoryCode, String stockDate, String updateBy,
                                               List<TcStock> stockList) {
        this.stockService.logicDeleteAndSaveBatch(factoryCode, DateUtil.parseDate(stockDate), updateBy, stockList);
        return AjaxResult.success(I18nUtil.getMessage("ui.tc.schedule.mes.stockSyncSuccess"));
    }

    /**
     * 失效并保存班次完成量快照。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate MES业务日期
     * @param updateBy 更新人
     * @param finishQtyList 完成量列表
     * @return 保存结果
     */
    @Override
    public AjaxResult logicDeleteAndSaveScheFinishQty(String factoryCode, String scheduleDate, String updateBy,
                                                       List<TcScheFinishQty> finishQtyList) {
        this.scheFinishQtyService.logicDeleteAndSaveBatch(factoryCode, DateUtil.parseDate(scheduleDate),
                updateBy, finishQtyList);
        return AjaxResult.success(I18nUtil.getMessage("ui.tc.schedule.mes.finishSyncSuccess"));
    }

    /**
     * 回写六班结果完成量。
     *
     * @param finishQtyList 完成量列表
     * @return 回写摘要
     */
    @Override
    public AjaxResult writeBackScheduleResultFinishQty(List<TcScheFinishQty> finishQtyList) {
        return this.scheFinishQtyService.writeBackScheduleResultFinishQty(finishQtyList);
    }

    /**
     * 失效并保存日完成量快照。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate MES业务日期
     * @param updateBy 更新人
     * @param finishQtyList 日完成量列表
     * @return 保存结果
     */
    @Override
    public AjaxResult logicDeleteAndSaveDayFinishQty(String factoryCode, String scheduleDate, String updateBy,
                                                      List<TcDayFinishQty> finishQtyList) {
        this.dayFinishQtyService.logicDeleteAndSaveBatch(factoryCode, DateUtil.parseDate(scheduleDate),
                updateBy, finishQtyList);
        return AjaxResult.success(I18nUtil.getMessage("ui.tc.schedule.mes.dayFinishSyncSuccess"));
    }

    /**
     * 应用MES发布反馈。
     *
     * @param feedback 发布反馈
     * @return 反馈处理摘要
     */
    @Override
    public AjaxResult releaseFeedback(TcReleaseFeedbackVo feedback) {
        return this.releaseFeedbackService.applyFeedback(feedback);
    }
}
