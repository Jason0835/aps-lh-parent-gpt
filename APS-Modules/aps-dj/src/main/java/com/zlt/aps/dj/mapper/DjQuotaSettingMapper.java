package com.zlt.aps.dj.mapper;

import java.util.List;

import com.zlt.aps.dj.api.domain.entity.DjQuotaSetting;

/**
 * 垫胶定额设定Mapper接口
 *
 * @author zlt
 * @date 2021-06-29
 */
public interface DjQuotaSettingMapper {
    /**
     * 查询垫胶定额设定
     *
     * @param id 垫胶定额设定ID
     * @return 垫胶定额设定
     */
    public DjQuotaSetting selectNcQuotaSettingById(Long id);

    /**
     * 查询垫胶定额设定列表
     *
     * @param ncQuotaSetting 垫胶定额设定
     * @return 垫胶定额设定集合
     */
    public List<DjQuotaSetting> selectNcQuotaSettingList(DjQuotaSetting ncQuotaSetting);

    /**
     * 新增垫胶定额设定
     *
     * @param ncQuotaSetting 垫胶定额设定
     * @return 结果
     */
    public int insertNcQuotaSetting(DjQuotaSetting ncQuotaSetting);

    /**
     * 修改垫胶定额设定
     *
     * @param ncQuotaSetting 垫胶定额设定
     * @return 结果
     */
    public int updateNcQuotaSetting(DjQuotaSetting ncQuotaSetting);

    /**
     * 删除垫胶定额设定
     *
     * @param id 垫胶定额设定ID
     * @return 结果
     */
    public int deleteNcQuotaSettingById(Long id);

    /**
     * 批量删除垫胶定额设定
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteNcQuotaSettingByIds(Long[] ids);

    /**
     * 校验唯一性
     */
    public List<DjQuotaSetting> checkNcQuotaSettingUnique(DjQuotaSetting ncQuotaSetting);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     *
     * @param list 要合并的集合
     */
    public void mergeSql(List<DjQuotaSetting> list);

}
