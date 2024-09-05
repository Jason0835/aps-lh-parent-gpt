package com.zlt.aps.nc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.nc.api.domain.dto.NcLossSettingDto;
import com.zlt.aps.nc.entity.NcLossSetting;

import java.util.List;

/**
 * 内衬损耗率设定Mapper接口
 *
 * @author chen
 * @date 2021-07-13
 */
public interface NcLossSettingMapper extends BaseMapper<NcLossSetting> {
    /**
     * 查询内衬损耗率设定
     *
     * @param id 内衬损耗率设定ID
     * @return 内衬损耗率设定
     */
    public NcLossSettingDto selectNcLossSettingById(Long id);

    /**
     * 查询内衬损耗率设定列表
     *
     * @param ncLossSetting 内衬损耗率设定
     * @return 内衬损耗率设定集合
     */
    public List<NcLossSettingDto> selectNcLossSettingList(NcLossSetting ncLossSetting);

    /**
     * 新增内衬损耗率设定
     *
     * @param ncLossSetting 内衬损耗率设定
     * @return 结果
     */
    public int insertNcLossSetting(NcLossSetting ncLossSetting);

    /**
     * 修改内衬损耗率设定
     *
     * @param ncLossSetting 内衬损耗率设定
     * @return 结果
     */
    public int updateNcLossSetting(NcLossSetting ncLossSetting);

    /**
     * 删除内衬损耗率设定
     *
     * @param id 内衬损耗率设定ID
     * @return 结果
     */
    public int deleteNcLossSettingById(Long id);

    /**
     * 批量删除内衬损耗率设定
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteNcLossSettingByIds(Long[] ids);

    /**
     * 校验内衬损耗率设定记录唯一性
     *
     * @param ncLossSetting 要校验的记录
     * @return 查询到的记录条数
     */
    public int checkNcLossSettingUnique(NcLossSetting ncLossSetting);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     *
     * @param list 要合并的集合
     */
    public void mergeSql(List<NcLossSettingDto> list);

    void deleteAll();
}
