package com.zlt.aps.cxlh.cx.api.domain.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.common.core.annotation.ImportValidated;
import lombok.Data;

import java.util.Date;

@Data
@TableName(value = "CX_ONLINE_IMPORT")
public class CxOnlineImport extends BaseEntity {
    @Excel(name = "ui.cx.online.zs")
    @ImportValidated(required = true, number = true)
    private Integer zs;         // 灶数

    @ImportValidated(required = true, number = true)
    @Excel(name = "ui.cx.online.bt")
    private Integer bt;         // 当日库存
    @Excel(name = "ui.cx.online.cl1")
    private Integer cl1;        // 产量1
    @Excel(name = "ui.cx.online.cl1hj")
    private Integer cl1hj;      // 产量1完成
    @Excel(name = "ui.cx.online.cl2")
    private Integer cl2;        // 产量2
    @Excel(name = "ui.cx.online.cl2hj")
    private Integer cl2hj;      // 产量2完成
    @Excel(name = "ui.cx.online.cxhj")
    private Integer cxhj;       // 成型合计
    @Excel(name = "ui.cx.online.lhhj")
    private Integer lhhj;       // 硫化合计
    @Excel(name = "ui.cx.online.lhcl1")
    private Integer lhcl1;      // 产量
    @Excel(name = "ui.cx.online.lhcl2")
    private Integer lhcl2;      // 产量

    @ImportValidated(required = true, number = true)
    @Excel(name = "ui.cx.online.jb")
    private Integer jb;         // 交班
    @Excel(name = "ui.cx.online.jb2")
    private Integer jb2;        // 1
    @Excel(name = "ui.cx.online.jb3")
    private Integer jb3;        // 1
    @Excel(name = "ui.cx.online.jt")
    private String jt;          // 机台
    @Excel(name = "ui.cx.online.cn")
    private Integer cn;         // 产能
    @Excel(name = "ui.cx.online.pcsj")
    private Integer pcsj;       // 排产时间
    @Excel(name = "ui.cx.online.ckfw")
    private String ckfw;        // 寸口范围
    @Excel(name = "ui.cx.online.spec")
    private String spec;        // 规格描述
    @Excel(name = "ui.cx.online.st")
    private String st;          // 生胎代码
    @Excel(name = "ui.cx.online.kahao")
    private String kahao;       // 施工卡
    @ImportValidated(required = true, number = true)
    @Excel(name = "ui.cx.online.suwei1")
    private Integer suwei1;     // 早班顺位
    @Excel(name = "ui.cx.online.miaoshi1")
    private String miaoshi1;    // 早班描述
    @ImportValidated(required = true, number = true)
    @Excel(name = "ui.cx.online.zbjh")
    private Integer zbjh;       // 早班计划
    @ImportValidated(required = true, number = true)
    @Excel(name = "ui.cx.online.zbwc")
    private Integer zbwc;       // 早班完成
    @Excel(name = "ui.cx.online.wbsw")
    private Integer wbsw;       // 晚班顺位
    @Excel(name = "ui.cx.online.wbfl")
    private String wbfl;        // 晚班描述
    @ImportValidated(required = true, number = true)
    @Excel(name = "ui.cx.online.wbjh")
    private Integer wbjh;       // 晚班计划
    @ImportValidated(required = true, number = true)
    @Excel(name = "ui.cx.online.wbwc")
    private Integer wbwc;       // 晚班完成
    @Excel(name = "ui.cx.online.biaoz")
    private Double biaoz;       // 标准重

    @ImportValidated(required = true, number = true)
    @Excel(name = "ui.cx.online.stz")
    private Double stz;         // 生态重

    @ImportValidated(required = true, number = true)
    @Excel(name = "ui.cx.online.sy")
    private Integer sy;         // 剩余量

    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @Excel(name = "ui.cx.online.rq",dateFormat = "yyyy-MM-dd")
    private Date rq;            // 日期


    public Integer getZs() {
        return zs;
    }

    public void setZs(Integer zs) {
        this.zs = zs;
    }

    public Integer getBt() {
        return bt;
    }

    public void setBt(Integer bt) {
        this.bt = bt;
    }

    public Integer getCl1() {
        return cl1;
    }

