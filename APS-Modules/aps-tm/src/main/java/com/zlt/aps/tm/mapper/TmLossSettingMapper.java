package com.zlt.aps.tm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.tm.api.domain.dto.TmLossSettingDto;
import com.zlt.aps.tm.entity.TmLossSetting;

import java.util.List;

/**
 * 胎面损耗率设定Mapper接口
 *
 * @author chen
 * @date 2021-07-12
 */
public interface TmLossSettingMapper extends BaseMapper<TmLossSetting> {
    /**
     * 查询胎面损耗率设定
     *
     * @param id 胎面损耗率设定ID
     * @return 胎面损耗率设定
     */
    public TmLossSettingDto selectTmLossSettingById(Long id);

    /**
     * 查询胎面损耗率设定列表
     *
     * @param tmLossSetting 胎面损耗率设定
     * @return 胎面损耗率设定集合
     */
    public List<TmLossSettingDto> selectTmLossSettingList(TmLossSetting tmLossSetting);

    /**
     * 新增胎面损耗率设定
     *
     * @param tmLossSetting 胎面损耗率设定
     * @return 结果
     */
    public int insertTmLossSetting(TmLossSetting tmLossSetting);

    /**
     * 修改胎面损耗率设定
     *
     * @param tmLossSetting 胎面损耗率设定
     * @return 结果
     */
    public int updateTmLossSetting(TmLossSetting tmLossSetting);

    /**
     * 删除胎面损耗率设定
     *
     * @param id 胎面损耗率设定ID
     * @return 结果
     */
    public int deleteTmLossSettingById(Long id);

    /**
     * 批量删除胎面损耗率设定
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteTmLossSettingByIds(Long[] ids);

    /**
     * 校验胎面损耗率设定记录唯一性
     *
     * @param tmLossSetting 要校验的记录
     * @return 查询到的记录条数
     */
    public int checkTmLossSettingUnique(TmLossSetting tmLossSetting);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     *
     * @param list 要合并的集合
     */
    public void mergeSql(List<TmLossSettingDto> list);

    void deleteAll();
}
