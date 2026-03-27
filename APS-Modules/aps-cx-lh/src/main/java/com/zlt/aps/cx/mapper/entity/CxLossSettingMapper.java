package com.zlt.aps.cx.mapper.entity;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cxlh.cx.api.domain.dto.CxLossSettingDto;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxLossSetting;


import java.util.List;

/**
 * 成型损耗率设定Mapper接口
 *
 * @author chen
 * @date 2021-07-19
 */
public interface CxLossSettingMapper extends BaseMapper<CxLossSetting> {
    /**
     * 查询成型损耗率设定
     *
     * @param id 成型损耗率设定ID
     * @return 成型损耗率设定
     */
    public CxLossSettingDto selectCxLossSettingById(Long id);

    /**
     * 查询成型损耗率设定列表
     *
     * @param cxLossSetting 成型损耗率设定
     * @return 成型损耗率设定集合
     */
    public List<CxLossSettingDto> selectCxLossSettingList(CxLossSetting cxLossSetting);

    /**
     * 新增成型损耗率设定
     *
     * @param cxLossSetting 成型损耗率设定
     * @return 结果
     */
    public int insertCxLossSetting(CxLossSetting cxLossSetting);

    /**
     * 修改成型损耗率设定
     *
     * @param cxLossSetting 成型损耗率设定
     * @return 结果
     */
    public int updateCxLossSetting(CxLossSetting cxLossSetting);

    /**
     * 删除成型损耗率设定
     *
     * @param id 成型损耗率设定ID
     * @return 结果
     */
    public int deleteCxLossSettingById(Long id);

    /**
     * 批量删除成型损耗率设定
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteCxLossSettingByIds(Long[] ids);

    /**
     * 校验成型损耗率设定记录唯一性
     *
     * @param cxLossSetting 要校验的记录
     * @return 查询到的记录条数
     */
    public int checkCxLossSettingUnique(CxLossSetting cxLossSetting);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    public void mergeSql(List<CxLossSetting> list);

}
