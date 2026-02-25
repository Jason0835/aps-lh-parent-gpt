package com.zlt.aps.maindata.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.maindata.domain.dto.MdmProductConstructionDto;
import com.zlt.aps.mp.api.domain.entity.MdmProductConstruction;
import com.zlt.aps.mp.api.domain.vo.MdmProductConstructionImportVo;
import com.zlt.aps.mp.api.domain.vo.MdmProductConstructionVO;
import com.zlt.bill.common.service.IDocService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmProductConstructionService.java
 * 描    述：IMdmProductConstructionServiceSAP与施工对照后端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-25
 */
public interface IMdmProductConstructionService extends IDocService<MdmProductConstruction> {

    /**
     * 根据分厂编号和产品、规格组合查询 SAP 与施工关系数据
     *
     * @param factoryCode 分厂编号
     * @param specCodes   产品与规格组合列表，每个对象包含 productCode 和 specCode
     *                    示例：["物料编码_规格代码", "P002_S002"]
     * @return 对应的施工记录列表
     */
    List<MdmProductConstructionVO> queryByFactoryCodeAndSpecCodes(String factoryCode, Set<String> specCodes);

    /**
     * 根据规格号查询物料List
     *
     * @param factoryCode
     * @param specCode
     * @return
     */
    List<MdmProductConstruction> selectListByFactoryCodeAndSpecCode(String factoryCode, List<String> specCode);

    /**
     * 获取分厂规格的硫化时间配置
     * 多条只取其中一条，硫化时间要一致
     *
     * @param factoryCode 分厂编码
     * @param productCode 物料编码
     * @param specCode    规格代号
     * @return
     */
    MdmProductConstructionDto getCuringTime(String factoryCode, String productCode, String specCode);

    /**
     * 导入客户格式施工信息
     *
     * @param importList    导入列表
     * @param updateSupport
     * @param importLogId
     * @return 结果
     */
    AjaxResult importOfflineData(List<MdmProductConstructionImportVo> importList, boolean updateSupport, Long importLogId);

    /**
     * 同步数据
     * @param syncData 同步数据
     * @param dataVersion 数据版本
     */
    @Transactional(rollbackFor = Exception.class)
    void syncProductConstructionInfo(List<MdmProductConstruction> syncData, String dataVersion);

    /**
     * 根据物料编码查询
     * @param productCode 物料编码
     * @return 结果
     */
    List<MdmProductConstruction> selectByProductCode(String productCode);

    /**
     * 根据物料编码和成型法删除
     * @param construction 物料号、成型法
     * @return 结果
     */
    AjaxResult removeByProductCodeAndMouldMethod(MdmProductConstruction construction);
}
