
<template>
  <basic-container>
    <page-table
      tableRef="productStatusMainTable"
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
        <el-button type="warning" @click="handleModifyQty">{{
          $t("ui.data.column.productStatus.modifyQty")
        }}</el-button>
        <el-button type="warning" @click="handleExport">{{
          $t("ui.frame.btn.export")
        }}</el-button>
        <el-button
          type="warning"
          :disabled="selection.length != 1"
          @click="handleProduction"
          >{{ $t("ui.data.column.scheduleResult.production") }}</el-button
        >
        <el-button type="primary" :disabled="selection.length == 0">{{
          $t("ui.data.column.productStatus.markUnProduct")
        }}</el-button>
        <el-button type="primary" @click="handleClose">{{
          $t("ui.frame.btn.close")
        }}</el-button>
      </template>
    </page-table>
    <editDialog ref="editRef" @success="handelSuccess" />
    <finishedProductionDialog ref="proRef" @success="handelSuccess" />
  </basic-container>
</template>
<script>
//lib
import moment from "moment";

import { downloadLink } from "@/utils/request";

//interface
import { listProductStatus, markUnProduct } from "@/api/cx/productStatus.js";
//components
import editDialog from "./components/editDialog.vue";
import finishedProductionDialog from "./components/finishedProductionDialog.vue";

