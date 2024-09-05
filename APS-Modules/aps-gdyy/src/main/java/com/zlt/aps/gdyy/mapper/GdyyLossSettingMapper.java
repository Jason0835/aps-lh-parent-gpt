package com.zlt.aps.gdyy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.gdyy.api.domain.dto.GdyyLossSettingDto;
import com.zlt.aps.gdyy.entity.GdyyLossSetting;

import java.util.List;

/**
 * 钢带压延损耗率设定Mapper接口
 *
 * @author chen
 * @date 2021-07-19
 */
public interface GdyyLossSettingMapper extends BaseMapper<GdyyLossSetting> {
    /**
     * 查询钢带压延损耗率设定
     *
     * @param id 钢带压延损耗率设定ID
     * @return 钢带压延损耗率设定
     */
    public GdyyLossSettingDto selectGdyyLossSettingById(Long id);

    /**
     * 查询钢带压延损耗率设定列表
     *
     * @param gdyyLossSetting 钢带压延损耗率设定
     * @return 钢带压延损耗率设定集合
     */
    public List<GdyyLossSettingDto> selectGdyyLossSettingList(GdyyLossSetting gdyyLossSetting);

    /**
     * 新增钢带压延损耗率设定
     *
     * @param gdyyLossSetting 钢带压延损耗率设定
     * @return 结果
     */
    public int insertGdyyLossSetting(GdyyLossSetting gdyyLossSetting);

    /**
     * 修改钢带压延损耗率设定
     *
     * @param gdyyLossSetting 钢带压延损耗率设定
     * @return 结果
     */
    public int updateGdyyLossSetting(GdyyLossSetting gdyyLossSetting);

    /**
     * 删除钢带压延损耗率设定
     *
     * @param id 钢带压延损耗率设定ID
     * @return 结果
     */
    public int deleteGdyyLossSettingById(Long id);

    /**
     * 批量删除钢带压延损耗率设定
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteGdyyLossSettingByIds(Long[] ids);

    /**
     * 校验钢带压延损耗率设定记录唯一性
     *
     * @param gdyyLossSetting 要校验的记录
     * @return 查询到的记录条数
     */
    public int checkGdyyLossSettingUnique(GdyyLossSetting gdyyLossSetting);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     *
     * @param list 要合并的集合
     */
    public void mergeSql(List<GdyyLossSettingDto> list);

    void deleteAll();
}
