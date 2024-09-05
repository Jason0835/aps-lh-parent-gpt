package com.zlt.aps.tq.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tq.api.domain.dto.TqMouthPlateDto;
import com.zlt.aps.tq.entity.TqMouthPlate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * 胎圈口型板信息维护 服务类
 * </p>
 *
 * @author chenxueyuan
 * @since 2021-05-27
 */
public interface TqMouthPlateService extends IService<TqMouthPlate> {

    /**
     * 查询胎圈口型板信息维护列表
     *
     * @param mouthPlate 胎圈口型板信息维护
     * @return 胎圈口型板信息维护集合
     */
    public List<TqMouthPlateDto> selectMouthPlateList(TqMouthPlate mouthPlate);

    /**
     * 查询胎圈口型板信息维护列表
     *
     * @param id 要查询的id
     * @return 胎圈口型板信息维护集合
     */
    public TqMouthPlate selectMouthPlateById(Long id);

    /**
     * 保存胎圈口型板信息维护
     *
     * @param mouthPlate 胎圈口型板信息维护
     */
    @Transactional
    void saveMouthPlate(TqMouthPlate mouthPlate);

    /**
     * 批量删除胎圈口型板信息维护
     *
     * @param ids 需要删除的胎圈口型板信息维护ID
     */
    @Transactional
    public void deleteMouthPlateByIds(Long[] ids);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<TqMouthPlateDto> list, boolean updateSupport, Long importLogId);

    void deleteAll();
}
