<template>
  <el-dialog
    :close-on-click-modal="false"
    :title="$t('ui.tc.schedule.changeQty')"
    :visible.sync="visible"
    append-to-body
    width="560px"
  >
    <el-form ref="form" :model="form" :rules="rules" label-width="125px">
      <el-form-item :label="$t('ui.tc.schedule.sidewallCode')">
        {{ row.sidewallCode || '-' }}
      </el-form-item>
      <el-form-item :label="$t('ui.tc.schedule.machineCode')">
        {{ row.machineCode || '-' }}
      </el-form-item>
      <el-form-item :label="$t('ui.tc.schedule.shiftOrder')" prop="shiftOrder">
        <el-select v-model="form.shiftOrder" filterable style="width:100%" @change="handleShiftChange">
          <el-option
            v-for="item in availableShifts"
            :key="item.shiftOrder"
            :label="item.label"
            :value="item.shiftOrder"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('ui.tc.schedule.finishQty')">
        {{ currentFinishQty }}
      </el-form-item>
      <el-form-item :label="$t('ui.tc.schedule.newPlanQty')" prop="newPlanQty">
        <el-input-number
          v-model="form.newPlanQty"
          :min="Number(currentFinishQty || 0)"
          :precision="2"
          controls-position="right"
          style="width:100%"
        />
      </el-form-item>
      <el-form-item :label="$t('ui.tc.schedule.reason')" prop="reason">
        <el-input v-model.trim="form.reason" :rows="3" maxlength="200" show-word-limit type="textarea" />
      </el-form-item>
    </el-form>
    <span slot="footer">
      <el-button @click="visible = false">{{ $t('ui.tc.schedule.cancel') }}</el-button>
      <el-button :loading="submitting" type="primary" @click="submit">{{ $t('ui.tc.schedule.confirm') }}</el-button>
    </span>
  </el-dialog>
</template>

<script>
import {changeQty, getManualOptions} from '@/api/tc/tcScheduleResult'
import {resolveErrorMessage} from '@/utils/errorMessage'

export default {
  name: 'TcChangeQtyDialog',
  data() {
    return {
      visible: false,
      submitting: false,
      row: {},
      shiftOptions: [],
      form: {
        shiftOrder: undefined,
        newPlanQty: 0,
        reason: ''
      },
      rules: {
        shiftOrder: [{ required: true, message: this.$t('ui.tc.schedule.shiftRequired'), trigger: 'change' }],
        newPlanQty: [{ required: true, message: this.$t('ui.tc.schedule.qtyRequired'), trigger: 'blur' }],
        reason: [{ required: true, message: this.$t('ui.tc.schedule.reasonRequired'), trigger: 'blur' }]
      }
    }
  },
  computed: {
    availableShifts() {
      return Array.from({ length: 6 }, (item, index) => index + 1)
        .filter(shiftOrder => Number(this.row[`class${shiftOrder}PlanQty`] || 0) > 0)
        .filter(shiftOrder => {
          const option = this.shiftOptions.find(item => item.shiftOrder === shiftOrder)
          return option && String(option.openFlag) === '1'
        })
        .map(shiftOrder => {
          const option = this.shiftOptions.find(item => item.shiftOrder === shiftOrder)
          return {
            shiftOrder,
            label: option ? `${shiftOrder}. ${option.shiftName || option.shiftCode || ''}` : `${this.$t('ui.tc.schedule.shift')} ${shiftOrder}`
          }
        })
    },
    currentFinishQty() {
      if (!this.form.shiftOrder) return 0
      return Number(this.row[`class${this.form.shiftOrder}FinishQty`] || 0)
    }
  },
  methods: {
    async show(row) {
      this.row = { ...row }
      this.form = { shiftOrder: undefined, newPlanQty: 0, reason: '' }
      this.shiftOptions = []
      this.visible = true
      try {
        const data = await getManualOptions({ factoryCode: row.factoryCode, scheduleDate: row.scheduleDate })
        this.shiftOptions = data.shiftList || []
      } catch (error) {
        this.$modal.alertError(resolveErrorMessage(
          error,
          this.$t('ui.tc.schedule.operationFailed')
        ))
      } finally {
        this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate())
      }
    },
    handleShiftChange(shiftOrder) {
      this.form.newPlanQty = Number(this.row[`class${shiftOrder}PlanQty`] || 0)
    },
    submit() {
      this.$refs.form.validate(async valid => {
        if (!valid) return
        if (Number(this.form.newPlanQty) < this.currentFinishQty) {
          this.$modal.alertWarning(this.$t('ui.tc.schedule.qtyBelowFinish'))
          return
        }
        this.submitting = true
        try {
          const task = await changeQty({
            resultId: this.row.id,
            shiftOrder: this.form.shiftOrder,
            newPlanQty: this.form.newPlanQty,
            expectedTaskVersion: this.row.taskVersion,
            reason: this.form.reason
          })
          this.visible = false
          this.$emit('success', task)
        } catch (error) {
          this.$modal.alertError(resolveErrorMessage(
            error,
            this.$t('ui.tc.schedule.operationFailed')
          ))
        } finally {
          this.submitting = false
        }
      })
    }
  }
}
</script>
