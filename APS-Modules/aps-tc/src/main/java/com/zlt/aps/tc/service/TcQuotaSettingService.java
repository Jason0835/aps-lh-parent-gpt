package com.zlt.aps.tc.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tc.api.domain.entity.TcQuotaSetting;

import java.util.List;


/**
 * 胎侧定额设定Service接口
 *
 * @author zlt
 * @date 2021-06-28
 */
public interface TcQuotaSettingService {
    /**
     * 查询胎侧定额设定
     *
     * @param id 胎侧定额设定ID
     * @return 胎侧定额设定
     */
    public TcQuotaSetting selectTcQuotaSettingById(Long id);

    /**
     * 查询胎侧定额设定列表
     *
     * @param tcQuotaSetting 胎侧定额设定
     * @return 胎侧定额设定集合
     */
    public List<TcQuotaSetting> selectTcQuotaSettingList(TcQuotaSetting tcQuotaSetting);

    /**
     * 新增胎侧定额设定
     *
     * @param tcQuotaSetting 胎侧定额设定
     * @return 结果
     */
    public int insertTcQuotaSetting(TcQuotaSetting tcQuotaSetting);

    /**
     * 修改胎侧定额设定
     *
     * @param tcQuotaSetting 胎侧定额设定
     * @return 结果
     */
    public int updateTcQuotaSetting(TcQuotaSetting tcQuotaSetting);

    /**
     * 批量删除胎侧定额设定
     *
     * @param ids 需要删除的胎侧定额设定ID
     * @return 结果
     */
    public int deleteTcQuotaSettingByIds(Long[] ids);

    /**
     * 删除胎侧定额设定信息
     *
     * @param id 胎侧定额设定ID
     * @return 结果
     */
    public int deleteTcQuotaSettingById(Long id);

    /**
     * 校验胎侧定额设定唯一性
     */
    public String checkTcQuotaSettingUnique(TcQuotaSetting tcQuotaSetting);

    /**
     * 导入数据
     */
    public AjaxResult importData(List<TcQuotaSetting> list, boolean updateSupport, Long importLogId);
}
