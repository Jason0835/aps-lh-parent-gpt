<template>
  <el-dialog
    :close-on-click-modal="false"
    :title="$t('ui.tc.schedule.autoPlan')"
    :visible.sync="visible"
    append-to-body
    width="520px"
  >
    <el-form ref="form" :model="form" :rules="rules" label-width="110px">
      <el-form-item :label="$t('ui.tc.schedule.factoryCode')" prop="factoryCode">
        <el-select v-model="form.factoryCode" filterable style="width:100%">
          <el-option
            v-for="item in factoryOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="$t('ui.tc.schedule.scheduleDate')" prop="scheduleDate">
        <el-date-picker
          v-model="form.scheduleDate"
          style="width:100%"
          type="date"
          value-format="yyyy-MM-dd"
        />
      </el-form-item>
    </el-form>
    <span slot="footer">
      <el-button @click="visible = false">{{ $t('ui.tc.schedule.cancel') }}</el-button>
      <el-button :loading="submitting" type="primary" @click="submit">
        {{ $t('ui.tc.schedule.confirm') }}
      </el-button>
    </span>
  </el-dialog>
</template>

<script>
import {autoPlan, validateAutoPlan} from '@/api/tc/tcScheduleResult'

export default {
  name: 'TcAutoPlanDialog',
  inject: ['parentDict'],
  data() {
    return {
      visible: false,
      submitting: false,
      form: {
        factoryCode: '',
        scheduleDate: ''
      },
      rules: {
        factoryCode: [{ required: true, message: this.$t('ui.tc.schedule.factoryRequired'), trigger: 'change' }],
        scheduleDate: [{ required: true, message: this.$t('ui.tc.schedule.dateRequired'), trigger: 'change' }]
      }
    }
  },
  computed: {
    factoryOptions() {
      return (this.parentDict && this.parentDict.type.biz_factory_name) || []
    }
  },
  methods: {
    show(factoryCode, scheduleDate) {
      this.form = {
        factoryCode: factoryCode || '',
        scheduleDate: scheduleDate || ''
      }
      this.visible = true
      this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate())
    },
    submit() {
      this.$refs.form.validate(async valid => {
        if (!valid) return
        this.submitting = true
        try {
          const request = {
            ...this.form,
            dataSource: 'BOARD',
            traceId: `TC-BOARD-${Date.now()}`,
            confirmOverwrite: false,
            language: this.$store.getters.language || 'zh_CN'
          }
          const validateResult = this.unwrap(await validateAutoPlan(request))
          if (validateResult.confirmRequired) {
            await this.$confirm(validateResult.message || this.$t('ui.tc.schedule.confirmOverwrite'), {
              type: 'warning'
            })
            request.confirmOverwrite = true
          }
          const task = this.unwrap(await autoPlan(request))
          this.visible = false
          this.$emit('success', this.form.scheduleDate, task)
        } finally {
          this.submitting = false
        }
      })
    },
    unwrap(response) {
      return response && response.data !== undefined ? response.data : (response || {})
    }
  }
}
</script>
