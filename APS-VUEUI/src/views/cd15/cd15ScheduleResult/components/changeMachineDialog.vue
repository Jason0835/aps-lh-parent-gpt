<template>
  <el-dialog
    :title="$t('ui.data.column.cd15ScheduleResult.changeMachineTitle')"
    :visible.sync="visible"
    width="900px"
    append-to-body
    :close-on-click-modal="false"
    @close="hide"
  >
    <el-form ref="form" v-loading="loading" :model="form" :rules="rules" label-width="100px">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item :label="$t('ui.data.column.cd15ScheduleResult.factoryCode')" prop="factoryCode">
            <el-select v-model="form.factoryCode" disabled style="width:100%">
              <el-option v-for="item in factoryOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="$t('ui.data.column.cd15ScheduleResult.scheduleDate')" prop="scheduleDate">
            <el-date-picker v-model="form.scheduleDate" type="date" value-format="yyyy-MM-dd" disabled style="width:100%" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item :label="$t('ui.data.column.cd15ScheduleResult.sourceMachineCode')" prop="sourceMachineCode">
            <el-input v-model="form.sourceMachineCode" disabled />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="$t('ui.data.column.cd15ScheduleResult.targetMachineCode')" prop="targetMachineCode">
            <el-select v-model="form.targetMachineCode" filterable style="width:100%">
              <el-option
                v-for="item in targetMachineOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item :label="$t('ui.data.column.cd15ScheduleResult.steelStripCode')" prop="steelStripCode">
            <el-input v-model="form.steelStripCode" disabled />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="$t('ui.common.column.remark')">
            <el-input v-model="form.remark" maxlength="900" show-word-limit />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item :label="$t('ui.data.column.cd15ScheduleResult.transferShift')">
        <el-table :data="shiftRows" border size="small" style="width:100%">
          <el-table-column :label="$t('ui.data.column.cd15ScheduleResult.transferStatus')" width="100" align="center">
            <template slot-scope="scope">
              <el-tag v-if="isTransferClass(scope.row)" size="mini" type="success">
                {{ $t('ui.data.column.cd15ScheduleResult.transferable') }}
              </el-tag>
              <el-tag v-else size="mini" type="info">
                {{ $t('ui.data.column.cd15ScheduleResult.notTransferable') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="$t('ui.data.column.cd15ScheduleResult.shiftName')" min-width="180">
            <template slot-scope="scope">
              <span>{{ scope.row.shiftName }} {{ scope.row.shiftDate }} {{ scope.row.classField }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('ui.data.column.scheduleResult.plan')" width="130" align="right">
            <template slot-scope="scope">
              <span>{{ scope.row.planQty || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('ui.data.column.scheduleResult.produceOrder')" width="180" align="center">
            <template slot-scope="scope">
              <el-input-number
                v-model="scope.row.produceOrder"
                :min="1"
                :precision="0"
                :disabled="!isTransferClass(scope.row)"
                controls-position="right"
                style="width:130px"
              />
            </template>
          </el-table-column>
        </el-table>
      </el-form-item>
    </el-form>

    <template slot="footer">
      <el-button @click="hide">{{ $t('ui.frame.btn.cancel') }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">{{ $t('ui.frame.btn.submit') }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import { getCd15MachineEnableOptions } from '@/api/cd15/cd15MachineInfo'
import { shiftDates, validateTransferMachine, transferMachine } from '@/api/cd15/scheduleResult'

const DEFAULT_FORM = () => ({
  factoryCode: '116',
  scheduleDate: '',
  sourceMachineCode: '',
  targetMachineCode: '',
  steelStripCode: '',
  startClassField: '',
  remark: '',
  confirmed: false
})

const CLASS_FIELDS = ['CLASS1', 'CLASS2', 'CLASS3', 'CLASS4', 'CLASS5', 'CLASS6']
const CLASS_PLAN_FIELDS = ['class1PlanQty', 'class2PlanQty', 'class3PlanQty', 'class4PlanQty', 'class5PlanQty', 'class6PlanQty']
const CLASS_ORDER_FIELDS = ['class1ProduceOrder', 'class2ProduceOrder', 'class3ProduceOrder', 'class4ProduceOrder', 'class5ProduceOrder', 'class6ProduceOrder']
const SHIFT_KEYS = ['middleShift', 'nightShift', 'morningShift', 'middleShift', 'nightShift', 'morningShift']

export default {
  name: 'Cd15ChangeMachineDialog',
  inject: ['parentDict'],
  data() {
    return {
      visible: false,
      loading: false,
      form: DEFAULT_FORM(),
      currentRow: null,
      shiftRows: [],
      machineOptions: [],
      editableFromClassIndex: 7,
      rules: {
        factoryCode: [{ required: true, message: this.$t('common.rule.select'), trigger: 'change' }],
        scheduleDate: [{ required: true, message: this.$t('common.rule.select'), trigger: 'change' }],
        sourceMachineCode: [{ required: true, message: this.$t('common.rule.select'), trigger: 'change' }],
        targetMachineCode: [{ required: true, message: this.$t('common.rule.select'), trigger: 'change' }],
        steelStripCode: [{ required: true, message: this.$t('common.rule.select'), trigger: 'change' }]
      }
    }
  },
  computed: {
    factoryOptions() {
      return (this.parentDict.type.biz_factory_name || []).map(item => ({
        label: item.label || item.dictLabel,
        value: item.value || item.dictValue
      }))
    },
    targetMachineOptions() {
      return this.machineOptions.filter(item => item.value !== this.form.sourceMachineCode)
    }
  },
  methods: {
    async show(row) {
      this.currentRow = row || {}
      this.form = {
        ...DEFAULT_FORM(),
        factoryCode: this.currentRow.factoryCode || '116',
        scheduleDate: this.normalizeDate(this.currentRow.scheduleDate),
        sourceMachineCode: this.currentRow.machineCode || '',
        steelStripCode: this.currentRow.steelStripCode || ''
      }
      this.visible = true
      this.loading = true
      try {
        await Promise.all([this.loadShiftDates(), this.loadMachines()])
      } finally {
        this.loading = false
      }
    },
    hide() {
      this.visible = false
      this.loading = false
      this.form = DEFAULT_FORM()
      this.currentRow = null
      this.shiftRows = []
      this.machineOptions = []
      this.editableFromClassIndex = 7
      if (this.$refs.form) {
        this.$refs.form.resetFields()
      }
    },
    buildShiftRows(row, shiftItems = []) {
      const shiftMap = shiftItems.reduce((map, item) => {
        map[item.classField] = item
        return map
      }, {})
      return CLASS_FIELDS.map((classField, index) => {
        const planQty = Number(row && row[CLASS_PLAN_FIELDS[index]]) || 0
        const shiftItem = shiftMap[classField] || {}
        const transferable = index + 1 >= this.editableFromClassIndex && planQty > 0
        return {
          classField,
          shiftName: shiftItem.shiftName || this.$t(`ui.data.column.scheduleResult.${SHIFT_KEYS[index]}`),
          shiftDate: this.normalizeDate(shiftItem.shiftDate),
          planQty,
          transferable,
          produceOrder: transferable
            ? (row && row[CLASS_ORDER_FIELDS[index]] ? Number(row[CLASS_ORDER_FIELDS[index]]) : 1)
            : undefined
        }
      })
    },
    async loadShiftDates() {
      const response = await shiftDates({
        factoryCode: this.form.factoryCode,
        scheduleDate: this.form.scheduleDate
      })
      const rows = Array.isArray(response) ? response : (response.data || [])
      const firstEditable = rows.find(item => item.changeQtyEditable)
      const firstEditableIndex = firstEditable ? this.getClassIndex(firstEditable.classField) : -1
      this.editableFromClassIndex = firstEditableIndex >= 0 ? firstEditableIndex + 1 : 7
      this.form.startClassField = this.editableFromClassIndex <= 6
        ? CLASS_FIELDS[this.editableFromClassIndex - 1]
        : ''
      this.shiftRows = this.buildShiftRows(this.currentRow, rows)
    },
    getClassIndex(classField) {
      return CLASS_FIELDS.indexOf(classField)
    },
    isTransferClass(row) {
      return !!(row && row.transferable)
    },
    async loadMachines() {
      const response = await getCd15MachineEnableOptions({ factoryCode: this.form.factoryCode })
      const rows = Array.isArray(response) ? response : (response.rows || response.data || [])
      this.machineOptions = rows.map(item => ({
        label: item.machineName ? `${item.machineCode} - ${item.machineName}` : item.machineCode,
        value: item.machineCode
      }))
    },
    handleConfirm() {
      this.$refs.form.validate(async valid => {
        if (!valid) {
          return
        }
        if (this.form.sourceMachineCode === this.form.targetMachineCode) {
          this.$modal.msgWarning(this.$t('ui.data.column.cd15ScheduleResult.sameMachine'))
          return
        }
        if (!this.shiftRows.some(row => this.isTransferClass(row))) {
          this.$modal.msgWarning(this.$t('ui.data.column.cd15ScheduleResult.noTransferPlan'))
          return
        }
        if (this.shiftRows.some(row => this.isTransferClass(row) && !row.produceOrder)) {
          this.$modal.msgWarning(this.$t('ui.data.column.cd15ScheduleResult.targetOrderRequired'))
          return
        }
        await this.submit()
      })
    },
    buildRequest() {
      const params = { ...this.form }
      this.shiftRows.forEach((row, index) => {
        params[CLASS_ORDER_FIELDS[index]] = this.isTransferClass(row) ? row.produceOrder : null
      })
      return params
    },
    async submit() {
      this.loading = true
      try {
        const params = this.buildRequest()
        await validateTransferMachine(params)
        const response = this.normalizeResponse(await transferMachine(params))
        if (response.batchCheckFailed) {
          this.showBatchCheckAlert(response.data, response.msg)
          return
        }
        this.$emit('success', params.scheduleDate, response.data)
        this.hide()
      } finally {
        this.loading = false
      }
    },
    normalizeResponse(result) {
      const data = (result && result.data) ? result.data : (result || {})
      return {
        data,
        msg: (result && result.msg) || '',
        batchCheckFailed: !!(data.batchCheckFailed || (result && result.batchCheckFailed))
      }
    },
    showBatchCheckAlert(data, fallbackMsg) {
      const errors = (data && data.errors) || []
      const warnings = (data && data.warnings) || []
      let html = '<div style="max-height:55vh;overflow:auto;text-align:left;">'
      if (fallbackMsg) {
        html += '<div style="margin-bottom:10px;color:#606266;">' + this.escapeHtml(fallbackMsg) + '</div>'
      }
      errors.concat(warnings).forEach(item => {
        html += '<div style="padding:8px 0;border-top:1px solid #EBEEF5;color:#606266;line-height:1.7;">'
        html += this.escapeHtml(item.message || item)
        html += '</div>'
      })
      html += '</div>'
      this.$alert(html, this.$t('ui.data.column.cd15ScheduleResult.batchCheckTitle'), {
        dangerouslyUseHTMLString: true,
        type: errors.length ? 'error' : 'warning',
        confirmButtonText: this.$t('common.button.confirm')
      })
    },
    normalizeDate(value) {
      return value ? String(value).slice(0, 10) : ''
    },
    escapeHtml(value) {
      const characters = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }
      return String(value == null ? '' : value).replace(/[&<>"']/g, character => characters[character])
    }
  }
}
</script>
