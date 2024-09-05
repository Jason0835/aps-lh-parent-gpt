package com.zlt.aps.cx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cx.api.domain.dto.CxSpecColorDto;
import com.zlt.aps.cx.entity.CxSpecColor;

import java.util.List;

/**
 * 规格字体颜色设置Mapper接口
 * 
 * @author chen
 * @date 2021-08-21
 */
public interface CxSpecColorMapper extends BaseMapper<CxSpecColor>
{
    /**
     * 查询规格字体颜色设置
     * 
     * @param id 规格字体颜色设置ID
     * @return 规格字体颜色设置
     */
    public CxSpecColorDto selectCxSpecColorById(Long id);

    /**
     * 查询规格字体颜色设置列表
     * 
     * @param cxSpecColor 规格字体颜色设置
     * @return 规格字体颜色设置集合
     */
    public List<CxSpecColorDto> selectCxSpecColorList(CxSpecColor cxSpecColor);

    /**
     * 校验记录唯一性
     * @param cxSpecColor 记录
     * @return 查询到的相同记录条数
     */
    public int checkCxSpecColorUnique(CxSpecColor cxSpecColor);

    /**
     * 新增规格字体颜色设置
     * 
     * @param cxSpecColor 规格字体颜色设置
     * @return 结果
     */
    public int insertCxSpecColor(CxSpecColor cxSpecColor);

    /**
     * 修改规格字体颜色设置
     * 
     * @param cxSpecColor 规格字体颜色设置
     * @return 结果
     */
    public int updateCxSpecColor(CxSpecColor cxSpecColor);

    /**
     * 删除规格字体颜色设置
     * 
     * @param id 规格字体颜色设置ID
     * @return 结果
     */
    public int deleteCxSpecColorById(Long id);

    /**
     * 批量删除规格字体颜色设置
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteCxSpecColorByIds(Long[] ids);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    public void mergeSql(List<CxSpecColorDto> list);
}
