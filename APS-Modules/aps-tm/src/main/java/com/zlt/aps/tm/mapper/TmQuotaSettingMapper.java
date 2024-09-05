package com.zlt.aps.tm.mapper;

import com.zlt.aps.tm.api.domain.entity.TmQuotaSetting;

import java.util.List;

/**
 * 胎面定额设定Mapper接口
 *
 * @author zlt
 * @date 2021-06-28
 */
public interface TmQuotaSettingMapper {
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
     * 删除胎面定额设定
     *
     * @param id 胎面定额设定ID
     * @return 结果
     */
    public int deleteTmQuotaSettingById(Long id);

    /**
     * 批量删除胎面定额设定
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteTmQuotaSettingByIds(Long[] ids);

    /**
     * 校验胎面定额设定唯一性
     *
     * @param tmQuotaSetting 胎面定额设定
     * @return 结果
     */
    public List<TmQuotaSetting> checkTmQuotaSettingUnique(TmQuotaSetting tmQuotaSetting);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     *
     * @param list 要合并的集合
     */
    public void mergeSql(List<TmQuotaSetting> list);

    /**
     * 根据机台id和帘布大卷编号查询记录
     *
     * @param quotaSetting 要查询的记录
     * @return 结果
     */
    public TmQuotaSetting selectByCodeAndMachineId(TmQuotaSetting quotaSetting);

    /**
     * 批量插入记录
     *
     * @param list 要插入的记录
     */
    void insertList(List<TmQuotaSetting> list);
}
