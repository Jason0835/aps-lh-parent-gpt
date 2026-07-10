package com.zlt.aps.nc.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.nc.api.domain.entity.NcQuotaSetting;
import com.zlt.bill.common.service.IDocService;

import java.util.List;


/**
 * 内衬定额设定Service接口
 *
 * @author zlt
 * @date 2021-06-29
 */
public interface NcQuotaSettingService extends IDocService<NcQuotaSetting> {
    /**
     * 校验内衬定额设定唯一性
     */
    public String checkNcQuotaSettingUnique(NcQuotaSetting ncQuotaSetting);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<NcQuotaSetting> list, boolean updateSupport, Long importLogId);
}
