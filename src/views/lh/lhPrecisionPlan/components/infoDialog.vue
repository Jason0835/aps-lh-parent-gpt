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
import { checkLhPrecisionPlanUnique, saveLhPrecisionPlan } from '@/api/lh/lhPrecisionPlan'
import { listMachine } from '@/api/lh/machine'
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
      yearList: [],
      rules: {
        factoryCode: [{ required: true, message: this.$t('common.rule.select'), trigger: 'change' }],
        year: [{ required: true, message: this.$t('common.rule.select'), trigger: 'change' }],
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
          prop: 'factoryCode',
          label: this.$t('common.factory'),
          type: 'select',
          dictData: this.dict.type.biz_factory_name,
          filterable: true,
          required: true
        },
        {
          prop: 'year',
          label: this.$t('ui.lh.precision.plan.year'),
          type: 'select',
          dictData: this.yearList,
          props: {
            label: 'label',
            value: 'value'
          },
          required: true
        },
        {
          prop: 'machineCode',
          label: this.$t('ui.lh.precision.plan.machine.code'),
          type: 'select',
          dictData: this.machineList,
          filterable: true,
          required: true
        },
        {
          prop: 'precisionType',
          label: this.$t('ui.lh.precision.plan.precision.type'),
          type: 'select',
          dictData: this.dict.type.lh_precision_type,
          filterable: true,
          disabled: true,
          required: true
        },
        {
          prop: 'planDate',
          label: this.$t('ui.lh.precision.plan.plan.date'),
          type: 'date',
          valueFormat: 'yyyy-MM-dd',
          required: true
        },
        {
          prop: 'actualDate',
          label: this.$t('ui.lh.precision.plan.actual.date'),
          type: 'date',
          valueFormat: 'yyyy-MM-dd',
          required: true
        },
        {
          prop: 'dataSource',
          label: this.$t('ui.lh.precision.plan.data.source'),
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
    initYearList() {
      const currentYear = new Date().getFullYear()
      const years = []
      for (let i = currentYear - 2; i <= currentYear + 2; i++) {
        years.push({
          label: i.toString(),
          value: i.toString()
        })
      }
      this.yearList = years
    },
    show(row) {
      this.visible = true
      this.machineList = []
      this.initYearList()
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
        const res = await listMachine(this.form.factoryCode ? { factoryCode: this.form.factoryCode } : {})
        const list = res.rows || []
        const map = new Map()
        // 如果是编辑模式，先将当前选中的硫化机加入列表
        if (this.isEdit && this.form.machineCode) {
          map.set(this.form.machineCode, {
            label: this.form.machineCode,
            value: this.form.machineCode
          })
        }
        // 再将接口返回的硫化机加入列表
        list.forEach((item) => {
          if (item && item.machineCode) {
            map.set(item.machineCode, {
              label: item.machineCode,
              value: item.machineCode
            })
          }
        })
        this.machineList = Array.from(map.values())
      } catch (e) {
        this.machineList = []
        console.error(e)
      }
    },
    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save)
    },
    async save(payload) {
      payload.dataSource = payload.dataSource || '1'
      const uniqueRes = await checkLhPrecisionPlanUnique(payload)
      if (uniqueRes === '1') {
        this.$modal.msgError(this.$t('ui.data.alert.lhPrecisionPlan.notUnique'))
        return
      }
      try {
        this.loading = true
        const res = await saveLhPrecisionPlan(payload)
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
