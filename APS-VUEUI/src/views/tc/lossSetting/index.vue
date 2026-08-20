<template>
  <basic-container>
    <page-table
      v-loading="loading"
      :calcHeight="true"
      :columns="columns"
      :data="data"
      :page="page"
      :search="search"
      :searchColumns="searchColumns"
      :selectArea="false"
      :showSummary="false"
      tableRef="tcLossSettingMainTable"
      @pageChange="handlePageChange"
      @refresh="getList"
      @search="handleSearch"
      @sort-change="handleSortChange"
      @selection-change="handleSelectionChange"
    >
      <template slot="header">
        <el-button
          v-hasPermi="['tc:tcLossSetting:edit']"
          plain
          type="primary"
          @click="handleAdd"
        >{{ $t("ui.frame.btn.add") }}</el-button>
        <el-button
          v-hasPermi="['tc:tcLossSetting:remove']"
          :disabled="selection.length == 0"
          type="danger"
          @click="handleDeleteAll"
        >{{ $t("ui.frame.btn.delete") }}</el-button>
        <el-button
          v-hasPermi="['tc:tcLossSetting:import']"
          @click="$refs.tltUpload.handleImport()"
        >{{ $t("ui.frame.btn.import") }}</el-button>
        <el-button
          v-hasPermi="['tc:tcLossSetting:export']"
          @click="handleExport"
        >{{ $t("ui.frame.btn.export") }}</el-button>
      </template>
    </page-table>
    <tlt-upload-form
      ref="tltUpload"
      :columns="importColumns"
      :updateSupport="true"
      downloadUrl="/tc/tcLossSetting/importTemplate"
      labelWidth="0"
      uploadUrl="/tc/tcLossSetting/importData"
      @uploadSuccess="getList"
    ></tlt-upload-form>
    <infoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>
<script>
import {mapState} from "vuex";
import {downloadLink} from "@/utils/request";
import {listTcLossSetting, removeTcLossSetting, saveTcLossSetting} from "@/api/tc/lossSetting";
import tltUpload from "@/components/tltUpload/tltUpload.vue";
import TltUploadForm from "@/views/components/tltUploadForm.vue";
import infoDialog from "./components/infoDialog.vue";

export default {
  name: "/tc/tcLossSetting",
  components: {
    tltUpload,
    infoDialog,
    TltUploadForm,
  },
  dicts: ["biz_factory_name", "biz_yes_no"],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    return {
      importColumns: [
        {
          label: "",
          prop: "updateSupport",
          render: (form) => {
            return (
              <el-checkbox
                label={this.$t("common.rule.updateSupport")}
                v-model={form.updateSupport}
              >
                {this.$t("common.rule.updateSupport")}
              </el-checkbox>
            );
          },
        },
      ],
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
      importDefaultValue: {},
      importRules: {},
    };
  },
  computed: {
    ...mapState({
      machines: (state) => state.tc.machines,
    }),
    columns() {
      return [
        { type: "selection", fixed: "left" },
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.tc.lossSetting.factoryCode"),
          type: "select",
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
        },
        {
          prop: "sidewallCode",
          halign: "center",
          label: this.$t("ui.data.column.tc.lossSetting.sidewallCode"),
        },
        {
          prop: "machineCode",
          halign: "center",
          label: this.$t("ui.data.column.tc.lossSetting.machineCode"),
        },
        {
          prop: "lossRate",
          halign: "center",
          label: this.$t("ui.data.column.tc.lossSetting.lossRate"),
        },
        {
          align: "center",
          prop: "enableStatus",
          halign: "center",
          label: this.$t("ui.data.column.tc.lossSetting.enableStatus"),
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
                        const data = await saveTcLossSetting({
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
          minWidth: 160,
          width: 160,
          fixed: "right",
          render: ({ row }) => {
            return (
              <div>
                <el-button
                  v-hasPermi={["tc:tcLossSetting:edit"]}
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.update")}
                </el-button>
                <el-button
                  v-hasPermi={["tc:tcLossSetting:remove"]}
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
    },
    searchColumns() {
      return [
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.tc.lossSetting.factoryCode"),
          type: "select",
          dictData: this.dict.type.biz_factory_name,
        },
        {
          prop: "sidewallCode",
          label: this.$t("ui.data.column.tc.lossSetting.sidewallCode"),
        },
        {
          prop: "machineCode",
          label: this.$t("ui.data.column.tc.lossSetting.machineCode"),
          type: "select",
          dictData: this.machines,
          labelKey: "machineCode",
          valueKey: "machineCode",
          filterable: true,
        },
        {
          prop: "enableStatus",
          label: this.$t("ui.data.column.tc.lossSetting.enableStatus"),
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
        removeTcLossSetting({ ids }).then((data) => {
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
        removeTcLossSetting({ ids }).then((data) => {
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
    handelSuccess() {
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
      downloadLink("/tc/tcLossSetting/export", this.formatParams(false));
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
        const data = await listTcLossSetting(this.formatParams());
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
    this.$store.dispatch("tc/getMachineList");
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
<style lang="scss" scoped>
.more-btn {
  margin: 2px 0;
  width: 100%;
}
</style>
