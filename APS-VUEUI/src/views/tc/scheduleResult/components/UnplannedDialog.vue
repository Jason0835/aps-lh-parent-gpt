<template>
  <el-dialog
    :title="$t('ui.tc.schedule.unplannedTitle')"
    :visible.sync="visible"
    append-to-body
    width="88%"
  >
    <el-table v-loading="loading" :data="rows" border max-height="520">
      <el-table-column :label="$t('ui.tc.schedule.factoryCode')" min-width="110" prop="factoryCode" />
      <el-table-column :label="$t('ui.tc.schedule.scheduleDate')" min-width="115" prop="scheduleDate" />
      <el-table-column :label="$t('ui.tc.schedule.batchNo')" min-width="170" prop="batchNo" show-overflow-tooltip />
      <el-table-column :label="$t('ui.tc.schedule.taskBusinessKey')" min-width="180" prop="taskBusinessKey" show-overflow-tooltip />
      <el-table-column :label="$t('ui.tc.schedule.sidewallCode')" min-width="140" prop="sidewallCode" />
      <el-table-column :label="$t('ui.tc.schedule.glueCode')" min-width="120" prop="glueCode" />
      <el-table-column :label="$t('ui.tc.schedule.mouthPlateCode')" min-width="120" prop="mouthPlateCode" />
      <el-table-column :label="$t('ui.tc.schedule.shiftOrder')" prop="shiftOrder" width="90" />
      <el-table-column :label="$t('ui.tc.schedule.demandQty')" prop="demandQty" width="110" />
      <el-table-column :label="$t('ui.tc.schedule.planQty')" prop="planQty" width="110" />
      <el-table-column :label="$t('ui.tc.schedule.unplannedReason')" min-width="190" prop="unplannedReasonDesc" show-overflow-tooltip />
      <el-table-column :label="$t('ui.data.btn.option')" fixed="right" width="110">
        <template slot-scope="scope">
          <el-button type="text" @click="$refs.explainRef.showUnplanned(scope.row.id)">
            {{ $t('ui.tc.schedule.viewExplain') }}
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
      <el-button @click="visible = false">{{ $t('ui.tc.schedule.close') }}</el-button>
    </span>
    <explain-drawer ref="explainRef" />
  </el-dialog>
</template>

<script>
import {listUnplanned} from '@/api/tc/tcScheduleResult'
import ExplainDrawer from './ExplainDrawer.vue'

export default {
  name: 'TcUnplannedDialog',
  components: { ExplainDrawer },
  data() {
    return {
      visible: false,
      loading: false,
      rows: [],
      total: 0,
      pageNum: 1,
      pageSize: 20,
      query: {}
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
        const page = await listUnplanned({
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
    }
  }
}
</script>
