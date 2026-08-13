package com.zlt.aps.cd90.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90StorageLaneLimit;
import com.zlt.bill.common.service.IDocService;

import java.util.List;
import java.util.Date;

public interface ICd90StorageLaneLimitService extends IDocService<Cd90StorageLaneLimit> {
    String checkUnique(Cd90StorageLaneLimit entity);

    AjaxResult importData(List<Cd90StorageLaneLimit> list, boolean updateSupport, Long importLogId);

    /**
     * 按工厂、日期和班次全量覆盖库排状态。
     *
     * @param factoryCode 工厂编码
     * @param laneDate 库排日期
     * @param shiftCode 班次编码
     * @param updateBy 更新人
     * @param list 库排状态
     */
    void logicDeleteAndSaveBatch(String factoryCode, Date laneDate, String shiftCode,
                                 String updateBy, List<Cd90StorageLaneLimit> list);
}
