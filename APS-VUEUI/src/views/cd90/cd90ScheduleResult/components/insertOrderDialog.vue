<template>
  <el-dialog
    :title="$t('ui.data.column.scheduleResult.insertOrder')"
    :visible.sync="visible"
    width="860px"
    append-to-body
    :close-on-click-modal="false"
    @close="hide"
  >
    <el-form ref="form" :model="form" :rules="rules" label-width="100px" v-loading="loading">
      <el-row :gutter="16">
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
        :empty-text="loading ? $t('common.message.loading') : $t('common.message.noData')"
      >
        <el-table-column :label="$t('ui.data.column.scheduleResult.shiftName')" min-width="160">
          <template slot-scope="scope">
            <span>{{ scope.row.shiftName }}</span>
            <span v-if="scope.row.shiftDate" style="margin-left:4px;color:#909399;">{{ scope.row.shiftDate }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('ui.data.column.scheduleResult.produceOrder')" width="150" align="center">
          <template slot-scope="scope">
            <el-input-number
              v-model="scope.row.produceOrder"
              :min="1"
              :precision="0"
              controls-position="right"
              style="width:120px"
              :placeholder="$t('common.placeholder.input')"
            />
          </template>
        </el-table-column>
        <el-table-column :label="$t('ui.data.column.scheduleResult.planQty')" width="170" align="center">
          <template slot-scope="scope">
            <el-input-number
              v-model="scope.row.planQty"
              :min="0"
              :precision="3"
              controls-position="right"
              style="width:140px"
              :placeholder="$t('common.placeholder.input')"
            />
          </template>
        </el-table-column>
        <el-table-column :label="$t('ui.data.column.scheduleResult.analysis')" min-width="220">
          <template slot-scope="scope">
            <el-input v-model="scope.row.analysisInput" maxlength="500" show-word-limit />
          </template>
        </el-table-column>
      </el-table>

      <!-- 填写提示 -->
      <div v-if="shiftRows.length > 0" style="margin-top:6px;font-size:12px;color:#909399;">
        {{ $t('common.tip.atLeastOneShift') || '至少填写一班次的生产顺序和计划数量' }}
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
        // 至少有一行同时填写了 produceOrder 和 planQty > 0
        const hasPlan = this.shiftRows.some(
          item => item.planQty > 0 && item.produceOrder > 0
        )
        if (!hasPlan) {
          this.$modal.msgWarning(
            this.$t('common.tip.atLeastOneShift') || '请至少填写一个班次的生产顺序和计划数量'
          )
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
