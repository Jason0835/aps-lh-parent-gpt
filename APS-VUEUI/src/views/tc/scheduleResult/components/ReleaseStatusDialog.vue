<template>
  <el-dialog
    :append-to-body="true"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :title="$t('ui.tc.schedule.changeReleaseStatus')"
    :visible="visible"
    width="420px"
    @close="hide"
  >
    <el-form ref="releaseStatusForm" :model="form" :rules="rules">
      <el-form-item :label="$t('ui.tc.schedule.releaseStatus')" prop="isRelease">
        <el-radio-group v-model="form.isRelease">
          <el-radio
            v-for="item in parentDict.type.IS_RELEASE"
            :key="item.value"
            :label="item.value"
          >
            {{ item.label }}
          </el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template slot="footer">
      <el-button @click="hide">{{ $t('common.button.cancel') }}</el-button>
      <el-button :loading="loading" type="primary" @click="handleConfirm">
        {{ $t('common.button.confirm') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script>
import {changeReleaseStatus} from '@/api/tc/tcScheduleResult'
import {resolveErrorMessage} from '@/utils/errorMessage'

export default {
  inject: ['parentDict'],
  data() {
    return {
      loading: false,
      visible: false,
      form: {
        isRelease: ''
      },
      tableRows: [],
      rules: {
        isRelease: [
          {
            required: true,
            message: this.$t('common.rule.select'),
            trigger: 'change'
          }
        ]
      }
    }
  },
  methods: {
    /**
     * 打开胎侧发布状态变更弹窗。
     *
     * @param {Array} rows 当前选中的胎侧排程结果
     * @returns {void}
     */
    show(rows) {
      this.tableRows = Array.isArray(rows) ? rows : []
      this.form = {
        isRelease: ''
      }
      this.visible = true
      this.$nextTick(() => {
        this.$refs.releaseStatusForm.clearValidate()
      })
    },
    /**
     * 关闭弹窗并清理选择数据。
     *
     * @returns {void}
     */
    hide() {
      this.visible = false
      this.tableRows = []
      this.form = {
        isRelease: ''
      }
    },
    /**
     * 校验并批量提交发布状态变更。
     *
     * @returns {void}
     */
    handleConfirm() {
      this.$refs.releaseStatusForm.validate(async valid => {
        if (!valid) {
          return
        }
        try {
          this.loading = true
          const result = await changeReleaseStatus({
            ids: this.tableRows.map(item => item.id).join(','),
            isRelease: this.form.isRelease
          })
          this.$modal.msgSuccess(result.msg)
          this.$emit('success')
          this.hide()
        } catch (error) {
          this.$modal.alertError(resolveErrorMessage(
            error,
            this.$t('ui.tc.schedule.operationFailed')
          ))
        } finally {
          this.loading = false
        }
      })
    }
  }
}
</script>
