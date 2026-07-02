<template>
  <el-dialog
    :title="$t('ui.data.column.scheduleResult.insertOrder')"
    :visible.sync="visible"
    width="820px"
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
            <el-select v-model="form.machineCode" filterable style="width:100%">
              <el-option v-for="item in machineOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item :label="$t('ui.data.column.cd90ScheduleResult.clothCode')" prop="clothCode">
            <el-select v-model="form.clothCode" filterable style="width:100%">
              <el-option v-for="item in clothOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="$t('ui.data.column.cd90ScheduleResult.remark')">
            <el-input v-model="form.remark" maxlength="900" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-table :data="shiftRows" border size="small">
        <el-table-column :label="$t('ui.data.column.scheduleResult.shiftName')" min-width="150">
          <template slot-scope="scope">{{ scope.row.shiftName }} {{ scope.row.shiftDate }}</template>
        </el-table-column>
        <el-table-column :label="$t('ui.data.column.scheduleResult.produceOrder')" width="150">
          <template slot-scope="scope">
            <el-input-number v-model="scope.row.produceOrder" :min="1" :precision="0" controls-position="right" style="width:120px" />
          </template>
        </el-table-column>
        <el-table-column :label="$t('ui.data.column.scheduleResult.planQty')" width="170">
          <template slot-scope="scope">
            <el-input-number v-model="scope.row.planQty" :min="0" :precision="3" controls-position="right" style="width:140px" />
          </template>
        </el-table-column>
        <el-table-column :label="$t('ui.data.column.scheduleResult.analysis')" min-width="220">
          <template slot-scope="scope"><el-input v-model="scope.row.analysisInput" maxlength="500" /></template>
        </el-table-column>
      </el-table>
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

export default {
  name: 'Cd90InsertOrderDialog',
  inject: ['parentDict'],
  data() {
    return {
      visible: false,
      loading: false,
      form: {},
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
        factoryCode: (data && data.factoryCode) || '116',
        scheduleDate: data && data.scheduleDate
      }
      await Promise.all([this.loadMachines(), this.loadCloths(), this.loadShiftDates()])
    },
    hide() {
      this.visible = false
      this.loading = false
      this.form = {}
      this.shiftRows = []
      if (this.$refs.form) this.$refs.form.resetFields()
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
        shiftDate: item.shiftDate,
        produceOrder: null,
        planQty: null,
        analysisInput: ''
      }))
    },
    handleConfirm() {
      this.$refs.form.validate(async valid => {
        if (!valid) return
        const hasPlan = this.shiftRows.some(item => item.planQty > 0 && item.produceOrder > 0)
        if (!hasPlan) {
          this.$modal.msgWarning(this.$t('common.rule.input'))
          return
        }
        await this.submit()
      })
    },
    buildRequest() {
      const params = { ...this.form }
      this.shiftRows.forEach((item, index) => {
        params[CLASS_PLAN_FIELDS[index]] = item.planQty
        params[CLASS_ORDER_FIELDS[index]] = item.produceOrder
        params[CLASS_ANALYSIS_FIELDS[index]] = item.analysisInput
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
