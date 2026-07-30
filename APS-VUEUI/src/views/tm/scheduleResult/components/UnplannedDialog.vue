<template>
  <el-dialog
    :title="$t('ui.tm.schedule.unplannedTitle')"
    :visible.sync="visible"
    append-to-body
    width="88%"
  >
    <el-table v-loading="loading" :data="rows" border max-height="520">
      <el-table-column :label="$t('ui.data.column.tm.scheduleUnplanned.factoryCode')" min-width="110" prop="factoryCode" />
      <el-table-column :label="$t('ui.data.column.tm.scheduleUnplanned.scheduleDate')" min-width="115" prop="scheduleDate" />
      <el-table-column :label="$t('ui.data.column.tm.scheduleUnplanned.batchNo')" min-width="170" prop="batchNo" show-overflow-tooltip />
      <el-table-column :label="$t('ui.data.column.tm.scheduleUnplanned.treadCode')" min-width="140" prop="treadCode" />
      <el-table-column :label="$t('ui.data.column.tm.scheduleUnplanned.glueCode')" min-width="120" prop="glueCode" />
      <el-table-column :label="$t('ui.data.column.tm.scheduleUnplanned.mouthPlateCode')" min-width="120" prop="mouthPlateCode" />
      <el-table-column :label="$t('ui.data.column.tm.scheduleUnplanned.unplannedReasonCode')" min-width="150" prop="unplannedReasonCode" />
      <el-table-column :label="$t('ui.data.column.tm.scheduleUnplanned.unplannedReasonDesc')" min-width="220" prop="unplannedReasonDesc" show-overflow-tooltip />
      <el-table-column :label="$t('ui.data.btn.option')" fixed="right" width="110">
        <template slot-scope="scope">
          <el-button :disabled="!scope.row.unplannedEvidenceJson" type="text" @click="showEvidence(scope.row)">
            {{ $t('ui.tm.schedule.viewEvidence') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination
      v-show="total > 0"
      :limit.sync="pageSize"
      :page.sync="pageNum"
      :total="total"
      @pagination="load"
    />
    <span slot="footer">
      <el-button @click="visible = false">{{ $t('ui.frame.btn.close') }}</el-button>
    </span>
    <el-dialog :title="$t('ui.tm.schedule.unplannedEvidence')" :visible.sync="evidenceVisible" append-to-body width="680px">
      <pre class="evidence-content">{{ evidenceText }}</pre>
    </el-dialog>
  </el-dialog>
</template>

<script>
import {listTmScheduleUnplanned} from '@/api/tm/scheduleResult'

export default {
  name: 'TmUnplannedDialog',
  data() {
    return {
      visible: false,
      loading: false,
      rows: [],
      total: 0,
      pageNum: 1,
      pageSize: 20,
      query: {},
      evidenceVisible: false,
      evidenceText: ''
    }
  },
  methods: {
    show(query) {
      this.query = { ...query }
      this.pageNum = 1
      this.visible = true
      this.load()
    },
    async load() {
      this.loading = true
      try {
        const page = await listTmScheduleUnplanned({
          ...this.query,
          pageNum: this.pageNum,
          pageSize: this.pageSize
        })
        this.rows = page.records || page.rows || []
        this.total = Number(page.total || 0)
        this.$emit('count-change', this.total)
      } finally {
        this.loading = false
      }
    },
    showEvidence(row) {
      try {
        this.evidenceText = JSON.stringify(JSON.parse(row.unplannedEvidenceJson), null, 2)
      } catch (error) {
        this.evidenceText = row.unplannedEvidenceJson || ''
      }
      this.evidenceVisible = true
    }
  }
}
</script>

<style scoped>
.evidence-content {
  max-height: 420px;
  margin: 0;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
