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
import {insertTask} from '@/api/tc/tcScheduleResult'
import {resolveErrorMessage} from '@/utils/errorMessage'

const MAX_INSERT_SHIFT_ORDER = 3

/**
 * 将日期格式化为排程页面使用的日期字符串。
 *
 * @param {Date} date 待格式化日期
 * @returns {String} yyyy-MM-dd 格式日期
 */
const formatDate = date => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export default {
  name: 'TcInsertTaskDialog',
  components: { InfoForm },
  inject: ['parentDict'],
  props: {
    machineOptions: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      loading: false,
      visible: false,
      form: this.createForm(),
      rules: {
        factoryCode: [
          {
            required: true,
            message: this.$t('common.rule.select'),
            trigger: 'change'
          }
        ],
        scheduleDate: [
          {
            required: true,
            message: this.$t('common.rule.select'),
            trigger: 'change'
          }
        ],
        machineCode: [
          {
            required: true,
            message: this.$t('common.rule.select'),
            trigger: 'change'
          }
        ],
        sidewallCode: [
          {
            required: true,
            message: this.$t('common.rule.input'),
            trigger: 'blur'
          }
        ]
      }
    }
  },
  computed: {
    title() {
      return `${this.$t('ui.data.column.scheduleResult.insertOrder')}${this.$t('ui.data.column.tc.scheduleResult.modelName')}`
    },
    columns() {
      return [
        {
          prop: 'factoryCode',
          label: this.$t('ui.data.column.tm.scheduleResult.factoryCode'),
          type: 'select',
          span: 12,
          dictData: this.parentDict.type.biz_factory_name,
          filterable: true
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
          span: 12,
          type: 'select',
          dictData: this.machineOptions,
          props: {
            label: 'machineCode',
            value: 'machineCode'
          },
          filterable: true
        },
        {
          prop: 'sidewallCode',
          label: this.$t('ui.data.column.tc.scheduleResult.sidewallCode'),
          span: 12,
          maxlength: 50
        },
        {
          label: this.$t('ui.tm.schedule.insert.middleShift'),
          span: 24,
          type: 'title'
        },
        {
          prop: 'class1PlanQty',
          label: this.$t('ui.tm.schedule.insert.middlePlanQty'),
          span: 8,
          type: 'number',
          min: 0
        },
        {
          prop: 'class1Sequence',
          label: this.$t('ui.tm.schedule.insert.middleSequence'),
          span: 8,
          type: 'number',
          min: 1,
          precision: 0
        },
        {
          prop: 'class1Analysis',
          label: this.$t('ui.tm.schedule.insert.middleAnalysis'),
          span: 8,
          maxlength: 200
        },
        {
          label: this.$t('ui.tm.schedule.insert.nightShift'),
          span: 24,
          type: 'title'
        },
        {
          prop: 'class2PlanQty',
          label: this.$t('ui.tm.schedule.insert.nightPlanQty'),
          span: 8,
          type: 'number',
          min: 0
        },
        {
          prop: 'class2Sequence',
          label: this.$t('ui.tm.schedule.insert.nightSequence'),
          span: 8,
          type: 'number',
          min: 1,
          precision: 0
        },
        {
          prop: 'class2Analysis',
          label: this.$t('ui.tm.schedule.insert.nightAnalysis'),
          span: 8,
          maxlength: 200
        },
        {
          label: this.$t('ui.tm.schedule.insert.morningShift'),
          span: 24,
          type: 'title'
        },
        {
          prop: 'class3PlanQty',
          label: this.$t('ui.tm.schedule.insert.morningPlanQty'),
          span: 8,
          type: 'number',
          min: 0
        },
        {
          prop: 'class3Sequence',
          label: this.$t('ui.tm.schedule.insert.morningSequence'),
          span: 8,
          type: 'number',
          min: 1,
          precision: 0
        },
        {
          prop: 'class3Analysis',
          label: this.$t('ui.tm.schedule.insert.morningAnalysis'),
          span: 8,
          maxlength: 200
        },
        {
          prop: 'remark',
          label: this.$t('ui.data.column.tm.scheduleResult.remark'),
          span: 24,
          type: 'textarea',
          rows: 3,
          maxlength: 500
        }
      ]
    }
  },
  methods: {
    /**
     * 创建胎面式三班次插单表单初始值。
     *
     * @returns {Object} 插单表单对象
     */
    createForm() {
      return {
        factoryCode: '',
        scheduleDate: formatDate(new Date()),
        machineCode: '',
        sidewallCode: '',
        class1PlanQty: undefined,
        class1Sequence: undefined,
        class1Analysis: '',
        class2PlanQty: undefined,
        class2Sequence: undefined,
        class2Analysis: '',
        class3PlanQty: undefined,
        class3Sequence: undefined,
        class3Analysis: '',
        remark: ''
      }
    },
    /**
     * 打开胎侧插单弹窗并回填页面当前查询范围。
     *
     * @param {String} factoryCode 工厂编码
     * @param {String} scheduleDate 排程日期
     * @returns {void}
     */
    show(factoryCode, scheduleDate) {
      this.visible = true
      this.form = {
        ...this.createForm(),
        factoryCode: factoryCode || '',
        scheduleDate: scheduleDate || formatDate(new Date())
      }
      this.$nextTick(() => this.$refs.form && this.$refs.form.triggerResetForm())
    },
    /**
     * 关闭弹窗并清理本次插单表单。
     *
     * @returns {void}
     */
    hide() {
      this.form = {}
      if (this.$refs.form) {
        this.$refs.form.triggerResetForm()
      }
      this.visible = false
    },
    /**
     * 校验每个插单班次的计划量、顺序和原因分析配对关系。
     *
     * @returns {Boolean} 是否通过校验
     */
    validateInsertShiftFields() {
      let hasPlanQty = false
      for (let shiftOrder = 1; shiftOrder <= MAX_INSERT_SHIFT_ORDER; shiftOrder += 1) {
        const planQty = this.form[`class${shiftOrder}PlanQty`]
        const sequence = this.form[`class${shiftOrder}Sequence`]
        const analysis = this.form[`class${shiftOrder}Analysis`]
        const hasPlanValue = planQty !== null && planQty !== undefined && planQty !== ''
        const hasSequence = sequence !== null && sequence !== undefined && sequence !== ''
        const hasAnalysis = typeof analysis === 'string' && analysis.trim().length > 0
        if (!hasPlanValue) {
          if (hasSequence || hasAnalysis) {
            this.$modal.alertWarning(this.$t('ui.tm.schedule.insert.shiftPairRequired'))
            return false
          }
          continue
        }
        if (Number(planQty) <= 0 || !hasSequence || Number(sequence) < 1 || !Number.isInteger(Number(sequence))) {
          this.$modal.alertWarning(this.$t('ui.tm.schedule.insert.shiftPairRequired'))
          return false
        }
        hasPlanQty = true
      }
      if (!hasPlanQty) {
        this.$modal.alertWarning(this.$t('ui.tm.schedule.insert.planQtyRequired'))
        return false
      }
      return true
    },
    /**
     * 将胎面式表单字段转换为胎侧人工插单接口的班次列表。
     *
     * @param {Object} params 已通过表单校验的插单参数
     * @returns {Array<Object>} 胎侧接口班次列表
     */
    buildShiftList(params) {
      return Array.from({ length: MAX_INSERT_SHIFT_ORDER }, (item, index) => {
        const shiftOrder = index + 1
        return {
          shiftOrder,
          planQty: params[`class${shiftOrder}PlanQty`],
          sequence: params[`class${shiftOrder}Sequence`],
          analysis: params[`class${shiftOrder}Analysis`]
        }
      }).filter(item => Number(item.planQty) > 0)
    },
    /**
     * 提交胎侧人工插单异步任务。
     *
     * @param {Object} params 已通过表单校验的插单参数
     * @returns {Promise<void>} 提交完成
     */
    async save(params) {
      try {
        this.loading = true
        const task = await insertTask({
          factoryCode: params.factoryCode,
          scheduleDate: params.scheduleDate,
          machineCode: params.machineCode,
          sidewallCode: params.sidewallCode,
          shiftList: this.buildShiftList(params),
          remark: params.remark
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
     * 执行胎面式班次校验后提交表单。
     *
     * @returns {void}
     */
    handleConfirm() {
      if (!this.validateInsertShiftFields()) {
        return
      }
      this.$refs.form.triggerConfirm(this.save)
    }
  }
}
</script>
