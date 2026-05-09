<template>
  <el-dialog
    :title="$t('ui.data.column.monthPlanFinalAdjustQuery.viewAdjustVersion')"
    :visible.sync="visible"
    width="900px"
    append-to-body
    :close-on-click-modal="false"
    @open="handleOpen"
    @close="handleClose"
  >
    <div v-loading="loading">
      <el-table :data="list" border size="small" max-height="440">
        <el-table-column
          :label="
            $t('ui.data.column.monthPlanFinalAdjustQuery.monthPlanVersionNo')
          "
          prop="monthPlanVersion"
          min-width="140"
          show-overflow-tooltip
        />
        <el-table-column
          :label="
            $t('ui.data.column.monthPlanFinalAdjustQuery.adjustVersionNo')
          "
          prop="version"
          min-width="160"
          show-overflow-tooltip
        />
        <el-table-column
          :label="$t('ui.data.column.scheduleAdjust.adjustType')"
          prop="adjustType"
          min-width="120"
          show-overflow-tooltip
        >
          <template slot-scope="scope">
            {{
              selectDictLabel(
                dict.type.week_roll_adjust_type,
                scope.row.adjustType
              ) ||
              scope.row.adjustType ||
              ""
            }}
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('ui.data.column.monthPlanFinalAdjustQuery.adjustBy')"
          prop="updateBy"
          width="120"
          show-overflow-tooltip
        />
        <el-table-column
          :label="$t('ui.data.column.monthPlanFinalAdjustQuery.adjustTime')"
          prop="updateTime"
          width="170"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ parseTime(scope.row.updateTime) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <span slot="footer" class="dialog-footer">
      <el-button @click="visible = false">{{
        $t("ui.frame.btn.close")
      }}</el-button>
    </span>
  </el-dialog>
</template>

<script>
import { adjustVersionList } from "@/api/monthplan/adjustStructure";

export default {
  name: "AdjustVersionDialog",
  dicts: ["week_roll_adjust_type"],
  data() {
    return {
      visible: false,
      loading: false,
      list: [],
      queryPayload: {},
    };
  },
  methods: {
    /**
     * 打开弹窗；query 可为空对象，或与列表页一致传入 factoryCode、year、month
     */
    show(query) {
      this.queryPayload = query && typeof query === "object" ? { ...query } : {};
      this.visible = true;
    },
    handleOpen() {
      this.fetchList();
    },
    handleClose() {
      this.list = [];
      this.queryPayload = {};
    },
    async fetchList() {
      this.loading = true;
      try {
        const res = await adjustVersionList(this.queryPayload);
        this.list = res.rows != null ? res.rows : [];
      } catch (e) {
        console.error(e);
        this.list = [];
      } finally {
        this.loading = false;
      }
    },
  },
};
</script>
