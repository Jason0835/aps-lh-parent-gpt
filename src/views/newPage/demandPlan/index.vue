<template>
  <basic-container>
    <page-table
      tableRef="DemandPlanMainTable"
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
          v-hasPermi="['monthplan:productionMouldConfiguration:add']"

          >{{ $t("生成需求计划") }}
        </el-button>
        <!-- <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:productionMouldConfiguration:add']"
          @click="handleAdd"
          >{{ $t("生成周度需求计划") }}
        </el-button> -->
        <el-button
          type="success"
          plain
          v-hasPermi="['monthplan:productionMouldConfiguration:add']"
          @click="handleChanged"
          >{{ $t("优先级调整") }}
        </el-button>
        <!-- <el-button
          type="primary"
          plain
          v-hasPermi="['monthplan:productionMouldConfiguration:add']"
          @click="handleAdd"
          >{{ $t("ui.frame.btn.add") }}
        </el-button> -->
        <!-- <el-button
          type="danger"
          plain
          :disabled="selection.length == 0"
          v-hasPermi="['monthplan:productionMouldConfiguration:remove']"
          @click="handleDelete(selection)"
          >{{ $t("ui.frame.btn.delete") }}
        </el-button> -->

        <!-- <el-button
          v-hasPermi="['monthplan:productionMouldConfiguration:import']"
          @click="$refs.tltUpload.handleImport()"
          >{{ $t("ui.frame.btn.import") }}
        </el-button> -->
        <el-button
          @click="handleExport"
          v-hasPermi="['monthplan:productionMouldConfiguration:export']"
          >{{ $t("ui.frame.btn.export") }}
        </el-button>
      </template>
    </page-table>
    <!-- <el-button style="display: none" ref="hidePopoverBtnRef"></el-button> -->
    <tlt-upload
      ref="tltUpload"
      downloadUrl="/monthplan/productionMouldConfiguration/importTemplate"
      uploadUrl="/monthplan/productionMouldConfiguration/importData"
      @uploadSuccess="getList"
    />
    <!-- <infoDialog ref="infoRef" @success="getList" /> -->
    <el-dialog
      :title="title"
      :visible="visible"
      width="400px"
      @close="hide"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :append-to-body="true"
    >
    <info-form
      class="form-item-height"
      ref="form"
      :form="form"
      :rules="rules"
      :columns="formColumns"
      label-position="right"
      label-width="160px"
      v-loading="loading"
    >
    </info-form>
    <template slot="footer">
      <el-button @click="hide">{{ this.$t("common.button.cancel") }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">{{
        this.$t("common.button.confirm")
      }}</el-button>
    </template>
    </el-dialog>
  </basic-container>
</template>
<script>
import { downloadLink } from "@/utils/request";
import {
  listProductionMouldConfiguration,
  editProductionMouldConfiguration,
  removeProductionMouldConfiguration,
  buildMouldingProduct,
} from "@/api/monthplan/productionMouldConfiguration";
import tltUpload from "@/components/tltUpload/tltUpload.vue";

import infoForm from "@/views/components/infoForm.vue";
// import infoDialog from "./components/infoDialog.vue";

