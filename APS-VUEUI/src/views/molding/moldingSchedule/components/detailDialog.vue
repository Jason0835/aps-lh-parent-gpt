<template>
  <el-dialog
    :title="$t('车次明细')"
    :visible.sync="visible"
    width="80%"
    append-to-body
    destroy-on-close
  >
    <el-table v-loading="loading" :data="data" border height="500">
      <el-table-column
        v-for="column in columns"
        :key="column.prop"
        :label="column.label"
        :prop="column.prop"
        :min-width="column.minWidth || 140"
        :show-overflow-tooltip="!column.children"
      >
        <template v-if="column.children">
          <el-table-column
            v-for="child in column.children"
            :key="child.prop"
            :label="child.label"
            :prop="child.prop"
            :min-width="child.minWidth || 140"
            show-overflow-tooltip
          />
        </template>
      </el-table-column>
    </el-table>
  </el-dialog>
</template>

<script>
import request from "@/utils/request";

const BASE_COLUMNS = [
  { label: "所属主表ID", prop: "mainId" },
  { label: "成型机台编码", prop: "cxMachineCode" },
  { label: "班次编码", prop: "shiftCode" },
  { label: "计划日期", prop: "scheduleDate" },
  { label: "胎胚代码", prop: "embryoCode" },
  { label: "主物料(胎胚描述)", prop: "mainMaterialDesc", minWidth: 220 },
  { label: "物料编码", prop: "materialCode" },
];

const SHIFT_COLUMN_KEYS = [
  { suffix: "TripNo", label: "车次号", minWidth: 140 },
  { suffix: "TripCapacity", label: "车次容量（整车条数）", minWidth: 180 },
  { suffix: "StockHours", label: "库存可供硫化时长", minWidth: 180 },
  { suffix: "Sequence", label: "顺位", minWidth: 120 },
  { suffix: "PlanStartTime", label: "计划开始时间", minWidth: 180 },
  { suffix: "PlanEndTime", label: "计划结束时间", minWidth: 180 },
];

const SHIFT_LABELS = ["一班", "二班", "三班", "四班", "五班", "六班", "七班", "八班"];

const SHIFT_GROUP_COLUMNS = SHIFT_LABELS.map((shiftLabel, index) => {
  const classNum = index + 1;
  return {
    label: shiftLabel,
    prop: `class${classNum}`,
    minWidth: 900,
    children: SHIFT_COLUMN_KEYS.map((item) => ({
      label: item.label,
      prop: `class${classNum}${item.suffix}`,
      minWidth: item.minWidth,
    })),
  };
});

const DETAIL_COLUMNS = [...BASE_COLUMNS, ...SHIFT_GROUP_COLUMNS];

export default {
  name: "MoldingScheduleDetailDialog",
  data() {
    return {
      visible: false,
      loading: false,
      data: [],
      columns: DETAIL_COLUMNS,
    };
  },
  methods: {
    async show(mainId) {
      if (mainId === undefined || mainId === null || mainId === "") {
        this.$modal.msgWarning(this.$t("主表id不能为空"));
        return;
      }
      this.visible = true;
      this.loading = true;
      this.data = [];
      try {
        const res = await request({
          url: `/cx/cxScheduleDetail/listByMainId/${mainId}`,
          method: "get",
        });
        const rows = Array.isArray(res?.rows)
          ? res.rows
          : Array.isArray(res)
          ? res
          : [];
        this.data = rows;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
  },
};
</script>
