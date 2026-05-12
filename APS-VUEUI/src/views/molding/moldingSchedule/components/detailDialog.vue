<template>
  <el-dialog
    :title="$t('车次明细')"
    :visible.sync="visible"
    width="80%"
    append-to-body
    destroy-on-close
  >
    <el-table v-loading="loading" :data="pagedData" border height="500">
      <el-table-column
        v-for="column in columns"
        :key="column.prop"
        :label="column.label"
        :prop="column.prop"
        :min-width="column.minWidth || 140"
        :show-overflow-tooltip="!column.children"
        :formatter="column.formatter"
      >
        <template v-if="column.children">
          <el-table-column
            v-for="child in column.children"
            :key="child.prop"
            :label="child.label"
            :prop="child.prop"
            :min-width="child.minWidth || 140"
          >
            <template v-slot="{ row }">
              <el-input
                v-model="row[child.prop]"
                size="small"
                placeholder="请输入"
                clearable
              />
            </template>
          </el-table-column>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-if="data.length > 0"
      style="margin-top: 10px; text-align: right;"
      :current-page="page.current"
      :page-sizes="[10, 20, 50, 100]"
      :page-size="page.pageSize"
      :total="page.total"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />
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
  { label: "车次号", prop: "tripNo", minWidth: 100,
    formatter: (row, column, value) => {
      if (!value) return "";
      return value + "车";
    }
  },
  { label: "车次容量（整车条数）", prop: "tripCapacity", minWidth: 160 },
];

const SHIFT_COLUMN_KEYS = [
  { suffix: "PlanQty", label: "计划数", minWidth: 120 },
  { suffix: "StockHours", label: "库存可供硫化时长", minWidth: 180 },
  { suffix: "Sequence", label: "顺位", minWidth: 100 },
];

const SHIFT_LABELS = ["一班", "二班", "三班", "四班", "五班", "六班", "七班", "八班"];

const SHIFT_GROUP_COLUMNS = SHIFT_LABELS.map((shiftLabel, index) => {
  const classNum = index + 1;
  return {
    label: shiftLabel,
    prop: `class${classNum}`,
    minWidth: 440,
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
      page: {
        current: 1,
        pageSize: 20,
        total: 0,
      },
    };
  },
  computed: {
    pagedData() {
      const start = (this.page.current - 1) * this.page.pageSize;
      const end = start + this.page.pageSize;
      return this.data.slice(start, end);
    },
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
      this.page.current = 1;
      this.page.total = 0;
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
        this.page.total = rows.length;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
    handleSizeChange(pageSize) {
      this.page.pageSize = pageSize;
      this.page.current = 1;
    },
    handleCurrentChange(current) {
      this.page.current = current;
    },
  },
};
</script>
