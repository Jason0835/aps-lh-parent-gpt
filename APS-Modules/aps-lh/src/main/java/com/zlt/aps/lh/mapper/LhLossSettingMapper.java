package com.zlt.aps.lh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.lh.api.domain.dto.LhLossSettingDto;
import com.zlt.aps.lh.entity.LhLossSetting;

import java.util.List;

/**
 * 硫化损耗率设定Mapper接口
 *
 * @author chen
 * @date 2021-07-19
 */
public interface LhLossSettingMapper extends BaseMapper<LhLossSetting> {
    /**
     * 查询硫化损耗率设定
     *
     * @param id 硫化损耗率设定ID
     * @return 硫化损耗率设定
     */
    public LhLossSettingDto selectLhLossSettingById(Long id);

    /**
     * 查询硫化损耗率设定列表
     *
     * @param lhLossSetting 硫化损耗率设定
     * @return 硫化损耗率设定集合
     */
    public List<LhLossSettingDto> selectLhLossSettingList(LhLossSetting lhLossSetting);

    /**
     * 新增硫化损耗率设定
     *
     * @param lhLossSetting 硫化损耗率设定
     * @return 结果
     */
    public int insertLhLossSetting(LhLossSetting lhLossSetting);

    /**
     * 修改硫化损耗率设定
     *
     * @param lhLossSetting 硫化损耗率设定
     * @return 结果
     */
    public int updateLhLossSetting(LhLossSetting lhLossSetting);

    /**
     * 删除硫化损耗率设定
     *
     * @param id 硫化损耗率设定ID
     * @return 结果
     */
    public int deleteLhLossSettingById(Long id);

    /**
     * 批量删除硫化损耗率设定
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteLhLossSettingByIds(Long[] ids);

    /**
     * 校验硫化损耗率设定记录唯一性
     *
     * @param lhLossSetting 要校验的记录
     * @return 查询到的记录条数
     */
    public int checkLhLossSettingUnique(LhLossSetting lhLossSetting);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    public void mergeSql(List<LhLossSetting> list);

}
