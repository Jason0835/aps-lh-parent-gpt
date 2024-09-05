package com.zlt.aps.nc.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.nc.api.domain.dto.NcMouthPlateDto;
import com.zlt.aps.nc.entity.NcMouthPlate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * 内衬口型板信息维护 服务类
 * </p>
 *
 * @author chenxueyuan
 * @since 2021-05-27
 */
public interface NcMouthPlateService extends IService<NcMouthPlate> {

    /**
     * 查询内衬口型板信息维护列表
     *
     * @param mouthPlate 内衬口型板信息维护
     * @return 内衬口型板信息维护集合
     */
    public List<NcMouthPlateDto> selectMouthPlateList(NcMouthPlate mouthPlate);

    /**
     * 查询内衬口型板信息维护列表
     *
     * @param id 要查询的id
     * @return 内衬口型板信息维护集合
     */
    public NcMouthPlate selectMouthPlateById(Long id);

    /**
     * 保存内衬口型板信息维护
     *
     * @param mouthPlate 内衬口型板信息维护
     */
    @Transactional
    void saveMouthPlate(NcMouthPlate mouthPlate);

    /**
     * 批量删除内衬口型板信息维护
     *
     * @param ids 需要删除的内衬口型板信息维护ID
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
    AjaxResult importData(List<NcMouthPlateDto> list, boolean updateSupport, Long importLogId);
}
