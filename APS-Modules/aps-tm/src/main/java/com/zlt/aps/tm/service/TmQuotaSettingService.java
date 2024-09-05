package com.zlt.aps.tm.service;


import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tm.api.domain.entity.TmQuotaSetting;

import java.util.List;

/**
 * 胎面定额设定Service接口
 *
 * @author zlt
 * @date 2021-06-28
 */
public interface TmQuotaSettingService {
    /**
     * 查询胎面定额设定
     *
     * @param id 胎面定额设定ID
     * @return 胎面定额设定
     */
    public TmQuotaSetting selectTmQuotaSettingById(Long id);

    /**
     * 查询胎面定额设定列表
     *
     * @param tmQuotaSetting 胎面定额设定
     * @return 胎面定额设定集合
     */
    public List<TmQuotaSetting> selectTmQuotaSettingList(TmQuotaSetting tmQuotaSetting);

    /**
     * 新增胎面定额设定
     *
     * @param tmQuotaSetting 胎面定额设定
     * @return 结果
     */
    public int insertTmQuotaSetting(TmQuotaSetting tmQuotaSetting);

    /**
     * 修改胎面定额设定
     *
     * @param tmQuotaSetting 胎面定额设定
     * @return 结果
     */
    public int updateTmQuotaSetting(TmQuotaSetting tmQuotaSetting);

    /**
     * 批量删除胎面定额设定
     *
     * @param ids 需要删除的胎面定额设定ID
     * @return 结果
     */
    public int deleteTmQuotaSettingByIds(Long[] ids);

    /**
     * 删除胎面定额设定信息
     *
     * @param id 胎面定额设定ID
     * @return 结果
     */
    public int deleteTmQuotaSettingById(Long id);

    /**
     * 校验胎面定额设定唯一性
     *
     * @param tmQuotaSetting 胎面定额设定
     * @return 结果
     */
    public String checkTmQuotaSettingUnique(TmQuotaSetting tmQuotaSetting);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<TmQuotaSetting> list, boolean updateSupport, Long importLogId);
}
