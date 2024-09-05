package com.zlt.aps.gdyy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.gdyy.api.domain.dto.GdyyQuotaSettingDto;
import com.zlt.aps.gdyy.entity.GdyyQuotaSetting;

import java.util.List;

/**
 * 钢带压延定额设定Mapper接口
 *
 * @author chen
 * @date 2021-06-30
 */
public interface GdyyQuotaSettingMapper extends BaseMapper<GdyyQuotaSetting> {

    /**
     * 查询钢带压延定额设定
     *
     * @param id 钢带压延定额设定ID
     * @return 钢带压延定额设定
     */
    public GdyyQuotaSetting selectQuotaSettingById(Long id);

    /**
     * 查询钢带压延定额设定列表
     *
     * @param quotaSetting 钢带压延定额设定
     * @return 钢带压延定额设定集合
     */
    public List<GdyyQuotaSettingDto> selectQuotaSettingList(GdyyQuotaSetting quotaSetting);

    /**
     * 校验定额设定记录唯一性
     *
     * @param quotaSetting 要校验的记录
     * @return 查询到的集合
     */
    public List<GdyyQuotaSetting> checkUnique(GdyyQuotaSetting quotaSetting);

    /**
     * 批量删除定额设定记录
     * @param ids id集合
     * @return 结果
     */
    public int deleteQuotaSettingByIds(Long[] ids);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     *
     * @param list 要合并的集合
     */
    public void mergeSql(List<GdyyQuotaSettingDto> list);
}
