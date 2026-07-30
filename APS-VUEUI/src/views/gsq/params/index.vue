
<template>
  <basic-container>
    <page-table
      tableRef="gsqParamsMainTable"
      :calcHeight="true"
      v-loading="loading"
      :columns="columns"
      :searchColumns="searchColumns"
      :data="data"
      :page="page"
      :search="search"
      @refresh="getList"
      @search="handleSearch"
      @pageChange="handlePageChange"
      @sort-change="handleSortChange"
      @selection-change="handleSelectionChange"
      :showSummary="false"
      :selectArea="false"
    >
      <template slot="header">
        <el-button
          type="primary"
          plain
          v-hasPermi="['gsq:params:edit']"
          @click="handleAdd"
        >{{ $t("ui.frame.btn.add") }}</el-button>
        <el-button
          type="danger"
          v-hasPermi="['gsq:params:remove']"
          :disabled="selection.length == 0"
          @click="handleDeleteAll"
        >{{ $t("ui.frame.btn.delete") }}</el-button>
        <el-button
          @click="handleExport"
          v-hasPermi="['gsq:params:export']"
        >{{ $t("ui.frame.btn.export") }}</el-button>
      </template>
    </page-table>
    <infoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>
<script>
import {downloadLink} from "@/utils/request";
import {listParams, removeParams, saveParams} from "@/api/gsq/params";
import infoDialog from "./components/infoDialog.vue";

