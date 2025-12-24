
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
        <!-- <el-button v-hasPermi="['setting:material:add']" @click="handleAdd">{{
          $t("ui.frame.btn.add")
        }}</el-button> -->
        <el-button
          v-hasPermi="['lean:productinfo:edit']"
          @click="handleEdit(selection[0])"
          :disabled="selection.length != 1"
          >{{ $t("ui.frame.btn.modify") }}</el-button
        >
        <!-- <el-button
          material="danger"
          @click="handleDeleteMulti"
          :disabled="selection.length == 0"
          >{{ $t("ui.frame.btn.delete") }}</el-button
        > -->
        <el-button
          v-hasPermi="['lean:productinfo:import']"
          @click="() => $refs.tltUploadForm.handleImport()"
          >{{ $t("ui.frame.btn.import") }}
        </el-button>
        <el-button @click="handleExport"
        v-hasPermi="['lean:productinfo:export']"
          >{{ $t("ui.frame.btn.export") }}
        </el-button>
        <!--<el-button-->
        <!--  v-hasPermi="['setting:material:export']"-->
        <!--  material="warning"-->
        <!--  @click="handleExport"-->
        <!--&gt;{{ $t("ui.frame.btn.export") }}-->
        <!--</el-button-->
        <!--&gt;-->
      </template>
    </page-table>
    <tlt-upload
      ref="tltUploadForm"
      :updateSupport="true"
      downloadUrl="/lean/productinfo/importTemplate"
      uploadUrl="/lean/productinfo/importData"
      @uploadSuccess="getList"
    />
    <InfoDialog ref="infoDialogRef" @success="handelSuccess" />
  </basic-container>
</template>
<script>
import moment from "moment";

