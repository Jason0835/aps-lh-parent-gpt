package com.zlt.aps.cx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cx.api.domain.dto.CxQuotaSettingDto;
import com.zlt.aps.cx.entity.CxQuotaSetting;

import java.util.List;

/**
 * 成型定额设定Mapper接口
 *
 * @author chen
 * @date 2021-06-16
 */
public interface CxQuotaSettingMapper extends BaseMapper<CxQuotaSetting> {
    /**
     * 查询成型定额设定列表
     *
     * @param cxQuotaSetting 成型定额设定
     * @return 成型定额设定集合
     */
    public List<CxQuotaSettingDto> selectCxQuotaSettingList(CxQuotaSetting cxQuotaSetting);

    /**
     * 校验记录唯一性
     *
     * @param quotaSetting 要校验的记录
     * @return 查询到的结果
     */
    public List<CxQuotaSettingDto> checkUnique(CxQuotaSetting quotaSetting);

    /**
     * 批量删除定额设定记录
     * @param ids id集合
     * @return 结果
     */
    public int deleteQuotaSettingByIds(Long[] ids);
}
