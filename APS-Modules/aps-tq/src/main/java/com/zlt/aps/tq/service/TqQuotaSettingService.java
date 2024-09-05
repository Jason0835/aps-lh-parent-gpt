package com.zlt.aps.tq.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tq.api.domain.entity.TqQuotaSetting;

import java.util.List;


/**
 * 胎圈定额设定Service接口
 *
 * @author zlt
 * @date 2021-06-29
 */
public interface TqQuotaSettingService {
    /**
     * 查询胎圈定额设定
     *
     * @param id 胎圈定额设定ID
     * @return 胎圈定额设定
     */
    public TqQuotaSetting selectTqQuotaSettingById(Long id);

    /**
     * 查询胎圈定额设定列表
     *
     * @param tqQuotaSetting 胎圈定额设定
     * @return 胎圈定额设定集合
     */
    public List<TqQuotaSetting> selectTqQuotaSettingList(TqQuotaSetting tqQuotaSetting);

    /**
     * 新增胎圈定额设定
     *
     * @param tqQuotaSetting 胎圈定额设定
     * @return 结果
     */
    public int insertTqQuotaSetting(TqQuotaSetting tqQuotaSetting);

    /**
     * 修改胎圈定额设定
     *
     * @param tqQuotaSetting 胎圈定额设定
     * @return 结果
     */
    public int updateTqQuotaSetting(TqQuotaSetting tqQuotaSetting);

    /**
     * 批量删除胎圈定额设定
     *
     * @param ids 需要删除的胎圈定额设定ID
     * @return 结果
     */
    public int deleteTqQuotaSettingByIds(Long[] ids);

    /**
     * 删除胎圈定额设定信息
     *
     * @param id 胎圈定额设定ID
     * @return 结果
     */
    public int deleteTqQuotaSettingById(Long id);

    /**
     * 校验胎圈定额设定唯一性
     */
    public String checkTqQuotaSettingUnique(TqQuotaSetting tqQuotaSetting);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<TqQuotaSetting> list, boolean updateSupport, Long importLogId);
}
