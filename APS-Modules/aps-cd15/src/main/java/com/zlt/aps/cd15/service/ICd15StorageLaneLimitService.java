package com.zlt.aps.cd15.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cd15.api.domain.entity.Cd15StorageLaneLimit;
import com.zlt.bill.common.service.IDocService;

import java.util.List;
import java.util.Date;

/**
 * 斜裁库排限制 Service。
 */
public interface ICd15StorageLaneLimitService extends IDocService<Cd15StorageLaneLimit> {

    /**
     * 校验同工厂、日期、班次、库排号唯一性。
     *
     * @param entity 斜裁库排限制
     * @return 唯一性结果
     */
    String checkUnique(Cd15StorageLaneLimit entity);

    /**
     * 校验业务规则。
     *
     * @param entity 斜裁库排限制
     * @return 错误国际化 key；为空表示校验通过
     */
    String validateBusiness(Cd15StorageLaneLimit entity);

    /**
     * 导入斜裁库排限制。
     *
     * @param list          导入数据
     * @param updateSupport 是否更新已有数据
     * @param importLogId   导入日志 ID
     * @return 导入结果
     */
    AjaxResult importData(List<Cd15StorageLaneLimit> list, boolean updateSupport, Long importLogId);

    /** 替换指定工厂、日期和班次的MES快照。 */
    void logicDeleteAndSaveBatch(String factoryCode, Date laneDate, String shiftCode,
                                 String updateBy, List<Cd15StorageLaneLimit> list);
}
