package com.zlt.aps.tq.mapper;

import com.zlt.aps.tq.api.domain.entity.TqQuotaSetting;

import java.util.List;

/**
 * 胎圈定额设定Mapper接口
 *
 * @author zlt
 * @date 2021-06-29
 */
public interface TqQuotaSettingMapper {
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
     * 删除胎圈定额设定
     *
     * @param id 胎圈定额设定ID
     * @return 结果
     */
    public int deleteTqQuotaSettingById(Long id);

    /**
     * 批量删除胎圈定额设定
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteTqQuotaSettingByIds(Long[] ids);

    /**
     * 校验唯一性
     */
    public List<TqQuotaSetting> checkTqQuotaSettingUnique(TqQuotaSetting tqQuotaSetting);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     *
     * @param list 要合并的集合
     */
    public void mergeSql(List<TqQuotaSetting> list);
}
