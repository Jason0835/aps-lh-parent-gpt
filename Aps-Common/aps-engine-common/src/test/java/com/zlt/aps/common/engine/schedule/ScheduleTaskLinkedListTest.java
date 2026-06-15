package com.zlt.aps.common.engine.schedule;

import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * 通用排程双向链表测试。
 *
 * <p>覆盖自动排程和人工调整共用的链表基础能力，确保插入、删除、转移和重排后顺序稳定。</p>
 */
public class ScheduleTaskLinkedListTest {

    @Test
    public void appendAndInsertAfterShouldKeepExpectedOrder() {
        ScheduleTaskLinkedList<String> chain = new ScheduleTaskLinkedList<>();
        ScheduleOperationContext context = context();
        ScheduleTaskNode<String> first = node("T1");
        ScheduleTaskNode<String> second = node("T2");
        ScheduleTaskNode<String> third = node("T3");

        chain.append(first, context);
        chain.append(third, context);
        ScheduleChainChangeResult<String> result = chain.insertAfter(first, second, context);

        assertEquals(3, result.getAffectedNodes().size());
        assertOrder(chain, "T1", "T2", "T3");
        assertEquals(Integer.valueOf(1), first.getSequence());
        assertEquals(Integer.valueOf(2), second.getSequence());
        assertEquals(Integer.valueOf(3), third.getSequence());
    }

    @Test
    public void insertAfterNullAnchorShouldAppendToTail() {
        ScheduleTaskLinkedList<String> chain = new ScheduleTaskLinkedList<>();
        ScheduleOperationContext context = context();

        chain.append(node("T1"), context);
        chain.insertAfter(null, node("T2"), context);

        assertOrder(chain, "T1", "T2");
    }

    @Test
    public void removeShouldUnlinkNodeAndResequenceRemainingNodes() {
        ScheduleTaskLinkedList<String> chain = new ScheduleTaskLinkedList<>();
        ScheduleOperationContext context = context();
        ScheduleTaskNode<String> first = node("T1");
        ScheduleTaskNode<String> second = node("T2");
        ScheduleTaskNode<String> third = node("T3");
        chain.append(first, context);
        chain.append(second, context);
        chain.append(third, context);

        ScheduleChainChangeResult<String> result = chain.remove(second, context);

        assertEquals("T2", result.getRemovedNodes().get(0).getTaskId());
        assertOrder(chain, "T1", "T3");
        assertNull(second.getPreviousNode());
        assertNull(second.getNextNode());
        assertEquals(Integer.valueOf(2), third.getSequence());
    }

    @Test
    public void transferToShouldMoveNodeBetweenChainsAndResequenceBothChains() {
        ScheduleOperationContext context = context();
        ScheduleTaskLinkedList<String> source = new ScheduleTaskLinkedList<>();
        ScheduleTaskLinkedList<String> target = new ScheduleTaskLinkedList<>();
        ScheduleTaskNode<String> first = node("T1");
        ScheduleTaskNode<String> second = node("T2");
        ScheduleTaskNode<String> targetFirst = node("T3");
        source.append(first, context);
        source.append(second, context);
        target.append(targetFirst, context);

        source.transferTo(second, target, targetFirst, context);

        assertOrder(source, "T1");
        assertOrder(target, "T3", "T2");
        assertEquals(Integer.valueOf(1), first.getSequence());
        assertEquals(Integer.valueOf(2), second.getSequence());
    }

    @Test(expected = IllegalStateException.class)
    public void appendShouldRejectNodeAlreadyInChain() {
        ScheduleTaskLinkedList<String> chain = new ScheduleTaskLinkedList<>();
        ScheduleOperationContext context = context();
        ScheduleTaskNode<String> first = node("T1");

        chain.append(first, context);
        chain.append(first, context);
    }

    @Test(expected = IllegalArgumentException.class)
    public void insertAfterShouldRejectAnchorOutsideCurrentChain() {
        ScheduleTaskLinkedList<String> chain = new ScheduleTaskLinkedList<>();
        ScheduleOperationContext context = context();

        chain.append(node("T1"), context);
        chain.insertAfter(node("OUT"), node("T2"), context);
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeShouldRejectNodeOutsideCurrentChain() {
        ScheduleTaskLinkedList<String> chain = new ScheduleTaskLinkedList<>();
        chain.remove(node("OUT"), context());
    }

    private ScheduleTaskNode<String> node(String taskId) {
        return new ScheduleTaskNode<>(taskId, taskId, "TM01", LocalDate.of(2026, 6, 13),
                "CLASS1", 1, BigDecimal.TEN);
    }

    private ScheduleOperationContext context() {
        return new ScheduleOperationContext("tester", "unit-test", "TRACE-001");
    }

    private void assertOrder(ScheduleTaskLinkedList<String> chain, String... taskIds) {
        List<ScheduleTaskNode<String>> nodes = chain.toList();
        assertEquals(taskIds.length, nodes.size());
        for (int i = 0; i < taskIds.length; i++) {
            assertEquals(taskIds[i], nodes.get(i).getTaskId());
        }
    }
}
