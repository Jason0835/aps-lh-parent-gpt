<template>
  <el-dialog
    :title="$t('ui.data.column.cd15ScheduleResult.changeQtyTitle')"
    :visible.sync="visible"
    width="960px"
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
          <el-form-item :label="$t('ui.data.column.cd15ScheduleResult.machineCode')" prop="machineCode">
            <el-input v-model="form.machineCode" disabled />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="$t('ui.data.column.cd15ScheduleResult.steelStripCode')" prop="steelStripCode">
            <el-input v-model="form.steelStripCode" disabled />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="24">
          <el-form-item :label="$t('ui.common.column.remark')">
            <el-input v-model="form.remark" maxlength="900" show-word-limit />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item :label="$t('ui.data.column.cd15ScheduleResult.changeQtyEditableShift')">
        <el-table :data="shiftRows" border size="small" style="width:100%">
          <el-table-column :label="$t('ui.data.column.cd15ScheduleResult.transferStatus')" width="100" align="center">
            <template slot-scope="scope">
              <el-tag :type="isChangeQtyClass(scope.row) ? 'success' : 'info'" size="mini">
                {{ changeQtyShiftStatusText(scope.row) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="$t('ui.data.column.cd15ScheduleResult.shiftName')" min-width="180">
            <template slot-scope="scope">
              <span>{{ scope.row.shiftName }} {{ scope.row.shiftDate }} {{ scope.row.classField }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('ui.data.column.cd15ScheduleResult.originalPlanQty')" width="130" align="right">
            <template slot-scope="scope">
              <span>{{ formatQty(scope.row.originalPlanQty) }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('ui.data.column.cd15ScheduleResult.finishQty')" width="120" align="right">
            <template slot-scope="scope">
              <span>{{ formatQty(scope.row.finishQty) }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('ui.data.column.scheduleResult.produceOrder')" width="100" align="center">
            <template slot-scope="scope">
              <span>{{ scope.row.produceOrder || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('ui.data.column.cd15ScheduleResult.targetPlanQty')" width="180" align="center">
            <template slot-scope="scope">
              <el-input-number
                v-model="scope.row.targetPlanQty"
                :min="scope.row.finishQty"
                :precision="3"
                :step="1"
                :disabled="!isChangeQtyClass(scope.row)"
                controls-position="right"
                style="width:150px"
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
import { shiftDates, validateChangeQty, changeQty } from '@/api/cd15/scheduleResult'

const DEFAULT_FORM = () => ({
  factoryCode: '116',
  scheduleDate: '',
  scheduleResultId: null,
  machineCode: '',
  steelStripCode: '',
  remark: '',
  confirmed: false
})

const CLASS_FIELDS = ['CLASS1', 'CLASS2', 'CLASS3', 'CLASS4', 'CLASS5', 'CLASS6']
const CLASS_PLAN_FIELDS = ['class1PlanQty', 'class2PlanQty', 'class3PlanQty', 'class4PlanQty', 'class5PlanQty', 'class6PlanQty']
const CLASS_FINISH_FIELDS = ['class1FinishQty', 'class2FinishQty', 'class3FinishQty', 'class4FinishQty', 'class5FinishQty', 'class6FinishQty']
const CLASS_ORDER_FIELDS = ['class1ProduceOrder', 'class2ProduceOrder', 'class3ProduceOrder', 'class4ProduceOrder', 'class5ProduceOrder', 'class6ProduceOrder']
const SHIFT_KEYS = ['middleShift', 'nightShift', 'morningShift', 'middleShift', 'nightShift', 'morningShift']

export default {
  name: 'Cd15ChangeQtyDialog',
  inject: ['parentDict'],
  data() {
    return {
      visible: false,
      loading: false,
      form: DEFAULT_FORM(),
      currentRow: null,
      shiftRows: [],
      rules: {
        factoryCode: [{ required: true, message: this.$t('common.rule.select'), trigger: 'change' }],
        scheduleDate: [{ required: true, message: this.$t('common.rule.select'), trigger: 'change' }],
        machineCode: [{ required: true, message: this.$t('common.rule.select'), trigger: 'change' }],
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
    }
  },
  methods: {
    async show(row) {
      this.currentRow = row || {}
      this.form = {
        ...DEFAULT_FORM(),
        factoryCode: this.currentRow.factoryCode || '116',
        scheduleDate: this.normalizeDate(this.currentRow.scheduleDate),
        scheduleResultId: this.currentRow.id || null,
        machineCode: this.currentRow.machineCode || '',
        steelStripCode: this.currentRow.steelStripCode || ''
      }
      this.shiftRows = this.buildShiftRows(this.currentRow)
      this.visible = true
      this.loading = true
      try {
        await this.loadShiftDates()
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
        const originalPlanQty = this.toNumber(row && row[CLASS_PLAN_FIELDS[index]])
        const shiftItem = shiftMap[classField] || {}
        return {
          classField,
          shiftName: shiftItem.shiftName || this.$t(`ui.data.column.scheduleResult.${SHIFT_KEYS[index]}`),
          shiftDate: this.normalizeDate(shiftItem.shiftDate),
          currentShift: !!shiftItem.currentShift,
          editable: !!shiftItem.changeQtyEditable,
          originalPlanQty,
          finishQty: this.toNumber(row && row[CLASS_FINISH_FIELDS[index]]),
          produceOrder: row && row[CLASS_ORDER_FIELDS[index]]
            ? Number(row[CLASS_ORDER_FIELDS[index]])
            : undefined,
          targetPlanQty: originalPlanQty
        }
      })
    },
    async loadShiftDates() {
      const response = await shiftDates({
        factoryCode: this.form.factoryCode,
        scheduleDate: this.form.scheduleDate
      })
      const rows = Array.isArray(response) ? response : (response.data || [])
      this.shiftRows = this.buildShiftRows(this.currentRow, rows)
    },
    isChangeQtyClass(row) {
      return !!(row && row.editable)
    },
    isChanged(row) {
      return this.isChangeQtyClass(row) && Number(row.targetPlanQty) !== row.originalPlanQty
    },
    changeQtyShiftStatusText(row) {
      if (row && row.currentShift) {
        return this.$t('ui.data.column.cd15ScheduleResult.changeQtyCurrentShift')
      }
      return this.isChangeQtyClass(row)
        ? this.$t('ui.data.column.cd15ScheduleResult.changeQtyEditable')
        : this.$t('ui.data.column.cd15ScheduleResult.changeQtyReadonly')
    },
    handleConfirm() {
      this.$refs.form.validate(async valid => {
        if (!valid) {
          return
        }
        if (!this.shiftRows.some(row => this.isChangeQtyClass(row))) {
          this.$modal.msgWarning(this.$t('ui.data.column.cd15ScheduleResult.changeQtyNoPlan'))
          return
        }
        if (this.shiftRows.some(row => this.isChangeQtyClass(row) && row.targetPlanQty == null)) {
          this.$modal.msgWarning(this.$t('ui.data.column.cd15ScheduleResult.changeQtyTargetRequired'))
          return
        }
        if (this.shiftRows.some(row => this.isChangeQtyClass(row) && Number(row.targetPlanQty) < row.finishQty)) {
          this.$modal.msgWarning(this.$t('ui.data.column.cd15ScheduleResult.changeQtyLessThanFinish'))
          return
        }
        if (!this.shiftRows.some(row => this.isChanged(row))) {
          this.$modal.msgWarning(this.$t('ui.data.column.cd15ScheduleResult.changeQtyNeedOne'))
          return
        }
        await this.submit()
      })
    },
    buildRequest() {
      const params = { ...this.form }
      this.shiftRows.forEach((row, index) => {
        params[CLASS_PLAN_FIELDS[index]] = this.isChanged(row) ? Number(row.targetPlanQty) : null
      })
      return params
    },
    async submit() {
      this.loading = true
      try {
        const params = this.buildRequest()
        await validateChangeQty(params)
        const response = this.normalizeResponse(await changeQty(params))
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
    toNumber(value) {
      const numberValue = Number(value)
      return Number.isFinite(numberValue) ? numberValue : 0
    },
    formatQty(value) {
      return value || value === 0
        ? Number(value).toFixed(3).replace(/\.0+$/, '').replace(/(\.\d*?)0+$/, '$1')
        : '-'
    },
    escapeHtml(value) {
      const characters = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }
      return String(value == null ? '' : value).replace(/[&<>"']/g, character => characters[character])
    }
  }
}
</script>
