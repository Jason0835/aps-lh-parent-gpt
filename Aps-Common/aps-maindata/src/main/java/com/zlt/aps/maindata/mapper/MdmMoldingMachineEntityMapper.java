package com.zlt.aps.maindata.mapper;

import com.zlt.aps.mp.api.domain.entity.MdmMoldingMachine;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import lombok.Data;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmMoldingMachineMapper.java
 * 描    述：基础数据-成型机档案Mapper接口
 *@author zlt
 *@date 2025-12-14
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Mapper
public interface MdmMoldingMachineEntityMapper extends CommBaseMapper<MdmMoldingMachine> {

    /**
     * 查询成型机台最近月份结构名称明细。
     *
     * @param factoryCode 工厂编码
     * @param yearMonths 年月集合，格式yyyy-MM
     * @return 成型机台月份结构名称明细
     */
    List<MachineMonthStructureName> selectMachineMonthStructureNames(@Param("factoryCode") String factoryCode,
                                                                     @Param("yearMonths") List<String> yearMonths);

    /**
     * 成型机台月份结构名称查询结果。
     */
    @Data
    class MachineMonthStructureName {
        /**
         * 年份。
         */
        private Integer year;

        /**
         * 月份。
         */
        private Integer month;

        /**
         * 年月，格式yyyy-MM。
         */
        private String yearMonth;

        /**
         * 成型机编码，可能多个以逗号分隔。
         */
        private String cxMachineCode;

        /**
         * 结构名称。
         */
        private String structureName;
    }
}
