package com.zlt.aps.dj.service;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.dj.api.domain.dto.DjMouthPlateDto;
import com.zlt.aps.dj.api.domain.entity.DjMouthPlate;

/**
 * <p>
 * 垫胶口型板信息维护 服务类
 * </p>
 *
 * @author chenxueyuan
 * @since 2021-05-27
 */
public interface DjMouthPlateService extends IService<DjMouthPlate> {

    /**
     * 查询垫胶口型板信息维护列表
     *
     * @param mouthPlate 垫胶口型板信息维护
     * @return 垫胶口型板信息维护集合
     */
    public List<DjMouthPlateDto> selectMouthPlateList(DjMouthPlate mouthPlate);

    /**
     * 查询垫胶口型板信息维护列表
     *
     * @param id 要查询的id
     * @return 垫胶口型板信息维护集合
     */
    public DjMouthPlate selectMouthPlateById(Long id);

    /**
     * 保存垫胶口型板信息维护
     *
     * @param mouthPlate 垫胶口型板信息维护
     */
    @Transactional
    void saveMouthPlate(DjMouthPlate mouthPlate);

    /**
     * 批量删除垫胶口型板信息维护
     *
     * @param ids 需要删除的垫胶口型板信息维护ID
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
    AjaxResult importData(List<DjMouthPlateDto> list, boolean updateSupport, Long importLogId);
}
