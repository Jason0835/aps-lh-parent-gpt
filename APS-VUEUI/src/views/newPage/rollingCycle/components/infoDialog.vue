<template>
  <el-dialog
    title="结构调整"
    :visible="visible"
    width="800px"
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
      :columns="columns"
      label-position="right"
      label-width="160px"
      v-loading="loading"
    >
    </info-form>
    <el-table :data="tableData" border style="width: 100%">
      <el-table-column prop="productStructure" label="产品结构" width="120">
      </el-table-column>
      <el-table-column prop="schedulingMachine" label="排产机台" width="120">
      </el-table-column>
      <el-table-column prop="ncMaterialCode" label="NC物料编码" width="120">
      </el-table-column>
      <el-table-column prop="materialDescription" label="物料描述" width="120">
      </el-table-column>
      <el-table-column prop="isContainMaterials" label="是否含材料" width="120">
      </el-table-column>
      <el-table-column
        prop="lastWeekNetDemand"
        label="调整前净需求量（上周）"
        width="120"
      >
      </el-table-column>
      <el-table-column prop="currentNetDemand" label="当前净需求量" width="120">
      </el-table-column>
      <el-table-column prop="netDemandChange" label="净需求变动" width="120">
      </el-table-column>
      <el-table-column
        prop="monthlyPlanProduction"
        label="月计划已排产量（第1轮结构内调整后）"
        width="120"
      >
      </el-table-column>
      <el-table-column
        prop="pendingAdjustmentAmount"
        label="待调整量（降序）"
        width="120"
      >
      </el-table-column>
      <el-table-column label="确认调整量" width="120">
        <template slot-scope="scope">
          <el-input
            v-model="scope.row.confirmAdjustmentAmount"
            placeholder="请输入内容"
            size="mini"
          ></el-input>
        </template>
      </el-table-column>
      <el-table-column prop="adjustPriorities" label="调整优先级" width="120">
        <template slot-scope="scope">
          <el-input
            v-model="scope.row.adjustPriorities"
            placeholder="请输入内容"
            size="mini"
          ></el-input>
        </template>
      </el-table-column>
      <el-table-column prop="actualAdjustment" label="实际调整" width="120">
      </el-table-column>
      <el-table-column prop="adjustmentReason" label="调整原因" width="120">
      </el-table-column>
      <el-table-column fixed="right" label="操作" width="100">
        <template slot-scope="scope">
          <el-button type="text" size="small" @click="handleDelete(scope.$index)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <template slot="footer">
      <el-button @click="hide">{{ this.$t("common.button.cancel") }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">{{
        this.$t("common.button.confirm")
      }}</el-button>
    </template>
  </el-dialog>
</template>

<script>
import { mapState } from "vuex";

// import { editCxSpecifyMachine } from "@/api/cx/cxSpecifyMachine";
import { editProductMoldingLimit } from "@/api/mdm/productMoldingLimit";

import infoForm from "@/views/components/infoForm.vue";

export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      selection:{},
      tableData: [
        {
          productStructure: "315/80R22.5-JD758零度",
          schedulingMachine: "H1101\\H1102",
          ncMaterialCode: "3302000915",
          materialDescription: "315/80R22.5 156/153K 20PR JD755 BL0EJY",
          isContainMaterials: "否",
          lastWeekNetDemand: "100",
          currentNetDemand: "0",
          netDemandChange: "-100",
          monthlyPlanProduction: "100",
          pendingAdjustmentAmount: "-100",
          confirmAdjustmentAmount: "-100",
          adjustPriorities: "",
          actualAdjustment: "",
          adjustmentReason: ""
        },
        {
          productStructure: "315/80R22.5-JD758零度",
          schedulingMachine: "H1101\\H1102",
          ncMaterialCode: "3302002306",
          materialDescription: "315/80R22.5 156/150J 20PR JD755 BL0EJY DL",
          isContainMaterials: "否",
          lastWeekNetDemand: "200",
          currentNetDemand: "150",
          netDemandChange: "-50",
          monthlyPlanProduction: "150",
          pendingAdjustmentAmount: "0",
          confirmAdjustmentAmount: "0",
          adjustPriorities: "",
          actualAdjustment: "",
          adjustmentReason: ""
        },
        {
          productStructure: "315/80R22.5-JD758零度",
          schedulingMachine: "H1101\\H1102",
          ncMaterialCode: "3302002356",
          materialDescription: "315/80R22.5 156/150J 20PR BD290 BL0EBL DL",
          isContainMaterials: "否",
          lastWeekNetDemand: "250",
          currentNetDemand: "330",
          netDemandChange: "80",
          monthlyPlanProduction: "200",
          pendingAdjustmentAmount: "130",
          confirmAdjustmentAmount: "130",
          adjustPriorities: "1",
          actualAdjustment: "",
          adjustmentReason: ""
        }
      ],
      form: {},
      rules: {
        factoryCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        sapCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        embryoCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        machineCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        lineType: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        jobType: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
      },
    };
  },
  computed: {
    ...mapState({
      moldingMachines: (state) => state.molding.machines,
    }),
    title: function () {
      return this.isEdit
        ? this.$t("common.button.edit")
        : this.$t("common.button.add");
    },
    columns() {
      return [
        {
          prop: "machine",
          label: this.$t("机台"),
          span:12
        },
        {
          prop: "productStructure",
          label: this.$t("产品结构"),
          span:12
        },
        {
          prop: "startDate",
          label: this.$t("开始日期"),
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
          span:12
        },
        {
          prop: "endDate",
          label: this.$t("结束日期"),
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
          span:12
        },
        {
          prop: "adjustEndDate",
          label: this.$t("调整结束日期"),
          type: "date",
          dateType: "month",
          span:12,
          valueFormat: "yyyy-MM",
        },
        {
          prop: "后续平移",
          label: this.$t("后续平移"),
          span:12,
          type:"checkbox"

        },
      ];
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        // const res = await editProductMoldingLimit(params);
        // this.$modal.msgSuccess(res.msg);
        this.$emit("success");
        this.hide();

        this.loading = false;
      } catch (error) {
        console.log(error);
        this.loading = false;
      }
    },

    // 删除操作
    handleDelete(index) {
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

    //utils
    show(data) {
      this.visible = true;
      if (data) {
        this.selection = data;
        this.form = {
          ...data,
        };
      } else {
        this.form = {
          factoryCode: "",
        };
      }
    },
    hide() {
      this.form = {};
      this.$refs.form.triggerResetForm();
      // this.resetForm("infoForm");
      this.isEdit = false;
      this.visible = false;
    },
    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
    },
  },
};
</script>
