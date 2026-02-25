package com.zlt.aps.mp.api.domain.vo;

import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 施工信息vo
 *
 * @author hsc
 * @date 2021/10/28
 */
@Getter
@Setter
public class MdmConstructionInfoVo extends BaseEntity {

    /**
     * 分公司编号
     */
    @Excel(name = "ui.data.column.mdmplanerproduction.companyCode", sort = 10)
    private String companyCode;

    /**
     * 施工代号
     */
    @Excel(name = "ui.data.column.constructionInfo.constructionCode", sort = 30)
    private String constructionCode;

    /**
     * 分厂成型法
     */
    @Excel(name = "ui.data.column.constructionInfo.mouldMethod", dictType = "molding_method", sort = 270)
    private String mouldMethod;

    /**
     * 寸口（保留2位小数）
     */
    @Excel(name = "ui.data.column.constructionInfo.proSize", sort = 20)
    private BigDecimal proSize;

    /**
     * 规格
     */
    @Excel(name = "ui.data.column.constructionInfo.specifications", sort = 100)
    private String specifications;

//    /**
//     * 创建时间
//     */
//    private Date createTime;
//
//    /**
//     * 修改时间
//     */
//    private Date updateTime;
//
//    /**
//     * 创建人
//     */
//    private String createBy;
//
//    /**
//     * 修改人
//     */
//    private String updateBy;

    /**
     * 机头宽度
     */
    @Excel(name = "ui.data.column.constructionInfo.headWidth", sort = 50)
    private Integer headWidth;

    /**
     * 扣圈盘直径
     */
    @Excel(name = "ui.data.column.constructionInfo.bucklePlateSize", sort = 60)
    private BigDecimal bucklePlateSize;

    /**
     * 1#胎体布代号
     */
    @Excel(name = "ui.data.column.constructionInfo.carcassClothCode1", sort = 70)
    private String carcassClothCode1;

    /**
     * 2#胎体布代号
     */
    @Excel(name = "ui.data.column.constructionInfo.carcassClothCode2", sort = 80)
    private String carcassClothCode2;

    /**
     * 3#胎体布代号
     */
    @Excel(name = "ui.data.column.constructionInfo.carcassClothCode3", sort = 90)
    private String carcassClothCode3;

    /**
     * 内衬代号
     */
    @Excel(name = "ui.data.column.constructionInfo.liningCode", sort = 110)
    private String liningCode;

    /**
     * 胎侧代号
     */
    @Excel(name = "ui.data.column.constructionInfo.sidewallCode", sort = 120)
    private String sidewallCode;

    /**
     * 钢带代号
     */
    @Excel(name = "ui.data.column.constructionInfo.steelStripCode")
    private String steelStripCode;

    /**
     * 冠带条代号
     */
    @Excel(name = "ui.data.column.constructionInfo.crownBandCode", sort = 140)
    private String crownBandCode;

    /**
     * 胎面代号
     */
    @Excel(name = "ui.data.column.constructionInfo.treadCode", sort = 150)
    private String treadCode;

    /**
     * 副股周长
     */
    @Excel(name = "ui.data.column.constructionInfo.viceStockCircumFerence")
    private String viceStockCircumFerence;

    /**
     * 支撑胶
     */
    @Excel(name = "ui.data.column.constructionInfo.beadFillerCode", sort = 130)
    private String beadFillerCode;

    /**
     * 贴合鼓周长
     */
    @Excel(name = "ui.data.column.constructionInfo.beltdrumCirumference", sort = 160)
    private BigDecimal beltdrumCirumference;

    /**
     * 卡盘直径
     */
    @Excel(name = "ui.data.column.constructionInfo.chuckDiameter", sort = 170)
    private BigDecimal chuckDiameter;

    /**
     * 拉伸宽度
     */
    @Excel(name = "ui.data.column.constructionInfo.extensionWidth", sort = 180)
    private BigDecimal extensionWidth;

    /**
     * 定型宽度
     */
    @Excel(name = "ui.data.column.constructionInfo.shapingWidth", sort = 190)
    private BigDecimal shapingWidth;

    /**
     * 胎胚周长
     */
    @Excel(name = "ui.data.column.constructionInfo.fetalCircumference", sort = 200)
    private BigDecimal fetalCircumference;

    /**
     * 1#带束层
     */
    @ImportExcelValidated(maxLength = 20)
    @Excel(name = "ui.data.column.constructionInfo.belt1", sort = 220)
    private String belt1;

    /**
     * 2#带束层
     */
    @ImportExcelValidated(maxLength = 20)
    @Excel(name = "ui.data.column.constructionInfo.belt2", sort = 230)
    private String belt2;

    /**
     * 3#带束层
     */
    @ImportExcelValidated(maxLength = 20)
    @Excel(name = "ui.data.column.constructionInfo.belt3", sort = 240)
    private String belt3;

    /**
     * 胎圈
     */
    @ImportExcelValidated(maxLength = 20)
    @Excel(name = "ui.data.column.constructionInfo.bead", sort = 250)
    private String bead;

    private static final long serialVersionUID = 1L;

    /**
     * 夏季硫化时间
     */
    @Excel(name = "ui.data.column.constructionInfo.xjCuringTime",sort = 260,type = Excel.Type.EXPORT)
    private BigDecimal xjCuringTime;

    /**
     * 冬季硫化时间
     */
    @Excel(name = "ui.data.column.constructionInfo.djCuringTime",sort = 260,type = Excel.Type.EXPORT)
    private BigDecimal djCuringTime;

    /**
     * 机头自动编码
     */
    private String noseAutoCode;

    /**
     * 预复合件总宽
     */
    private BigDecimal preComponentsWidth;

    /**
     * 胎侧居中（贴合位置）
     */
    private BigDecimal sidewallCenterPosition;

    /**
     * 分厂编号
     */
    private String factoryCode;

    /**
     * 规格描述
     */
    @Excel(name = "ui.data.column.constructionInfo.productDescription", sort = 40)
    private String productDescription;

    /**
     * 合模压力
     */
    @Excel(name = "ui.data.column.constructionInfo.mouldClampingPressure", sort = 210)
    private Integer mouldClampingPressure;


}
