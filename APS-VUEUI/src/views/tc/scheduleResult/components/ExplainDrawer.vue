<template>
  <el-drawer
    :title="$t('ui.tc.schedule.explainTitle')"
    :visible.sync="visible"
    append-to-body
    size="58%"
  >
    <div v-loading="loading" class="explain-body">
      <div v-if="!loading && list.length === 0" class="empty-text">
        {{ $t('ui.tc.schedule.noExplain') }}
      </div>
      <el-collapse v-else accordion>
        <el-collapse-item
          v-for="(item, index) in list"
          :key="item.id || index"
          :name="index"
          :title="explainTitle(item, index)"
        >
          <el-form class="explain-form" label-width="130px">
            <el-row :gutter="12">
              <el-col :span="12"><el-form-item :label="$t('ui.tc.schedule.taskBusinessKey')">{{ item.taskBusinessKey || '-' }}</el-form-item></el-col>
              <el-col :span="12"><el-form-item :label="$t('ui.tc.schedule.assignStatus')">{{ item.assignStatus || '-' }}</el-form-item></el-col>
              <el-col :span="12"><el-form-item :label="$t('ui.tc.schedule.shiftOrder')">{{ item.shiftOrder || '-' }}</el-form-item></el-col>
              <el-col :span="12"><el-form-item :label="$t('ui.tc.schedule.selectedMachine')">{{ item.selectedMachineCode || '-' }}</el-form-item></el-col>
              <el-col :span="12"><el-form-item :label="$t('ui.tc.schedule.finalPlanQty')">{{ item.finalPlanQty == null ? '-' : item.finalPlanQty }}</el-form-item></el-col>
              <el-col :span="12"><el-form-item :label="$t('ui.tc.schedule.unplannedReason')">{{ item.unplannedReasonCode || '-' }}</el-form-item></el-col>
            </el-row>
          </el-form>
          <div v-for="field in jsonFields" :key="field.prop" class="json-block">
            <div class="json-title">{{ $t(field.label) }}</div>
            <pre>{{ formatJson(item[field.prop]) }}</pre>
          </div>
        </el-collapse-item>
      </el-collapse>
    </div>
  </el-drawer>
</template>

<script>
import { getResultExplain, getUnplannedExplain } from '@/api/tc/tcScheduleResult'

export default {
  name: 'TcExplainDrawer',
  data() {
    return {
      visible: false,
      loading: false,
      list: [],
      jsonFields: [
        { prop: 'planQtyBreakdownJson', label: 'ui.tc.schedule.planBreakdown' },
        { prop: 'candidateMachineJson', label: 'ui.tc.schedule.candidateMachines' },
        { prop: 'ruleHitJson', label: 'ui.tc.schedule.ruleHits' },
        { prop: 'unplannedEvidenceJson', label: 'ui.tc.schedule.unplannedEvidence' },
        { prop: 'issueJson', label: 'ui.tc.schedule.issueDetail' }
      ]
    }
  },
  methods: {
    showResult(resultId) {
      this.load(() => getResultExplain(resultId))
    },
    showUnplanned(unplannedId) {
      this.load(() => getUnplannedExplain(unplannedId))
    },
    async load(loader) {
      this.visible = true
      this.loading = true
      this.list = []
      try {
        const response = await loader()
        const data = response && response.data !== undefined ? response.data : response
        this.list = Array.isArray(data) ? data : []
      } finally {
        this.loading = false
      }
    },
    explainTitle(item, index) {
      return `${index + 1}. ${item.sidewallCode || ''} / ${item.taskBusinessKey || ''}`
    },
    formatJson(value) {
      if (!value) return '-'
      try {
        return JSON.stringify(typeof value === 'string' ? JSON.parse(value) : value, null, 2)
      } catch (error) {
        return String(value)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.explain-body {
  padding: 0 20px 24px;
}
.json-block {
  margin-top: 14px;
}
.empty-text {
  padding: 48px 0;
  color: #909399;
  text-align: center;
}
.explain-form {
  padding: 8px 8px 0;
  border: 1px solid #ebeef5;
}
.json-title {
  margin-bottom: 6px;
  color: #606266;
  font-weight: 600;
}
pre {
  max-height: 260px;
  margin: 0;
  padding: 10px;
  overflow: auto;
  border-radius: 4px;
  background: #f5f7fa;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
