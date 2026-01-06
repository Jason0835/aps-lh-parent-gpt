
<template>
  <basic-container>
    <page-table
      tableRef="ProductConstructionRelationMainTable"
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
          @click="mesCaptureing"
          plain
          >{{ $t("ui.data.column.moldLedger.mes") }}</el-button
        >
        <!-- <el-button
          type="primary"
          plain
          v-hasPermi="['maindata:mdmProductConstruction:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}</el-button
        >
        <el-button
          type="primary"
          plain
          :disabled="selection.length !== 1"
          v-hasPermi="['maindata:mdmProductConstruction:edit']"
          @click="handleEdit(selection[0])"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        >
        <el-button
          type="danger"
          plain
          :disabled="selection.length === 0"
          v-hasPermi="['maindata:mdmProductConstruction:remove']"
          @click="handleDelete(selection)"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        > -->
        <el-button
          v-hasPermi="['monthplan:mdmSkuConstructionRef:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}</el-button
        >
        <el-button
          @click="handleExport"
          v-hasPermi="['monthplan:mdmSkuConstructionRef:export']"
          >{{ $t("ui.frame.btn.export") }}</el-button
        >
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <!-- <tlt-upload
      ref="tltUpload"
      downloadUrl="/monthplan/mdmSkuConstructionRef/importTemplate"
      uploadUrl="/monthplan/mdmSkuConstructionRef/importData"
      @uploadSuccess="getList"
    /> -->
    <tlt-upload-form
      ref="tltUpload"
      :updateSupport="true"
      downloadUrl="/monthplan/mdmSkuConstructionRef/importTemplate"
      uploadUrl="/monthplan/mdmSkuConstructionRef/importData"
      @uploadSuccess="getList"
      labelWidth="0"
      :columns="importColumns"
    ></tlt-upload-form>
    <!-- <infoDialog ref="infoRef" @success="getList" /> -->
  </basic-container>
</template>
<script>
//lib
import moment from "moment";
//utils
import { downloadLink } from "@/utils/request";
//interface
import {
  listMdmProductConstruction,mesCapture
} from "@/api/maindata/mdmProductConstruction";
//components
import tltUpload from "@/components/tltUpload/tltUpload.vue";
import TltUploadForm from "@/views/components/tltUploadForm.vue";
// import infoDialog from "./components/infoDialog.vue";

