package com.zlt.aps.cd90.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cd90.api.domain.dto.Cd90QuotaSettingDto;
import com.zlt.aps.cd90.entity.Cd90QuotaSetting;

import java.util.List;

/**
 * 90度裁断定额设定Mapper接口
 *
 * @author chen
 * @date 2021-06-29
 */
public interface Cd90QuotaSettingMapper extends BaseMapper<Cd90QuotaSetting> {

    /**
     * 查询90度裁断定额设定
     *
     * @param id 90度裁断定额设定ID
     * @return 90度裁断定额设定
     */
    public Cd90QuotaSetting selectQuotaSettingById(Long id);

    /**
     * 查询90度裁断定额设定列表
     *
     * @param quotaSetting 90度裁断定额设定
     * @return 90度裁断定额设定集合
     */
    public List<Cd90QuotaSettingDto> selectQuotaSettingList(Cd90QuotaSetting quotaSetting);

    /**
     * 校验定额设定记录唯一性
     *
     * @param quotaSetting 要校验的记录
     * @return 查询到的集合
     */
    public List<Cd90QuotaSetting> checkUnique(Cd90QuotaSetting quotaSetting);

    /**
     * 批量删除定额设定记录
     * @param ids id集合
     * @return 结果
     */
    public int deleteCd90QuotaSettingByIds(Long[] ids);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    public void mergeSql(List<Cd90QuotaSetting> list);
}
