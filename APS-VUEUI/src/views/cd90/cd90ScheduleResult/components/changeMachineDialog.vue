<template>
  <el-dialog
    title="转机台"
    :visible.sync="visible"
    width="760px"
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
          <el-form-item label="开始班次" prop="startClassField">
            <el-select v-model="form.startClassField" filterable style="width:100%">
              <el-option
                v-for="item in classOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item :label="$t('ui.data.column.cd90ScheduleResult.remark')">
        <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="900" show-word-limit />
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
import { validateTransferMachine, transferMachine } from '@/api/cd90/scheduleResult'

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
      machineOptions: [],
      rules: {
        factoryCode: [{ required: true, message: this.$t('common.rule.select'), trigger: 'change' }],
        scheduleDate: [{ required: true, message: this.$t('common.rule.select'), trigger: 'change' }],
        sourceMachineCode: [{ required: true, message: this.$t('common.rule.select'), trigger: 'change' }],
        targetMachineCode: [{ required: true, message: this.$t('common.rule.select'), trigger: 'change' }],
        clothCode: [{ required: true, message: this.$t('common.rule.select'), trigger: 'change' }],
        startClassField: [{ required: true, message: this.$t('common.rule.select'), trigger: 'change' }]
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
    },
    classOptions() {
      return CLASS_FIELDS.map((classField, index) => ({
        value: classField,
        label: `${this.$t(`ui.data.column.scheduleResult.${SHIFT_KEYS[index]}`)} ${classField}`
      }))
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
        startClassField: this.getDefaultStartClass(this.currentRow)
      }
      this.visible = true
      await this.loadMachines()
    },
    hide() {
      this.visible = false
      this.loading = false
      this.form = DEFAULT_FORM()
      this.currentRow = null
      this.machineOptions = []
      if (this.$refs.form) {
        this.$refs.form.resetFields()
      }
    },
    getDefaultStartClass(row) {
      const index = CLASS_PLAN_FIELDS.findIndex(field => Number(row && row[field]) > 0)
      return index >= 0 ? CLASS_FIELDS[index] : 'CLASS1'
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
        await this.submit()
      })
    },
    async submit() {
      this.loading = true
      try {
        const params = { ...this.form }
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
