package com.zlt.aps.cd15.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cd15.api.domain.dto.Cd15QuotaSettingDto;
import com.zlt.aps.cd15.entity.Cd15QuotaSetting;

import java.util.List;

/**
 * 15度裁断定额设定Mapper接口
 *
 * @author chen
 * @date 2021-06-28
 */
public interface Cd15QuotaSettingMapper extends BaseMapper<Cd15QuotaSetting> {
    /**
     * 查询15度裁断定额设定
     *
     * @param id 15度裁断定额设定ID
     * @return 15度裁断定额设定
     */
    public Cd15QuotaSetting selectCd15QuotaSettingById(Long id);

    /**
     * 查询15度裁断定额设定列表
     *
     * @param cd15QuotaSetting 15度裁断定额设定
     * @return 15度裁断定额设定集合
     */
    public List<Cd15QuotaSettingDto> selectQuotaSettingList(Cd15QuotaSetting cd15QuotaSetting);

    /**
     * 校验定额设定记录唯一性
     *
     * @param quotaSetting 要校验的记录
     * @return 查询到的集合
     */
    public List<Cd15QuotaSetting> checkUnique(Cd15QuotaSetting quotaSetting);

    /**
     * 批量删除定额设定记录
     * @param ids id集合
     * @return 结果
     */
    public int deleteCd15QuotaSettingByIds(Long[] ids);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     *
     * @param list 要合并的集合
     */
    public void mergeSql(List<Cd15QuotaSettingDto> list);
}