import { listMaterial, removeMaterial } from "@/api/setting/material";
import InfoDialog from "./components/infoDialog.vue";
import tltUpload from "@/components/tltUpload/tltUpload.vue";
import { downloadLink } from "@/utils/request";
import {
  tableListProductinfo,
  listProductinfo,
  editProductinfo,
  removeProductinfo,
  // configurationMould,
} from "@/api/lean/productinfo";
export default {
  name: "RubberMaterial",
  components: { InfoDialog, tltUpload },
  dicts: ["GLUE_TYPE", "MAJOR_TYPE", "biz_factory_name","material_type",'biz_brand_type','biz_yes_no','biz_product_type','product_category'],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    let tomorrow = moment().add(1, "days").format("YYYY-MM-DD");
    return {

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
  computed: {
    searchColumns() {
      return [
        // {
        //   label: this.$t("setting.material.materialCode"),
        //   prop: "materialCode",
        // },
        // {
        //   label: this.$t("setting.material.materialName"),
        //   prop: "materialName",
        // },
        // {
        //   label: this.$t("setting.material.glueType"),
        //   prop: "glueType",
        //   type: "select", //GLUE_TYPE
        //   dictData: this.dict.type.GLUE_TYPE,
        // },
        {
          label: this.$t("common.factory"),
          prop: "factoryCode",
          type: "select", //GLUE_TYPE
          dictData: this.dict.type.biz_factory_name,
        },
        {
          label: this.$t("ui.data.column.facMonthPlanInit.productSpecsName"),
           prop: "materialCategory",
            type: "select", //GLUE_TYPE
          dictData: this.dict.type.material_type,
        },
        {
          label: this.$t("ui.data.colume.wms.unused.productCode")+'(NC)',
          prop: "materialCode",
        },
        {
          label: this.$t("ui.data.colume.wms.unused.productCode")+'(MES)',
           prop: "mesMaterialCode",
        },
        // {
        //   label: this.$t("物料名称"),
        //   prop: "materialCode",
        // },
        {
          label: this.$t("ui.data.column.reportClassAccuracy.materialCode"),
           prop: "specifications",
        },
        {
          label: this.$t("ui.data.column.confMinProd.pattern"),
          prop: "pattern",
        },
        {
          label: this.$t("ui.data.column.scheduleAdjust.productCodeDesc"),
          prop: "materialDesc",
        },
      ];
    },
    columns() {

      return [
      { type: "selection", fixed: "left" },
        {
          prop: "factoryCode",
          halign: "center",
          align: "center",
          width: 120,
          label: this.$t("common.factory"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_factory_name, value);
          },
        },
        {
          prop: "productCategory",
          label: this.$t("ui.data.column.capsuleChuck.productTypeCode"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.product_category, value);
          },
        },
        {
           prop: "materialCategory",
          halign: "center",
          align: "center",
          width: 120,
          label: this.$t("ui.data.column.facMonthPlanInit.productSpecsName"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.material_type, value);
          },
        },
        {
          prop: "structureName",
          halign: "center",
          align: "center",
          width: 120,
          label: this.$t("ui.data.column.scheduleAdjust.structureCode"),
        },
        {
          prop: "materialCode",
          halign: "center",
          align: "center",
          width: 120,
          label: this.$t("ui.data.colume.wms.unused.productCode")+'(NC)',
        },
        {
           prop: "mesMaterialCode",
          halign: "center",
          align: "center",
          width: 120,
          label: this.$t("ui.data.colume.wms.unused.productCode")+'(MES)',
        },
        // {
        //   prop: "物料名称",
        //   halign: "center",
        //   align: "center",
        //   width: 120,
        //   label: this.$t("物料名称"),
        // },
        {
          prop: "materialDesc",
          halign: "center",
          align: "center",
          width: 120,
          label: this.$t("ui.data.column.scheduleAdjust.productCodeDesc"),
        },
        {
           prop: "specifications",
          halign: "center",
          align: "center",
          width: 120,
          label: this.$t("ui.data.column.reportClassAccuracy.materialCode"),
        },
        {
          prop: "pattern",
          halign: "center",
          align: "center",
          width: 120,
          label: this.$t("ui.data.column.confMinProd.pattern"),
        },
        {
          prop: "brand",
          halign: "center",
          align: "center",
          width: 120,
          label: this.$t("ui.data.colume.plan.first.draft.brand"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_brand_type, value);
          },
        },
        {
          prop: "speed",
          halign: "center",
          align: "center",
          width: 120,
          label: this.$t("ui.data.column.scheduleAdjust.seep"),
        },
        {
          prop: "hierarchy",
          halign: "center",
          align: "center",
          width: 120,
          label: this.$t("ui.data.column.scheduleAdjust.hierarchy"),
        },
        {
          prop: "proSize",
          halign: "center",
          align: "center",
          width: 120,
          label: this.$t("ui.data.column.scheduleAdjust.proSize"),
        },
        {
          prop: "ability",
          halign: "center",
          align: "center",
          width: 120,
          label: this.$t("ui.data.column.lean.productinfo.ability"),
        },
        {
          prop: "cantProduce",
          halign: "center",
          align: "center",
          width: 120,
          label: this.$t("ui.data.column.scheduleAdjust.cantProduce"),
          formatter: (row, column, value) => {
            return this.selectDictLabel(this.dict.type.biz_yes_no, value);
          },

        },
        {
          prop: "remark",
          halign: "center",
          align: "center",
          width: 120,
          label: this.$t("common.remark"),
        },
        {
          prop: "updateTime",
          halign: "center",
          align: "center",
          width: 180,
          label: this.$t("ui.data.column.scheduleAdjust.updata"),
        },
           {
          align: "center",
          halign: "center",
          label: this.$t("common.option"),
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
                  v-hasPermi={["lean:productinfo:edit"]}
                >
                  {this.$t("ui.frame.btn.update")}
                </el-button>
                {/* <el-button
                  class="minus"
                  type="danger"
                  onClick={() => this.handleDelete(row)}
                >
                  {this.$t("ui.frame.btn.delete")}
                </el-button> */}
              </div>
            );
          },
        },
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
        material: "warning",
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
        material: "warning",
      }).then(() => {
        const ids = row.id;
        removeMaterial({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
      });
    },
    handleExport() {
      downloadLink("/lean/productinfo/exportData", this.formatParams(false));
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
        const data = await listProductinfo(this.formatParams());
        console.log(data)
        this.data =  data.rows
        this.page.total = data.total;
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
  },
  mounted() {
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
    // this.getList();
  },
};
</script>
