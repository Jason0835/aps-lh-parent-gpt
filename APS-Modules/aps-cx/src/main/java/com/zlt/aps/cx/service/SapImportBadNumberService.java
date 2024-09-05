package com.zlt.aps.cx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cx.api.domain.entity.SapImportBadNumber;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * SAP导入不良数Service接口
 *
 * @author Joran.zhang
 * @date 2022-01-15
 */
public interface SapImportBadNumberService extends IService<SapImportBadNumber> {

    /**
     * 查询SAP导入不良数列表
     *
     * @param sapImportBadNumber SAP导入不良数
     * @return SAP导入不良数集合
     */
    public List<SapImportBadNumber> selectSapImportBadNumberList(SapImportBadNumber sapImportBadNumber);

    /**
     * 导入SAP导入不良数数据
     */
    @Transactional
    public AjaxResult importData(List<SapImportBadNumber> list, boolean updateSupport, Long importLogId);
}