export default {
  name: "GsqParams",
  components: {
    infoDialog,
  },
  dicts: ["biz_factory_name", "biz_yes_no"],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    return {
      loading: false,
      data: [],
      selection: [],
      page: {
        current: 1,
        pageSize: 20,
        total: 0,
      },
      sort: {},
      search: {},
      query: {},
    };
  },
  computed: {
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.factoryCode"),
          type: "select",
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
        },
        {
          prop: "paramCode",
          halign: "center",
          label: this.$t("ui.data.column.gsq.params.paramCode"),
        },
        {
          prop: "paramName",
          halign: "center",
          label: this.$t("ui.data.column.gsq.params.paramName"),
          titleTooltip: true,
        },
        {
          prop: "paramValue",
          halign: "center",
          label: this.$t("ui.data.column.gsq.params.paramValue"),
        },
        {
          prop: "paramGroup",
          halign: "center",
          label: this.$t("ui.data.column.gsq.params.paramGroup"),
          formatter: (row, column, value) => {
            const map = { GLOBAL: "全局参数", SHIFT: "班次参数", MACHINE: "机台参数" };
            return map[value] || value;
          },
        },
        {
          prop: "valueType",
          halign: "center",
          label: this.$t("ui.data.column.gsq.params.valueType"),
          formatter: (row, column, value) => {
            const map = { STRING: "字符串", NUMBER: "数值", BOOLEAN: "布尔", JSON: "结构化对象" };
            return map[value] || value;
          },
        },
        {
          prop: "enableStatus",
          halign: "center",
          label: this.$t("ui.data.column.gsq.params.enableStatus"),
          render: ({ row }) => {
            return (
              <el-switch
                active-value="1"
                inactive-value="0"
                disabled={this.loading}
                value={row.enableStatus}
                onChange={(val) => {
                  let confirmMsg = val == "0"
                    ? this.$t("ui.lhMachineInfo.confirm.disable")
                    : this.$t("ui.lhMachineInfo.confirm.enable");
                  this.$confirm(confirmMsg, { type: "warning" }).then(
                    async () => {
                      try {
                        this.loading = true;
                        const data = await saveParams({
                          ...row,
                          enableStatus: val,
                        });
                        this.$modal.msgSuccess(data.msg);
                        this.getList();
                      } catch (error) {
                        console.error(error);
                      } finally {
                        this.loading = false;
                      }
                    }
                  );
                }}
              ></el-switch>
            );
          },
        },
        {
          prop: "remark",
          halign: "center",
          label: this.$t("ui.common.column.remark"),
          minWidth: 100,
        },
        {
          prop: "updateTime",
          label: this.$t("ui.data.column.updateTime"),
          width: 180,
        },
        {
          align: "center",
          halign: "center",
          label: this.$t("ui.data.btn.option"),
          minWidth: 180,
          width: 200,
          fixed: "right",
          render: ({ row }) => {
            return (
              <div>
                <el-button
                  v-hasPermi={["gsq:params:edit"]}
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.update")}
                </el-button>
                <el-button
                  v-hasPermi={["gsq:params:remove"]}
                  class="minus"
                  type="danger"
                  onClick={() => this.handleDelete(row)}
                >
                  {this.$t("ui.frame.btn.delete")}
                </el-button>
              </div>
            );
          },
        },
      ];

      return columns;
    },
    searchColumns() {
      return [
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.factoryCode"),
          type: "select",
          dictData: this.dict.type.biz_factory_name,
        },
        {
          prop: "paramCode",
          label: this.$t("ui.data.column.gsq.params.paramCode"),
        },
        {
          prop: "paramName",
          label: this.$t("ui.data.column.gsq.params.paramName"),
        },
        {
          prop: "paramGroup",
          label: this.$t("ui.data.column.gsq.params.paramGroup"),
          type: "select",
          options: [
            { label: "全局参数", value: "GLOBAL" },
            { label: "班次参数", value: "SHIFT" },
            { label: "机台参数", value: "MACHINE" },
          ],
        },
        {
          prop: "valueType",
          label: this.$t("ui.data.column.gsq.params.valueType"),
          type: "select",
          options: [
            { label: "字符串", value: "STRING" },
            { label: "数值", value: "NUMBER" },
            { label: "布尔", value: "BOOLEAN" },
            { label: "结构化对象", value: "JSON" },
          ],
        },
        {
          prop: "enableStatus",
          label: this.$t("ui.data.column.gsq.params.enableStatus"),
          type: "select",
          dictData: this.dict.type.biz_yes_no,
        },
      ];
    },
  },
  methods: {
    handleAdd() {
      if (this.$refs.infoRef) {
        this.$refs.infoRef.show();
      }
    },
    handleEdit(row) {
      if (this.$refs.infoRef) {
        this.$refs.infoRef.show(row);
      }
    },
    handleDelete(row) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = row.id;
        removeParams({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleDeleteAll() {
      let ids = "";
      for (let i = 0; i < this.selection.length; i++) {
        if (i == this.selection.length - 1) {
          ids = ids + this.selection[i].id;
        } else {
          ids = ids + this.selection[i].id + ",";
        }
      }
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        removeParams({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleSearch(data) {
      this.query = data;
      this.$set(this.page, "current", 1);
      this.getList();
    },
    handlePageChange(current, pageSize) {
      this.$set(this.page, "current", current);
      this.$set(this.page, "pageSize", pageSize);
      this.getList();
    },
    handleSortChange({ column, prop, order }) {
      if (order) {
        this.sort = {
          orderByColumn: prop,
          isAsc: order == "ascending" ? "asc" : "desc",
        };
      } else {
        this.sort = {};
      }
      this.getList();
    },
    handleSelectionChange(rows) {
      this.selection = rows;
    },
    handleExport() {
      downloadLink("/gsq/params/export", this.formatParams(false));
    },
    formatParams(hasPage = true) {
      const params = {
        ...this.query,
        ...this.sort,
      };

      if (hasPage) {
        params.pageSize = this.page.pageSize;
        params.pageNum = this.page.current;
      }

      if (params.createTime && params.createTime[0]) {
        params.createTimeStart = params.createTime[0];
        params.createTimeEnd = params.createTime[1];
        params.createTime = undefined;
      }

      return params;
    },
    async getList() {
      try {
        this.loading = true;
        const data = await listParams(this.formatParams());
        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
  },
  created() {
    let defaultParams = {
      factoryCode: "116",
    };
    this.search = {
      ...defaultParams,
    };
    this.query = {
      ...defaultParams,
    };
    this.getList();
  },
  activated() {
    // this.getList();
  },
};
</script>
