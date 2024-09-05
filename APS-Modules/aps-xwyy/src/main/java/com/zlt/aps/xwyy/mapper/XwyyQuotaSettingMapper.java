package com.zlt.aps.xwyy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.xwyy.api.domain.dto.XwyyQuotaSettingDto;
import com.zlt.aps.xwyy.entity.XwyyQuotaSetting;

import java.util.List;

/**
 * 纤维压延定额设定Mapper接口
 *
 * @author chen
 * @date 2021-06-29
 */
public interface XwyyQuotaSettingMapper extends BaseMapper<XwyyQuotaSetting> {
    /**
     * 查询纤维压延定额设定
     *
     * @param id 纤维压延定额设定ID
     * @return 纤维压延定额设定
     */
    public XwyyQuotaSetting selectQuotaSettingById(Long id);

    /**
     * 查询纤维压延定额设定列表
     *
     * @param quotaSetting 纤维压延定额设定
     * @return 纤维压延定额设定集合
     */
    public List<XwyyQuotaSettingDto> selectQuotaSettingList(XwyyQuotaSetting quotaSetting);

    /**
     * 校验定额设定记录唯一性
     *
     * @param quotaSetting 要校验的记录
     * @return 查询到的集合
     */
    public List<XwyyQuotaSetting> checkUnique(XwyyQuotaSetting quotaSetting);

    /**
     * 根据集合查询记录是否已存在
     *
     * @param list 要查询的记录集合
     * @return 已存在的记录
     */
    public List<XwyyQuotaSettingDto> selectExistByList(List<XwyyQuotaSettingDto> list);

    /**
     * 批量删除定额设定记录
     * @param ids id集合
     * @return 结果
     */
    public int deleteQuotaSettingByIds(Long[] ids);

    /**
     * 根据机台id和帘布大卷编号查询记录
     *
     * @param dto 要查询的记录
     * @return 结果
     */
    public XwyyQuotaSettingDto selectByBigRollCodeAndMachineId(XwyyQuotaSettingDto dto);

    /**
     * 批量插入记录
     *
     * @param list 要插入的记录
     */
    void insertList(List<XwyyQuotaSettingDto> list);

    /**
     * 根据id批量删除记录（物理删除）
     *
     * @param list 要删除的记录
     */
    void deleteById(List<XwyyQuotaSettingDto> list);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     *
     * @param list 要合并的集合
     */
    public void mergeSql(List<XwyyQuotaSetting> list);
}
