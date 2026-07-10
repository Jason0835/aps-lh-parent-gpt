<template>
  <el-dialog
    title="转机台"
    :visible.sync="visible"
    width="900px"
    append-to-body
    :close-on-click-modal="false"
    @close="hide"
  >
    <el-form ref="form" v-loading="loading" :model="form" :rules="rules" label-width="100px">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item :label="$t('ui.data.column.cd90ScheduleResult.factoryCode')" prop="factoryCode">
            <el-select v-model="form.factoryCode" disabled style="width:100%">
              <el-option v-for="item in factoryOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="$t('ui.data.column.cd90ScheduleResult.scheduleDate')" prop="scheduleDate">
            <el-date-picker v-model="form.scheduleDate" type="date" value-format="yyyy-MM-dd" disabled style="width:100%" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item :label="'原' + $t('ui.data.column.cd90ScheduleResult.machineCode')" prop="sourceMachineCode">
            <el-input v-model="form.sourceMachineCode" disabled />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="'目标' + $t('ui.data.column.cd90ScheduleResult.machineCode')" prop="targetMachineCode">
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
          <el-form-item :label="$t('ui.data.column.cd90ScheduleResult.clothCode')" prop="clothCode">
            <el-input v-model="form.clothCode" disabled />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="$t('ui.data.column.cd90ScheduleResult.remark')">
            <el-input v-model="form.remark" maxlength="900" show-word-limit />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="转出班次">
        <el-table :data="shiftRows" border size="small" style="width:100%">
          <el-table-column label="状态" width="90" align="center">
            <template slot-scope="scope">
              <el-tag v-if="isTransferClass(scope.row)" size="mini" type="success">可转</el-tag>
              <el-tag v-else size="mini" type="info">不可转</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="$t('ui.data.column.cd90ScheduleResult.shiftName')" min-width="130">
            <template slot-scope="scope">
              <span>{{ scope.row.shiftName }} {{ scope.row.classField }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('ui.data.column.cd90ScheduleResult.planQty')" width="130" align="right">
            <template slot-scope="scope">
              <span>{{ scope.row.planQty || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('ui.data.column.cd90ScheduleResult.produceOrder')" width="180" align="center">
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
      <el-button @click="hide">{{ $t('common.button.cancel') }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">{{ $t('common.button.confirm') }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import { getCd90MachineEnableOptions } from '@/api/cd90/cd90MachineInfo'
import { shiftDates, validateTransferMachine, transferMachine } from '@/api/cd90/scheduleResult'

const DEFAULT_FORM = () => ({
  factoryCode: '116',
  scheduleDate: '',
  sourceMachineCode: '',
  targetMachineCode: '',
  clothCode: '',
  startClassField: '',
  remark: ''
})

const CLASS_FIELDS = ['CLASS1', 'CLASS2', 'CLASS3', 'CLASS4', 'CLASS5', 'CLASS6']
const CLASS_PLAN_FIELDS = ['class1PlanQty', 'class2PlanQty', 'class3PlanQty', 'class4PlanQty', 'class5PlanQty', 'class6PlanQty']
const CLASS_ORDER_FIELDS = ['class1ProduceOrder', 'class2ProduceOrder', 'class3ProduceOrder', 'class4ProduceOrder', 'class5ProduceOrder', 'class6ProduceOrder']
const SHIFT_KEYS = ['middleShift', 'nightShift', 'morningShift', 'middleShift', 'nightShift', 'morningShift']

export default {
  name: 'Cd90ChangeMachineDialog',
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
        scheduleDate: this.currentRow.scheduleDate || '',
        sourceMachineCode: this.currentRow.machineCode || '',
        clothCode: this.currentRow.clothCode || '',
        startClassField: ''
      }
      this.visible = true
      await this.loadShiftDates()
      await this.loadMachines()
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
          planQty,
          currentShift: !!shiftItem.currentShift,
          transferable,
          produceOrder: transferable ? (row && row[CLASS_ORDER_FIELDS[index]] ? Number(row[CLASS_ORDER_FIELDS[index]]) : 1) : undefined
        }
      })
    },
    async loadShiftDates() {
      const response = await shiftDates({ factoryCode: this.form.factoryCode, scheduleDate: this.form.scheduleDate })
      const rows = Array.isArray(response) ? response : (response.data || [])
      const firstEditable = rows.find(item => item.changeQtyEditable)
      this.editableFromClassIndex = firstEditable ? this.getClassIndex(firstEditable.classField) + 1 : 7
      this.form.startClassField = this.editableFromClassIndex <= 6 ? CLASS_FIELDS[this.editableFromClassIndex - 1] : ''
      this.shiftRows = this.buildShiftRows(this.currentRow, rows)
    },
    getClassIndex(classField) {
      return CLASS_FIELDS.indexOf(classField)
    },
    isTransferClass(row) {
      return !!(row && row.transferable)
    },
    async loadMachines() {
      const res = await getCd90MachineEnableOptions({ factoryCode: this.form.factoryCode })
      const rows = Array.isArray(res) ? res : (res.rows || res.data || [])
      this.machineOptions = rows.map(item => ({ label: item.machineCode, value: item.machineCode }))
    },
    handleConfirm() {
      this.$refs.form.validate(async valid => {
        if (!valid) {
          return
        }
        if (this.form.sourceMachineCode === this.form.targetMachineCode) {
          this.$modal.msgWarning('原机台和目标机台不能相同')
          return
        }
        if (!this.shiftRows.some(row => this.isTransferClass(row))) {
          this.$modal.msgWarning('当前班次及后续没有可转走的帘布计划')
          return
        }
        if (this.shiftRows.some(row => this.isTransferClass(row) && !row.produceOrder)) {
          this.$modal.msgWarning('转机台目标顺序不能为空')
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
        let response = this.normalizeResponse(await transferMachine({ ...params, confirmed: false }))
        if (response.needConfirm) {
          this.loading = false
          try {
            await this.$confirm(this.buildCarryoverConfirmHtml(response.carryoverDetails, response.msg), '转机台顺延确认', {
              dangerouslyUseHTMLString: true,
              type: 'warning',
              confirmButtonText: this.$t('common.button.confirm'),
              cancelButtonText: this.$t('common.button.cancel')
            })
          } catch (error) {
            return
          }
          this.loading = true
          response = this.normalizeResponse(await transferMachine({ ...params, confirmed: true }))
        }
        this.$modal.msgSuccess(response.msg || this.$t('common.message.operationSuccess'))
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
        needConfirm: !!(data.needConfirm || (result && result.needConfirm)),
        carryoverDetails: data.carryoverDetails || (result && result.carryoverDetails) || []
      }
    },
    buildCarryoverConfirmHtml(details, fallbackMsg) {
      let html = '<div style="max-height:55vh;overflow:auto;text-align:left;">'
      if (fallbackMsg) {
        html += '<div style="margin-bottom:12px;color:#606266;">' + this.escapeHtml(fallbackMsg) + '</div>'
      }
      const carryoverDetails = details || []
      carryoverDetails.forEach(item => {
        const targetClass = item.targetClassField || '窗口结束'
        html += '<div style="padding:10px 0;border-top:1px solid #EBEEF5;font-size:13px;line-height:1.8;color:#606266;">'
        html += '<b>' + this.escapeHtml(item.clothCode || '-') + '</b> '
        html += this.escapeHtml(item.sourceClassField || '-') + ' -> ' + this.escapeHtml(targetClass)
        html += '，顺延量 ' + this.escapeHtml(item.carryoverQty)
        html += '<br/><span style="color:#E6A23C;">' + this.escapeHtml(item.reasonMessage || item.reasonCode || '') + '</span>'
        html += '</div>'
      })
      html += '</div>'
      return html
    },
    escapeHtml(value) {
      const characters = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }
      return String(value == null ? '' : value).replace(/[&<>"']/g, character => characters[character])
    }
  }
}
</script>