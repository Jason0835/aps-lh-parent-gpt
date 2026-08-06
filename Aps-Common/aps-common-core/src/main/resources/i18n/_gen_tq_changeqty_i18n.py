# -*- coding: utf-8 -*-
"""一次性生成胎圈调量 i18n key 的 properties 文件内容（unicode 转义）。"""
msgs = [
    ('ui.tq.scheduleResult.changeQty.selectRecord', '请选择需要调量的排程记录'),
    ('ui.tq.scheduleResult.changeQty.recordNotExist', '排程记录不存在或已删除'),
    ('ui.tq.scheduleResult.changeQty.planQtyLessThanZero', '第%d班计划量不能小于0'),
    ('ui.tq.scheduleResult.changeQty.historyShiftForbidden', '不能修改历史班次（第%d班）的计划量'),
    ('ui.tq.scheduleResult.changeQty.planQtyLessThanFinish', '第%d班计划量不能小于完成量%d'),
    ('ui.tq.scheduleResult.changeQty.noAdjustField', '未检测到需要调整的计划量'),
    ('ui.tq.scheduleResult.changeQty.errorSeparator', '；'),
    ('ui.tq.scheduleResult.changeQty.validatePass', '校验通过'),
    ('ui.tq.scheduleResult.changeQty.noChange', '没有需要保存的修改内容'),
    ('ui.tq.scheduleResult.changeQty.success', '调量成功'),
    ('ui.tq.scheduleResult.delete.selectRecord', '请选择需要删除的记录'),
    ('ui.tq.scheduleResult.delete.publishedForbidden', '已发布成功的计划不允许删除，只能调量。胎圈代码：%s'),
    ('ui.tq.scheduleResult.delete.success', '删除成功'),
    ('ui.tq.scheduleResult.insertOrder.success', '插单成功'),
]


def to_unicode_escape(text):
    sb = []
    for c in text:
        code = ord(c)
        if code > 127:
            sb.append('\\u' + format(code, '04X'))
        else:
            sb.append(c)
    return ''.join(sb)


# 中文
zh_lines = []
for key, value in msgs:
    zh_lines.append('{}={}'.format(key, to_unicode_escape(value)))

# 英文翻译
en_msgs = {
    'ui.tq.scheduleResult.changeQty.selectRecord': 'Please select a schedule record to adjust quantity',
    'ui.tq.scheduleResult.changeQty.recordNotExist': 'Schedule record does not exist or has been deleted',
    'ui.tq.scheduleResult.changeQty.planQtyLessThanZero': 'Shift %d plan quantity cannot be less than 0',
    'ui.tq.scheduleResult.changeQty.historyShiftForbidden': 'Cannot modify the plan quantity of historical shift (Shift %d)',
    'ui.tq.scheduleResult.changeQty.planQtyLessThanFinish': 'Shift %d plan quantity cannot be less than finished quantity %d',
    'ui.tq.scheduleResult.changeQty.noAdjustField': 'No plan quantity to adjust detected',
    'ui.tq.scheduleResult.changeQty.errorSeparator': '; ',
    'ui.tq.scheduleResult.changeQty.validatePass': 'Validation passed',
    'ui.tq.scheduleResult.changeQty.noChange': 'No changes to save',
    'ui.tq.scheduleResult.changeQty.success': 'Quantity adjusted successfully',
    'ui.tq.scheduleResult.delete.selectRecord': 'Please select records to delete',
    'ui.tq.scheduleResult.delete.publishedForbidden': 'Published schedules cannot be deleted, only quantity can be adjusted. Bead code: %s',
    'ui.tq.scheduleResult.delete.success': 'Deleted successfully',
    'ui.tq.scheduleResult.insertOrder.success': 'Insert order successfully',
}
en_lines = ['{}={}'.format(k, en_msgs[k]) for k, _ in msgs]

# 越南语翻译
vi_msgs = {
    'ui.tq.scheduleResult.changeQty.selectRecord': 'Vui lòng chọn bản ghi lịch trình để điều chỉnh sản lượng',
    'ui.tq.scheduleResult.changeQty.recordNotExist': 'Bản ghi lịch trình không tồn tại hoặc đã bị xóa',
    'ui.tq.scheduleResult.changeQty.planQtyLessThanZero': 'Sản lượng kế hoạch ca %d không thể nhỏ hơn 0',
    'ui.tq.scheduleResult.changeQty.historyShiftForbidden': 'Không thể sửa sản lượng kế hoạch của ca lịch sử (ca %d)',
    'ui.tq.scheduleResult.changeQty.planQtyLessThanFinish': 'Sản lượng kế hoạch ca %d không thể nhỏ hơn sản lượng hoàn thành %d',
    'ui.tq.scheduleResult.changeQty.noAdjustField': 'Không phát hiện sản lượng kế hoạch cần điều chỉnh',
    'ui.tq.scheduleResult.changeQty.errorSeparator': '; ',
    'ui.tq.scheduleResult.changeQty.validatePass': 'Xác thực thành công',
    'ui.tq.scheduleResult.changeQty.noChange': 'Không có thay đổi để lưu',
    'ui.tq.scheduleResult.changeQty.success': 'Điều chỉnh sản lượng thành công',
    'ui.tq.scheduleResult.delete.selectRecord': 'Vui lòng chọn bản ghi để xóa',
    'ui.tq.scheduleResult.delete.publishedForbidden': 'Lịch trình đã phát hành không thể xóa, chỉ có thể điều chỉnh sản lượng. Mã vành lốp: %s',
    'ui.tq.scheduleResult.delete.success': 'Xóa thành công',
    'ui.tq.scheduleResult.insertOrder.success': 'Thêm đơn thành công',
}
vi_lines = ['{}={}'.format(k, to_unicode_escape(vi_msgs[k])) for k, _ in msgs]

# 默认 properties (与中文相同)
default_lines = zh_lines

print('=== apsui_zh_CN.properties ===')
print('\n'.join(zh_lines))
print('\n=== apsui.properties ===')
print('\n'.join(default_lines))
print('\n=== apsui_en_US.properties ===')
print('\n'.join(en_lines))
print('\n=== apsui_vi_VN.properties ===')
print('\n'.join(vi_lines))
