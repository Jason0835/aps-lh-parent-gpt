<template>
  <el-dialog
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :title="title"
    :visible="visible"
    width="400px"
    @close="hide"
  >
    <info-form
      ref="form"
      v-loading="loading"
      :columns="columns"
      :form="form"
      :rules="rules"
      class="form-item-height"
      label-position="right"
      label-width="80px"
    />
    <template slot="footer">
      <el-button @click="hide">{{ this.$t("common.button.cancel") }}</el-button>
      <el-button :loading="loading" type="primary" @click="handleConfirm">
        {{ this.$t("common.button.confirm") }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script>
import moment from 'moment'

import infoForm from '@/views/components/infoForm.vue'
import {autoPlan, validateAutoPlan} from '@/api/tm/scheduleResult'

export default {
  components: { infoForm },
  inject: ['parentDict'],
  data() {
    return {
      loading: false,
      visible: false,
      form: {
        scheduleDate: moment().add(2, 'days').format('YYYY-MM-DD'),
        factoryCode: ''
      },
      rules: {
        scheduleDate: [
          {
            required: true,
            message: this.$t('common.rule.select'),
            trigger: 'blur'
          }
        ],
        factoryCode: [
          {
            required: true,
            message: this.$t('common.rule.select'),
            trigger: 'blur'
          }
        ]
      }
    }
  },
  computed: {
    title() {
      return this.$t('ui.data.column.scheduleResult.autoPlan')
    },
    columns() {
      return [
        {
          label: this.$t('ui.data.column.factoryCode'),
          prop: 'factoryCode',
          span: 24,
          type: 'select',
          dictData: this.parentDict.type.biz_factory_name,
          filterable: true
        },
        {
          label: this.$t('ui.data.column.scheduleResult.scheduleDate'),
          prop: 'scheduleDate',
          span: 24,
          type: 'date',
          valueFormat: 'yyyy-MM-dd'
        }
      ]
    }
  },
  methods: {
    /**
     * 展示自动排程弹窗，并带入当前查询条件中的工厂和排程日期。
     *
     * @param {String} factoryCode 工厂编码
     * @param {String} scheduleDate 排程日期
     * @returns {void}
     */
    show(factoryCode, scheduleDate) {
      this.visible = true
      this.form = {
        factoryCode: factoryCode || '',
        scheduleDate: scheduleDate || moment().add(2, 'days').format('YYYY-MM-DD')
      }
    },
    /**
     * 关闭自动排程弹窗并重置表单状态。
     *
     * @returns {void}
     */
    hide() {
      this.form = {}
      if (this.$refs.form) {
        this.$refs.form.triggerResetForm()
      }
      this.visible = false
      this.loading = false
    },
    /**
     * 表单确认入口，通过 infoForm 统一校验后提交。
     *
     * @returns {void}
     */
    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save)
    },
    /**
     * 校验旧批次状态，必要时由用户确认后再执行胎面自动排程。
     *
     * @param {Object} form 表单参数
     * @returns {Promise<void>} 自动排程提交结果
     */
    async save(form) {
      const params = {
        ...form,
        dataSource: 'AUTO'
      }
      try {
        this.loading = true
        const validateResult = await validateAutoPlan(params)
        const result = validateResult.data || {}
        if (result.confirmRequired) {
          this.$confirm(result.message, {
            type: 'warning'
          })
            .then(() => {
              this.doAutoPlan({
                ...params,
                confirmOverwrite: true
              })
            })
            .catch(() => {
              this.loading = false
            })
          return
        }
        await this.doAutoPlan(params)
      } catch (error) {
        console.error(error)
        this.loading = false
      }
    },
    /**
     * 调用胎面自动排程接口，成功后通知父页面刷新。
     *
     * @param {Object} params 自动排程参数
     * @returns {Promise<void>} 自动排程执行结果
     */
    async doAutoPlan(params) {
      try {
        const data = await autoPlan(params)
        const result = data.data || {}
        const message = result.message || data.msg
        this.$modal.msgSuccess(message)
        this.$emit('success', params.scheduleDate, result)
        this.hide()
      } catch (error) {
        console.error(error)
        this.loading = false
      }
    }
  }
}
</script>
