package com.zlt.aps.dj.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.dj.api.domain.dto.DjLossSettingDto;
import com.zlt.aps.dj.api.domain.entity.DjLossSetting;

/**
 * 垫胶损耗率设定Mapper接口
 *
 * @author chen
 * @date 2021-07-13
 */
public interface DjLossSettingMapper extends BaseMapper<DjLossSetting> {
    /**
     * 查询垫胶损耗率设定
     *
     * @param id 垫胶损耗率设定ID
     * @return 垫胶损耗率设定
     */
    public DjLossSettingDto selectNcLossSettingById(Long id);

    /**
     * 查询垫胶损耗率设定列表
     *
     * @param ncLossSetting 垫胶损耗率设定
     * @return 垫胶损耗率设定集合
     */
    public List<DjLossSettingDto> selectNcLossSettingList(DjLossSetting ncLossSetting);

    /**
     * 新增垫胶损耗率设定
     *
     * @param ncLossSetting 垫胶损耗率设定
     * @return 结果
     */
    public int insertNcLossSetting(DjLossSetting ncLossSetting);

    /**
     * 修改垫胶损耗率设定
     *
     * @param ncLossSetting 垫胶损耗率设定
     * @return 结果
     */
    public int updateNcLossSetting(DjLossSetting ncLossSetting);

    /**
     * 删除垫胶损耗率设定
     *
     * @param id 垫胶损耗率设定ID
     * @return 结果
     */
    public int deleteNcLossSettingById(Long id);

    /**
     * 批量删除垫胶损耗率设定
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteNcLossSettingByIds(Long[] ids);

    /**
     * 校验垫胶损耗率设定记录唯一性
     *
     * @param ncLossSetting 要校验的记录
     * @return 查询到的记录条数
     */
    public int checkNcLossSettingUnique(DjLossSetting ncLossSetting);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     *
     * @param list 要合并的集合
     */
    public void mergeSql(List<DjLossSettingDto> list);

    void deleteAll();
}
