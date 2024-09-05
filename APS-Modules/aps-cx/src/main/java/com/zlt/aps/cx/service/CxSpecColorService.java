package com.zlt.aps.cx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cx.api.domain.dto.CxSpecColorDto;
import com.zlt.aps.cx.entity.CxSpecColor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 规格字体颜色设置Service接口
 * 
 * @author chen
 * @date 2021-08-21
 */
public interface CxSpecColorService extends IService<CxSpecColor>
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
     * 新增规格字体颜色设置
     * 
     * @param cxSpecColor 规格字体颜色设置
     * @return 结果
     */
    @Transactional
    public int insertCxSpecColor(CxSpecColor cxSpecColor);

    /**
     * 修改规格字体颜色设置
     * 
     * @param cxSpecColor 规格字体颜色设置
     * @return 结果
     */
    @Transactional
    public int updateCxSpecColor(CxSpecColor cxSpecColor);

    /**
     * 批量删除规格字体颜色设置
     * 
     * @param ids 需要删除的规格字体颜色设置ID
     * @return 结果
     */
    @Transactional
    public int deleteCxSpecColorByIds(Long[] ids);

    /**
     * 删除规格字体颜色设置信息
     * 
     * @param id 规格字体颜色设置ID
     * @return 结果
     */
    @Transactional
    public int deleteCxSpecColorById(Long id);

    /**
     * 校验规格字体颜色设置唯一性
     */
    public String checkCxSpecColorUnique(CxSpecColor cxSpecColor);

    /**
     * 导入数据
     */
    @Transactional
    AjaxResult importData(List<CxSpecColorDto> list, boolean updateSupport, Long importLogId);
}
