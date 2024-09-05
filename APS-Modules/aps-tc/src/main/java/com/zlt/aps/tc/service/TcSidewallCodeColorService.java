package com.zlt.aps.tc.service;

import java.util.List;
import com.zlt.aps.tc.api.domain.entity.TcSidewallCodeColor;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.web.domain.AjaxResult;

/**
 * 胎侧代码前缀颜色设定Service接口
 * 
 * @author zlt
 * @date 2022-01-14
 */
public interface TcSidewallCodeColorService
{
    /**
     * 查询胎侧代码前缀颜色设定
     * 
     * @param id 胎侧代码前缀颜色设定ID
     * @return 胎侧代码前缀颜色设定
     */
    public TcSidewallCodeColor selectTcSidewallCodeColorById(Long id);

    /**
     * 查询胎侧代码前缀颜色设定列表
     * 
     * @param tcSidewallCodeColor 胎侧代码前缀颜色设定
     * @return 胎侧代码前缀颜色设定集合
     */
    public List<TcSidewallCodeColor> selectTcSidewallCodeColorList(TcSidewallCodeColor tcSidewallCodeColor);

    /**
     * 新增胎侧代码前缀颜色设定
     * 
     * @param tcSidewallCodeColor 胎侧代码前缀颜色设定
     * @return 结果
     */
    @Transactional
    public int insertTcSidewallCodeColor(TcSidewallCodeColor tcSidewallCodeColor);

    /**
     * 修改胎侧代码前缀颜色设定
     * 
     * @param tcSidewallCodeColor 胎侧代码前缀颜色设定
     * @return 结果
     */
    @Transactional
    public int updateTcSidewallCodeColor(TcSidewallCodeColor tcSidewallCodeColor);

    /**
     * 批量删除胎侧代码前缀颜色设定
     * 
     * @param ids 需要删除的胎侧代码前缀颜色设定ID
     * @return 结果
     */
    @Transactional
    public int deleteTcSidewallCodeColorByIds(Long[] ids);

    /**
     * 删除胎侧代码前缀颜色设定信息
     * 
     * @param id 胎侧代码前缀颜色设定ID
     * @return 结果
     */
    @Transactional
    public int deleteTcSidewallCodeColorById(Long id);

    /**
     * 校验胎侧代码前缀颜色设定唯一性
     */
    public String checkTcSidewallCodeColorUnique(TcSidewallCodeColor tcSidewallCodeColor);

    /**
     * 导入胎侧代码前缀颜色设定数据
     */
    @Transactional
    public AjaxResult importData(List<TcSidewallCodeColor> list, boolean updateSupport, Long importLogId);
}
