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
    <el-form
      ref="form"
      :model="form"
      :rules="rules"
      label-position="right"
      label-width="120px"
      v-loading="loading"
    >
      <el-row>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.cxPrecisionPlan.factoryCode')" prop="factoryCode">
        <el-select
          v-model="form.factoryCode"
          filterable
          clearable
          :placeholder="$t('common.rule.select')"
          style="width: 100%"
          @change="handleFactoryChange"
        >
              <el-option
                v-for="item in dict.type.biz_factory_name"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.cxPrecisionPlan.machineCode')" prop="machineCode">
            <el-select
              v-model="form.machineCode"
              filterable
              clearable
              :placeholder="$t('common.rule.select')"
              style="width: 100%"
              @change="handleMachineChange"
            >
        <el-option
          v-for="item in machineList"
          :key="item.id"
          :label="item.cxMachineCode"
          :value="item.cxMachineCode"
        />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.cxPrecisionPlan.machineName')" prop="machineName">
            <el-input v-model="form.machineName" :placeholder="$t('common.rule.input')" disabled />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.cxPrecisionPlan.planDate')" prop="planDate">
            <el-date-picker
              v-model="form.planDate"
              type="date"
              :placeholder="$t('ui.frame.placeholder.selectDate')"
              value-format="yyyy-MM-dd"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.cxPrecisionPlan.planShift')" prop="planShift">
            <el-select
              v-model="form.planShift"
              filterable
              clearable
              :placeholder="$t('common.rule.select')"
              style="width: 100%"
            >
            <el-option
              v-for="item in dict.type.class_num_three_plan"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.cxPrecisionPlan.planStartTime')" prop="planStartTime">
            <el-date-picker
              v-model="form.planStartTime"
              type="datetime"
              :placeholder="$t('ui.frame.placeholder.selectDateTime')"
              value-format="yyyy-MM-dd HH:mm:ss"
              style="width: 100%"
              @change="handlePlanStartTimeChange"
            />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.cxPrecisionPlan.planEndTime')" prop="planEndTime">
            <el-date-picker
              v-model="form.planEndTime"
              type="datetime"
              :placeholder="$t('ui.frame.placeholder.selectDateTime')"
              value-format="yyyy-MM-dd HH:mm:ss"
              style="width: 100%"
              @change="handlePlanEndTimeChange"
            />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.cxPrecisionPlan.estimatedHours')" prop="estimatedHours">
            <el-input-number v-model="form.estimatedHours" :min="0" :precision="1" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.cxPrecisionPlan.lastPrecisionDate')" prop="lastPrecisionDate">
            <el-date-picker
              v-model="form.lastPrecisionDate"
              type="date"
              :placeholder="$t('ui.frame.placeholder.selectDate')"
              value-format="yyyy-MM-dd"
              style="width: 100%"
              @change="handleLastPrecisionDateChange"
            />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.data.column.cxPrecisionPlan.dueDate')" prop="dueDate">
            <el-date-picker
              v-model="form.dueDate"
              type="date"
              :placeholder="$t('ui.frame.placeholder.selectDate')"
              value-format="yyyy-MM-dd"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item :label="$t('ui.common.column.remark')" prop="remark">
            <el-input type="textarea" v-model="form.remark" :rows="3" :placeholder="$t('common.rule.input')" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template slot="footer">
      <el-button @click="hide">{{ $t('common.button.cancel') }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">{{ $t('common.button.confirm') }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import { saveCxPrecisionPlan, checkCxPrecisionPlanUnique, getMachineList } from '@/api/cx/cxPrecisionPlan'

export default {
  name: 'InfoDialog',
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
        factoryCode: [{ required: true, message: this.$t('common.rule.select'), trigger: 'change' }],
        machineCode: [{ required: true, message: this.$t('common.rule.select'), trigger: 'change' }],
        machineName: [{ required: true, message: this.$t('common.rule.input'), trigger: 'blur' }],
        planDate: [{ required: true, message: this.$t('common.rule.select'), trigger: 'change' }],
        estimatedHours: [{ required: true, message: this.$t('common.rule.input'), trigger: 'blur' }]
      }
    }
  },
  computed: {
    title() {
      return this.$t('ui.data.column.cxPrecisionPlan.modelName')
    }
  },
  methods: {
    show(row) {
      this.visible = true
      this.machineList = []
      if (row) {
        this.isEdit = true
        this.form = { ...row }
        this.getMachineList()
      } else {
        this.isEdit = false
        this.form = {
          estimatedHours: 0
        }
      }
    },
    hide() {
      this.visible = false
      this.form = {}
      this.machineList = []
      if (this.$refs.form) {
        this.$refs.form.resetFields()
      }
    },
    async getMachineList() {
      if (!this.form.factoryCode) {
        this.machineList = []
        return
      }
      try {
        const res = await getMachineList({ factoryCode: this.form.factoryCode })
        this.machineList = res || []
      } catch (e) {
        console.error('获取成型机列表失败', e)
      }
    },
    handleFactoryChange() {
      this.$set(this.form, 'machineCode', '')
      this.getMachineList()
    },
    handleMachineChange(val) {
      const machine = this.machineList.find(item => item.cxMachineCode === val)
      if (machine) {
        this.$set(this.form, 'machineCode', machine.cxMachineCode)
        this.$set(this.form, 'machineName', machine.cxMachineCode)
      }
    },
    handlePlanStartTimeChange(val) {
      if (this.form.planDate && val) {
        const planDate = this.form.planDate.substring(0, 10)
        const startTime = val.substring(0, 10)
        if (startTime < planDate) {
          this.$modal.msgError(this.$t('ui.data.alert.cxPrecisionPlan.startTimeBeforePlanDate'))
          this.$set(this.form, 'planStartTime', '')
          return
        }
      }
      this.calculateEstimatedHours()
    },
    handlePlanEndTimeChange(val) {
      if (this.form.planStartTime && val) {
        if (val < this.form.planStartTime) {
          this.$modal.msgError(this.$t('ui.data.alert.cxPrecisionPlan.endTimeBeforeStartTime'))
          this.$set(this.form, 'planEndTime', '')
          return
        }
      }
      this.calculateEstimatedHours()
    },
    handleLastPrecisionDateChange(val) {
      if (this.form.planDate && val) {
        if (val >= this.form.planDate) {
          this.$modal.msgError(this.$t('ui.data.alert.cxPrecisionPlan.lastPrecisionDateAfterStartTime'))
          this.$set(this.form, 'lastPrecisionDate', '')
          return
        }
      }
    },
    calculateEstimatedHours() {
      if (this.form.planStartTime && this.form.planEndTime) {
        const start = new Date(this.form.planStartTime).getTime()
        const end = new Date(this.form.planEndTime).getTime()
        const hours = (end - start) / (1000 * 60 * 60)
        this.$set(this.form, 'estimatedHours', Math.round(hours * 10) / 10)
      } else {
        this.$set(this.form, 'estimatedHours', 0)
      }
    },
    handleConfirm() {
      this.$refs.form.validate(async valid => {
        if (!valid) return

        if (this.form.planDate && this.form.planStartTime) {
          const planDate = this.form.planDate.substring(0, 10)
          const startTime = this.form.planStartTime.substring(0, 10)
          if (startTime < planDate) {
            this.$modal.msgError(this.$t('ui.data.alert.cxPrecisionPlan.startTimeBeforePlanDate'))
            return
          }
        }

        if (this.form.planStartTime && this.form.planEndTime) {
          if (this.form.planEndTime < this.form.planStartTime) {
            this.$modal.msgError(this.$t('ui.data.alert.cxPrecisionPlan.endTimeBeforeStartTime'))
            return
          }
        }

        if (this.form.planDate && this.form.lastPrecisionDate) {
          if (this.form.lastPrecisionDate >= this.form.planDate) {
            this.$modal.msgError(this.$t('ui.data.alert.cxPrecisionPlan.lastPrecisionDateAfterStartTime'))
            return
          }
        }

        if (this.form.planDate && this.form.dueDate) {
          if (this.form.dueDate <= this.form.planDate) {
            this.$modal.msgError(this.$t('ui.data.alert.cxPrecisionPlan.dueDateBeforePlanDate'))
            return
          }
        }

        const uniqueRes = await checkCxPrecisionPlanUnique(this.form)
        if (uniqueRes === '1') {
          this.$modal.msgError(this.$t('ui.data.alert.cxPrecisionPlan.notUnique'))
          return
        }

        this.loading = true
        try {
          const res = await saveCxPrecisionPlan(this.form)
          this.$modal.msgSuccess(res.msg)
          this.$emit('success')
          this.hide()
        } finally {
          this.loading = false
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.el-form-item {
  margin-bottom: 25px;
}
</style>
