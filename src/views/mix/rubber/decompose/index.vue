
<template>
  <basic-container>
    <page-table
      tableRef="insideLinerMachineMainTable"
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
        <el-button v-hasPermi="['setting:decompose:add']" @click="handleAdd">{{
          $t("ui.frame.btn.add")
        }}</el-button>
        <el-button
          v-hasPermi="['setting:decompose:edit']"
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
          v-hasPermi="['setting:decompose:import']"
          @click="() => $refs.tltUploadForm.handleImport(importDefaultValue)"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          v-hasPermi="['setting:decompose:export']"
           @click="handleExport"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <tlt-upload-form
      ref="tltUploadForm"
      title="导入机台信息数据"
      downloadUrl="/setting/decompose/importTemplate"
      uploadUrl="/setting/decompose/importData"
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

import { listDecompose,removeDecompose, exportData } from "@/api/setting/decompose.js";
import InfoDialog from "./components/infoDialog.vue";
import TltUploadForm from "@/views/components/tltUploadForm.vue";

export default {
  name: "RubberDecompose",
  components: { InfoDialog, TltUploadForm },
  dicts: [
    // "sys_yes_no",
    // "MOLD_CHANGE_TYPE",
    // "IS_RELEASE"
    "STATUS",
    "SEGMENT",
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
          label: this.$t("setting.decompose.glue"),
          prop: "glue",
        },

        {
          label: this.$t("setting.decompose.segment"),
          prop: "segment",
          render: (form) => {
            return (
              <dict-select
                v-model={form.segment}
                options={this.dict.type.SEGMENT}
              />
            );
          },
        },
      ],
      columns: [
        { type: "selection", fixed: "left" },
        {
          prop: "glue",
          // sortable: "custom",
          halign: "center",
          align: "center",
          label: this.$t("setting.decompose.glue"),
        },
        {
          prop: "segment",
          // sortable: "custom",
          halign: "center",
          align: "center",
          label: this.$t("setting.decompose.segment"),
          // formatter: (row, column, value, index) => {
          //   return this.selectDictLabel(segmentDatas, value);
          // },
        },
        {
          prop: "motherGlue1",
          // sortable: "custom",
          halign: "center",
          align: "center",
          label: this.$t("setting.decompose.motherGlue1"),
        },
        {
          prop: "motherGlue2",
          // sortable: "custom",
          halign: "center",
          align: "center",
          label: this.$t("setting.decompose.motherGlue2"),
        },
        {
          prop: "motherGlue3",
          // sortable: "custom",
          halign: "center",
          align: "center",
          label: this.$t("setting.decompose.motherGlue3"),
        },
        {
          prop: "motherGlue4",
          // sortable: "custom",
          halign: "center",
          align: "center",
          label: this.$t("setting.decompose.motherGlue4"),
        },
        {
          prop: "motherGlue5",
          // sortable: "custom",
          halign: "center",
          align: "center",
          label: this.$t("setting.decompose.motherGlue5"),
        },
        {
          prop: "motherGlue6",
          // sortable: "custom",
          halign: "center",
          align: "center",
          label: this.$t("setting.decompose.motherGlue6"),
        },
        {
          prop: "motherGlue7",
          // sortable: "custom",
          halign: "center",
          align: "center",
          label: this.$t("setting.decompose.motherGlue7"),
        },
        {
          prop: "motherGlue8",
          // sortable: "custom",
          halign: "center",
          align: "center",
          label: this.$t("setting.decompose.motherGlue8"),
        },
        {
          prop: "motherGlue9",
          // sortable: "custom",
          halign: "center",
          align: "center",
          label: this.$t("setting.decompose.motherGlue9"),
        },
        {
          prop: "remark",
          // sortable: "custom",
          label: this.$t("ui.common.column.remark"),
          halign: "center",
          align: "left",
          minWidth: 100,
          // formatter: (row, column, value, index) => {
          //   return $.table.tooltip(value, 20);
          // },
          label: this.$t("setting.decompose.remark"),
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
                  v-hasPermi={['setting:decompose:edit']}
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.update")}
                </el-button>
                <el-button
                  v-hasPermi={['setting:decompose:remove']}
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
          const res = await editMachine({
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
        removeDecompose({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleExport() {
      this.$confirm(this.$t(`确定导出？`), {
        type: "warning",
      }).then(() => {
        try {
          this.loading = true;
          let params = this.formatParams(false);

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
        const data = await listDecompose(this.formatParams());

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
