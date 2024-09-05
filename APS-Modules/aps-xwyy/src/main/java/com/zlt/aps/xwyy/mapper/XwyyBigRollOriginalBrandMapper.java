package com.zlt.aps.xwyy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.xwyy.api.domain.entity.XwyyBigRollOriginalBrand;

import java.util.List;

/**
 * 帘布大卷原线品牌Mapper接口
 *
 * @author chen
 * @date 2022-05-11
 */
public interface XwyyBigRollOriginalBrandMapper extends BaseMapper<XwyyBigRollOriginalBrand> {
    /**
     * 查询帘布大卷原线品牌
     *
     * @param id 帘布大卷原线品牌ID
     * @return 帘布大卷原线品牌
     */
    public XwyyBigRollOriginalBrand selectXwyyBigRollOriginalBrandById(Long id);

    /**
     * 查询帘布大卷原线品牌列表
     *
     * @param xwyyBigRollOriginalBrand 帘布大卷原线品牌
     * @return 帘布大卷原线品牌集合
     */
    public List<XwyyBigRollOriginalBrand> selectXwyyBigRollOriginalBrandList(XwyyBigRollOriginalBrand xwyyBigRollOriginalBrand);

    /**
     * 新增帘布大卷原线品牌
     *
     * @param xwyyBigRollOriginalBrand 帘布大卷原线品牌
     * @return 结果
     */
    public int insertXwyyBigRollOriginalBrand(XwyyBigRollOriginalBrand xwyyBigRollOriginalBrand);

    /**
     * 修改帘布大卷原线品牌
     *
     * @param xwyyBigRollOriginalBrand 帘布大卷原线品牌
     * @return 结果
     */
    public int updateXwyyBigRollOriginalBrand(XwyyBigRollOriginalBrand xwyyBigRollOriginalBrand);

    /**
     * 删除帘布大卷原线品牌
     *
     * @param id 帘布大卷原线品牌ID
     * @return 结果
     */
    public int deleteXwyyBigRollOriginalBrandById(Long id);

    /**
     * 批量删除帘布大卷原线品牌
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteXwyyBigRollOriginalBrandByIds(Long[] ids);

    /**
     * 合并操作，如果记录存在则更新，否则新增
     */
    public void mergeSql(List<XwyyBigRollOriginalBrand> list);
}
