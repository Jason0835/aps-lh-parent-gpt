
<template>
  <basic-container>
    <page-table
      tableRef="curingApsmoldAdjustTable"
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
          v-hasPermi="['lh:lhMachineInfo:edit']"
          type="primary"
          plain
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <!-- <el-button
          v-hasPermi="['lh:lhMachineInfo:edit']"
          type="primary"
          plain
          @click="handleEdit(selection[0])"
          :disabled="selection.length != 1"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        > -->
        <el-button
          v-hasPermi="['lh:lhMachineInfo:remove']"
          type="danger"
          plain
          @click="handleDeleteMulti"
          :disabled="selection.length == 0"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        >
        <el-button
          v-hasPermi="['lh:lhMachineInfo:import']"
          @click="() => $refs.tltUploadForm.handleImport(importDefaultValue)"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button v-hasPermi="['lh:lhMachineInfo:export']" @click="handleExport">{{
          $t("ui.frame.btn.export")
        }}</el-button>
      </template>
    </page-table>
    <tlt-upload-form
      ref="tltUploadForm"
      title=""
      downloadUrl="/lh/lhMachineInfo/importTemplate"
      uploadUrl="/lh/lhMachineInfo/importData"
      @uploadSuccess="getList"
      labelWidth="0"
      :columns="importColumns"
      :rules="importRules"
    />
    <InfoDialog ref="infoDialogRef" @success="handelSuccess" />
  </basic-container>
</template>
<script>
import moment from "moment";

import {
  listMachine,
  exportData,
  editMachine,
  removeMachine,
} from "@/api/lh/machine";
// import {
//   listVulcanizingMachine,
//   removeVulcanizingMachine,
//   editVulcanizingMachine,
// } from "@/api/mdm/vulcanizingMachine";

