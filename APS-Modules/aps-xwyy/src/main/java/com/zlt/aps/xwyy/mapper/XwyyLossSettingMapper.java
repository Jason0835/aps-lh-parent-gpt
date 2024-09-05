package com.zlt.aps.xwyy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.xwyy.api.domain.dto.XwyyLossSettingDto;
import com.zlt.aps.xwyy.entity.XwyyLossSetting;

import java.util.List;

/**
 * 纤维压延损耗率设定Mapper接口
 *
 * @author chen
 * @date 2021-07-19
 */
public interface XwyyLossSettingMapper extends BaseMapper<XwyyLossSetting> {
    /**
     * 查询纤维压延损耗率设定
     *
     * @param id 纤维压延损耗率设定ID
     * @return 纤维压延损耗率设定
     */
    public XwyyLossSettingDto selectXwyyLossSettingById(Long id);

    /**
     * 查询纤维压延损耗率设定列表
     *
     * @param xwyyLossSetting 纤维压延损耗率设定
     * @return 纤维压延损耗率设定集合
     */
    public List<XwyyLossSettingDto> selectXwyyLossSettingList(XwyyLossSetting xwyyLossSetting);

    /**
     * 新增纤维压延损耗率设定
     *
     * @param xwyyLossSetting 纤维压延损耗率设定
     * @return 结果
     */
    public int insertXwyyLossSetting(XwyyLossSetting xwyyLossSetting);

    /**
     * 修改纤维压延损耗率设定
     *
     * @param xwyyLossSetting 纤维压延损耗率设定
     * @return 结果
     */
    public int updateXwyyLossSetting(XwyyLossSetting xwyyLossSetting);

    /**
     * 删除纤维压延损耗率设定
     *
     * @param id 纤维压延损耗率设定ID
     * @return 结果
     */
    public int deleteXwyyLossSettingById(Long id);

    /**
     * 批量删除纤维压延损耗率设定
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteXwyyLossSettingByIds(Long[] ids);

    /**
     * 校验纤维压延损耗率设定记录唯一性
     *
     * @param xwyyLossSetting 要校验的记录
     * @return 查询到的记录条数
     */
    public int checkXwyyLossSettingUnique(XwyyLossSetting xwyyLossSetting);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    public void mergeSql(List<XwyyLossSetting> list);

    void deleteAll();
}
