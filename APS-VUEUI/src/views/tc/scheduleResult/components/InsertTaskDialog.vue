<template>
  <el-dialog
    :close-on-click-modal="false"
    :title="$t('ui.tc.schedule.insertTask')"
    :visible.sync="visible"
    append-to-body
    width="860px"
  >
    <el-form ref="form" v-loading="loading" :model="form" :rules="rules" label-width="115px">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item :label="$t('ui.tc.schedule.factoryCode')" prop="factoryCode">
            <el-select v-model="form.factoryCode" filterable style="width:100%" @change="loadOptions">
              <el-option v-for="item in factoryOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="$t('ui.tc.schedule.scheduleDate')" prop="scheduleDate">
            <el-date-picker
              v-model="form.scheduleDate"
              :picker-options="datePickerOptions"
              style="width:100%"
              type="date"
              value-format="yyyy-MM-dd"
              @change="loadOptions"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="$t('ui.tc.schedule.sidewallCode')" prop="constructionKey">
            <el-select v-model="form.constructionKey" filterable style="width:100%" @change="handleConstructionChange">
              <el-option
                v-for="item in constructionList"
                :key="constructionKey(item)"
                :label="constructionLabel(item)"
                :value="constructionKey(item)"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="$t('ui.tc.schedule.machineCode')" prop="machineCode">
            <el-select v-model="form.machineCode" filterable style="width:100%">
              <el-option
                v-for="item in machineList"
                :key="item.machineCode"
                :label="machineLabel(item)"
                :value="item.machineCode"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-table :data="form.shiftList" border class="shift-table" size="small">
        <el-table-column :label="$t('ui.tc.schedule.shiftOrder')" width="180">
          <template slot-scope="scope">{{ shiftLabel(scope.row.shiftOrder) }}</template>
        </el-table-column>
        <el-table-column :label="$t('ui.tc.schedule.planQty')">
          <template slot-scope="scope">
            <el-input-number
              v-model="scope.row.planQty"
              :disabled="!isShiftEnabled(scope.row.shiftOrder)"
              :min="0"
              :precision="2"
              controls-position="right"
              style="width:100%"
            />
          </template>
        </el-table-column>
        <el-table-column :label="$t('ui.tc.schedule.sequence')">
          <template slot-scope="scope">
            <el-input-number
              v-model="scope.row.sequence"
              :disabled="!isShiftEnabled(scope.row.shiftOrder)"
              :min="1"
              :precision="0"
              controls-position="right"
              style="width:100%"
            />
          </template>
        </el-table-column>
      </el-table>

      <el-row :gutter="16" class="construction-detail">
        <el-col :span="8">{{ $t('ui.tc.schedule.glueCode') }}：{{ selectedConstruction.glueCode || '-' }}</el-col>
        <el-col :span="8">{{ $t('ui.tc.schedule.baseGlueCode') }}：{{ selectedConstruction.baseGlueCode || '-' }}</el-col>
        <el-col :span="8">{{ $t('ui.tc.schedule.mouthPlateCode') }}：{{ selectedConstruction.mouthPlateCode || '-' }}</el-col>
      </el-row>

      <el-form-item :label="$t('ui.tc.schedule.reason')" prop="reason">
        <el-input v-model.trim="form.reason" :rows="2" maxlength="200" show-word-limit type="textarea" />
      </el-form-item>
    </el-form>
    <span slot="footer">
      <el-button @click="visible = false">{{ $t('ui.tc.schedule.cancel') }}</el-button>
      <el-button :loading="submitting" type="primary" @click="submit">{{ $t('ui.tc.schedule.confirm') }}</el-button>
    </span>
  </el-dialog>
</template>

<script>
import {getManualOptions, insertTask} from '@/api/tc/tcScheduleResult'
import {resolveErrorMessage} from '@/utils/errorMessage'

const formatDate = date => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const createShiftList = () => Array.from({ length: 6 }, (item, index) => ({
  shiftOrder: index + 1,
  planQty: 0,
  sequence: undefined
}))

