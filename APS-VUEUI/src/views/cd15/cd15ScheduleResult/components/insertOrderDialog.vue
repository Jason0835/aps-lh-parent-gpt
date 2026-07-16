<template>
  <el-dialog
    :title="$t('ui.data.column.cd15ScheduleResult.insertOrder')"
    :visible.sync="visible"
    width="1000px"
    append-to-body
    :close-on-click-modal="false"
    @close="hide"
  >
    <el-form ref="form" v-loading="loading" :model="form" :rules="rules" label-width="100px">
      <el-row :gutter="16" style="margin-bottom:20px;">
        <el-col :span="8">
          <el-form-item :label="$t('ui.data.column.cd15ScheduleResult.factoryCode')" prop="factoryCode">
            <el-select v-model="form.factoryCode" filterable disabled style="width:100%">
              <el-option v-for="item in factoryOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item :label="$t('ui.data.column.cd15ScheduleResult.scheduleDate')" prop="scheduleDate">
            <el-date-picker v-model="form.scheduleDate" type="date" value-format="yyyy-MM-dd" style="width:100%" disabled />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item :label="$t('ui.data.column.cd15ScheduleResult.machineCode')" prop="machineCode">
            <el-select v-model="form.machineCode" filterable style="width:100%">
              <el-option v-for="item in machineOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16" style="margin-bottom:18px;">
        <el-col :span="12">
          <el-form-item :label="$t('ui.data.column.cd15ScheduleResult.steelStripCode')" prop="steelStripCode">
            <el-select v-model="form.steelStripCode" filterable style="width:100%">
              <el-option v-for="item in steelStripOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="$t('ui.common.column.remark')">
            <el-input v-model="form.remark" maxlength="900" show-word-limit />
          </el-form-item>
        </el-col>
      </el-row>

      <el-table
        :data="shiftRows"
        border
        size="small"
        style="width:100%"
        :empty-text="$t('common.api.role.tips.loding')"
      >
        <el-table-column :label="$t('ui.data.column.cd90ScheduleResult.shiftName')" width="120">
          <template slot-scope="scope">
            <span>{{ scope.row.shiftName }}<span v-if="scope.row.shiftDate" style="color:#909399;">{{ formatDateMMDD(scope.row.shiftDate) }}</span></span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('ui.data.column.scheduleResult.produceOrder')" width="150" align="center">
          <template slot-scope="scope">
            <el-input-number v-model="scope.row.produceOrder" :min="1" :precision="0" controls-position="right" style="width:120px" />
          </template>
        </el-table-column>
        <el-table-column :label="$t('ui.data.column.scheduleResult.plan')" width="170" align="center">
          <template slot-scope="scope">
            <el-input-number v-model="scope.row.planQty" :min="0" :precision="2" controls-position="right" style="width:140px" />
          </template>
        </el-table-column>
        <el-table-column :label="$t('ui.data.column.scheduleResult.analysis')" min-width="220">
          <template slot-scope="scope">
            <el-input v-model="scope.row.analysisInput" maxlength="500" show-word-limit />
          </template>
        </el-table-column>
      </el-table>
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
import moment from 'moment'
import { getCd15MachineEnableOptions } from '@/api/cd15/cd15MachineInfo'
import { listSteelStripCodes } from '@/api/cd15/specifyMachine'
import { validateInsert as validateInsertOrder, insert as insertOrder } from '@/api/cd15/scheduleResult'

