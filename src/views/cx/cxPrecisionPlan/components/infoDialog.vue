<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="700px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :append-to-body="true"
  >
    <info-form
      class="form-item-height"
      ref="form"
      :form="form"
      :rules="rules"
      :columns="columns"
      label-position="right"
      label-width="140px"
      v-loading="loading"
    />
    <template slot="footer">
      <el-button @click="hide">{{ $t('common.button.cancel') }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">
        {{ $t('common.button.confirm') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script>
import { checkCxPrecisionPlanUnique, listCxMachineInfo, saveCxPrecisionPlan } from '@/api/cx/cxPrecisionPlan'
import infoForm from '@/views/components/infoForm.vue'

export default {
  name: 'InfoDialog',
  components: { infoForm },
  inject: ['parentDict'],
  data() {
    return {
      visible: false,
      loading: false,
      isEdit: false,
      dict: this.parentDict,
      form: {},
      machineList: [],
      rules: {
        machineCode: [{ required: true, message: this.$t('common.rule.select'), trigger: 'change' }],
        precisionType: [{ required: true, message: this.$t('common.rule.select'), trigger: 'change' }],
        planDate: [{ required: true, message: this.$t('common.rule.select'), trigger: 'change' }],
        actualDate: [{ required: true, message: this.$t('common.rule.select'), trigger: 'change' }],
        dataSource: [{ required: true, message: this.$t('common.rule.select'), trigger: 'change' }]
      }
    }
  },
  computed: {
    title() {
      return this.isEdit ? this.$t('common.button.edit') : this.$t('common.button.add')
    },
    columns() {
      return [
        {
          prop: 'machineCode',
          label: this.$t('ui.data.column.cxPrecisionPlan.machineCode'),
          type: 'input',
          disabled: true
        },
        {
          prop: 'precisionType',
          label: this.$t('ui.data.column.cxPrecisionPlan.accuracyType'),
          type: 'select',
          dictData: this.dict.type.cx_precision_plan_type,
          filterable: true,
          listeners: { change: this.handlePrecisionTypeChange },
          required: true
        },
        {
          prop: 'planDate',
          label: this.$t('ui.data.column.cxPrecisionPlan.planDate'),
          type: 'date',
          valueFormat: 'yyyy-MM-dd',
          listeners: { change: this.handlePlanDateChange },
          required: true
        },
        {
          prop: 'actualDate',
          label: this.$t('ui.data.column.cxPrecisionPlan.actualDate'),
          type: 'date',
          valueFormat: 'yyyy-MM-dd',
          required: true
        },
        {
          prop: 'precisionCycle',
          label: this.$t('ui.data.column.cxPrecisionPlan.cycle'),
          type: 'input',
          disabled: true
        },
        {
          prop: 'daysToDue',
          label: this.$t('ui.data.column.cxPrecisionPlan.dueDate'),
          type: 'input',
          disabled: true
        },
        // {
        //   prop: 'scheduleDate',
        //   label: this.$t('ui.data.column.cxPrecisionPlan.scheduleDate'),
        //   type: 'date',
        //   valueFormat: 'yyyy-MM-dd',
        //   disabled: true
        // },
        {
          prop: 'dataSource',
          label: this.$t('ui.data.column.lhPrecisionPlan.dataSource'),
          type: 'select',
          dictData: this.dict.type.lh_precision_data_source,
          disabled: true,
          required: true
        },
        {
          prop: 'remark',
          label: this.$t('ui.common.column.remark'),
          type: 'textarea',
          rows: 3,
          maxlength: 300
        }
      ]
    }
  },
  methods: {
    getCycleValue(precisionType) {
      const text = this.selectDictLabel(this.dict.type.cx_precision_plan_type, precisionType) || precisionType || ''
      if (text.includes('60')) {
        return '60'
      }
      if (text.includes('15')) {
        return '15'
      }
      return ''
    },
    getDaysToDueValue(planDate) {
      if (!planDate) {
        return ''
      }
      const target = new Date(planDate)
      if (Number.isNaN(target.getTime())) {
        return ''
      }
      const now = new Date()
      const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate())
      const startOfTarget = new Date(target.getFullYear(), target.getMonth(), target.getDate())
      return String(Math.floor((startOfToday.getTime() - startOfTarget.getTime()) / 86400000))
    },
    fillCalculatedFields() {
      this.$set(this.form, 'precisionCycle', this.getCycleValue(this.form.precisionType))
      this.$set(this.form, 'daysToDue', this.getDaysToDueValue(this.form.planDate))
    },
    show(row) {
      this.visible = true
      this.machineList = []
      if (row) {
        this.isEdit = true
        this.form = { ...row }
      } else {
        this.isEdit = false
        this.form = { factoryCode: '116', dataSource: '1' }
      }
      if (!this.form.dataSource) {
        this.form.dataSource = '1'
      }
      this.fillCalculatedFields()
      this.getMachineList()
    },
    hide() {
      this.visible = false
      this.form = {}
      this.machineList = []
      this.$refs.form && this.$refs.form.triggerResetForm()
    },
    async getMachineList() {
      try {
        const res = await listCxMachineInfo(this.form.factoryCode ? { factoryCode: this.form.factoryCode } : {})
        const list = Array.isArray(res) ? res : []
        const map = new Map()
        list.forEach((item) => {
          if (item && item.machineCode) {
            map.set(item.machineCode, { machineCode: item.machineCode })
          }
        })
        this.machineList = Array.from(map.values())
      } catch (e) {
        this.machineList = []
        console.error(e)
      }
    },
    handlePlanDateChange() {
      this.$set(this.form, 'daysToDue', this.getDaysToDueValue(this.form.planDate))
    },
    handlePrecisionTypeChange() {
      this.$set(this.form, 'precisionCycle', this.getCycleValue(this.form.precisionType))
    },
    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save)
    },
    async save(payload) {
      payload.dataSource = payload.dataSource || '1'
      payload.daysToDue = this.getDaysToDueValue(payload.planDate)
      payload.precisionCycle = this.getCycleValue(payload.precisionType)
      const uniqueRes = await checkCxPrecisionPlanUnique(payload)
      if (uniqueRes === '1') {
        this.$modal.msgError(this.$t('ui.data.alert.cxPrecisionPlan.notUnique'))
        return
      }
      try {
        this.loading = true
        const res = await saveCxPrecisionPlan(payload)
        this.$modal.msgSuccess(res.msg || this.$t('common.success'))
        this.$emit('success')
        this.hide()
      } finally {
        this.loading = false
      }
    }
  }
}
</script>