export default {
  name: "productStatus",
  components: {
    editDialog,
    finishedProductionDialog,
  },
  dicts: [],
  data() {
    return {
      searchColumns: [
        {
          label: this.$t("ui.data.column.scheduleResult.scheduleDate"),
          prop: "scheduleDate",
          type: "date",
          dateType: "daterange",
          valueFormat: "yyyy-MM-dd",
        },
        {
          label: this.$t("ui.data.column.productStatus.monthPlanApsVersion"),
          prop: "monthPlanApsVersion",
        },
        {
          label: this.$t("ui.data.column.productStatus.sapCode"),
          prop: "sapCode",
        },
        {
          label: this.$t("ui.data.column.productStatus.embryoCode"),
          prop: "embryoCode",
        },
        {
          label: this.$t("ui.data.column.productStatus.bomDataVersion"),
          prop: "bomDataVersion",
        },
        {
          label: this.$t("ui.data.column.productStatus.productStatus"),
          prop: "productStatus",
          type: "select",
          dictData: [], // "PRODUCT_STATUS",
        },
        {
          label: this.$t("ui.data.column.productStatus.markUnProduct"),
          prop: "markUnProduct",
          type: "select",
          dictData: [], // "MARK_UN_PRODUCT",
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
      search: {
        scheduleDate: "",
      },
      query: {
        scheduleDate: "",
      },
    };
  },
  computed: {
    columns() {
      let columns = [
        { type: "selection", fixed: "left" },
        {
          prop: "monthPlanApsVersion",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.productStatus.monthPlanApsVersion"),
          sortable: "custom",
        },
        {
          prop: "bomDataVersion",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.productStatus.bomDataVersion"),
          sortable: "custom",
        },
        {
          prop: "monthPlanIds",
          sortable: "custom",
          visible: false,
        },
        {
          prop: "sapCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.productStatus.sapCode"),
          sortable: "custom",
        },
        {
          prop: "embryoCode",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.productStatus.embryoCode"),
          sortable: "custom",
        },
        {
          prop: "monthPlanTotalQty",
          align: "right",
          halign: "center",
          label: this.$t("ui.data.column.productStatus.monthPlanTotalQty"),
          sortable: "custom",
        },
        {
          prop: "beginDate",
          sortable: "custom",
          visible: false,
        },
        {
          prop: "endDate",
          sortable: "custom",
          visible: false,
        },
        {
          prop: "productDetail",
          sortable: "custom",
          visible: false,
        },
        {
          prop: "specDimension",
          align: "right",
          halign: "center",
          label: this.$t("ui.data.column.productStatus.specDimension"),
          sortable: "custom",
        },
        {
          prop: "productStatus",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.productStatus.productStatus"),
          sortable: "custom",
          // formatter: function (value, row, index) {
          //     return $.table.selectDictLabel(productStatusDatas, value);
          // }
        },
        {
          prop: "markUnProduct",
          align: "center",
          halign: "center",
          label: this.$t("ui.data.column.productStatus.markUnProduct"),
          sortable: "custom",
          // formatter: function (value, row, index) {
          //     return $.table.selectDictLabel(markUnProductDatas, value);
          // }
        },
        {
          prop: "remark",
          halign: "center",
          label: this.$t("ui.data.column.remark"),
          sortable: "custom",
          // formatter: function (value, row, index) {
          //     debugger;
          //     var _length = 20;
          //     var _text = "";
          //     var _value = $.common.nullToStr(value);
          //     if (_value.length > _length) {
          //         _text = _value.substr(0, _length) + "...";
          //         _value = _value.replace(/\'/g,"&apos;");
          //         _value = _value.replace(/\"/g,"&quot;");
          //         var actions = [];
          //         var content = ($.common.sprintf('<a href="javascript:void(0)" onclick="editRemark(\'' +row.id+'\')"  data-placement="top" data-toggle="popover" data-trigger="hover" data-content="%s"><span>%s</span></a>', _value, _text))
          //         actions.push(content)
          //         return actions.join('');
          //     } else {
          //         var actions = [];
          //         actions.push('<a href="javascript:void(0)" onclick="editRemark(\'' +row.id+'\')">' + $.table.tooltip(value) + '</a> ');
          //         return actions.join('');
          //     }
          // }
        },
      ];

      return columns;
    },
  },
  methods: {
    handleAdd() {
      if (this.$refs.addRef) {
        this.$refs.addRef.show();
      }
    },
    handleEdit(row) {
      if (this.$refs.editRef) {
        this.$refs.editRef.show(row);
      }
    },
    handleDelete(row) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = row.id;
        console.log(ids);
        // removeArea({ ids }).then((data) => {
        //   this.$modal.msgSuccess(data.msg);
        //   this.$set(this.page, "current", 1);
        //   this.getList();
        // });
      });
    },
    handleModifyQty() {
      if (this.$refs.editRef) {
        const row = this.selection[0];
        this.$refs.editRef.show(row, "2");
      }
    },
    handleExport() {
      downloadLink("/cx/productStatus/export", {});
    },
    handleProduction() {
      if (this.$refs.proRef) {
        let row = this.selection[0];
        this.$refs.proRef.show(row);
      }
    },
    handleMarkUnProduct() {
      this.$confirm(
        this.$t("ui.data.column.productStatus.markUnProduct") + "?"
      ).then(async () => {
        const ids = this.selection.map((row) => row.id).join(",");
        const data = await markUnProduct({ ids });
        this.$modal.msgSuccess(data.msg);
        this.handelSuccess();
      });
    },
    handleClose() {
      this.$tab.closePage().then(() => {
        this.$router.push({
          path: "/moldingPlanManagement/moldingSchedule",
        });
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
      // this.$set(this.page, "current", current);
      // this.$set(this.page, "pageSize", pageSize);
      this.getList();
    },
    handelSuccess() {
      this.getList();
    },
    handleSelectionChange(rows) {
      this.selection = rows;
    },
    handleSortChange({ column, prop, order }) {
      if (order) {
        this.sort = {
          orderBy: prop,
          isAsc: order == "ascending",
        };
      } else {
        //默认排序
        this.sort = {};
      }
      this.getList();
    },

    // utils
    updateTableHeaderTitle() {
      //  TODO 更新表头标题
    },

    formatParams() {
      const params = {
        pageSize: this.page.pageSize,
        pageNum: this.page.current,
        ...this.query,
        params: {
          ...this.sort,
        },
      };
      if (params.scheduleDate && params.scheduleDate[0]) {
        params.beginDate = params.scheduleDate[0];
        params.endDate = params.scheduleDate[1];
        params.scheduleDate = undefined;
      }

      return params;
    },
    // api
    async getList() {
      try {
        this.loading = true;
        const data = await listProductStatus(this.formatParams());
        console.log(data);
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
    //设置默认排程时间
    let date = moment().add(1, "days").format("YYYY-MM-DD");
    // date = "2023-06-01"; //test

    date = ["2023-07-01", "2024-07-31"];
    this.query.scheduleDate = date;
    this.search.scheduleDate = date;
  },
  mounted() {
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
