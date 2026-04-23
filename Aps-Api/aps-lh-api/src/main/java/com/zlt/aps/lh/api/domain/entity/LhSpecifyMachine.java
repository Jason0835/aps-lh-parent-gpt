package com.zlt.aps.lh.api.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.common.annotation.ImportExcelValidated;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;

import java.io.Serializable;
import java.util.Date;

@ApiModel(value = "硫化定点机台信息对象", description = "硫化定点机台信息对象 ")
@Data
@TableName(value = "T_LH_SPECIFY_MACHINE")
public class LhSpecifyMachine  extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1149060431772573064L;

    /** 分厂编号 */
    @ApiModelProperty(value = "分厂编号", name = "factoryCode")
    @ImportExcelValidated(required = true)
    @TableField(value = "FACTORY_CODE")
    @Excel(name = "ui.data.column.result.factoryCode", dictType = "biz_factory_name")
    private String factoryCode;

     /** 物料编码 */
    @Excel(name = "ui.data.column.lhSpecifyMachine.specCode")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "物料编码", name = "specCode")
    @TableField(value = "SPEC_CODE")
    private String specCode;

    /** 物料描述（虚拟字段，不入库） */
    @Excel(name = "ui.data.column.lhSpecifyMachine.materialDesc",width = 60, align = Excel.Align.LEFT)
    @ApiModelProperty(value = "物料描述", name = "materialDesc")
    @TableField(exist = false)
    private String materialDesc;

    /** 机台编号 */
    @Excel(name = "ui.data.column.lhSpecifyMachine.machineCode")
    @ApiModelProperty(value = "机台编号", name = "machineCode")
    @ImportExcelValidated(required = true)
    @TableField(value = "MACHINE_CODE")
    private String machineCode;


    /** 线路，数据维护在数据字典：0-生产线、1-备用线 */
//    @Excel(name = "ui.data.column.lhSpecifyMachine.lineType", dictType = "LINE_TYPE")
    @ApiModelProperty(value = "线路，数据维护在数据字典：0-生产线、1-备用线", name = "lineType")
    @TableField(value = "LINE_TYPE")
    private String lineType;

    /** 作业类型，数据维护在数据字典：0-限制作业；1-不可作业 */
    @Excel(name = "ui.data.column.lhSpecifyMachine.jobType", dictType = "JOB_TYPE")
    @ImportExcelValidated(required = true)
    @ApiModelProperty(value = "作业类型，数据维护在数据字典：0-限制作业；1-不可作业", name = "jobType")
    @TableField(value = "JOB_TYPE")
    private String jobType;

    /** 创建人名称 */
//    @Excel(name = "ui.data.column.createBy")
    @ApiModelProperty(value = "创建人", name = "createByName")
    @TableField(exist = false)
    private String createByName;



    @TableField(
            value = "UPDATE_TIME",
            fill = FieldFill.INSERT_UPDATE,
            jdbcType = JdbcType.TIMESTAMP
    )
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间", name = "updateTime")
    @Excel(name = "ui.data.column.updateTime",dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    @ApiModelProperty("备注")
    @Excel(name = "ui.data.column.remark")
    @TableField("REMARK")
    private String remark;



}
