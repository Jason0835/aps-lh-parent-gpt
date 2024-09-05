package com.zlt.aps.tc.mapper;

import java.util.List;
import com.zlt.aps.tc.api.domain.entity.TcSidewallCodeColor;

/**
 * 胎侧代码前缀颜色设定Mapper接口
 * 
 * @author zlt
 * @date 2022-01-14
 */
public interface TcSidewallCodeColorMapper 
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

    public List<TcSidewallCodeColor> checkTcSidewallCodeColorUnique(TcSidewallCodeColor tcSidewallCodeColor);



    /**
     * 新增胎侧代码前缀颜色设定
     * 
     * @param tcSidewallCodeColor 胎侧代码前缀颜色设定
     * @return 结果
     */
    public int insertTcSidewallCodeColor(TcSidewallCodeColor tcSidewallCodeColor);

    /**
     * 修改胎侧代码前缀颜色设定
     * 
     * @param tcSidewallCodeColor 胎侧代码前缀颜色设定
     * @return 结果
     */
    public int updateTcSidewallCodeColor(TcSidewallCodeColor tcSidewallCodeColor);

    /**
     * 删除胎侧代码前缀颜色设定
     * 
     * @param id 胎侧代码前缀颜色设定ID
     * @return 结果
     */
    public int deleteTcSidewallCodeColorById(Long id);

    /**
     * 批量删除胎侧代码前缀颜色设定
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteTcSidewallCodeColorByIds(Long[] ids);

    /**
    * 合并操作，如果记录存在则更新，否则新增
    */
    public void mergeSql(List<TcSidewallCodeColor> list);
}
