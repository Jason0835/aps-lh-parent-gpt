package com.zlt.aps.tc.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tc.api.domain.dto.TcMouthPlateDto;
import com.zlt.aps.tc.entity.TcMouthPlate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * 胎侧口型板信息维护 服务类
 * </p>
 *
 * @author chenxueyuan
 * @since 2021-06-02
 */
public interface TcMouthPlateService extends IService<TcMouthPlate> {

    /**
     * 查询胎侧口型板信息维护列表
     *
     * @param mouthPlate 胎侧口型板信息维
     * @return 胎侧口型板信息维护集合
     */
    public List<TcMouthPlateDto> selectMouthPlateList(TcMouthPlate mouthPlate);

    /**
     * 查询胎侧口型板信息维护列表
     *
     * @param id 要查询的id
     * @return 胎侧口型板信息维护集合
     */
    public TcMouthPlate selectTmMouthPlateById(Long id);

    /**
     * 保存胎侧口型板信息维护
     *
     * @param mouthPlate 胎侧口型板信息维护
     */
    @Transactional
    void saveTmMouthPlate(TcMouthPlate mouthPlate);

    /**
     * 批量删除胎侧口型板信息维护
     *
     * @param ids 需要删除的胎侧口型板信息维护ID
     */
    @Transactional
    public void deleteTmMouthPlateByIds(Long[] ids);

    /**
     * 导入数据
     */
    public AjaxResult importData(List<TcMouthPlateDto> list, boolean updateSupport, Long importLogId);

    void deleteAll();
}
