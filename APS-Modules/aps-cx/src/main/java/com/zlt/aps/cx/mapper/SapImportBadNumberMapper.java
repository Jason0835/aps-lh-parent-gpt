package com.zlt.aps.cx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.cx.api.domain.entity.SapImportBadNumber;

import java.util.List;

/**
 * SAP导入不良数Mapper接口
 *
 * @author Joran.zhang
 * @date 2022-01-15
 */
public interface SapImportBadNumberMapper extends BaseMapper<SapImportBadNumber> {

    /**
     * 查询SAP导入不良数列表
     *
     * @param sapImportBadNumber SAP导入不良数
     * @return SAP导入不良数集合
     */
    public List<SapImportBadNumber> selectSapImportBadNumberList(SapImportBadNumber sapImportBadNumber);

    /**
     * 新增SAP导入不良数
     *
     * @param sapImportBadNumber SAP导入不良数
     * @return 结果
     */
    public int insertSapImportBadNumber(SapImportBadNumber sapImportBadNumber);

    /**
     * 删除SAP导入不良数
     *
     * @return 结果
     */
    public int deleteAll();
}