    public void setCl1(Integer cl1) {
        this.cl1 = cl1;
    }

    public Integer getCl1hj() {
        return cl1hj;
    }

    public void setCl1hj(Integer cl1hj) {
        this.cl1hj = cl1hj;
    }

    public Integer getCl2() {
        return cl2;
    }

    public void setCl2(Integer cl2) {
        this.cl2 = cl2;
    }

    public Integer getCl2hj() {
        return cl2hj;
    }

    public void setCl2hj(Integer cl2hj) {
        this.cl2hj = cl2hj;
    }

    public Integer getCxhj() {
        return cxhj;
    }

    public void setCxhj(Integer cxhj) {
        this.cxhj = cxhj;
    }

    public Integer getLhhj() {
        return lhhj;
    }

    public void setLhhj(Integer lhhj) {
        this.lhhj = lhhj;
    }

    public Integer getLhcl1() {
        return lhcl1;
    }

    public void setLhcl1(Integer lhcl1) {
        this.lhcl1 = lhcl1;
    }

    public Integer getLhcl2() {
        return lhcl2;
    }

    public void setLhcl2(Integer lhcl2) {
        this.lhcl2 = lhcl2;
    }

    public Integer getJb() {
        return jb;
    }

    public void setJb(Integer jb) {
        this.jb = jb;
    }

    public Integer getJb2() {
        return jb2;
    }

    public void setJb2(Integer jb2) {
        this.jb2 = jb2;
    }

    public Integer getJb3() {
        return jb3;
    }

    public void setJb3(Integer jb3) {
        this.jb3 = jb3;
    }

    public String getJt() {
        return jt;
    }

    public void setJt(String jt) {
        this.jt = jt;
    }

    public Integer getCn() {
        return cn;
    }

    public void setCn(Integer cn) {
        this.cn = cn;
    }

    public Integer getPcsj() {
        return pcsj;
    }

    public void setPcsj(Integer pcsj) {
        this.pcsj = pcsj;
    }

    public String getCkfw() {
        return ckfw;
    }

    public void setCkfw(String ckfw) {
        this.ckfw = ckfw;
    }

    public String getSpec() {
        return spec;
    }

    public void setSpec(String spec) {
        this.spec = spec;
    }

    public String getSt() {
        return st;
    }

    public void setSt(String st) {
        this.st = st;
    }

    public String getKahao() {
        return kahao;
    }

    public void setKahao(String kahao) {
        this.kahao = kahao;
    }

    public Integer getSuwei1() {
        return suwei1;
    }

    public void setSuwei1(Integer suwei1) {
        this.suwei1 = suwei1;
    }

    public String getMiaoshi1() {
        return miaoshi1;
    }

    public void setMiaoshi1(String miaoshi1) {
        this.miaoshi1 = miaoshi1;
    }

    public Integer getZbjh() {
        return zbjh;
    }

    public void setZbjh(Integer zbjh) {
        this.zbjh = zbjh;
    }

    public Integer getZbwc() {
        return zbwc;
    }

    public void setZbwc(Integer zbwc) {
        this.zbwc = zbwc;
    }

    public Integer getWbsw() {
        return wbsw;
    }

    public void setWbsw(Integer wbsw) {
        this.wbsw = wbsw;
    }

    public String getWbfl() {
        return wbfl;
    }

    public void setWbfl(String wbfl) {
        this.wbfl = wbfl;
    }

    public Integer getWbjh() {
        return wbjh;
    }

    public void setWbjh(Integer wbjh) {
        this.wbjh = wbjh;
    }

    public Integer getWbwc() {
        return wbwc;
    }

    public void setWbwc(Integer wbwc) {
        this.wbwc = wbwc;
    }

    public Double getBiaoz() {
        return biaoz;
    }

    public void setBiaoz(Double biaoz) {
        this.biaoz = biaoz;
    }

    public Double getStz() {
        return stz;
    }

    public void setStz(Double stz) {
        this.stz = stz;
    }

    public Integer getSy() {
        return sy;
    }

    public void setSy(Integer sy) {
        this.sy = sy;
    }

    public Date getRq() {
        return rq;
    }

    public void setRq(Date rq) {
        this.rq = rq;
    }
}
