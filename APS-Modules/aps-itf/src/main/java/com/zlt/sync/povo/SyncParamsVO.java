package com.zlt.sync.povo;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

/**
 * 同步请求或通知参数
 */
@Data
public class SyncParamsVO {
    /**
     * 每个接口定义的数据同步标志，由调用处自行根据 sync-data-${spring.profiles.active}.yml 获取
     */
    private String syncKey;

    /**
     * 对接系统, syncRequest,syncNotice 根据 syncKey 获取 (前端不必输入)
     */
    private String dockSys;

    /**
     * 通知时需要 版本号 2021080700001 格式 由 SyncDataHandle.getDataVersion方法提供
     */
    private String dataVersion;

    /**
     * 参数，由接口自定义
     */
    private JSONObject params;

    /////////////////////////////////////////////////////////////

    private String msgId; //主键 uuid
    private String msgCode;
    private String msg;
    private Integer hasData; //1表示有数据
    private Integer status; //0请求状态, 1有数据同步(主动，被动), 2成功, 3失败
    private String dataSys; //来源系统 APS, MPS
    private Integer backIssue; //是否回传下发 1是
    private String companyCode; //分公司编号
    private String factoryCode; //厂别

    /**
     * 是否发送MQ
     */
    private Integer noMq;

    public String toJSONString() {
        return JSONObject.toJSONString(this);
    }

    @Override
    public String toString() {
        return "SyncParamsVO{" +
                "syncKey='" + syncKey + '\'' +
                ", dockSys='" + dockSys + '\'' +
                ", dataVersion='" + dataVersion + '\'' +
                ", params=" + params +
                ", msgId='" + msgId + '\'' +
                ", msgCode='" + msgCode + '\'' +
                ", msg='" + msg + '\'' +
                ", hasData=" + hasData +
                ", status=" + status +
                ", dataSys='" + dataSys + '\'' +
                ", backIssue=" + backIssue +
                ", companyCode = '" + companyCode + '\'' +
                ", factoryCode = '" + factoryCode + '\'' +
                '}';
    }
}
