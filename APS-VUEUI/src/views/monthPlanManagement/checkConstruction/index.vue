
<template>
  <basic-container>
    <page-table
      tableRef="CheckConstructionMainTable"
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
      :showSummary="false"
      :selectArea="false"
    >
      <template slot="header">
        <el-button type="primary" @click="handleCheckConstruction">检测施工</el-button>
      </template>
    </page-table>
    <el-button style="display: none" ref="hidePopoverBtnRef"></el-button>
  </basic-container>
</template>
<script>
import moment from "moment";
import { downloadLink } from "@/utils/request";
import { listCheckConstruction,checkConstruction } from "@/api/cx/checkConstruction";
export default {
 name: "CheckConstruction",
  components: {},
  dicts: [
    // "sys_yes_no"
  ],
  data() {
    return {
      columns: [
        { type: "selection", fixed: "left" },
        // { type: "index", fixed: "left" },
        // {
        //   label: this.$t("common.option"),
        //   prop: "option",
        //   width: "100px",
        //   fixed: "left",
        //   render: ({ row }) => {
        //     return (
        //       <div>
        //         <text-button
        //           onClick={() => {
        //             this.handleEdit(row);
        //           }}
        //         >
        //           {this.$t("common.button.modify")}
        //         </text-button>
        //       </div>
        //     );
        //   },
        // },
        {
          label: this.$t("主键ID"),
          prop: "id",
          minWidth: 100,
          // sortable: "custom",
          visible: false,
        },
        {
          label: this.$t("ui.data.column.construction.check.month2"),
          prop: "planMonth",
          minWidth: 100,
          // sortable: "custom",
        },
        {
          label: this.$t("ui.data.column.construction.check.isComplete"),
          prop: "isComplete",
          minWidth: 100,
          // sortable: "custom",
          render: ({ row }) => {
            return (
              <dict-tag
                value={row.isComplete}
                options={[
                  { label: "是", value: "0",raw:{} },
                  { label: "否", value: "1",raw:{} },
                ]}
              />
            );
          },
        },
        {
          label: this.$t("ui.data.column.export.exportParams"),
          prop: "exportParams",
          minWidth: 100,
          // sortable: "custom",
          visible: false,
        },
        {
          label: this.$t("ui.data.column.construction.check.file"),
          prop: "fileName",
          minWidth: 100,
          // sortable: "custom",
          render: ({ row }) => {
            if (row.fileName) {
              return (
                <el-link
                  type="primary"
                  onClick={() => {
                    downloadLink("/cx/checkConstruction/download", {
                      name: row.fileName,
                      url: row.filePath,
                    });
                    // window.location.href = prefix + "/download?name="+row.fileName+"&&url="+row.filePath;
                  }}
                >
                  {row.fileName}
                </el-link>
              );
            } else {
              return "-";
            }
          },
        },
        {
          label: this.$t("ui.data.column.export.fileUrl"),
          prop: "filePath",
          minWidth: 100,
          // sortable: "custom",
          visible: false,
          formatter:(row)=>{
            return row.filePath || "-"
          }
        },
        {
          label: this.$t("ui.data.column.construction.check.time"),
          prop: "createTime",
          minWidth: 100,
          // sortable: "custom",
        },
      ],
      searchColumns: [
        {
          label: this.$t("ui.data.column.construction.check.month1"),
          prop: "planMonth",
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
          clearable: false,
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
        planMonth: "",
      },
      query: {
        planMonth: "",
      },
    };
  },
  computed: {},
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
    handleCheckConstruction() {
      this.$confirm(this.$t(`确认要检测 ${this.query.planMonth} 的施工？`), {
        type: "warning",
      }).then(async () => {
        try {
          this.loading = true;
          const data = await checkConstruction({
            planMonth:this.query.planMonth
          });
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        } catch (error) {
          console.error(error)
        }finally {
          this.loading = false;
        }
      });
    },
    handleDelete(row) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = row.id;
        removeArea({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleWorkingProcedure() {
      if (this.$refs.workingProcedureRef) {
        this.$refs.workingProcedureRef.show();
      }
    },
    handleWorkingProcedureCalendar() {
      if (this.$refs.workingProcedureCalendarRef) {
        this.$refs.workingProcedureCalendarRef.show();
      }
    },
    handleShiftRules() {
      if (this.$refs.shiftRulesRef) {
        this.$refs.shiftRulesRef.show();
      }
    },
    handleShiftSetting() {
      if (this.$refs.shiftSettingRef) {
        this.$refs.shiftSettingRef.show();
      }
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

      if (params.createTime && params.createTime[0]) {
        params.createTimeStart = params.createTime[0];
        params.createTimeEnd = params.createTime[1];
        params.createTime = undefined;
      }

      return params;
    },
    //
    async getList() {
      try {
        this.loading = true;
        const data = await listCheckConstruction(this.formatParams());
        // const data = await this.$axios.get("monthPlan/monthProductionPlan/list");

        this.data = data.rows;
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
  },
  mounted() {
    //设置默认月度
    this.query.planMonth = moment().format("YYYY-MM");
    this.search.planMonth = moment().format("YYYY-MM");
  },
  activated() {
    this.getList();
  },
};
</script>
