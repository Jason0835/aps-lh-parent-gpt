package com.zlt.aps.gsq.mapper;

import com.zlt.aps.gsq.api.domain.entity.GsqQuotaSetting;

import java.util.List;

/**
 * 钢丝圈定额设定Mapper接口
 *
 * @author zlt
 * @date 2021-06-29
 */
public interface GsqQuotaSettingMapper {
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
     * 删除钢丝圈定额设定
     *
     * @param id 钢丝圈定额设定ID
     * @return 结果
     */
    public int deleteGsqQuotaSettingById(Long id);

    /**
     * 批量删除钢丝圈定额设定
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteGsqQuotaSettingByIds(Long[] ids);

    /**
     * 校验唯一性
     */
    public List<GsqQuotaSetting> checkGsqQuotaSettingUnique(GsqQuotaSetting gsqQuotaSetting);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     *
     * @param list 要合并的集合
     */
    public void mergeSql(List<GsqQuotaSetting> list);
}
