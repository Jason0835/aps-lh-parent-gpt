
<template>
  <basic-container>
    <page-table
      tableRef="insideLinerLhflMachineMainTable"
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
          v-hasPermi="['setting:lhflMachine:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <el-button
          v-hasPermi="['setting:lhflMachine:edit']"
          @click="handleEdit(selection[0])"
          :disabled="selection.length != 1"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        >
        <!-- <el-button
          type="danger"
          @click="handleDeleteMulti"
          :disabled="selection.length == 0"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        > -->
        <el-button
          v-hasPermi="['setting:lhflMachine:import']"
          @click="() => $refs.tltUploadForm.handleImport(importDefaultValue)"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          v-hasPermi="['setting:lhflMachine:export']"
           @click="handleExport"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <tlt-upload-form
      ref="tltUploadForm"
      title="导入信息数据"
      downloadUrl="/setting/lhflMachine/importTemplate"
      uploadUrl="/setting/lhflMachine/importData"
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
  listLhflMachine,
  removeLhflMachine,
  exportData,
} from "@/api/setting/lhflMachine";
import InfoDialog from "./components/infoDialog.vue";
import TltUploadForm from "@/views/components/tltUploadForm.vue";
export default {
  name: "rubberLhflMachine",
  components: { InfoDialog, TltUploadForm },
  dicts: [
    // "sys_yes_no",
    // "MOLD_CHANGE_TYPE",
    // "IS_RELEASE"
    "STATUS",
  ],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    let tomorrow = moment().add(1, "days").format("YYYY-MM-DD");
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
                label="是否更新已经存在的用户数据"
                true-label={true}
                false-label={false}
                v-model={form.updateSupport}
              >
                是否更新已经存在的用户数据
              </el-checkbox>
            );
          },
        },
      ],
      importRules: {
        planDate: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
      },
      searchColumns: [
        {
          label: this.$t("setting.lhflMachine.machineName"),
          prop: "machineName",
        },
        {
          label: this.$t("setting.lhflMachine.mixArea"),
          prop: "mixArea",
        },
        {
          label: this.$t("setting.machine.status"),
          prop: "status",
          render: (form) => {
            return (
              <dict-select
                v-model={form.status}
                options={this.dict.type.STATUS}
              />
            );
          },
        },
      ],
      columns: [
        {
          prop: "mixArea",
          // sortable: "custom",
          halign: "center",
          align: "center",
          valign: "middle",
          label: this.$t("setting.lhflMachine.mixArea"),
          // formatter: (row, column, value, index) => {
          //   return this.selectDictLabel(mixAreaDatas, value);
          // },
        },
        {
          prop: "machineCode",
          // sortable: "custom",
          halign: "center",
          align: "center",
          valign: "middle",
          label: this.$t("setting.lhflMachine.machineCode"),
        },
        {
          prop: "machineName",
          // sortable: "custom",
          halign: "center",
          align: "center",
          valign: "middle",
          label: this.$t("setting.lhflMachine.machineName"),
        },
        {
          prop: "classShift",
          // sortable: "custom",
          halign: "center",
          align: "center",
          valign: "middle",
          label: this.$t("setting.lhflMachine.classShift"),
          // formatter: (row, column, value, index) => {
          //   return haveSulfurSteelyardTools(row);
          // },
        },
        {
          prop: "capacity",
          // sortable: "custom",
          halign: "center",
          align: "center",
          valign: "middle",
          label: this.$t("setting.lhflMachine.capacity"),
          // formatter: (row, column, value, index) => {
          //   return haveMaterialSteelyardTools(row);
          // },
        },
        {
          prop: "status",
          // sortable: "custom",
          halign: "center",
          align: "center",
          valign: "middle",
          label: this.$t("setting.machine.status"),
          render: ({ row }) => {
            return (
              <el-switch
                active-value="1"
                inactive-value="0"
                v-model={row.status}
              />
            );
          },
          // formatter: (row, column, value, index) => {
          //   return statusTools(row);
          // },
        },
        {
          label: this.$t("setting.machine.classShift.status.title"),
          align: "center",
          children: [
            {
              prop: "midStatus",
              halign: "center",
              align: "center",
              valign: "middle",
              width: 5,
              widthUnit: "%",
              label: this.$t("setting.machine.mid"),
              render: ({ row }) => {
                return (
                  <el-switch
                    active-value="1"
                    inactive-value="0"
                    v-model={row.midStatus}
                  />
                );
              },
            },
            {
              prop: "nightStatus",
              halign: "center",
              align: "center",
              valign: "middle",
              width: 5,
              widthUnit: "%",
              label: this.$t("setting.machine.night"),
              render: ({ row }) => {
                return (
                  <el-switch
                    active-value="1"
                    inactive-value="0"
                    v-model={row.nightStatus}
                  />
                );
              },
            },
            {
              prop: "dayStatus",
              halign: "center",
              align: "center",
              valign: "middle",
              width: 5,
              widthUnit: "%",
              label: this.$t("setting.machine.day"),
              render: ({ row }) => {
                return (
                  <el-switch
                    active-value="1"
                    inactive-value="0"
                    v-model={row.dayStatus}
                  />
                );
              },
            },
          ],
        },
        {
          prop: "remark",
          // sortable: "custom",
          halign: "center",
          align: "left",
          valign: "middle",
          label: this.$t("setting.lhflMachine.remark"),
        },
        {
          align: "center",
          halign: "center",
          label: this.$t("ui.data.btn.option"),
          minWidth: 180,
          width: 180,
          fixed: "right",
          render: ({ row }) => {
            return (
              <div>
                <el-button
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.update")}
                </el-button>
                <el-button
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
      ],
      loading: false,
      data: [],
      page: {
        current: 1,
        pageSize: 20,
        total: 0,
      },
      sort: {},
      search: {
        planDate: tomorrow,
      },
      query: {
        planDate: tomorrow,
      },
      selection: [],
    };
  },
  computed: {},
  methods: {
    handleChangeStatus(status, row) {
      console.log(status);
      let title =
        status === "1"
          ? this.$t("ui.biz.alter.isOpen")
          : this.$t("ui.biz.alter.isStop");

      this.$confirm(title, {
        type: "warning",
      }).then(async () => {
        try {
          this.loading = true;
          const res = await editLhflMachine({
            ...row,
            status,
          });
          this.$modal.msgSuccess(res.msg);
          this.getList();
        } catch (error) {
          this.loading = false;
        }
      });
    },
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
        removeLhflMachine({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleExport() {
      this.$confirm(this.$t(`确定导出所有机台信息？`), {
        type: "warning",
      }).then(() => {
        try {
          this.loading = true;
          let params = this.formatParams(false);
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
    /**
     * 获得列表参数
     * @param {boolean} hasPage
     * @returns {object}
     */
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
    // a

    //
    async getList() {
      try {
        this.loading = true;
        const data = await listLhflMachine(this.formatParams());

        this.data = data.rows.map((el) => {
          return {
            ...el,
            // tempStatus: el.status,
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
