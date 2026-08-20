
<template>
  <basic-container>
    <page-table
      tableRef="cxFixedMachineMainTable"
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
          v-hasPermi="['monthplan:mdmMoldingMachine:edit']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <el-button
          type="danger"
          v-hasPermi="['monthplan:mdmMoldingMachine:remove']"
          :disabled="selection.length== 0"
          @click="handleDeleteAll"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <el-button
          v-hasPermi="['monthplan:mdmMoldingMachine:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          @click="handleExport"
          v-hasPermi="['monthplan:mdmMoldingMachine:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <!-- <tlt-upload
      ref="tltUpload"
      downloadUrl="/monthplan/mdmMoldingMachine/importTemplate"
      uploadUrl="/monthplan/mdmMoldingMachine/importData"
      @uploadSuccess="getList"
    /> -->
    <tlt-upload-form
      ref="tltUpload"
      :updateSupport="true"
      downloadUrl="/monthplan/mdmMoldingMachine/importTemplate"
      uploadUrl="/monthplan/mdmMoldingMachine/importData"
      @uploadSuccess="getList"
      labelWidth="0"
      :columns="importColumns"
    ></tlt-upload-form>
    <infoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>
<script>
//lib
import { mapState } from "vuex";
//utils
import { downloadLink } from "@/utils/request";

import {
  listMdmMoldingMachine,
  saveMdmMoldingMachine,
  removeMdmMoldingMachine,
} from "@/api/monthplan/mdmMoldingMachine";

//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";
import TltUploadForm from "@/views/components/tltUploadForm.vue";
import infoDialog from "./components/infoDialog.vue";

export default {
  name: "FormingCapacity",
  components: {
    tltUpload,
    infoDialog,
    TltUploadForm
  },
  dicts: ["roll_over_type", "biz_yes_no", "biz_factory_name",'biz_machine_brand','biz_class_type','cx_machine_type_code', "biz_available_status"],
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
      moldingMachines: (state) => state.molding.machines,
    }),
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.factoryCode"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
        },
        {
          prop: "cxMachineCode",
          label: this.$t("setting.machine.machineCode"),
        },
        {
          prop: "cxMachineTypeCode",
          label: this.$t("ui.data.column.curingPlan.cxMachineTypeCode"),
          minWidth: 100,
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.cx_machine_type_code, value);
          },
        },
        {
          prop: "cxMachineBrandCode",
          label: this.$t("ui.data.column.docMoldingMachine.moldingMachineClassName"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_machine_brand, value);
          },
        },
        {
          prop: "rollOverType",
          label: this.$t("ui.data.column.capsuleChuck.rollOverType"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.roll_over_type, value);
          },
        },
        {
          prop: "isZeroRack",
          label: this.$t("ui.data.column.capsuleChuck.isZeroRack"),
          align: 'center',
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },
        },
        {
          prop: "lhMachineMaxQty",
          label: this.$t("ui.data.column.capsuleChuck.lhMachineMaxQty"),
        },
        {
          prop: "maxDayCapacity",
          label: this.$t("ui.data.column.capsuleChuck.maxDayCapacity"),
        },
        {
          prop: "isActive",
          label: this.$t("ui.data.column.machine.status"),
          align: "center",
          minWidth: 100,
          render: ({ row }) => {
            return (
              <el-switch
                active-value="1"
                inactive-value="0"
                disabled={this.loading}
                value={row.isActive}
                onChange={(val) => {
                  let confirmMsg = val == "0" ? this.$t("ui.lhMachineInfo.confirm.disable") : this.$t("ui.lhMachineInfo.confirm.enable");
                  this.$confirm(confirmMsg, { type: "warning" }).then(
                    async () => {
                      try {
                        this.loading = true;
                        const data = await saveMdmMoldingMachine({
                          ...row,
                          isActive: val,
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
          label: this.$t("common.remark"),
        },
        {
          prop: "updateBy",
          align: "center",
          label: this.$t("ui.data.column.updateBy"),
          width: 100,
        },
        {
          prop: "updateTime",
          width: 180,
          label: this.$t("ui.data.column.updateTime"),
        },

        {
          align: "center",
          label: this.$t("ui.data.btn.option"),
          fixed: "right",
          render: ({ row }) => {
            return (
              <div>
                <el-button
                  v-hasPermi={["monthplan:mdmMoldingMachine:edit"]}
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.update")}
                </el-button>
                <el-button
                  v-hasPermi={["monthplan:mdmMoldingMachine:remove"]}
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

      const optionColumn = columns.pop();
      columns.push(...this.buildMonthStructureColumns());
      columns.push(optionColumn);
      return columns;
    },
    searchColumns() {
      return [
        {
          prop: "factoryCode",
          label: this.$t("ui.data.column.factoryCode"),
          type: "select",
          filterable: true,
          dictData: this.dict.type.biz_factory_name,
        },

        {
          prop: "cxMachineCode",
          label: this.$t("setting.machine.machineCode"),
        },
        {
          prop: "isZeroRack",
          label: this.$t("ui.data.column.capsuleChuck.isZeroRack"),
          type: "select",
          filterable: true,
          dictData: this.dict.type.biz_yes_no,
        },
        {
          prop: "cxMachineTypeCode",
          label: this.$t("ui.data.column.curingPlan.cxMachineTypeCode"),
          type: "select",
          filterable: true,
          dictData: this.dict.type.cx_machine_type_code,
        },
        {
          prop: "isActive",
          label: this.$t("ui.data.column.machine.status"),
          render: (form) => {
            return (
              <dict-select
                v-model={form.isActive}
                options={this.dict.type.biz_available_status}
              />
            );
          },
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
        removeMdmMoldingMachine({ ids }).then((data) => {
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
        removeMdmMoldingMachine({ ids }).then((data) => {
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
        //默认排序
        this.sort = {};
      }
      this.getList();
    },
    handleSelectionChange(rows) {
      this.selection = rows;
    },
    handleExport() {
      downloadLink("/monthplan/mdmMoldingMachine/export", this.formatParams(false));
    },
    buildMonthStructureColumns() {
      const columns = [];
      const current = new Date();
      for (let i = 1; i <= 12; i++) {
        const date = new Date(current.getFullYear(), current.getMonth() - i, 1);
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, "0");
        const shortYear = String(year).slice(2);
        columns.push({
          prop: `monthStructureNameMap.${year}-${month}`,
          label: `${shortYear}-${month}`,
          minWidth: 180,
          align: "left",
        });
      }
      return columns;
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
    // api
    async getList() {
      try {
        this.loading = true;
        const data = await listMdmMoldingMachine(this.formatParams());
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

  },
};
</script>
<style lang="scss" scoped>
.more-btn {
  margin: 2px 0;
  width: 100%;
}
</style>
