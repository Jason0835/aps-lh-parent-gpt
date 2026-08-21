<template>
  <el-dialog
    :append-to-body="true"
    :close-on-press-escape="false"
    :title="title"
    :close-on-click-modal="false"
    :visible="visible"
    width="1000px"
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
      label-width="160px"
    />
    <template slot="footer">
      <el-button @click="hide">{{ $t('common.button.cancel') }}</el-button>
      <el-button :loading="loading" type="primary" @click="handleConfirm">
        {{ $t('common.button.confirm') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script>
import InfoForm from '@/views/components/infoForm.vue'
import {changeQty} from '@/api/tc/tcScheduleResult'
import {resolveErrorMessage} from '@/utils/errorMessage'

const MAX_SHIFT_ORDER = 6
const CHANGE_QTY_EDITABLE_FIELDS = Array.from({ length: MAX_SHIFT_ORDER }, (item, index) => {
  const shiftOrder = index + 1
  return [`class${shiftOrder}PlanQty`, `class${shiftOrder}Analysis`]
}).reduce((fieldList, shiftFields) => fieldList.concat(shiftFields), [])

export default {
  name: 'TcChangeQtyDialog',
  components: { InfoForm },
  inject: ['parentDict'],
  data() {
    return {
      loading: false,
      visible: false,
      form: {},
      originalForm: {},
      rules: {
        remark: [
          {
            required: true,
            message: this.$t('ui.tc.schedule.reasonRequired'),
            trigger: 'blur'
          }
        ]
      }
    }
  },
  computed: {
    title() {
      return `${this.$t('ui.data.column.scheduleResult.changePlan')}${this.$t('ui.data.column.tc.scheduleResult.modelName')}`
    },
    columns() {
      const columns = [
        {
          prop: 'factoryCode',
          label: this.$t('ui.data.column.tm.scheduleResult.factoryCode'),
          type: 'select',
          span: 12,
          dictData: this.parentDict.type.biz_factory_name,
          filterable: true
        },
        {
          prop: 'batchNo',
          label: this.$t('ui.data.column.tm.scheduleResult.batchNo'),
          span: 12
        },
        {
          prop: 'orderNo',
          label: this.$t('ui.data.column.tm.scheduleResult.orderNo'),
          span: 12
        },
        {
          prop: 'scheduleDate',
          label: this.$t('ui.data.column.tm.scheduleResult.scheduleDate'),
          type: 'date',
          span: 12,
          valueFormat: 'yyyy-MM-dd'
        },
        {
          prop: 'machineCode',
          label: this.$t('ui.data.column.tm.scheduleResult.machineCode'),
          span: 12
        },
        {
          prop: 'sidewallCode',
          label: this.$t('ui.data.column.tc.scheduleResult.sidewallCode'),
          span: 12,
          maxlength: 50
        },
        {
          prop: 'sidewallCraft',
          label: this.$t('ui.tc.schedule.sidewallCraft'),
          span: 12
        },
        {
          prop: 'glueCode',
          label: this.$t('ui.data.column.tm.scheduleResult.glueCode'),
          span: 12,
          maxlength: 50
        },
        {
          prop: 'baseGlueCode',
          label: this.$t('ui.tc.schedule.baseGlueCode'),
          span: 12,
          maxlength: 50
        },
        {
          prop: 'wholeGlueCode',
          label: this.$t('ui.data.column.tm.scheduleResult.wholeGlueCode'),
          span: 12,
          maxlength: 100
        },
        {
          prop: 'glueSeq',
          label: this.$t('ui.data.column.tm.scheduleResult.glueSeq'),
          span: 12,
          maxlength: 50
        },
        {
          prop: 'mouthPlateCode',
          label: this.$t('ui.data.column.tm.scheduleResult.mouthPlateCode'),
          span: 12,
          maxlength: 50
        },
        {
          prop: 'releaseStatus',
          label: this.$t('ui.data.column.tm.scheduleResult.releaseStatus'),
          type: 'select',
          span: 12,
          dictData: this.parentDict.type.IS_RELEASE,
          filterable: true
        },
        {
          prop: 'dataSource',
          label: this.$t('ui.data.column.tm.scheduleResult.dataSource'),
          span: 12
        },
        {
          prop: 'tailFlag',
          label: this.$t('ui.data.column.tm.scheduleResult.tailFlag'),
          span: 12
        }
      ]

      for (let shiftOrder = 1; shiftOrder <= MAX_SHIFT_ORDER; shiftOrder += 1) {
        columns.push(
          {
            label: this.$t(`ui.data.column.tm.scheduleResult.class${shiftOrder}Sequence`),
            span: 24,
            type: 'group'
          },
          {
            prop: `class${shiftOrder}Sequence`,
            label: this.$t(`ui.data.column.tm.scheduleResult.class${shiftOrder}Sequence`),
            span: 8,
            type: 'number'
          },
          {
            prop: `class${shiftOrder}StartTime`,
            label: this.$t(`ui.data.column.tm.scheduleResult.class${shiftOrder}StartTime`),
            span: 8,
            type: 'date',
            dateType: 'datetime',
            valueFormat: 'yyyy-MM-dd HH:mm:ss'
          },
          {
            prop: `class${shiftOrder}EndTime`,
            label: this.$t(`ui.data.column.tm.scheduleResult.class${shiftOrder}EndTime`),
            span: 8,
            type: 'date',
            dateType: 'datetime',
            valueFormat: 'yyyy-MM-dd HH:mm:ss'
          },
          {
            prop: `class${shiftOrder}PlanQty`,
            label: this.$t(`ui.data.column.tm.scheduleResult.class${shiftOrder}PlanQty`),
            span: 8,
            type: 'number',
            min: 0
          },
          {
            prop: `class${shiftOrder}FinishQty`,
            label: this.$t(`ui.data.column.tm.scheduleResult.class${shiftOrder}FinishQty`),
            span: 8,
            type: 'number'
          },
          {
            prop: `class${shiftOrder}Analysis`,
            label: this.$t(`ui.data.column.tm.scheduleResult.class${shiftOrder}Analysis`),
            span: 8,
            maxlength: 200
          }
        )
      }

      columns.push({
        prop: 'remark',
        label: this.$t('ui.data.column.tm.scheduleResult.remark'),
        span: 24,
        type: 'textarea',
        rows: 3,
        maxlength: 500
      })

      return columns.map(column => ({
        ...column,
        disabled: column.prop !== 'remark' && !CHANGE_QTY_EDITABLE_FIELDS.includes(column.prop)
      }))
    }
  },
  methods: {
    /**
     * 打开胎侧修改表单并回填排程结果数据。
     *
     * @param {Object} row 待修改的胎侧排程结果
     * @returns {void}
     */
    show(row) {
      this.visible = true
      this.form = {
        ...row,
        remark: ''
      }
      this.originalForm = { ...row }
      this.$nextTick(() => this.$refs.form && this.$refs.form.triggerResetForm())
    },
    /**
     * 关闭修改弹窗并清理表单状态。
     *
     * @returns {void}
     */
    hide() {
      this.form = {}
      this.originalForm = {}
      if (this.$refs.form) {
        this.$refs.form.triggerResetForm()
      }
      this.visible = false
    },
    /**
     * 比较表单与原始排程结果，定位本次修改的唯一班次。
     *
     * @returns {Array<number>} 发生计划量或原因分析变化的班次
     */
    getChangedShiftOrders() {
      return Array.from({ length: MAX_SHIFT_ORDER }, (item, index) => index + 1)
        .filter(shiftOrder => {
          const fieldPrefix = `class${shiftOrder}`
          const oldPlanQty = Number(this.originalForm[`${fieldPrefix}PlanQty`] || 0)
          const newPlanQty = Number(this.form[`${fieldPrefix}PlanQty`] || 0)
          const oldAnalysis = this.originalForm[`${fieldPrefix}Analysis`] || ''
          const newAnalysis = this.form[`${fieldPrefix}Analysis`] || ''
          return oldPlanQty !== newPlanQty || oldAnalysis !== newAnalysis
        })
    },
    /**
     * 校验计划量不低于已完成量，并提交 TC 单班次调量请求。
     *
     * @param {Object} params 已通过表单校验的修改参数
     * @returns {Promise<void>} 提交完成
     */
    async save(params) {
      const changedShiftOrders = this.getChangedShiftOrders()
      if (changedShiftOrders.length !== 1) {
        this.$modal.alertWarning(this.$t('ui.tc.schedule.changeQty.singleShiftOnly'))
        return
      }

      const shiftOrder = changedShiftOrders[0]
      const finishQty = Number(this.form[`class${shiftOrder}FinishQty`] || 0)
      const newPlanQty = Number(params[`class${shiftOrder}PlanQty`] || 0)
      if (newPlanQty < finishQty) {
        this.$modal.alertWarning(this.$t('ui.tc.schedule.qtyBelowFinish'))
        return
      }

      try {
        this.loading = true
        const task = await changeQty({
          resultId: this.form.id,
          shiftOrder,
          newPlanQty,
          newAnalysis: params[`class${shiftOrder}Analysis`] || '',
          expectedTaskVersion: this.form.currentTaskVersion == null
            ? this.form.taskVersion
            : this.form.currentTaskVersion,
          reason: params.remark
        })
        this.$emit('success', task)
        this.hide()
      } catch (error) {
        this.$modal.alertError(resolveErrorMessage(
          error,
          this.$t('ui.tc.schedule.operationFailed')
        ))
      } finally {
        this.loading = false
      }
    },
    /**
     * 执行表单校验并提交修改。
     *
     * @returns {void}
     */
    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save)
    }
  }
}
</script>
