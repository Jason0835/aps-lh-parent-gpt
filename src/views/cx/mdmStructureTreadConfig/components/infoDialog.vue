<template>
  <el-dialog
    :title="dialogTitle"
    :visible="visible"
    width="720px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    append-to-body
  >
    <info-form
      class="form-item-height"
      ref="form"
      :form="form"
      :rules="rules"
      :columns="columns"
      label-position="right"
      label-width="160px"
      v-loading="loading"
    />
    <template #footer>
      <el-button @click="hide">{{ $t('common.button.cancel') }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">
        {{ $t('common.button.confirm') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script>
import infoForm from '@/views/components/infoForm.vue'
import { addMdmStructureTreadConfig, editMdmStructureTreadConfig } from '@/api/cx/mdmStructureTreadConfig'

export default {
  components: { infoForm },
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      form: {},
      rules: {
        stockDate: [{ required: true, message: this.$t('common.rule.select'), trigger: 'blur' }],
        structureCode: [{ required: true, message: this.$t('common.rule.input'), trigger: 'blur' }],
        treadCount: [{ required: true, message: this.$t('common.rule.input'), trigger: 'blur' }],
        factoryCode: [{ required: true, message: this.$t('common.rule.select'), trigger: 'change' }]
      }
    }
  },
  computed: {
    dialogTitle() {
      return (this.isEdit ? this.$t('common.button.edit') : this.$t('common.button.add')) + this.$t('ui.data.column.mdmStructureTreadConfig.modelName')
    },
    columns() {
      return [
        {
          label: this.$t('ui.data.column.mdmStructureTreadConfig.stockDate'),
          prop: 'stockDate',
          span: 24,
          required: true,
          type: 'date',
          valueFormat: 'yyyy-MM-dd',
          disabled: this.isEdit
        },
        {
          label: this.$t('ui.data.column.mdmStructureTreadConfig.structureCode'),
          prop: 'structureCode',
          span: 24,
          required: true,
          maxlength: 50
        },
        {
          label: this.$t('ui.data.column.mdmStructureTreadConfig.treadCount'),
          prop: 'treadCount',
          span: 24,
          type: 'number',
          min: 1,
          max: 999,
          precision: 0,
          required: true
        },
        {
          label: this.$t('ui.data.column.mdmStructureTreadConfig.factoryCode'),
          prop: 'factoryCode',
          span: 24,
          type: 'select',
          dictType: 'biz_factory_name',
          filterable: true,
          required: true
        },
        {
          label: this.$t('ui.data.column.mdmStructureTreadConfig.dataVersion'),
          prop: 'dataVersion',
          span: 24,
          maxlength: 50
        },
        {
          label: this.$t('ui.common.column.remark'),
          prop: 'remark',
          span: 24,
          type: 'textarea',
          maxlength: 300
        }
      ]
    }
  },
  methods: {
    show(data) {
      this.visible = true
      if (data) {
        this.isEdit = true
        this.form = { ...data }
      }
    },
    hide() {
      this.form = {}
      this.$refs.form && this.$refs.form.triggerResetForm()
      this.isEdit = false
      this.visible = false
    },
    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save)
    },
    async save(payload) {
      try {
        this.loading = true
        const api = this.isEdit ? editMdmStructureTreadConfig : addMdmStructureTreadConfig
        const res = await api(payload)
        this.$modal.msgSuccess(res.msg)
        this.$emit('success')
        this.hide()
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
</style>
