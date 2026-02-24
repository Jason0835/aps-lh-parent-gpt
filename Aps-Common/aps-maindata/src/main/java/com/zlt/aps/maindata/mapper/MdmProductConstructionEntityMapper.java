package com.zlt.aps.maindata.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductConstruction;
import com.zlt.aps.monthplan.api.domain.vo.MdmProductConstructionImportVo;
import com.zlt.aps.monthplan.api.domain.vo.MdmProductConstructionVO;
import com.zlt.aps.monthplan.api.domain.vo.SpecCodeAndProductCodeVO;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmProductConstructionMapper.java
 * 描    述：SAP与施工对照Mapper接口
 *@author zlt
 *@date 2025-02-25
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Mapper
public interface MdmProductConstructionEntityMapper extends CommBaseMapper<MdmProductConstruction> {

    /**
     * 查询 SAP 与施工关系数据
     *
     * @param queryWrapper 查询条件
     * @return 结果
     */
    List<MdmProductConstruction> selectMdmProductConstructionList(@Param(Constants.WRAPPER) QueryWrapper<MdmProductConstruction> queryWrapper);

    /**
     * 根据分厂编号和产品、规格组合查询 SAP 与施工关系数据
     *
     * @param factoryCode 分厂编号
     * @param specCodes 产品与规格组合列表，每个对象包含 productCode 和 specCode
     * 示例：["物料编码_规格代码", "P002_S002"]
     * @return 对应的施工记录列表
     */
    List<MdmProductConstructionVO> queryByFactoryCodeAndSpecCodes(@Param("factoryCode") String factoryCode,
                                                                         @Param("specCodes") List<String> specCodes);

    /**
     * 根据分厂编号和产品、规格组合查询 SAP 与施工关系数据
     *
     * @param factoryCode 分厂编号
     * @param specCodes 产品与规格组合列表，每个对象包含 productCode 和 specCode
     * 示例：["物料编码_规格代码", "P002_S002"]
     * @return 对应的施工记录列表
     */
    List<MdmProductConstructionVO> queryByFactoryCodeAndSpecCodes2(@Param("factoryCode") String factoryCode,
                                                                  @Param("specCodes") List<String> specCodes);

    /**
     * 根据分厂编号和产品、规格组合查询 SAP 与施工关系数据
     *
     * @param factoryCode 分厂编号
     * @param specCodes 产品与规格组合列表，每个对象包含 productCode 和 specCode
     * 示例：["物料编码_规格代码", "P002_S002"]
     * @return 对应的施工记录列表
     */
    List<SpecCodeAndProductCodeVO> queryBySpecCodeAndProductCode(@Param("factoryCode") String factoryCode,
                                                                 @Param("specCodes") List<String> specCodes);

    /**
     * 根据分厂编号和产品、规格组合查询 SAP 与施工关系数据
     *
     * @param factoryCode   分厂编号
     * @param uniqueKeyList 产品与规格组合列表，每个对象包含 productCode 和 specCode
     *                      示例：["物料编码_规格代码", "P002|S002"]
     * @return 对应的施工记录列表
     */
    List<MdmProductConstruction> queryByProductCodeAndSpecCodes(@Param("factoryCode") String factoryCode,
                                                                @Param("uniqueKeyList") List<String> uniqueKeyList);

    /**
     * 根据工程厂编号、规格代码、胎胚号、生产版本、BOM版本，更新硫化时间、合模压力、型腔
     *
     * @param list 要更新的列表
     * @return 更新的行数
     */
    int batchUpdate4OfflineData(@Param("list") List<MdmProductConstruction> list);

    /**
     * 批量插入数据
     *
     * @param list 要插入的列表
     * @return 插入的行数
     */
    int batchInsert4OfflineData(@Param("list") List<MdmProductConstruction> list);

    /**
     * 查询已存在的数据，用于导入（查询后的数据直接用于更新）
     *
     * @param importList 导入列表
     * @return 结果
     */
    List<MdmProductConstruction> selectExistData(@Param("list") List<MdmProductConstructionImportVo> importList);

    /**
     * 查询最大时间
     * @return 结果
     */
    String selectMaxTime();

    /**
     * 根据物料编号、成型法查询数据
     * @param list 物料号、成型法
     * @return 结果
     */
    List<MdmProductConstruction> selectByProductCodeAndMethod(@Param("list") List<MdmProductConstruction> list);

    /**
     * 根据生胎代码查询数据
     * @param list 生胎代码
     * @return 结果
     */
    List<MdmProductConstruction> selectByEmbryoCode(@Param("list") List<MdmProductConstruction> list);

    /**
     * 根据规格代号查询数据
     * @param list 规格代号
     * @return 结果
     */
    List<MdmProductConstruction> selectBySpecCode(@Param("list") List<MdmProductConstruction> list);

    /**
     * 根据物料编号、成型法更新规格代号、施工代码、版本号
     * @param productCodeAndMethodUpdateList 物料号、成型法
     * @return 影响行数
     */
    int updateSpecConsVersionByProductCodeAndMethod(@Param("list") List<MdmProductConstruction> productCodeAndMethodUpdateList);

    /**
     * 根据生胎代码更新硫化时间、模具型腔、合模压力
     * @param productCodeAndMethodUpdateList 生胎代码
     * @return 影响行数
     */
    int updateCuringTimeByEmbryoCodeList(@Param("list") List<MdmProductConstruction> productCodeAndMethodUpdateList);

    /**
     * 根据ID更新SAP代码，成型法，施工代号、版本号、生胎代码
     * @param productCodeAndMethodUpdateList ID、物料号、成型法
     * @return 影响行数
     */
    int updateProductCodeById(@Param("list") List<MdmProductConstruction> productCodeAndMethodUpdateList);

    /**
     * 批量插入数据
     * @param productCodeAndMethodUpdateList 要插入的列表
     * @return 插入的行数
     */
    int insertBatch(@Param("list") List<MdmProductConstruction> productCodeAndMethodUpdateList);

    /**
     * 根据生胎代码批量更新硫化时间、模具型腔、合模压力
     * @param updateBy 更新人
     * @return 影响行数
     */
    int batchUpdateCuringTime(@Param("updateBy") String updateBy);

}