export default {
  name: 'TcInsertTaskDialog',
  inject: ['parentDict'],
  data() {
    return {
      visible: false,
      loading: false,
      submitting: false,
      constructionList: [],
      machineList: [],
      shiftOptions: [],
      selectedConstruction: {},
      datePickerOptions: {
        disabledDate(date) {
          const today = new Date()
          today.setHours(0, 0, 0, 0)
          return date.getTime() < today.getTime()
        }
      },
      form: this.createForm(),
      rules: {
        factoryCode: [{ required: true, message: this.$t('ui.tc.schedule.factoryRequired'), trigger: 'change' }],
        scheduleDate: [{ required: true, message: this.$t('ui.tc.schedule.dateRequired'), trigger: 'change' }],
        constructionKey: [{ required: true, message: this.$t('ui.tc.schedule.sidewallRequired'), trigger: 'change' }],
        machineCode: [{ required: true, message: this.$t('ui.tc.schedule.machineRequired'), trigger: 'change' }],
        reason: [{ required: true, message: this.$t('ui.tc.schedule.reasonRequired'), trigger: 'blur' }]
      }
    }
  },
  computed: {
    factoryOptions() {
      return (this.parentDict && this.parentDict.type.biz_factory_name) || []
    }
  },
  methods: {
    createForm() {
      return {
        factoryCode: '',
        scheduleDate: formatDate(new Date()),
        constructionKey: '',
        machineCode: '',
        reason: '',
        shiftList: createShiftList()
      }
    },
    show(factoryCode, scheduleDate) {
      this.form = this.createForm()
      this.form.factoryCode = factoryCode || ''
      this.form.scheduleDate = scheduleDate || formatDate(new Date())
      this.selectedConstruction = {}
      this.visible = true
      this.loadOptions()
      this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate())
    },
    async loadOptions() {
      if (!this.form.factoryCode || !this.form.scheduleDate) return
      this.loading = true
      try {
        const data = await getManualOptions({
          factoryCode: this.form.factoryCode,
          scheduleDate: this.form.scheduleDate
        })
        this.constructionList = data.constructionList || []
        this.machineList = data.machineList || []
        this.shiftOptions = data.shiftList || []
      } catch (error) {
        this.$modal.alertError(resolveErrorMessage(
          error,
          this.$t('ui.tc.schedule.operationFailed')
        ))
      } finally {
        this.loading = false
      }
    },
    constructionKey(item) {
      return `${item.sidewallCode || ''}|${item.constructionVersion || ''}`
    },
    constructionLabel(item) {
      return `${item.sidewallCode || ''} / ${item.constructionVersion || '-'}`
    },
    machineLabel(item) {
      return item.machineName ? `${item.machineCode} / ${item.machineName}` : item.machineCode
    },
    handleConstructionChange(value) {
      this.selectedConstruction = this.constructionList.find(item => this.constructionKey(item) === value) || {}
    },
    shiftLabel(shiftOrder) {
      const option = this.shiftOptions.find(item => item.shiftOrder === shiftOrder)
      return option ? `${option.shiftOrder}. ${option.shiftName || option.shiftCode || ''}` : `${this.$t('ui.tc.schedule.shift')} ${shiftOrder}`
    },
    isShiftEnabled(shiftOrder) {
      const option = this.shiftOptions.find(item => item.shiftOrder === shiftOrder)
      return option && String(option.openFlag) === '1'
    },
    submit() {
      this.$refs.form.validate(async valid => {
        if (!valid) return
        const pairInvalid = this.form.shiftList.some(item =>
          (Number(item.planQty) > 0) !== (item.sequence !== undefined && item.sequence !== null)
        )
        if (pairInvalid) {
          this.$modal.alertWarning(this.$t('ui.tc.schedule.planSequencePair'))
          return
        }
        const shiftList = this.form.shiftList.filter(item => Number(item.planQty) > 0)
        if (shiftList.length === 0) {
          this.$modal.alertWarning(this.$t('ui.tc.schedule.planRequired'))
          return
        }
        this.submitting = true
        try {
          const task = await insertTask({
            factoryCode: this.form.factoryCode,
            scheduleDate: this.form.scheduleDate,
            machineCode: this.form.machineCode,
            sidewallCode: this.selectedConstruction.sidewallCode,
            constructionVersion: this.selectedConstruction.constructionVersion,
            shiftList,
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
.shift-table {
  margin-bottom: 16px;
}
.construction-detail {
  margin-bottom: 16px;
  padding: 10px 4px;
  color: #606266;
  line-height: 24px;
}
</style>
