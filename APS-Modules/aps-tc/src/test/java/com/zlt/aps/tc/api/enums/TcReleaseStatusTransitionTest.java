package com.zlt.aps.tc.api.enums;

import org.junit.Assert;
import org.junit.Test;

/**
 * 胎侧发布状态流转规则测试。
 */
public class TcReleaseStatusTransitionTest {

    /**
     * 验证未发布、失败、超时和待发布状态均可直接进入发布中。
     */
    @Test
    public void shouldAllowPublishableStatusesEnterReleasing() {
        Assert.assertTrue(TcReleaseStatusTransition.canTransit("0", "3"));
        Assert.assertTrue(TcReleaseStatusTransition.canTransit("2", "3"));
        Assert.assertTrue(TcReleaseStatusTransition.canTransit("4", "3"));
        Assert.assertTrue(TcReleaseStatusTransition.canTransit("5", "3"));
        Assert.assertFalse(TcReleaseStatusTransition.canTransit("1", "3"));
    }

    /**
     * 验证人工调整只将已发布状态回退待发布，其他可编辑状态保持原值。
     */
    @Test
    public void shouldResolveManualEditedStatus() {
        Assert.assertEquals("0", TcReleaseStatusTransition.resolveEditedStatus("0"));
        Assert.assertEquals("5", TcReleaseStatusTransition.resolveEditedStatus("1"));
        Assert.assertEquals("2", TcReleaseStatusTransition.resolveEditedStatus("2"));
        Assert.assertEquals("5", TcReleaseStatusTransition.resolveEditedStatus("5"));
        Assert.assertTrue(TcReleaseStatusTransition.isEditable("2"));
        Assert.assertFalse(TcReleaseStatusTransition.isEditable("3"));
        Assert.assertFalse(TcReleaseStatusTransition.isEditable("4"));
    }
}
