package com.zlt.aps.gsq.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.gsq.api.domain.entity.GsqQuotaSetting;

import java.util.List;


/**
 * 钢丝圈定额设定Service接口
 *
 * @author zlt
 * @date 2021-06-29
 */
public interface GsqQuotaSettingService {
    /**
     * 查询钢丝圈定额设定
     *
     * @param id 钢丝圈定额设定ID
     * @return 钢丝圈定额设定
     */
    public GsqQuotaSetting selectGsqQuotaSettingById(Long id);

    /**
     * 查询钢丝圈定额设定列表
     *
     * @param gsqQuotaSetting 钢丝圈定额设定
     * @return 钢丝圈定额设定集合
     */
    public List<GsqQuotaSetting> selectGsqQuotaSettingList(GsqQuotaSetting gsqQuotaSetting);

    /**
     * 新增钢丝圈定额设定
     *
     * @param gsqQuotaSetting 钢丝圈定额设定
     * @return 结果
     */
    public int insertGsqQuotaSetting(GsqQuotaSetting gsqQuotaSetting);

    /**
     * 修改钢丝圈定额设定
     *
     * @param gsqQuotaSetting 钢丝圈定额设定
     * @return 结果
     */
    public int updateGsqQuotaSetting(GsqQuotaSetting gsqQuotaSetting);

    /**
     * 批量删除钢丝圈定额设定
     *
     * @param ids 需要删除的钢丝圈定额设定ID
     * @return 结果
     */
    public int deleteGsqQuotaSettingByIds(Long[] ids);

    /**
     * 删除钢丝圈定额设定信息
     *
     * @param id 钢丝圈定额设定ID
     * @return 结果
     */
    public int deleteGsqQuotaSettingById(Long id);

    /**
     * 校验钢丝圈定额设定唯一性
     */
    public String checkGsqQuotaSettingUnique(GsqQuotaSetting gsqQuotaSetting);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<GsqQuotaSetting> list, boolean updateSupport, Long importLogId);
}
