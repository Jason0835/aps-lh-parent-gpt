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
        const decodedRow = {
          ...row,
          remark: this.decodeRemark(row.remark)
        }
        this.form = decodedRow
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
    // 对备注中的特殊字符进行编码，避免后端 URLDecoder/HTML 转义解析失败
    encodeRemark(remark) {
      if (!remark) return remark;
      // 将特殊字符替换为占位符，避免后端转义
      return remark
        .replace(/%/g, '__PERCENT__')
        .replace(/&/g, '__AMP__')
        .replace(/</g, '__LT__')
        .replace(/>/g, '__GT__')
        .replace(/"/g, '__QUOT__')
        .replace(/'/g, '__APOS__');
    },
    // 解码备注中的占位符
    decodeRemark(remark) {
      if (!remark) return remark;
      return remark
        .replace(/__PERCENT__/g, '%')
        .replace(/__AMP__/g, '&')
        .replace(/__LT__/g, '<')
        .replace(/__GT__/g, '>')
        .replace(/__QUOT__/g, '"')
        .replace(/__APOS__/g, "'");
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
        const saveParams = {
          id: payload.id,
          factoryCode: payload.factoryCode,
          companyCode: payload.companyCode,
          year: payload.year,
          machineCode: payload.machineCode,
          precisionType: payload.precisionType,
          planDate: payload.planDate,
          actualDate: payload.actualDate,
          remark: this.encodeRemark(payload.remark),
          dataSource: payload.dataSource,
          dataVersion: payload.dataVersion
        }
        const res = await saveLhPrecisionPlan(saveParams)
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