import InfoDialog from "./components/infoDialog.vue";
import TltUploadForm from "@/views/components/tltUploadForm.vue";
export default {
  name: "CuringMachine",
  components: { InfoDialog, TltUploadForm },
  dicts: [
    // "sys_yes_no",
    // "MOLD_CHANGE_TYPE",
    // "IS_RELEASE"
    "biz_available_status",
    "CLASS_SHIFT",
    // "CENTRIPETAL_MECHANISM",
    "CLASS_NUM",
    "CLASS_NUM_THREE",
    "biz_product_name",
    "biz_factory_name",
    "biz_mould_Type",
    "biz_yes_no",
    "is_sealed",
    "LH_MACHINE_TYPE",
  ],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    return {
      importDefaultValue: {
        updateSupport: false,
      },
      importColumns: [
        {
          label: "",
          prop: "updateSupport",
          render: (form) => {
            console.log(form);
            return (
              <el-checkbox
                label= {this.$t('common.rule.updateSupport')}
                true-label={true}
                false-label={false}
                v-model={form.updateSupport}
              >
                {this.$t('common.rule.updateSupport')}
              </el-checkbox>
            );
          },
        },
      ],
      importRules: {},
      loading: false,
      data: [],
      page: {
        current: 1,
        pageSize: 20,
        total: 0,
      },
      sort: {},
      search: {},
      query: {},
      selection: [],
    };
  },
  computed: {
    columns() {
      return [
        { type: "selection", fixed: "left" },
        // { type: "index", fixed: "left" },

        {
          label: this.$t("common.factory"),
          prop: "factoryCode",
          formatter: (row) => {
            return this.selectDictLabel(
              this.dict.type.biz_factory_name,
              row.factoryCode
            );
          },
        },
        {
          label: this.$t("ui.data.column.machine.machineCode"),
          prop: "machineCode",
          minWidth: 150,
          // width: 150,
          // sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.machine.machineName"),
          prop: "machineName",
          minWidth: 100,
          width: 180,
          // sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.machine.lhMachineType"),
          prop: "machineType",
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.LH_MACHINE_TYPE, value);
          },
        },
        // {
        //   label: this.$t("ui.data.column.machine.dimension"),
        //   prop: "dimension",
        //   minWidth: 100,
        //   // sortable: "custom",
        // },
        {
          label: this.$t("ui.data.column.machine.dimensionMinmum"),
          prop: "dimensionMinimum",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.machine.dimensionMaximum"),
          prop: "dimensionMaximum",
          minWidth: 100,
          // sortable: "custom",
        },
        // {
        //   label: this.$t("ui.data.column.machine.centripetalMechanism"),
        //   prop: "centripetalMechanism",
        //   minWidth: 100,
        //   // sortable: "custom",
        //   formatter: (row) => {
        //     return this.selectDictLabel(
        //       this.dict.type.CENTRIPETAL_MECHANISM,
        //       row.centripetalMechanism
        //     );
        //   },
        // },
        {
          label: this.$t("ui.data.column.machine.mouldNum"),
          prop: "maxMoldNum",
          minWidth: 100,
          // sortable: "custom",
          type: "number",
        },
        {
          label: this.$t("ui.data.column.machine.quata"),
          prop: "quota",
          minWidth: 100,
          // sortable: "custom",
          type: "number",
        },
        {
          label: this.$t("ui.data.column.machine.machineOrder"),
          prop: "machineOrder",
          minWidth: 100,
          // sortable: "custom",
          type: "number",
        },
        // {
        //   label: this.$t("ui.data.column.machine.classShift"),
        //   prop: "classShift",
        //   minWidth: 100,
        //   // sortable: "custom",
        //   formatter: (row) => {
        //     return this.selectDictLabel(
        //       this.dict.type.CLASS_SHIFT,
        //       row.classShift
        //     );
        //   },
        // },
        // {
        //   label: this.$t("ui.data.column.machine.openMachineClass"),
        //   prop: "openMachineClass",
        //   minWidth: 100,
        //   // sortable: "custom",
        //   render: ({ row }) => {
        //     let value = row.openMachineClass;
        //     if (this.isEmpty(value)) {
        //       return "";
        //     }
        //     if (row.classShift === "3") {
        //       return this.selectDictLabels(
        //         this.dict.type.CLASS_NUM_THREE,
        //         value
        //       );
        //     }
        //     return this.selectDictLabels(this.dict.type.CLASS_NUM, value);
        //   },
        // },

        // {
        //   label: this.$t("ui.data.column.machine.single"),
        //   prop: "single",
        //   type: "select",
        //   formatter: (row) => {
        //     return this.selectDictLabel(this.dict.type.biz_yes_no, row.single);
        //   },
        // },
        {
          label: this.$t("ui.data.column.machine.status"),
          prop: "status",
          minWidth: 100,
          // sortable: "custom",
          render: ({ row }) => {
            return (
              <el-switch
                active-value="1"
                inactive-value="0"
                disabled={this.loading}
                value={row.status}
                onChange={(val) => {
                  let text = val == "0" ? "禁用" : "启用";
                  this.$confirm(`确认${text}吗？`, { type: "warning" }).then(
                    async () => {
                      try {
                        this.loading = true;
                        const data = await editMachine({
                          ...row,
                          status: val,
                        });
                        this.$modal.msgSuccess(data.msg);
                        // this.$set(this.page, "current", 1);
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
          label: this.$t("ui.common.column.remark"),
          prop: "remark",
          minWidth: 100,
          // sortable: "custom",
          formatter: (row) => {
            return row.remark || "-";
          },
        },
        {
          label: this.$t("common.option"),
          prop: "option",
          width: "160px",
          fixed: "right",
          align: "center",
          render: ({ row }) => {
            return (
              <div>
                <el-button
                  class="minus"
                  type="success"
                  v-hasPermi={["lh:lhMachineInfo:edit"]}
                  onClick={() => {
                    this.handleEdit(row);
                  }}
                >
                  {this.$t("common.button.modify")}
                </el-button>
                <el-button
                  v-hasPermi={["maindata:rawMaterialRequirePlan:remove"]}
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
          label: this.$t("ui.data.column.machine.machineCode"),
          prop: "machineCode",
        },
        {
          label: this.$t("ui.data.column.machine.machineName"),
          prop: "machineName",
        },
        // {
        //   label: this.$t("ui.data.column.machine.lhMachineType"),
        //   prop: "machineType",
        //   type: "select",
        //   dictData: this.dict.type.LH_MACHINE_TYPE,
        // },
        {
          label: this.$t("ui.data.column.machine.status"),
          prop: "status",
          render: (form) => {
            return (
              <dict-select
                v-model={form.status}
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
      if (this.$refs.infoDialogRef) {
        this.$refs.infoDialogRef.show();
      }
    },
    handleEdit(row) {

      if (this.$refs.infoDialogRef) {
        this.$refs.infoDialogRef.show(row);
      }
    },

    handleDelete(row) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = row.id;
        removeMachine({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleDeleteMulti() {
      // if (this.selection.length == 0) {
      //   this.$modal.msgWarning(this.$t("请至少选择一条记录"));
      //   return;
      // }
      this.$confirm(this.$t("common.confirm.delete"), { type: "warning" })
        .then(async () => {
          //确认提交
          try {
            this.loading = true;
            let ids = [];
            this.selection.forEach((element) => {
              ids.push(element.id);
            });
            const params = {
              ids: ids.join(),
            };
            const data = await removeMachine(params);
            this.$modal.msgSuccess(data.msg);
            this.$set(this.page, "current", 1);
            this.getList();
          } catch (error) {
            console.log(error);
          } finally {
            this.loading = false;
          }
        })
        .catch(() => {});
    },
    handleExport() {
      this.$confirm(this.$t(`确定导出所有机台信息？`), {
        type: "warning",
      }).then(() => {
        try {
          this.loading = true;
          let params = this.formatParams();
          params = {
            ...params,
            pageSize: undefined,
            pageNum: undefined,
          };
          exportData(params);
        } catch (error) {
          console.error(error);
        } finally {
          this.loading = false;
        }
      });
    },
    handleQuery() {},
    handleHistoryQuery() {},

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
    handleSelectionChange(rows) {
      this.selection = rows;
    },

    //util
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

    formatParams() {
      const params = {
        pageSize: this.page.pageSize,
        pageNum: this.page.current,
        ...this.query,
        ...this.sort,
      };

      return params;
    },
    //
    async getList() {
      try {
        this.loading = true;
        const data = await listMachine(this.formatParams());

        this.data = data.rows.map((el) => {
          return {
            ...el,
            tempStatus: el.status,
          };
        });
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
  },
  mounted() {},
  activated() {
    this.getList();
  },
};
</script>
