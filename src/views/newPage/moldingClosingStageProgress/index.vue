
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
        <!-- <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:ProductMoldingLimit:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        > -->
        <!-- <el-button
          type="warning"
          v-hasPermi="['monthplan:ProductMoldingLimit:edit']"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        > -->
        <!-- <el-button
          v-hasPermi="['monthplan:ProductMoldingLimit:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        > -->
        <el-button
          @click="handleExport"
          v-hasPermi="['monthplan:ProductMoldingLimit:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/mdm/productMoldingLimit/importTemplate"
      uploadUrl="/mdm/productMoldingLimit/importData"
      @uploadSuccess="getList"
    />
    <infoDialog ref="infoRef" @success="getList" />
  </basic-container>
</template>
<script>
//lib
import { mapState } from "vuex";
//utils
import { downloadLink } from "@/utils/request";

// import {
//   listProductMoldingLimit,
//   removeProductMoldingLimit,
// } from "@/api/mdm/productMoldingLimit";

//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoDialog from "./components/infoDialog.vue";

export default {
  name: "MoldingClosingStageProgress",
  components: {
    tltUpload,
    infoDialog,
  },
  dicts: ["LINE_TYPE", "JOB_TYPE", "biz_factory_name"],
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
          prop: "序号",
          label: this.$t("序号"),
        },
        {
          prop: "年月",
          label: this.$t("年月"),
        },
        {
          prop: "机台",
          label: this.$t("机台"),
        },
        {
          prop: "规格",
          label: this.$t("规格"),
        },
        {
          prop: "余量",
          label: this.$t("余量"),
        },
        {
          prop: "收尾时间（4天以内）",
          label: this.$t("收尾时间（4天以内）"),
          children: [
            {
              prop: "预计",
              label: this.$t("预计"),
            },
            {
              prop: "月计划",
              label: this.$t("月计划"),
            },
          ],
        },
        {
          prop: "备注",
          label: this.$t("备注"),
        },
        {
          prop: "后规格",
          label: this.$t("后规格"),
        },
        {
          prop: "开产时间",
          label: this.$t("收尾时间（4天以内）"),
          children: [
            {
              prop: "预计",
              label: this.$t("预计"),
            },
            {
              prop: "月计划",
              label: this.$t("月计划"),
            },
          ],
        },
        {
          prop: "进度",
          label: this.$t("进度"),
        },
        {
          align: "center",
          label: this.$t("ui.data.btn.option"),
          fixed: "right",
          render: ({ row }) => {
            return (
              <div>
                <el-button
                  v-hasPermi={["monthplan:ProductMoldingLimit:edit"]}
                  class="minus"
                  type="success"
                  onClick={() => this.handleEdit(row)}
                >
                  {this.$t("ui.frame.btn.update")}
                </el-button>
                <el-button
                  v-hasPermi={["monthplan:ProductMoldingLimit:remove"]}
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
          prop: "年月",
          label: this.$t("年月"),
          type:'date',
          format:'YYYY-MM'
        },
        {
          prop: "机台",
          label: this.$t("机台"),
        },
        {
          prop: "规格",
          label: this.$t("规格"),
        },
        {
          prop: "后规格",
          label: this.$t("后规格"),
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
        // removeProductMoldingLimit({ ids }).then((data) => {
        //   this.$modal.msgSuccess(data.msg);
        //   this.$set(this.page, "current", 1);
        //   this.getList();
        // });
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
      downloadLink("/mdm/productMoldingLimit/export", this.formatParams(false));
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
        let list=[{
          序号:'1',
          年月:'2025-11',
          机台:'H1502',
          规格:'12R22.5-JY601标载零度',
          余量:'790',
          预计:'按胎体大卷收尾',
          月计划:'08.31日',
          备注:'',
          后规格:'425/65R22.5',
          进度:'SW/TQ中班备料,其他有料',


        },
        {
          序号:'2',
          年月:'2025-11',
          机台:'H1502',
          规格:'12R22.5-JY601标载零度',
          余量:'790',
          预计:'按胎体大卷收尾',
          月计划:'08.31日',
          备注:'',
          后规格:'425/65R22.5',
          进度:'SW/TQ中班备料,其他有料',


        },
        {
          序号:'3',
          年月:'2025-11',
          机台:'H1502',
          规格:'12R22.5-JY601标载零度',
          余量:'790',
          预计:'按胎体大卷收尾',
          月计划:'08.31日',
          备注:'',
          后规格:'425/65R22.5',
          进度:'SW/TQ中班备料,其他有料',


        },
        {
          序号:'4',
          年月:'2025-11',
          机台:'H1502',
          规格:'12R22.5-JY601标载零度',
          余量:'790',
          预计:'按胎体大卷收尾',
          月计划:'08.31日',
          备注:'',
          后规格:'425/65R22.5',
          进度:'SW/TQ中班备料,其他有料',


        },]
          this.data = list
        this.page.total = 4
        // const data = await listProductMoldingLimit(this.formatParams());
        // this.data = data.rows;
        // this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
  },
  created() {
    if (this.moldingMachines.length === 0) {
      this.$store.dispatch("molding/getMachineList");
    }
    this.getList();
  },
  activated() {
    this.getList();
  },
};
</script>
<style lang="scss" scoped>
.more-btn {
  margin: 2px 0;
  width: 100%;
}
</style>
