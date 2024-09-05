package com.zlt.aps.nc.mapper;

import com.zlt.aps.nc.api.domain.entity.NcQuotaSetting;

import java.util.List;

/**
 * 内衬定额设定Mapper接口
 *
 * @author zlt
 * @date 2021-06-29
 */
public interface NcQuotaSettingMapper {
    /**
     * 查询内衬定额设定
     *
     * @param id 内衬定额设定ID
     * @return 内衬定额设定
     */
    public NcQuotaSetting selectNcQuotaSettingById(Long id);

    /**
     * 查询内衬定额设定列表
     *
     * @param ncQuotaSetting 内衬定额设定
     * @return 内衬定额设定集合
     */
    public List<NcQuotaSetting> selectNcQuotaSettingList(NcQuotaSetting ncQuotaSetting);

    /**
     * 新增内衬定额设定
     *
     * @param ncQuotaSetting 内衬定额设定
     * @return 结果
     */
    public int insertNcQuotaSetting(NcQuotaSetting ncQuotaSetting);

    /**
     * 修改内衬定额设定
     *
     * @param ncQuotaSetting 内衬定额设定
     * @return 结果
     */
    public int updateNcQuotaSetting(NcQuotaSetting ncQuotaSetting);

    /**
     * 删除内衬定额设定
     *
     * @param id 内衬定额设定ID
     * @return 结果
     */
    public int deleteNcQuotaSettingById(Long id);

    /**
     * 批量删除内衬定额设定
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteNcQuotaSettingByIds(Long[] ids);

    /**
     * 校验唯一性
     */
    public List<NcQuotaSetting> checkNcQuotaSettingUnique(NcQuotaSetting ncQuotaSetting);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     *
     * @param list 要合并的集合
     */
    public void mergeSql(List<NcQuotaSetting> list);

}
