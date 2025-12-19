
<template>
  <basic-container>
    <page-table
      tableRef="insideLinerGlueFinishMainTable"
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
        <!-- <el-button v-hasPermi="['setting:glueFinish:add']" @click="handleAdd">{{
          $t("ui.frame.btn.add")
        }}</el-button> -->
        <!-- <el-button
          v-hasPermi="['setting:glueFinish:edit']"
          @click="handleEdit(selection[0])"
          :disabled="selection.length != 1"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        > -->
        <!-- <el-button
          type="danger"
          @click="handleDeleteMulti"
          :disabled="selection.length == 0"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        > -->
        <el-button
          v-hasPermi="['setting:glueFinish:import']"
          @click="() => $refs.tltUploadForm.handleImport(importDefaultValue)"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          v-hasPermi="['setting:glueFinish:export']"
           @click="handleExport"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <tlt-upload-form
      ref="tltUploadForm"
      title="导入信息数据"
      downloadUrl="/setting/glueFinish/importTemplate"
      uploadUrl="/setting/glueFinish/importData"
      @uploadSuccess="getList"
      labelWidth="0"
      :columns="importColumns"
      :rules="importRules"
    />
    <InfoDialog ref="infoDialogRef" @success="handelSuccess" />
  </basic-container>
</template>
<script>
// import moment from "moment";


import { listGlueFinish, removeGlueFinish,exportData } from "@/api/setting/glueFinish";
import InfoDialog from "./components/infoDialog.vue";
import TltUploadForm from "@/views/components/tltUploadForm.vue";
export default {
  name: "RubberCompleted",
  components: { InfoDialog, TltUploadForm },
  dicts: ["MIX_AREA"],
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
    searchColumns() {
      return [
        {
          label: this.$t("setting.glueFinish.orderNo"),
          prop: "orderNo",
        },
        {
          label: this.$t("setting.glueFinish.scheduleDate"),
          prop: "scheduleDate",
          type: 'date',
          dateType: "date",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("setting.glueFinish.mixArea"),
          prop: "mixArea",
          type: "select",
          dictData: this.dict.type.MIX_AREA,
        },
        {
          label: this.$t("setting.glueFinish.glue"),
          prop: "glue",
        },
      ];
    },
    columns() {
      return [
        { type: "selection", align: "right" },
        {
          prop: "orderNo",
          // sortable: "custom",
          halign: "center",
          align: "center",
          valign: "middle",
          width: 200,
          label: this.$t("setting.glueFinish.orderNo"),
        },
        {
          prop: "scheduleDate",
          // sortable: "custom",
          halign: "center",
          align: "center",
          valign: "middle",
          minWidth: 100,
          label: this.$t("setting.glueFinish.scheduleDate"),
        },
        {
          prop: "mixArea",
          // sortable: "custom",
          halign: "center",
          align: "center",
          valign: "middle",
          label: this.$t("setting.glueFinish.mixArea"),
        },
        {
          prop: "glue",
          // sortable: "custom",
          halign: "center",
          align: "center",
          valign: "middle",
          label: this.$t("setting.glueFinish.glue"),
        },
        {
          prop: "machineName",
          // sortable: "custom",
          halign: "center",
          align: "center",
          valign: "middle",
          label: this.$t("setting.glueFinish.machineCode"),
        },
        {
          prop: "totalFinishQty",
          // sortable: "custom",
          halign: "center",
          align: "center",
          valign: "middle",
          label: this.$t("setting.glueFinish.totalFinishQty"),
        },
        {
          prop: "midFinishQty",
          // sortable: "custom",
          halign: "center",
          align: "center",
          valign: "middle",
          label: this.$t("setting.glueFinish.midFinishQty"),
        },
        {
          prop: "nightFinishQty",
          // sortable: "custom",
          halign: "center",
          align: "center",
          valign: "middle",
          label: this.$t("setting.glueFinish.nightFinishQty"),
        },
        // {
        //   prop: "dayFinishQty",
        //   sortable: "custom",
        //   halign: "center",
        //   align: "center",
        //   valign: "middle",
        //   label: this.$t("setting.glueFinish.dayFinishQty"),
        // },
        // {
        //   prop: "remark",
        //   sortable: "custom",
        //   halign: "center",
        //   align: "left",
        //   valign: "middle",
        //   label: this.$t("ui.remark"),
        // },
        // {
        //   align: "center",
        //   halign: "center",
        //   label: this.$t("ui.data.btn.option"),
        //   minWidth: 180,
        //   width: 180,
        //   fixed: "right",
        //   render: ({ row }) => {
        //     return (
        //       <div>
        //         <el-button
        //           class="minus"
        //           type="success"
        //           onClick={() => this.handleEdit(row)}
        //         >
        //           {this.$t("ui.frame.btn.update")}
        //         </el-button>
        //         <el-button
        //           class="minus"
        //           type="danger"
        //           onClick={() => this.handleDelete(row)}
        //         >
        //           {this.$t("ui.frame.btn.delete")}
        //         </el-button>
        //       </div>
        //     );
        //   },
        // },
      ];
    },
  },
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
          const res = await editGlueFinish({
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
        removeGlueFinish({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleExport() {
      this.$confirm(this.$t(`确定导出所有信息？`), {
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
        const data = await listGlueFinish(this.formatParams());

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
