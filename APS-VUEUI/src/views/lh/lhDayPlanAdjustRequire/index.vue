<template>
  <basic-container>
    <page-table
      tableRef="lhDayPlanAdjustRequireMainTable"
      :calcHeight="true"
      v-loading="loading"
      :columns="columns"
      :searchColumns="searchColumns"
      :data="data"
      :page="page"
      :search="search"
      @refresh="handleRefresh"
      @search="handleSearch"
      @pageChange="handlePageChange"
      :showSummary="false"
      :selectArea="false"
    />
  </basic-container>
</template>

<script>
import moment from "moment";
import {
  listLhDayPlanAdjustRequire,
  saveLhDayPlanAdjustRequire,
} from "@/api/lh/lhDayPlanAdjustRequire";

const ADJUST_SLOT_COUNT = 3;

export default {
  name: "LhDayPlanAdjustRequire",
  dicts: ["biz_factory_name", "lh_trial_status"],
  data() {
    const currentYearMonth = moment().format("yyyy-MM");
    return {
      loading: false,
      data: [],
      savingRowKeys: {},
      page: {
        current: 1,
        pageSize: 20,
        total: 0,
      },
      search: {
        factoryCode: "116",
        yearMonth: currentYearMonth,
      },
      query: {
        factoryCode: "116",
        yearMonth: currentYearMonth,
      },
    };
  },
  computed: {
    columns() {
      return [
        {
          prop: "materialCode",
          label: this.$t("ui.data.column.lhDayPlanAdjustRequire.materialCode"),
          minWidth: 150,
          fixed: "left",
        },
        {
          prop: "materialDesc",
          label: this.$t("ui.data.column.lhDayPlanAdjustRequire.materialDesc"),
          minWidth: 260,
          align: "left",
        },
        {
          prop: "monthPlanQty",
          label: this.$t("ui.data.column.lhDayPlanAdjustRequire.monthPlanQty"),
          minWidth: 110,
          align: "right",
        },
        {
          label: this.$t("ui.data.column.lhDayPlanAdjustRequire.adjustGroup"),
          align: "center",
          children: this.buildAdjustQtyColumns(),
        },
        {
          prop: "adjustedTotalQty",
          label: this.$t("ui.data.column.lhDayPlanAdjustRequire.adjustedTotalQty"),
          minWidth: 130,
          align: "right",
          render: ({ row }) => this.calculateAdjustedTotal(row),
        },
        {
          prop: "treadGlueTd",
          label: this.$t("ui.data.column.lhDayPlanAdjustRequire.treadGlueTd"),
          minWidth: 150,
          align: "left",
        },
        {
          label: this.$t("ui.data.column.lhDayPlanAdjustRequire.reasonGroup"),
          align: "center",
          children: this.buildAdjustReasonColumns(),
        },
        {
          prop: "productStatus",
          label: this.$t("ui.data.column.lhDayPlanAdjustRequire.productStatus"),
          minWidth: 110,
          formatter: (row, column, value) =>
            this.selectDictLabel(this.dict.type.lh_trial_status, value),
        },
        {
          prop: "option",
          label: this.$t("ui.data.btn.option"),
          width: 100,
          fixed: "right",
          align: "center",
          render: ({ row }) => {
            const rowKey = this.buildRowKey(row);
            return (
              <el-button
                v-hasPermi={["lh:dayPlanAdjustRequire:save"]}
                type="primary"
                size="mini"
                icon="el-icon-check"
                loading={Boolean(this.savingRowKeys[rowKey])}
                onClick={() => this.handleSave(row)}
              >
                {this.$t("ui.frame.btn.save")}
              </el-button>
            );
          },
        },
      ];
    },
    searchColumns() {
      return [
        {
          label: this.$t("ui.data.column.lhDayPlanAdjustRequire.factoryCode"),
          prop: "factoryCode",
          type: "select",
          dictData: this.dict.type.biz_factory_name,
          filterable: true,
          clearable: false,
        },
        {
          label: this.$t("ui.data.column.lhDayPlanAdjustRequire.yearMonth"),
          prop: "yearMonth",
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
          clearable: false,
        },
        {
          label: this.$t("ui.data.column.lhDayPlanAdjustRequire.materialCode"),
          prop: "materialCode",
        },
        {
          label: this.$t("ui.data.column.lhDayPlanAdjustRequire.materialDesc"),
          prop: "materialDesc",
        },
        {
          label: this.$t("ui.data.column.lhDayPlanAdjustRequire.productStatus"),
          prop: "productStatus",
          type: "select",
          dictData: this.dict.type.lh_trial_status,
          filterable: true,
        },
      ];
    },
  },
  methods: {
    buildAdjustQtyColumns() {
      return Array.from({ length: ADJUST_SLOT_COUNT }, (item, index) => {
        const adjustIndex = index + 1;
        const fieldName = `adjustQty${adjustIndex}`;
        return {
          prop: fieldName,
          label: this.$t("ui.data.column.lhDayPlanAdjustRequire.adjust", [adjustIndex]),
          minWidth: 130,
          align: "center",
          render: ({ row }) => (
            <el-input-number
              v-model={row[fieldName]}
              controls={false}
              precision={0}
              min={-99999999}
              max={99999999}
              style="width: 112px"
            />
          ),
        };
      });
    },
    buildAdjustReasonColumns() {
      return Array.from({ length: ADJUST_SLOT_COUNT }, (item, index) => {
        const adjustIndex = index + 1;
        const fieldName = `adjustReason${adjustIndex}`;
        return {
          prop: fieldName,
          label: this.$t("ui.data.column.lhDayPlanAdjustRequire.reason", [adjustIndex]),
          minWidth: 220,
          align: "left",
          render: ({ row }) => (
            <el-input
              v-model={row[fieldName]}
              maxlength={2000}
              clearable
            />
          ),
        };
      });
    },
    buildRowKey(row) {
      return [row.factoryCode, row.yearMonth, row.materialCode, row.productStatus].join("|");
    },
    normalizeAdjustQty(value) {
      return value === null || value === undefined ? null : Number(value);
    },
    buildSnapshot(row) {
      const snapshot = {};
      for (let adjustIndex = 1; adjustIndex <= ADJUST_SLOT_COUNT; adjustIndex += 1) {
        snapshot[`adjustQty${adjustIndex}`] = this.normalizeAdjustQty(
          row[`adjustQty${adjustIndex}`]
        );
        snapshot[`adjustReason${adjustIndex}`] = row[`adjustReason${adjustIndex}`] || "";
      }
      return snapshot;
    },
    decorateRow(row) {
      const decoratedRow = { ...row };
      for (let adjustIndex = 1; adjustIndex <= ADJUST_SLOT_COUNT; adjustIndex += 1) {
        const fieldName = `adjustQty${adjustIndex}`;
        if (decoratedRow[fieldName] === null) {
          decoratedRow[fieldName] = undefined;
        }
      }
      decoratedRow._adjustSnapshot = this.buildSnapshot(decoratedRow);
      return decoratedRow;
    },
    isRowDirty(row) {
      return JSON.stringify(this.buildSnapshot(row)) !== JSON.stringify(row._adjustSnapshot || {});
    },
    hasDirtyRows() {
      return this.data.some((row) => this.isRowDirty(row));
    },
    calculateAdjustedTotal(row) {
      let totalQty = Number(row.monthPlanQty || 0);
      for (let adjustIndex = 1; adjustIndex <= ADJUST_SLOT_COUNT; adjustIndex += 1) {
        totalQty += Number(row[`adjustQty${adjustIndex}`] || 0);
      }
      return totalQty;
    },
    validateRow(row) {
      for (let adjustIndex = 1; adjustIndex <= ADJUST_SLOT_COUNT; adjustIndex += 1) {
        const adjustQty = row[`adjustQty${adjustIndex}`];
        const adjustReason = (row[`adjustReason${adjustIndex}`] || "").trim();
        if ((adjustQty === null || adjustQty === undefined) && adjustReason) {
          this.$modal.msgError(
            this.$t("ui.data.alert.lhDayPlanAdjustRequire.adjustQtyRequired", [adjustIndex])
          );
          return false;
        }
        if (adjustQty !== null && adjustQty !== undefined && !adjustReason) {
          this.$modal.msgError(
            this.$t("ui.data.alert.lhDayPlanAdjustRequire.adjustReasonRequired", [adjustIndex])
          );
          return false;
        }
      }
      return true;
    },
    buildSavePayload(row) {
      const payload = {
        factoryCode: row.factoryCode,
        yearMonth: row.yearMonth,
        materialCode: row.materialCode,
        productStatus: row.productStatus,
      };
      for (let adjustIndex = 1; adjustIndex <= ADJUST_SLOT_COUNT; adjustIndex += 1) {
        payload[`adjustId${adjustIndex}`] = row[`adjustId${adjustIndex}`];
        payload[`adjustQty${adjustIndex}`] = row[`adjustQty${adjustIndex}`];
        payload[`adjustReason${adjustIndex}`] = row[`adjustReason${adjustIndex}`];
      }
      return payload;
    },
    async confirmDiscardChanges() {
      if (!this.hasDirtyRows()) {
        return true;
      }
      try {
        await this.$confirm(
          this.$t("ui.data.alert.lhDayPlanAdjustRequire.unsavedChanges"),
          this.$t("common.prompt"),
          { type: "warning" }
        );
        return true;
      } catch (error) {
        return false;
      }
    },
    async handleSearch(data) {
      if (!(await this.confirmDiscardChanges())) {
        return;
      }
      this.query = { ...data };
      this.$set(this.page, "current", 1);
      await this.getList();
    },
    async handlePageChange(current, pageSize) {
      if (!(await this.confirmDiscardChanges())) {
        return;
      }
      this.$set(this.page, "current", current);
      this.$set(this.page, "pageSize", pageSize);
      await this.getList();
    },
    async handleRefresh() {
      if (await this.confirmDiscardChanges()) {
        await this.getList();
      }
    },
    async handleSave(row) {
      if (!this.validateRow(row)) {
        return;
      }
      const rowKey = this.buildRowKey(row);
      try {
        this.$set(this.savingRowKeys, rowKey, true);
        const result = await saveLhDayPlanAdjustRequire(this.buildSavePayload(row));
        row.adjustedTotalQty = this.calculateAdjustedTotal(row);
        row._adjustSnapshot = this.buildSnapshot(row);
        this.$modal.msgSuccess(result.msg);
      } finally {
        this.$delete(this.savingRowKeys, rowKey);
      }
    },
    formatParams() {
      return {
        ...this.query,
        yearMonth: String(this.query.yearMonth).replace("-", ""),
        pageNum: this.page.current,
        pageSize: this.page.pageSize,
      };
    },
    async getList() {
      try {
        this.loading = true;
        const result = await listLhDayPlanAdjustRequire(this.formatParams());
        this.data = (result.rows || []).map((row) => this.decorateRow(row));
        this.page.total = result.total || 0;
      } finally {
        this.loading = false;
      }
    },
  },
  beforeRouteLeave(to, from, next) {
    if (!this.hasDirtyRows()) {
      next();
      return;
    }
    this.$confirm(
      this.$t("ui.data.alert.lhDayPlanAdjustRequire.unsavedChanges"),
      this.$t("common.prompt"),
      { type: "warning" }
    )
      .then(() => next())
      .catch(() => next(false));
  },
  activated() {
    this.getList();
  },
};
</script>

<style lang="scss" scoped></style>
