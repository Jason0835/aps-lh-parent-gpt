package com.zlt.aps.gsq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.gsq.api.domain.dto.GsqLossSettingDto;
import com.zlt.aps.gsq.entity.GsqLossSetting;

import java.util.List;

/**
 * 钢丝圈损耗率设定Mapper接口
 *
 * @author chen
 * @date 2021-07-13
 */
public interface GsqLossSettingMapper extends BaseMapper<GsqLossSetting> {

    /**
     * 查询钢丝圈损耗率设定
     *
     * @param id 钢丝圈损耗率设定ID
     * @return 钢丝圈损耗率设定
     */
    public GsqLossSettingDto selectGsqLossSettingById(Long id);

    /**
     * 查询钢丝圈损耗率设定列表
     *
     * @param gsqLossSetting 钢丝圈损耗率设定
     * @return 钢丝圈损耗率设定集合
     */
    public List<GsqLossSettingDto> selectGsqLossSettingList(GsqLossSetting gsqLossSetting);

    /**
     * 新增钢丝圈损耗率设定
     *
     * @param gsqLossSetting 钢丝圈损耗率设定
     * @return 结果
     */
    public int insertGsqLossSetting(GsqLossSetting gsqLossSetting);

    /**
     * 修改钢丝圈损耗率设定
     *
     * @param gsqLossSetting 钢丝圈损耗率设定
     * @return 结果
     */
    public int updateGsqLossSetting(GsqLossSetting gsqLossSetting);

    /**
     * 删除钢丝圈损耗率设定
     *
     * @param id 钢丝圈损耗率设定ID
     * @return 结果
     */
    public int deleteGsqLossSettingById(Long id);

    /**
     * 批量删除钢丝圈损耗率设定
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteGsqLossSettingByIds(Long[] ids);

    /**
     * 校验钢丝圈损耗率设定记录唯一性
     *
     * @param gsqLossSetting 要校验的记录
     * @return 查询到的记录条数
     */
    public int checkGsqLossSettingUnique(GsqLossSetting gsqLossSetting);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     *
     * @param list 要合并的集合
     */
    public void mergeSql(List<GsqLossSettingDto> list);

    void deleteAll();
}
