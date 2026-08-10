package com.zlt.aps.cd90.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheFinishQty;
import com.zlt.bill.common.service.IDocService;

import java.util.Date;
import java.util.List;

/**
 * 直裁排程每日完成量服务。
 */
public interface ICd90ScheFinishQtyService extends IDocService<Cd90ScheFinishQty> {

    /**
     * 替换指定工厂、归属日期的每日完成量。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate MES完成量归属日期
     * @param updateBy 更新人
     * @param finishQtyList 每日完成量列表
     */
    void logicDeleteAndSaveBatch(String factoryCode, Date scheduleDate, String updateBy,
                                 List<Cd90ScheFinishQty> finishQtyList);

    /**
     * 将每日三班完成量动态映射到直裁排程结果班次。
     *
     * @param finishQtyList 每日完成量列表
     * @return 回写结果
     */
    AjaxResult writeBackScheduleResultFinishQty(List<Cd90ScheFinishQty> finishQtyList);
}
