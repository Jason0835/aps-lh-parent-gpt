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

const MAX_INSERT_SHIFT_ORDER = 6

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
    },
    shiftDateList: {
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
      const baseColumns = [
        {
          prop: 'factoryCode',
          label: this.$t('ui.data.column.tm.scheduleResult.factoryCode'),
          type: 'select',
          span: 12,
          dictData: this.parentDict.type.biz_factory_name,
          filterable: true,
          listeners: {
            change: this.handleFactoryChange
          }
        },
        {
          prop: 'scheduleDate',
          label: this.$t('ui.data.column.tm.scheduleResult.scheduleDate'),
          type: 'date',
          span: 12,
          valueFormat: 'yyyy-MM-dd',
          listeners: {
            change: this.handleScheduleDateChange
          }
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
          prop: 'remark',
          label: this.$t('ui.data.column.tm.scheduleResult.remark'),
          span: 24,
          type: 'textarea',
          rows: 3,
          maxlength: 500
        }
      ]
      const shiftColumns = Array.from({ length: MAX_INSERT_SHIFT_ORDER }, (item, index) => {
        const shiftOrder = index + 1
        const fieldPrefix = `class${shiftOrder}`
        const shiftDisabled = this.isShiftStarted(shiftOrder)
        return [
          {
            label: this.getShiftTitle(shiftOrder),
            span: 24,
            type: 'title'
          },
          {
            prop: `${fieldPrefix}PlanQty`,
            label: this.$t('ui.tc.schedule.planQty'),
            span: 8,
            type: 'number',
            min: 0,
            disabled: shiftDisabled
          },
          {
            prop: `${fieldPrefix}Sequence`,
            label: this.$t('ui.tc.schedule.sequence'),
            span: 8,
            type: 'number',
            min: 1,
            precision: 0,
            disabled: shiftDisabled
          },
          {
            prop: `${fieldPrefix}Analysis`,
            label: this.$t('ui.tc.schedule.analysis'),
            span: 8,
            maxlength: 200,
            disabled: shiftDisabled
          }
        ]
      }).reduce((columns, shiftColumn) => columns.concat(shiftColumn), [])
      return baseColumns.slice(0, 4).concat(shiftColumns, baseColumns.slice(4))
    }
  },
  methods: {
    /**
     * 创建胎侧六班次插单表单初始值。
     *
     * @returns {Object} 插单表单对象
     */
    createForm() {
      const form = {
        factoryCode: '',
        scheduleDate: formatDate(new Date()),
        machineCode: '',
        sidewallCode: '',
        remark: ''
      }
      for (let shiftOrder = 1; shiftOrder <= MAX_INSERT_SHIFT_ORDER; shiftOrder += 1) {
        form[`class${shiftOrder}PlanQty`] = undefined
        form[`class${shiftOrder}Sequence`] = undefined
        form[`class${shiftOrder}Analysis`] = ''
      }
      return form
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
     * 获取与胎侧列表页二级表头一致的班次分组标题。
     *
     * @param {Number} shiftOrder 班次顺序
     * @returns {String} 班次名称和实际日期
     */
    getShiftTitle(shiftOrder) {
      const shiftOption = this.shiftDateList.find(item => item.shiftOrder === shiftOrder) || {}
      const shiftName = shiftOption.shiftName || shiftOption.shiftCode ||
        `${this.$t('ui.tc.schedule.shift')} ${shiftOrder}`
      const scheduleDate = shiftOption.scheduleDate
        ? String(shiftOption.scheduleDate).substring(0, 10)
        : ''
      const displayDate = scheduleDate ? scheduleDate.substring(5, 10).replace('-', '/') : ''
      return `${shiftName} ${displayDate}`.trim()
    },
    /**
     * 判断班次实际开始时间是否已到达。
     *
     * @param {Number} shiftOrder 班次顺序
     * @returns {Boolean} true 表示班次已开始
     */
    isShiftStarted(shiftOrder) {
      const shiftOption = this.shiftDateList.find(item => item.shiftOrder === shiftOrder) || {}
      if (!shiftOption.shiftStartTime) {
        return false
      }
      const normalizedStartTime = typeof shiftOption.shiftStartTime === 'string'
        ? shiftOption.shiftStartTime.replace(' ', 'T')
        : shiftOption.shiftStartTime
      const timestamp = new Date(normalizedStartTime).getTime()
      return Number.isFinite(timestamp) && timestamp <= Date.now()
    },
    /**
     * 清除已开始班次的输入值，避免日期或工厂切换后提交失效班次。
     *
     * @returns {void}
     */
    clearStartedShiftValues() {
      for (let shiftOrder = 1; shiftOrder <= MAX_INSERT_SHIFT_ORDER; shiftOrder += 1) {
        if (!this.isShiftStarted(shiftOrder)) {
          continue
        }
        const fieldSuffixList = ['PlanQty', 'Sequence', 'Analysis']
        fieldSuffixList.forEach(suffix => {
          this.$set(this.form, `class${shiftOrder}${suffix}`, undefined)
        })
      }
    },
    /**
     * 清空全部班次输入值。
     *
     * @returns {void}
     */
    clearShiftValues() {
      const fieldSuffixList = ['PlanQty', 'Sequence', 'Analysis']
      for (let shiftOrder = 1; shiftOrder <= MAX_INSERT_SHIFT_ORDER; shiftOrder += 1) {
        fieldSuffixList.forEach(suffix => {
          this.$set(this.form, `class${shiftOrder}${suffix}`, undefined)
        })
      }
    },
    /**
     * 工厂变化时清空旧机台和班次输入，并通知父页面刷新选项。
     *
     * @param {String} factoryCode 新工厂编码
     * @returns {void}
     */
    handleFactoryChange(factoryCode) {
      this.$set(this.form, 'machineCode', undefined)
      this.clearShiftValues()
      this.$emit('scope-change', {
        factoryCode,
        scheduleDate: this.form.scheduleDate
      })
    },
    /**
     * 排程日期变化时清空旧班次输入，并通知父页面刷新选项。
     *
     * @param {String} scheduleDate 新排程日期
     * @returns {void}
     */
    handleScheduleDateChange(scheduleDate) {
      this.clearShiftValues()
      this.$emit('scope-change', {
        factoryCode: this.form.factoryCode,
        scheduleDate
      })
    },
    /**
     * 校验每个插单班次的计划量、顺序和原因分析配对关系。
     *
     * @returns {Boolean} 是否通过校验
     */
    validateInsertShiftFields() {
      this.clearStartedShiftValues()
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
