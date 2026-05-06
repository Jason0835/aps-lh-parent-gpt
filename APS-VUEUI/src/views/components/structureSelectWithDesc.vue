<template>
  <select-dialog
    ref="dialogRef"
    :title="getTitle()"
    :value="showValue"
    :disabled="disabled"
    @confirm="handleConfirm"
    @clear="handleClear"
    @show="handleShow"
    @cancel="handleCancel"
  >
    <div class="table-container" v-loading="loading">
      <page-table
        max-height="400"
        :calcHeight="false"
        ref="tableRef"
        :columns="columns"
        :searchColumns="searchColumns"
        :data="data"
        :toolbar="false"
        :page="page"
        :search="search"
        :highlight-current-row="true"
        @current-change="handleCurrentChange"
        @row-dblclick="handleDbClick"
        @pageChange="handlePageChange"
        @selection-change="handleSelectionChange"
        @search="handleSearch"
      />
    </div>
  </select-dialog>
</template>

<script>
import { deepClone } from "@/utils";
import selectDialog from "@/components/Table/SelectDialog.vue";
import { selectSkuStructureWithDesc } from "@/api/monthplan/skuStructure";

export default {
  components: { selectDialog },
  inject: ["parentDict"],
  model: {
    prop: "value",
    event: "change",
  },
  props: {
    value: [String, Number],
    title: String,
    disabled: Boolean,
    factoryCode: [String, Number],
    machineType: [String, Number],
    label: String,
    oldList: Array,
    multiple: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      searchKey: "",
      search: {},
      filterKey: "",
      page: {
        current: 1,
        pageSize: 10,
        total: 0,
      },
      query: {},
      showValue: "",
      loading: false,
      valueProp: "structureName",
      labelProp: "structureName",
      data: [],
    };
  },
  computed: {
    searchColumns() {
      return [
        {
          prop: "structureName",
          label: this.$t("ui.data.column.cxStructureTreadConfig.structureCode"),
        },
        {
          prop: "embryoCode",
          label: this.$t("ui.data.column.cxStructureTreadConfig.embryoCode"),
        },
        {
          prop: "mainMaterialDesc",
          label: this.$t("ui.data.column.cxStructureTreadConfig.mainMaterialDesc"),
        },
      ];
    },
    columns() {
      const list = [
        {
          prop: "structureName",
          label: this.$t("ui.data.column.cxStructureTreadConfig.structureCode"),
          minWidth: 220,
        },
        {
          prop: "embryoCode",
          label: this.$t("ui.data.column.cxStructureTreadConfig.embryoCode"),
          minWidth: 160,
        },
        {
          prop: "mainMaterialDesc",
          label: this.$t("ui.data.column.cxStructureTreadConfig.mainMaterialDesc"),
          minWidth: 250,
          align: "left",
        },
      ];
      if (this.multiple) {
        list.unshift({
          type: "selection",
          reserveSelection: true,
        });
      }
      return list;
    },
  },
  watch: {
    value(val) {
      if (!val) {
        this.showValue = "";
      }
    },
    label(val) {
      this.showValue = val || this.value;
    },
  },
  methods: {
    async getList() {
      try {
        this.loading = true;
        const data = await selectSkuStructureWithDesc(this.formatParams());
        this.data = data.rows || [];
        this.page.total = data.total || 0;
      } catch (error) {
        console.log(error);
      } finally {
        this.loading = false;
      }
    },
    formatParams() {
      return {
        pageSize: this.page.pageSize,
        pageNum: this.page.current,
        machineType: this.machineType,
        ...this.query,
        status: 0,
      };
    },
    getTitle() {
      return this.title || this.$t("common.button.select");
    },
    handleSearch(query) {
      this.query = query;
      this.$set(this.page, "current", 1);
      this.getList();
    },
    handlePageChange(current, pageSize) {
      this.$set(this.page, "current", current);
      this.$set(this.page, "pageSize", pageSize);
      this.getList();
    },
    handleShow() {
      const defaultParams = {
        factoryCode: this.factoryCode || "116",
      };
      this.search = { ...defaultParams };
      this.query = { ...defaultParams };
      this.getList();
    },
    handleCancel() {
      this.data = [];
      this.query = {
        factoryCode: "116",
      };
      this.searchKey = "";
      this.filterKey = "";
      this.page = {
        current: 1,
        pageSize: 10,
        total: 0,
      };
    },
    handleCurrentChange(row) {
      this.currentRow = row;
    },
    handleDbClick(row) {
      this.currentRow = row;
      this.handleConfirm(() => {
        this.$refs.dialogRef.hide();
      });
    },
    handleSelectionChange(rows) {
      this.selection = rows;
    },
    handleClear() {
      if (this.$refs.tableRef) {
        const tableRef = this.$refs.tableRef.getTableRef();
        tableRef && tableRef.setCurrentRow();
      }
      this.showValue = "";
      this.$emit("updateValue", undefined);
      this.$emit("clear");
      this.$emit("change", undefined);
    },
    handleConfirm(done) {
      if (!this.multiple) {
        if (this.currentRow) {
          this.showValue = `${this.currentRow[this.labelProp]}`;
          this.$emit("updateValue", this.currentRow[this.valueProp]);
          this.$emit("change", this.currentRow[this.valueProp], {
            ...this.currentRow,
          });
        }
        done();
        return;
      }

      if (this.selection) {
        const ids = this.selection.map((item) => item[this.valueProp]).join(",");
        this.showValue = this.selection.map((item) => item[this.labelProp]).join(",");
        this.$emit("updateValue", ids);
        this.$emit("change", ids, deepClone(this.selection));
        done();
      } else {
        this.$modal.msgWarning(this.$t("common.rule.select"));
      }
    },
  },
  mounted() {
    this.showValue = this.label || this.value;
  },
};
</script>

<style lang="scss" scoped>
.table-container {
  height: 450px;
}
</style>
