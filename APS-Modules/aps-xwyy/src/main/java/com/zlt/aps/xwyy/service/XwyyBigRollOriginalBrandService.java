package com.zlt.aps.xwyy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.xwyy.api.domain.entity.XwyyBigRollOriginalBrand;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 帘布大卷原线品牌Service接口
 *
 * @author chen
 * @date 2022-05-11
 */
public interface XwyyBigRollOriginalBrandService extends IService<XwyyBigRollOriginalBrand> {
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
    @Transactional
    public int insertXwyyBigRollOriginalBrand(XwyyBigRollOriginalBrand xwyyBigRollOriginalBrand);

    /**
     * 修改帘布大卷原线品牌
     *
     * @param xwyyBigRollOriginalBrand 帘布大卷原线品牌
     * @return 结果
     */
    @Transactional
    public int updateXwyyBigRollOriginalBrand(XwyyBigRollOriginalBrand xwyyBigRollOriginalBrand);

    /**
     * 批量删除帘布大卷原线品牌
     *
     * @param ids 需要删除的帘布大卷原线品牌ID
     * @return 结果
     */
    @Transactional
    public int deleteXwyyBigRollOriginalBrandByIds(Long[] ids);

    /**
     * 删除帘布大卷原线品牌信息
     *
     * @param id 帘布大卷原线品牌ID
     * @return 结果
     */
    @Transactional
    public int deleteXwyyBigRollOriginalBrandById(Long id);

    /**
     * 校验帘布大卷原线品牌唯一性
     */
    public String checkXwyyBigRollOriginalBrandUnique(XwyyBigRollOriginalBrand xwyyBigRollOriginalBrand);

    /**
     * 导入帘布大卷原线品牌数据
     */
    @Transactional
    public AjaxResult importData(List<XwyyBigRollOriginalBrand> list, boolean updateSupport, Long importLogId);
}