const CLASS_PLAN_FIELDS = ['class1PlanQty', 'class2PlanQty', 'class3PlanQty', 'class4PlanQty', 'class5PlanQty', 'class6PlanQty']
const CLASS_ORDER_FIELDS = ['class1ProduceOrder', 'class2ProduceOrder', 'class3ProduceOrder', 'class4ProduceOrder', 'class5ProduceOrder', 'class6ProduceOrder']
const CLASS_ANALYSIS_FIELDS = ['class1AnalysisInput', 'class2AnalysisInput', 'class3AnalysisInput', 'class4AnalysisInput', 'class5AnalysisInput', 'class6AnalysisInput']
const SHIFT_CONFIG = [
  { shiftKey: 'middleShift', dayOffset: -1 },
  { shiftKey: 'nightShift', dayOffset: 0 },
  { shiftKey: 'morningShift', dayOffset: 0 },
  { shiftKey: 'middleShift', dayOffset: 0 },
  { shiftKey: 'nightShift', dayOffset: 1 },
  { shiftKey: 'morningShift', dayOffset: 1 }
]
const DEFAULT_FORM = () => ({
  factoryCode: '',
  scheduleDate: '',
  machineCode: '',
  steelStripCode: '',
  remark: ''
})

export default {
  name: 'Cd15InsertOrderDialog',
  inject: ['parentDict'],
  data() {
    return {
      visible: false,
      loading: false,
      form: DEFAULT_FORM(),
      shiftRows: [],
      machineOptions: [],
      steelStripOptions: [],
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
    async show(data) {
      this.visible = true
      this.form = {
        ...DEFAULT_FORM(),
        factoryCode: (data && data.factoryCode) || '116',
        scheduleDate: (data && data.scheduleDate) || moment().add(1, 'days').format('YYYY-MM-DD')
      }
      this.buildShiftRows()
      await Promise.all([this.loadMachines(), this.loadSteelStrips()])
    },
    hide() {
      this.visible = false
      this.loading = false
      this.form = DEFAULT_FORM()
      this.shiftRows = []
      this.machineOptions = []
      this.steelStripOptions = []
      if (this.$refs.form) this.$refs.form.resetFields()
    },
    formatDateMMDD(dateStr) {
      if (!dateStr) return ''
      return moment(dateStr).format('MM/DD')
    },
    buildShiftRows() {
      this.shiftRows = SHIFT_CONFIG.map((item, index) => ({
        classField: `CLASS${index + 1}`,
        shiftName: this.$t(`ui.data.column.scheduleResult.${item.shiftKey}`),
        shiftDate: moment(this.form.scheduleDate).add(item.dayOffset, 'days').format('YYYY-MM-DD'),
        produceOrder: undefined,
        planQty: undefined,
        analysisInput: ''
      }))
    },
    async loadMachines() {
      const res = await getCd15MachineEnableOptions({ factoryCode: this.form.factoryCode })
      const rows = Array.isArray(res) ? res : (res.rows || res.data || [])
      this.machineOptions = rows.map(item => ({
        label: item.machineCode || item.label || item.value,
        value: item.machineCode || item.value
      }))
    },
    async loadSteelStrips() {
      const res = await listSteelStripCodes()
      const rows = Array.isArray(res) ? res : (res.rows || res.data || [])
      this.steelStripOptions = rows.map(item => ({
        label: item.label || item.steelStripCode || item.code || item,
        value: item.value || item.steelStripCode || item.code || item
      }))
    },
    handleConfirm() {
      this.$refs.form.validate(async valid => {
        if (!valid) return
        const hasPlan = this.shiftRows.some(item => item.planQty > 0 && item.produceOrder > 0)
        if (!hasPlan) {
          this.$modal.msgWarning(this.$t('ui.dj.schedule.validate.atLeastOneShiftQty'))
          return
        }
        await this.submit()
      })
    },
    buildRequest() {
      const params = { ...this.form }
      this.shiftRows.forEach((item, index) => {
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
        const result = await insertOrder({ ...params, confirmed: false })
        const data = (result && result.data) ? result.data : (result || {})
        this.$modal.msgSuccess((result && result.msg) || this.$t('common.message.operationSuccess'))
        this.$emit('success', params.scheduleDate, data)
        this.hide()
      } finally {
        this.loading = false
      }
    }
  }
}
</script>