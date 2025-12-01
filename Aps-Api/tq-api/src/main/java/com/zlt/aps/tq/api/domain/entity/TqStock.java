package com.zlt.aps.tq.api.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.zlt.aps.common.core.annotation.ImportValidated;
import com.zlt.aps.common.core.domain.ApsBaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 胎圈库存信息对象
 *
 * @author zlt
 * @date 2021-05-31
 */
@ApiModel(value = "胎圈库存信息对象", description = "胎圈库存信息对象")
public class TqStock extends ApsBaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，对应自增序列为：SEQ_tq_STOCK
     */
    @ApiModelProperty(value = "主键ID", position = 10)
    private Long id;

    /**
     * 库存日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @Excel(name = "ui.data.column.stock.stockDate", width = 30, dateFormat = "yyyy-MM-dd")
    @ApiModelProperty(value = "库存日期", position = 20)
    @ImportValidated(required = true, date = true)
    private Date stockDate;

    @ApiModelProperty(value = "查询库存的开始日期yyyy-MM-dd", position = 21)
    private String startTime;

    @ApiModelProperty(value = "查询库存的结束日期yyyy-MM-dd", position = 22)
    private String endTime;

    /**
     * 库存物料编号
     */
    @ApiModelProperty(value = "库存物料编号", position = 30)
    @Excel(name = "ui.data.column.tq.scheduleResult.beadCode")
    @ImportValidated(required = true, maxLength = 50, isCode = true)
    private String materialCode;

    /**
     * 库存量
     */
    @ApiModelProperty(value = "库存量", position = 40)
    @Excel(name = "ui.data.column.stock.num", scale = 1)
    @ImportValidated(required = true, number = true, min = 0, max = 999999, digits = true)
    private BigDecimal stockNum;

    /**
     * 修正数量
     */
    @ApiModelProperty(value = "修正数量", position = 50)
    @Excel(name = "ui.data.column.stock.modify")
    @ImportValidated(number = true, min = -999999, max = 999999, digits = true)
    private BigDecimal modifyNum;

    /**
     * 不良数量
     */
    @ApiModelProperty(value = "不良数量", position = 60)
    @Excel(name = "ui.data.column.stock.bad")
    @ImportValidated(number = true, min = 0, max = 999999, digits = true)
    private BigDecimal badNum;

    @Excel(name = "ui.data.column.stock.remark")
    @ImportValidated(maxLength = 300)
    private String remark;

    /**
     * 删除标识：0--正常，1-删除
     */
    private String delFlag;


    @Override
    public String getRemark() {
        return remark;
    }

    @Override
    public void setRemark(String remark) {
        this.remark = remark;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setStockDate(Date stockDate) {
        this.stockDate = stockDate;
    }

    public Date getStockDate() {
        return stockDate;
    }

    public void setMaterialCode(String materialCode) {
        this.materialCode = materialCode;
    }

    public String getMaterialCode() {
        return materialCode;
    }

    public void setStockNum(BigDecimal stockNum) {
        this.stockNum = stockNum;
    }

    public BigDecimal getStockNum() {
        return stockNum;
    }

    public void setModifyNum(BigDecimal modifyNum) {
        this.modifyNum = modifyNum;
    }

    public BigDecimal getModifyNum() {
        return modifyNum;
    }

    public void setBadNum(BigDecimal badNum) {
        this.badNum = badNum;
    }

    public BigDecimal getBadNum() {
        return badNum;
    }

    public void setDelFlag(String delFlag) {
        this.delFlag = delFlag;
    }

    public String getDelFlag() {
        return delFlag;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    @Override
    public String toString() {
        return "TcStock{" +
                "id=" + id +
                ", stockDate=" + stockDate +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", materialCode='" + materialCode + '\'' +
                ", stockNum=" + stockNum +
                ", modifyNum=" + modifyNum +
                ", badNum=" + badNum +
                ", delFlag='" + delFlag + '\'' +
                '}';
    }
}