export default {
  name: "SkuConstruction",
  components: {
    tltUpload,
    TltUploadForm
    // infoDialog,
  },
  dicts: ["molding_method",'biz_factory_name','biz_yes_no'],
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
      search: {
        mainPlanMonth: "",
      },
      query: {
        mainPlanMonth: "",
      },
      importDefaultValue: {},
      importRules: {},
    };
  },
  computed: {
    columns() {
      let columns = [

         {
          prop: "factoryCode",
          label: this.$t("common.factory"),
          width: 120,

          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
        },
         {
          prop: "bomVersion",
          label: this.$t("ui.data.column.boom.boomVersion"),
          width: 120,
        },
        {
          prop: "materialCode",
          label: this.$t("ui.data.column.monthplan.oriMaterialCode"),
          width: 180,
        },
        {
          prop: "mouldCavity",
          label: this.$t("ui.data.column.skuConstruction.mouldCavity"),
          width: 180,
        },
        // {
        //   prop: "mouldClampingPressure",
        //   label: this.$t("ui.data.column.lhTireConstructionInfo.clampingPressure"),
        //   width: 180,
        // },
        // {
        //   prop: "mouldMethod",
        //   label: this.$t("ui.data.column.sizeCapacity.mouldMethod"),
        //   formatter: (row, column, value) => {
        //     return this.selectDictLabel(this.dict.type.molding_method, value);
        //   },
        //   width: 180,
        // },
        {
          prop: "isZeroRack",
          label: this.$t("ui.data.column.mpMonthlySaleQty.isZeroRack"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },
          width: 120,
        },
        {
          prop: "constructionCode",
          label: this.$t("ui.data.column.skuConstruction.constructionCode"),
          width: 180,
        },
        {
          prop: "embryoCode",
          label: this.$t("ui.data.column.skuConstruction.embryoCode"),
          width: 180,
        },
        {
          prop: "curingTime",
          label: this.$t("ui.data.column.skuConstruction.curingTime"),
          align:'left',
          width: 160,
        },
        {
          prop: "curingTime2",
          label: this.$t("ui.data.column.skuConstruction.curingTime2"),
          align:'left',
          width: 160,
        },

        // {
        //   prop: "hydraulicPressureCuringTime",
        //   label: this.$t("ui.data.column.skuConstruction.hydraulicPressureCuringTime"),
        //   align:'left',
        //   width: 160,
        // },
        // {
        //   prop: "hydraulicPressureCuringTime2",
        //   label: this.$t("ui.data.column.skuConstruction.hydraulicPressureCuringTime2"),
        //   width: 160,
        // },
        {
          prop: "embryoNo",
          label: this.$t("ui.data.column.trialPlan.embryoNo"),
          width: 180,
        },
        {
          prop: "embryoType",
          label: this.$t("ui.data.column.trialPlan.embryoType"),
          width: 180,
        },
        {
          prop: "embryoReleaseDate",
          label: this.$t("ui.data.column.trialPlan.embryoReleaseDate"),
          width: 180,
        },
        {
          prop: "textNo",
          label: this.$t("ui.data.column.trialPlan.textNo"),
          width: 180,
        },
        {
          prop: "textType",
          label: this.$t("ui.data.column.trialPlan.textType"),
          width: 180,
        },
        {
          prop: "textReleaseDate",
          label: this.$t("ui.data.column.trialPlan.textReleaseDate"),
          width: 180,
        },
        {
          prop: "lhNo",
          label: this.$t("ui.data.column.trialPlan.lhNo"),
          width: 180,
        },
        {
          prop: "lhType",
          label: this.$t("ui.data.column.trialPlan.lhType"),
          width: 180,
        },
        {
          prop: "lhReleaseDate",
          label: this.$t("ui.data.column.trialPlan.lhReleaseDate"),
          width: 180,
        },

      ];

      return columns;
    },
    searchColumns() {
      return [

        {
          label: this.$t("common.factory"),
          prop: "factoryCode",
          type:'select',
          dictData: this.dict.type.biz_factory_name,
        },
        {
          label: this.$t("ui.data.column.monthplan.oriMaterialCode"),
          prop: "materialCode",
        },
        // {
        //   label: this.$t("ui.data.column.sizeCapacity.mouldMethod"),
        //   prop: "mouldMethod",
        //   type:'select',
        //   dictData: this.dict.type.molding_method,
        // },
        {
          label: this.$t("ui.data.column.skuConstruction.embryoCode"),
          prop: "embryoCode",
        },


      ];
    },
  },
  methods: {
    async mesCaptureing(){
      try {
        this.loading = true;
        const res = await mesCapture();
        this.$modal.msgSuccess(res.msg);
        this.$set(this.page, "current", 1);
        this.getList();
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
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
    handleDelete(rows) {
      this.$confirm(this.$t("common.confirm.delete"), {
        type: "warning",
      }).then(() => {
        const ids = rows.map((row) => row.id).join(",");
        console.log(ids);
        // removeMdmProductConstruction({ ids }).then((data) => {
        //   this.$modal.msgSuccess(data.msg);
        //   this.$set(this.page, "current", 1);
        //   this.getList();
        // });
      });
    },
    handleChangeStatus(status, row) {
      console.log(status);
      let label =
        status === "0"
          ? this.$t("ui.biz.alter.isOpen")
          : this.$t("ui.biz.alter.isStop");

      this.$confirm(label, {
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

    handleExport() {
      downloadLink(
        "/monthplan/mdmSkuConstructionRef/export",
        this.formatParams(false)
      );
    },

    handleSelectionChange(rows) {
      this.selection = rows;
    },

    // utils
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
        const data = await listMdmProductConstruction(this.formatParams());
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
