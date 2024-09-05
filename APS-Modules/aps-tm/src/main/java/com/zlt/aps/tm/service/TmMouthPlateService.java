package com.zlt.aps.tm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tm.api.domain.dto.TmMouthPlateDto;
import com.zlt.aps.tm.entity.TmMouthPlate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * 胎面口型板信息维护 服务类
 * </p>
 *
 * @author chenxueyuan
 * @since 2021-05-27
 */
public interface TmMouthPlateService extends IService<TmMouthPlate> {

    /**
     * 查询胎面口型板信息维护列表
     *
     * @param tmMouthPlate 胎面口型板信息维护
     * @return 胎面口型板信息维护集合
     */
    public List<TmMouthPlateDto> selectMouthPlateList(TmMouthPlate tmMouthPlate);

    /**
     * 查询胎面口型板信息维护列表
     *
     * @param id 要查询的id
     * @return 胎面口型板信息维护集合
     */
    public TmMouthPlate selectTmMouthPlateById(Long id);

    /**
     * 保存胎面口型板信息维护
     *
     * @param tmMouthPlate 胎面口型板信息维护
     */
    @Transactional
    void saveTmMouthPlate(TmMouthPlate tmMouthPlate);

    /**
     * 批量删除胎面口型板信息维护
     *
     * @param ids 需要删除的胎面口型板信息维护ID
     */
    @Transactional
    public void deleteTmMouthPlateByIds(Long[] ids);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<TmMouthPlateDto> list, boolean updateSupport, Long importLogId);

    void deleteAll();
}