export default {
  name: "DemandPlan",
  components: {
    tltUpload,
    infoForm
    // infoDialog,
  },
  dicts: [],
  provide() {
    return {
      parentDict: this.dict,
    };
  },
  data() {
    return {
      title:"优先级调整",
      loading: false,
      visible:false,
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
      form:{},
      rules: {
        供应链优先级: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        是否排产: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],

      },
      formColumns: [
        {
          prop: "供应链优先级",
          label: this.$t("供应链优先级"),
          type: "select",
        },

      ],
    };
  },
  computed: {
    columns() {
      let columns = [
        // { type: "selection", fixed: "left" },
        {
          prop: "origin",
          label: this.$t("工厂"),
        },
        {
          prop: "year",
          label: this.$t("ui.data.column.productionMouldConfiguration.year"),
        },
        {
          prop: "month",
          label: this.$t("ui.data.column.productionMouldConfiguration.month"),
        },
        {
          prop: "version",
          label: this.$t("产品分类"),
        },

        {
          prop: "productCode",
          label: this.$t("类型"),
        },
        {
          prop: "mouldCode",
          label: this.$t("品牌"),
        },
        {
          prop: "供应链优先级",
          label: this.$t("供应链优先级"),
        },
        {
          prop: "产品结构",
          label: this.$t("产品结构"),
        },
        {
          prop: "主花纹",
          label: this.$t("主花纹"),
        },
        {
          prop: "NC物料编码",
          label: this.$t("NC物料编码"),
        },
        {
          prop: "物料描述",
          label: this.$t("物料描述"),
        },
        {
          prop: "排产分类",
          label: this.$t("排产分类"),
        },
        {
          prop: "年周号",
          label: this.$t("年周号"),
        },
        {
          prop: "均匀性",
          label: this.$t("均匀性"),
        },
        {
          prop: "动平衡",
          label: this.$t("动平衡"),
        },
        {
          prop: "订单量",
          label: this.$t("订单量"),
        },
        {
          prop: "库存",
          label: this.$t("库存"),
        },
        {
          prop: "月底余量",
          label: this.$t("月底余量"),
        },
        {
          prop: "排产净需求",
          label: this.$t("排产净需求"),
        },
        {
          prop: "是否排产",
          label: this.$t("是否排产"),
        },
        {
          prop: "净需求",
          label: this.$t("净需求(含暂缓)"),
        },
        {
          prop: "净需求No",
          label: this.$t("净需求(不含暂缓)"),
        },
        {
          prop: "高优先级",
          label: this.$t("高优先级"),
        },
        {
          prop: "中优先级",
          label: this.$t("中优先级"),
        },
        {
          prop: "暂缓订单",
          label: this.$t("暂缓订单"),
        },
        {
          prop: "周期排产储备",
          label: this.$t("周期排产储备"),
        },
        {
          prop: "常规排产储备",
          label: this.$t("常规排产储备"),
        },
        {
          prop: "是否满足最小投产量",
          label: this.$t("是否满足最小投产量"),
        },
        {
          prop: "最小投产量值",
          label: this.$t("最小投产量值"),
        },
        {
          prop: "备注",
          label: this.$t("备注"),
        },
        {
          prop: "更新日期",
          label: this.$t("更新日期"),
        },
      ];

      return columns;
    },
    searchColumns() {
      return [
        {
          prop: "origin",
          label: this.$t("工厂"),
          type: "select",
        },
        {
          label: "年月",
          prop: "yearMonth",
          type: "date",
          format: "YYYY-MM",
        },

        {
          prop: "需求计划版本号",
          label: this.$t("需求计划版本号"),
          type: "select",
        },
        {
          prop: "mouldCode",
          label: this.$t("产品分类"),
          type: "select",
        },
        {
          prop: "优先级(订单类型)",
          label: this.$t("优先级(订单类型)"),
        },
        {
          prop: "是否替换料",
          label: this.$t("是否替换料"),
          type: "checkbox",
        },
        {
          prop: "不足最小投产量",
          label: this.$t("不足最小投产量"),
          type: "checkbox",
        },
        {
          prop: "NC物料编码",
          label: this.$t("NC物料编码"),
        },
        {
          prop: "物料描述",
          label: this.$t("物料描述"),
        },
      ];
    },
  },
  methods: {
    save(){},
    hide() {
      this.$refs.form.triggerResetForm();
      this.visible = false;

    },
    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
    },
    handleChanged() {
      this.formColumns= [
        {
          prop: "供应链优先级",
          label: this.$t("供应链优先级"),
          type: "select",
        },

      ],
      this.title="优先级调整"
      this.visible = true;
    },
    handleRow(){
      this.formColumns= [
        {
          prop: "是否排产",
          label: this.$t("是否排产"),
          type: "select",
        },

      ],
      this.title="是否排产"
      this.visible = true;
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
        removeProductionMouldConfiguration({ ids }).then((data) => {
          this.$modal.msgSuccess(data.msg);
          this.$set(this.page, "current", 1);
          this.getList();
        });
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
          const res = await editProductionMouldConfiguration({
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
        "/monthplan/productionMouldConfiguration/export",
        this.formatParams(false)
      );
    },

    handleSelectionChange(rows) {
      this.selection = rows;
    },

    handleBuild() {
      if (this.$refs.buildRef) {
        this.$refs.buildRef.show();
      }
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
        let list=[
          {
            origin:'116',
            year:'2025',
            month:'11',
            version:'导向',
            productCode:'正规',
            mouldCode:'AMULET',
            供应链优先级:12,
            产品结构:'ST235/80R16',
            主花纹:'AT505',
            NC物料编码:'3302002547',
            物料描述:'ST235/80R16 129/125M 14PR AT505 BL3HAM',
            排产分类:'周期排产',
            年周号:'2586',
            均匀性:'是',
            动平衡:'是',
            订单量:'1247',
            库存:'256',
            月底余量:'158',
            排产净需求:'3425',
            是否排产:'是',
            净需求:'3425',
            净需求No:'3425',
            高优先级:'300',
            中优先级:'120',
            暂缓订单:'10',
            周期排产储备:'3000',
            常规排产储备:'2000',
            是否满足最小投产量:'是',
            最小投产量值:'1000',
            备注:'',
            更新日期:'2025-11-20'
          },
          {
            origin:'116',
            year:'2025',
            month:'11',
            version:'导向',
            productCode:'正规',
            mouldCode:'AMULET',
            供应链优先级:12,
            产品结构:'11R22.5-JD571四层',
            主花纹:'JF568',
            NC物料编码:'330201108',
            物料描述:'11R22.5 146/143L 16PR JD571 BL4HJY',
            排产分类:'按单产',
            年周号:'2586',
            均匀性:'是',
            动平衡:'是',
            订单量:'1247',
            库存:'256',
            月底余量:'158',
            排产净需求:'3425',
            是否排产:'是',
            净需求:'3425',
            净需求No:'3425',
            高优先级:'300',
            中优先级:'120',
            暂缓订单:'10',
            周期排产储备:'3000',
            常规排产储备:'2000',
            是否满足最小投产量:'是',
            最小投产量值:'1000',
            备注:'',
            更新日期:'2025-11-20'
          },
          {
            origin:'116',
            year:'2025',
            month:'11',
            version:'导向',
            productCode:'正规',
            mouldCode:'AMULET',
            供应链优先级:12,
            产品结构:'ST235/80R16',
            主花纹:'AT505',
            NC物料编码:'3302002547',
            物料描述:'ST235/80R16 129/125M 14PR AT505 BL3HAM',
            排产分类:'常规产品',
            年周号:'2586',
            均匀性:'是',
            动平衡:'是',
            订单量:'1247',
            库存:'256',
            月底余量:'158',
            排产净需求:'3425',
            是否排产:'是',
            净需求:'3425',
            净需求No:'3425',
            高优先级:'300',
            中优先级:'120',
            暂缓订单:'10',
            周期排产储备:'3000',
            常规排产储备:'2000',
            是否满足最小投产量:'是',
            最小投产量值:'1000',
            备注:'',
            更新日期:'2025-11-20'
          },
        ]
        // const data = await listProductionMouldConfiguration(
        //   this.formatParams()
        // );
        // console.log(data);
        this.data = list;
        this.page.total = 3
      } catch (error) {
        console.error(error);
      } finally {
        this.loading = false;
      }
    },
  },
  created() {},
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
