<template>
  <el-dialog
    :close-on-click-modal="false"
    :title="$t('ui.tc.schedule.changeMachine')"
    :visible.sync="visible"
    append-to-body
    width="920px"
  >
    <el-form ref="form" v-loading="loading" :model="form" :rules="rules" label-width="125px">
      <el-form-item :label="$t('ui.tc.schedule.targetMachine')" prop="targetMachineCode">
        <el-select v-model="form.targetMachineCode" filterable style="width:100%">
          <el-option
            v-for="item in machineList"
            :key="item.machineCode"
            :label="machineLabel(item)"
            :value="item.machineCode"
          />
        </el-select>
      </el-form-item>
      <el-table :data="form.taskList" border class="task-table" size="small">
        <el-table-column :label="$t('ui.tc.schedule.sidewallCode')" min-width="150" prop="sidewallCode" />
        <el-table-column :label="$t('ui.tc.schedule.sourceMachine')" min-width="120" prop="machineCode" />
        <el-table-column :label="$t('ui.tc.schedule.scheduleDate')" min-width="120" prop="scheduleDate" />
        <el-table-column :label="$t('ui.tc.schedule.shiftOrder')" min-width="180">
          <template slot-scope="scope">
            <el-select v-model="scope.row.shiftOrder" filterable style="width:100%">
              <el-option
                v-for="item in rowShiftOptions(scope.row)"
                :key="item.shiftOrder"
                :label="item.label"
                :value="item.shiftOrder"
              />
            </el-select>
          </template>
        </el-table-column>
      </el-table>
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
import {changeMachine, getManualOptions} from '@/api/tc/tcScheduleResult'
import {resolveErrorMessage} from '@/utils/errorMessage'

export default {
  name: 'TcChangeMachineDialog',
  data() {
    return {
      visible: false,
      loading: false,
      submitting: false,
      machineList: [],
      shiftOptions: [],
      form: {
        targetMachineCode: '',
        taskList: [],
        reason: ''
      },
      rules: {
        targetMachineCode: [{ required: true, message: this.$t('ui.tc.schedule.machineRequired'), trigger: 'change' }],
        reason: [{ required: true, message: this.$t('ui.tc.schedule.reasonRequired'), trigger: 'blur' }]
      }
    }
  },
  methods: {
    async show(rows) {
      const firstRow = rows[0] || {}
      this.form = {
        targetMachineCode: '',
        taskList: rows.map(row => ({ ...row, shiftOrder: undefined })),
        reason: ''
      }
      this.visible = true
      this.loading = true
      try {
        const data = await getManualOptions({
          factoryCode: firstRow.factoryCode,
          scheduleDate: firstRow.scheduleDate
        })
        this.machineList = data.machineList || []
        this.shiftOptions = data.shiftList || []
      } catch (error) {
        this.$modal.alertError(resolveErrorMessage(
          error,
          this.$t('ui.tc.schedule.operationFailed')
        ))
      } finally {
        this.loading = false
        this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate())
      }
    },
    machineLabel(item) {
      return item.machineName ? `${item.machineCode} / ${item.machineName}` : item.machineCode
    },
    rowShiftOptions(row) {
      return Array.from({ length: 6 }, (item, index) => index + 1)
        .filter(shiftOrder => Number(row[`class${shiftOrder}PlanQty`] || 0) > 0)
        .filter(shiftOrder => {
          const option = this.shiftOptions.find(item => item.shiftOrder === shiftOrder)
          const targetMachine = this.machineList.find(item => item.machineCode === this.form.targetMachineCode)
          return option && this.machineOpenShiftCodes(targetMachine).includes(String(option.shiftCode || '').trim())
        })
        .map(shiftOrder => {
          const option = this.shiftOptions.find(item => item.shiftOrder === shiftOrder)
          return {
            shiftOrder,
            label: option ? `${shiftOrder}. ${option.shiftName || option.shiftCode || ''}` : `${this.$t('ui.tc.schedule.shift')} ${shiftOrder}`
          }
        })
    },
    machineOpenShiftCodes(machine) {
      if (!machine || !machine.openShiftCode) return []
      return [...new Set(String(machine.openShiftCode).split(',').map(item => item.trim()).filter(Boolean))]
    },
    submit() {
      this.$refs.form.validate(async valid => {
        if (!valid) return
        if (this.form.taskList.some(item => !item.shiftOrder)) {
          this.$modal.alertWarning(this.$t('ui.tc.schedule.shiftRequiredForEach'))
          return
        }
        if (this.form.taskList.some(item => item.machineCode === this.form.targetMachineCode)) {
          this.$modal.alertWarning(this.$t('ui.tc.schedule.targetSameAsSource'))
          return
        }
        this.submitting = true
        try {
          const task = await changeMachine({
            targetMachineCode: this.form.targetMachineCode,
            taskList: this.form.taskList.map(item => ({
              resultId: item.id,
              shiftOrder: item.shiftOrder,
              expectedTaskVersion: item.taskVersion
            })),
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

<style lang="scss" scoped>
.task-table {
  margin-bottom: 18px;
}
</style>
