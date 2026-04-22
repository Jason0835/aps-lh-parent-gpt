<template>
  <el-dialog
    :title="dialogTitle"
    :visible="visible"
    width="720px"
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
      label-width="160px"
      v-loading="loading"
    >
    </info-form>
    <template slot="footer">
      <el-button @click="hide">{{ this.$t('common.button.cancel') }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">
        {{ this.$t('common.button.confirm') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script>
import { saveCxMachineOnlineInfo } from '@/api/cx/cxMachineOnlineInfo'
import infoForm from '@/views/components/infoForm.vue'

export default {
  components: { infoForm },
  inject: ['parentDict'],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      form: {},
      rules: {
        factoryCode: [
          { required: true, message: this.$t('common.rule.select'), trigger: 'change' }
        ],
        onlineDate: [
          { required: true, message: this.$t('common.rule.select'), trigger: 'change' }
        ],
        cxCode: [
          { required: true, message: this.$t('common.rule.input'), trigger: 'blur' }
        ]
      }
    }
  },
  computed: {
    dialogTitle() {
      return (
        (this.isEdit ? this.$t('common.button.edit') : this.$t('common.button.add')) +
        this.$t('ui.data.column.cxMachineOnlineInfo.modelName')
      )
    },
    columns() {
      return [
        {
          label: this.$t('ui.data.column.cxMachineOnlineInfo.factoryCode'),
          prop: 'factoryCode',
          span: 24,
          type: 'select',
          dictData: this.parentDict.type.biz_factory_name,
          filterable: true,
          required: true
        },
        {
          label: this.$t('ui.data.column.cxMachineOnlineInfo.onlineDate'),
          prop: 'onlineDate',
          span: 24,
          type: 'date',
          valueFormat: 'yyyy-MM-dd',
          required: true
        },
        {
          label: this.$t('ui.data.column.cxMachineOnlineInfo.cxCode'),
          prop: 'cxCode',
          span: 24,
          required: true,
          maxlength: 50
        },
        {
          label: this.$t('ui.data.column.cxMachineOnlineInfo.materialCode'),
          prop: 'materialCode',
          span: 24,
          maxlength: 50
        },
        {
          label: this.$t('ui.data.column.cxMachineOnlineInfo.mesMaterialCode'),
          prop: 'mesMaterialCode',
          span: 24,
          maxlength: 50
        },
        {
          label: this.$t('ui.data.column.cxMachineOnlineInfo.specDesc'),
          prop: 'specDesc',
          span: 24,
          maxlength: 200
        },
        {
          label: this.$t('ui.data.column.cxMachineOnlineInfo.embryoSpec'),
          prop: 'embryoSpec',
          span: 24,
          maxlength: 200
        },
        {
          label: this.$t('ui.data.column.cxMachineOnlineInfo.dataVersion'),
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
        const res = await saveCxMachineOnlineInfo(payload)
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

