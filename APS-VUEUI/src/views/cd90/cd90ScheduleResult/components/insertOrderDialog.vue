<template>
  <el-dialog
    :title="$t('ui.data.column.cd90ScheduleResult.insertOrder')"
    :visible.sync="visible"
    width="1000px"
    append-to-body
    :close-on-click-modal="false"
    @close="hide"
  >
    <el-form ref="form" :model="form" :rules="rules" label-width="100px" v-loading="loading">
      <el-row :gutter="16" style="margin-bottom:20px;">
        <el-col :span="8">
          <el-form-item :label="$t('ui.data.column.cd90ScheduleResult.factoryCode')" prop="factoryCode">
            <el-select v-model="form.factoryCode" filterable disabled style="width:100%">
              <el-option v-for="item in factoryOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item :label="$t('ui.data.column.cd90ScheduleResult.scheduleDate')" prop="scheduleDate">
            <el-date-picker v-model="form.scheduleDate" type="date" value-format="yyyy-MM-dd" style="width:100%" disabled />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item :label="$t('ui.data.column.cd90ScheduleResult.machineCode')" prop="machineCode">
            <el-select v-model="form.machineCode" filterable style="width:100%" @change="onMachineChange">
              <el-option v-for="item in machineOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16" style="margin-bottom:18px;">
        <el-col :span="12">
          <el-form-item :label="$t('ui.data.column.cd90ScheduleResult.clothCode')" prop="clothCode">
            <el-select v-model="form.clothCode" filterable style="width:100%">
              <el-option v-for="item in clothOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="$t('ui.data.column.cd90ScheduleResult.remark')">
            <el-input v-model="form.remark" maxlength="900" show-word-limit />
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 班次明细表格 -->
      <el-table
        :data="shiftRows"
        border
        size="small"
        style="width:100%"
        :empty-text="loading ? $t('common.api.role.tips.loding') : $t('common.api.role.tips.loding')"
      >
        <el-table-column :label="$t('ui.data.column.cd90ScheduleResult.shiftName')" width="120">
          <template slot-scope="scope">
            <span>{{ scope.row.shiftName }}<span v-if="scope.row.shiftDate" style="color:#909399;">{{ formatDateMMDD(scope.row.shiftDate) }}</span></span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('ui.data.column.cd90ScheduleResult.produceOrder')" width="150" align="center">
          <template slot-scope="scope">
            <el-input-number
              v-model="scope.row.produceOrder"
              :min="1"
              :precision="0"
              controls-position="right"
              style="width:120px"
            />
          </template>
        </el-table-column>
        <el-table-column :label="$t('ui.data.column.cd90ScheduleResult.planQty')" width="170" align="center">
          <template slot-scope="scope">
            <el-input-number
              v-model="scope.row.planQty"
              :min="0"
              :precision="0"
              controls-position="right"
              style="width:140px"
            />
          </template>
        </el-table-column>
        <el-table-column :label="$t('ui.data.column.cd90ScheduleResult.analysis')" min-width="220">
          <template slot-scope="scope">
            <el-input v-model="scope.row.analysisInput" maxlength="500" show-word-limit />
          </template>
        </el-table-column>
      </el-table>

      <!-- 填写提示 -->
      <div v-if="shiftRows.length > 0" style="margin-top:6px;font-size:12px;color:#909399;">
        {{ $t('ui.dj.schedule.validate.atLeastOneShiftQty') }}
      </div>
    </el-form>

    <template slot="footer">
      <el-button @click="hide">{{ $t('common.button.cancel') }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">{{ $t('common.button.confirm') }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import { getCd90MachineEnableOptions } from '@/api/cd90/cd90MachineInfo'
import { listTireFabricCodes } from '@/api/cd90/specifyMachine'
import { shiftDates, validateInsert as validateInsertOrder, insertOrder } from '@/api/cd90/scheduleResult'

const CLASS_PLAN_FIELDS = ['class1PlanQty', 'class2PlanQty', 'class3PlanQty', 'class4PlanQty', 'class5PlanQty', 'class6PlanQty']
const CLASS_ORDER_FIELDS = ['class1ProduceOrder', 'class2ProduceOrder', 'class3ProduceOrder', 'class4ProduceOrder', 'class5ProduceOrder', 'class6ProduceOrder']
const CLASS_ANALYSIS_FIELDS = ['class1AnalysisInput', 'class2AnalysisInput', 'class3AnalysisInput', 'class4AnalysisInput', 'class5AnalysisInput', 'class6AnalysisInput']

const DEFAULT_FORM = () => ({
  factoryCode: '',
  scheduleDate: '',
  machineCode: '',
  clothCode: '',
  remark: ''
})

export default {
  name: 'Cd90InsertOrderDialog',
  inject: ['parentDict'],
  data() {
    return {
      visible: false,
      loading: false,
      form: DEFAULT_FORM(),
      shiftRows: [],
      machineOptions: [],
      clothOptions: [],
      rules: {
        factoryCode: [{ required: true, message: this.$t('common.rule.select'), trigger: 'change' }],
        scheduleDate: [{ required: true, message: this.$t('common.rule.select'), trigger: 'change' }],
        machineCode: [{ required: true, message: this.$t('common.rule.select'), trigger: 'change' }],
        clothCode: [{ required: true, message: this.$t('common.rule.select'), trigger: 'change' }]
      }
    }
  },
  computed: {
    factoryOptions() {
      return (this.parentDict.type.biz_factory_name || []).map(item => ({
        label: item.label || item.dictLabel,
        value: item.value || item.dictValue
      }))
    }
  },
  methods: {
    async show(data) {
      this.visible = true
      this.form = {
        ...DEFAULT_FORM(),
        factoryCode: (data && data.factoryCode) || '116',
        scheduleDate: (data && data.scheduleDate) || ''
      }
      await Promise.all([this.loadMachines(), this.loadCloths(), this.loadShiftDates()])
    },
    formatDateMMDD(dateStr) {
      if (!dateStr) return ''
      const parts = dateStr.split(/[-/]/)
      return parts.length === 3 ? `${parts[1]}/${parts[2]}` : dateStr
    },
    hide() {
      this.visible = false
      this.loading = false
      this.form = DEFAULT_FORM()
      this.shiftRows = []
      this.machineOptions = []
      this.clothOptions = []
      if (this.$refs.form) this.$refs.form.resetFields()
    },
    onMachineChange() {
      // 机台切换时清空帘布选择，避免选错组合
      this.form.clothCode = ''
    },
    async loadMachines() {
      const res = await getCd90MachineEnableOptions({ factoryCode: this.form.factoryCode })
      const rows = Array.isArray(res) ? res : (res.rows || res.data || [])
      this.machineOptions = rows.map(item => ({ label: item.machineCode, value: item.machineCode }))
    },
    async loadCloths() {
      const res = await listTireFabricCodes()
      const rows = Array.isArray(res) ? res : (res.rows || res.data || [])
      this.clothOptions = rows.map(item => ({
        label: item.label || item.clothCode || item.code || item,
        value: item.value || item.clothCode || item.code || item
      }))
    },
    async loadShiftDates() {
      const res = await shiftDates({ factoryCode: this.form.factoryCode, scheduleDate: this.form.scheduleDate })
      const rows = Array.isArray(res) ? res : (res.data || [])
      this.shiftRows = rows.map((item, index) => ({
        classField: item.classField || `CLASS${index + 1}`,
        shiftName: item.shiftName || item.shiftCode,
        shiftDate: item.shiftDate || '',
        produceOrder: undefined,
        planQty: undefined,
        analysisInput: ''
      }))
    },
    handleConfirm() {
      this.$refs.form.validate(async valid => {
        if (!valid) return
        // 每行填写了 produceOrder 和 planQty > 0 的必须同时填写原因分析
        const hasPlan = this.shiftRows.some(
          item => item.planQty > 0 && item.produceOrder > 0
        )
        if (!hasPlan) {
          this.$modal.msgWarning(
            this.$t('ui.dj.schedule.validate.atLeastOneShiftQty')
          )
          return
        }
        // 校验原因分析必填
        const missingAnalysis = this.shiftRows.some(
          item => item.planQty > 0 && item.produceOrder > 0 && !item.analysisInput
        )
        if (missingAnalysis) {
          this.$modal.msgWarning('原因分析为必填项')
          return
        }
        await this.submit()
      })
    },
    buildRequest() {
      const params = { ...this.form }
      this.shiftRows.forEach((item, index) => {
        // 只有同时有 planQty 和 produceOrder 的行才传值，否则传 null
        const hasValue = item.planQty > 0 && item.produceOrder > 0
        params[CLASS_PLAN_FIELDS[index]] = hasValue ? item.planQty : null
        params[CLASS_ORDER_FIELDS[index]] = hasValue ? item.produceOrder : null
        params[CLASS_ANALYSIS_FIELDS[index]] = hasValue ? (item.analysisInput || '') : null
      })
      return params
    },
    async submit() {
      this.loading = true
      try {
        const params = this.buildRequest()
        await validateInsertOrder(params)
        const result = await insertOrder(params)
        // 兼容响应拦截器两种返回形态：
        //   - 剥离后：result = { taskId, batchCheckFailed, errors, warnings, ... }
        //   - 完整体：result = { code, msg, data: { ... } }
        const data = (result && result.data) ? result.data : (result || {})
        const msg = (result && result.msg) || ''
        const batchCheckFailed = !!(data.batchCheckFailed || result.batchCheckFailed)
        // 批次级基础数据检查失败：不创建任务、不进入滚动，直接展示结构化错误（与自动排程一致）
        if (batchCheckFailed) {
          this.loading = false
          this.showBatchCheckAlert(data, msg)
          return
        }
        this.$modal.msgSuccess(msg || this.$t('common.message.operationSuccess'))
        this.$emit('success', params.scheduleDate, data)
        this.hide()
      } finally {
        this.loading = false
      }
    },
    /**
     * 渲染批次级数据检查失败的结构化错误/警告面板。
     * 渲染逻辑与 autoScheduleDialog.vue 的 showBatchCheckAlert 保持一致。
     */
    showBatchCheckAlert(data, fallbackMsg) {
      const errors = (data && data.errors) || []
      const warnings = (data && data.warnings) || []
      const summary = fallbackMsg
        || (errors.length > 0 ? errors[0].message : '')
        || this.$t('ui.data.column.cxScheduleResult.scheduleFailed')

      let html = ''
      html += '<div style="margin-bottom:16px;padding:10px;background:#fef0f0;border:1px solid #fde2e2;border-radius:4px;">'
      html += '<div style="color:#F56C6C;font-size:14px;font-weight:bold;">⚠️ '
        + this.$t('ui.data.column.cxScheduleResult.scheduleFailed') + '</div>'
      html += '<div style="color:#909399;font-size:13px;margin-top:4px;">' + summary + '</div>'
      html += '</div>'

      if (errors.length > 0) {
        html += '<div style="margin-bottom:12px;">'
        html += '<div style="color:#F56C6C;font-size:13px;font-weight:bold;margin-bottom:8px;display:flex;align-items:center;">'
        html += '<span style="display:inline-block;width:4px;height:14px;background:#F56C6C;margin-right:6px;border-radius:2px;"></span>'
        html += this.$t('ui.data.column.cxScheduleResult.errorLabel') + ' ('
          + errors.length + ' ' + this.$t('ui.data.column.cxScheduleResult.itemsLabel') + ')</div>'
        errors.forEach((item) => {
          html += '<div style="margin-bottom:12px;padding:10px;background:#fef0f0;border-left:3px solid #F56C6C;border-radius:3px;">'
          html += '<div style="font-weight:bold;color:#303133;font-size:13px;margin-bottom:6px;">' + (item.field || item.reasonCode || '') + '</div>'
          html += '<div style="color:#F56C6C;font-size:13px;line-height:1.6;margin-bottom:4px;">' + (item.message || '') + '</div>'
          if (item.suggestion) {
            html += '<div style="color:#909399;font-size:12px;line-height:1.6;"><span style="opacity:0.7;">💡</span> ' + item.suggestion + '</div>'
          }
          html += '</div>'
        })
        html += '</div>'
      }

      if (warnings.length > 0) {
        if (errors.length > 0) html += '<hr style="border:none;border-top:1px solid #EBEEF5;margin:16px 0;"/>'
        html += '<div>'
        html += '<div style="color:#E6A23C;font-size:13px;font-weight:bold;margin-bottom:8px;display:flex;align-items:center;">'
        html += '<span style="display:inline-block;width:4px;height:14px;background:#E6A23C;margin-right:6px;border-radius:2px;"></span>'
        html += this.$t('ui.data.column.cxScheduleResult.warningLabel') + ' ('
          + warnings.length + ' ' + this.$t('ui.data.column.cxScheduleResult.itemsLabel') + ')</div>'
        warnings.forEach((item) => {
          html += '<div style="margin-bottom:12px;padding:10px;background:#fdf6ec;border-left:3px solid #E6A23C;border-radius:3px;">'
          html += '<div style="font-weight:bold;color:#303133;font-size:13px;margin-bottom:6px;">' + (item.field || item.reasonCode || '') + '</div>'
          html += '<div style="color:#E6A23C;font-size:13px;line-height:1.6;margin-bottom:4px;">' + (item.message || '') + '</div>'
          if (item.suggestion) {
            html += '<div style="color:#909399;font-size:12px;line-height:1.6;"><span style="opacity:0.7;">💡</span> ' + item.suggestion + '</div>'
          }
          html += '</div>'
        })
        html += '</div>'
      }

      this.$alert(html, this.$t('ui.data.column.cxScheduleResult.scheduleFailed'), {
        dangerouslyUseHTMLString: true,
        type: 'error',
        customClass: 'cd90-insert-order-batch-check',
        confirmButtonText: this.$t('ui.data.column.cxScheduleResult.gotIt')
      })
    }
  }
}
</script>

<style>
/* 批次级数据检查错误/警告弹窗，内容过多时可滚动（非scoped，因为 $alert 挂在 body 级别） */
.cd90-insert-order-batch-check {
  width: auto !important;
  max-width: 1200px;
  max-height: 85vh;
  margin: 0 auto;
  position: relative;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  flex-direction: column;
}
.cd90-insert-order-batch-check .el-message-box__content {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.cd90-insert-order-batch-check .el-message-box__message {
  flex: 1;
  overflow-y: auto;
  max-height: calc(85vh - 130px);
  padding-right: 8px;
}
</style>
