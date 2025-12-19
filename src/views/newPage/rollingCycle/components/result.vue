<template>
    <basic-container>
  <!-- <el-dialog
    title="调整结果"
    :visible="visible"
    width="800px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :append-to-body="true"
  > -->

    <el-table :data="tableData" border >
      <el-table-column prop="formingMachine" label="成型机台" >
      </el-table-column>
      <el-table-column prop="productStructure" label="产品结构" width="180">
      </el-table-column>
      <el-table-column prop="ncMaterialCode" label="NC物料编码"  width="120">
      </el-table-column>
      <el-table-column prop="materialDescription" label="物料描述"  width="200">
      </el-table-column>
      <el-table-column prop="isContainMaterials" label="是否含材料"  width="120">
      </el-table-column>
      <el-table-column prop="planQuantity" label="计划量">
      </el-table-column>
      <el-table-column prop="startDate" label="开始日期" >
      </el-table-column>
      <el-table-column prop="endDate" label="结束日期">
      </el-table-column>
      <el-table-column v-for="item in daysList" :key="item.label"  :label="item.label" >
      </el-table-column>
      <el-table-column v-if="isLock" prop="lockDate" label="锁定上机日期" width="180" >
      </el-table-column>
    </el-table>
    <!-- <template slot="footer">
      <el-button @click="hide">{{ this.$t("common.button.cancel") }}</el-button>
      <el-button type="primary" :loading="loading" @click="hide">{{
        this.$t("common.button.confirm")
      }}</el-button>
    </template>
  </el-dialog> -->
</basic-container>
</template>

<script>
import { mapState } from "vuex";
import moment from "moment";
// import { editCxSpecifyMachine } from "@/api/cx/cxSpecifyMachine";
import { editProductMoldingLimit } from "@/api/mdm/productMoldingLimit";

import infoForm from "@/views/components/infoForm.vue";

export default {
  components: { infoForm },
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      tableData: [],
      form: {},
      daysList:[],
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
      isLock:false,
    };
  },
  mounted() {
  const data = [
    {
      productStructure: "315/80R22.5-JD758零度",
      schedulingMachine: "H1101\\H1102",
      ncMaterialCode: "3302000915",
      materialDescription: "315/80R22.5 156/153K 20PR JD755 BL0EJY",
      isContainMaterials: "否",
      lastWeekOriginalNetDemand: "100",
      currentNetDemand: "0",
      netDemandChange: "-100",
      monthlyPlanProductionQuantity: "100",
      pendingAdjustmentAmount: "-100",
      confirmAdjustmentAmount: "-100",
      adjustPriorities: "",
      actualAdjustment: "",
      adjustmentReason: "",
      formingMachine: "H1101",
      planQuantity: "100",
      startDate: "1",
      endDate: "5",
      adjustPlanQuantity: "100",
      adjustStartDate: "1",
      adjustEndDate: "10",
      day1: "46",
      day2: "46",
      day3: "46",
      lockDate: "2024-01-15" // 锁定上机日期
    },
    {
      productStructure: "315/80R22.5-JD758零度",
      schedulingMachine: "H1101\\H1102",
      ncMaterialCode: "3302002306",
      materialDescription: "315/80R22.5 156/150J 20PR JD755 BL0EJY DL",
      isContainMaterials: "否",
      lastWeekOriginalNetDemand: "200",
      currentNetDemand: "150",
      netDemandChange: "-50",
      monthlyPlanProductionQuantity: "150",
      pendingAdjustmentAmount: "0",
      confirmAdjustmentAmount: "0",
      adjustPriorities: "",
      actualAdjustment: "",
      adjustmentReason: "",
      formingMachine: "H1101",
      planQuantity: "120",
      startDate: "6",
      endDate: "10",
      adjustPlanQuantity: "100",
      adjustStartDate: "1",
      adjustEndDate: "10",
      day1: "32",
      day2: "46",
      day3: "46",
      lockDate: "2024-01-15" // 锁定上机日期
    },
    {
      productStructure: "315/80R22.5-JD758零度",
      schedulingMachine: "H1101\\H1102",
      ncMaterialCode: "3302002356",
      materialDescription: "315/80R22.5 156/150J 20PR BD290 BL0EBL DL",
      isContainMaterials: "否",
      lastWeekOriginalNetDemand: "250",
      currentNetDemand: "330",
      netDemandChange: "80",
      monthlyPlanProductionQuantity: "200",
      pendingAdjustmentAmount: "130",
      confirmAdjustmentAmount: "130",
      adjustPriorities: "1",
      actualAdjustment: "",
      adjustmentReason: "",
      formingMachine: "H1102",
      planQuantity: "150",
      startDate: "11",
      endDate: "15",
      adjustPlanQuantity: "100",
      adjustStartDate: "1",
      adjustEndDate: "10",
      day1: "32",
      day2: "46",
      day3: "46",
      lockDate: "2024-01-15" // 锁定上机日期
    },
  ];

  // 将数据赋值给 tableData
  this.tableData = data;
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
          prop: "机台",
          label: this.$t("机台"),
          span: 12,
        },
        {
          prop: "产品结构",
          label: this.$t("产品结构"),
          span: 12,
        },
        {
          prop: "开始日期",
          label: this.$t("开始日期"),
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
          span: 12,
        },
        {
          prop: "结束日期",
          label: this.$t("结束日期"),
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
          span: 12,
        },
        {
          prop: "调整结束日期",
          label: this.$t("调整结束日期"),
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
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

    //utils
    show(data) {
      this.visible = true;
      this.isLock=data
      console.log(data,'asasas')
      const date = moment(this.form.yearMonth);
      const days = date.daysInMonth();
      let dayList = [];
      for (let i = 0; i < days; i++) {
        dayList.push({
          // label: `${i + 1}号`,
          label: this.$t("ui.data.column.mouldingDayResult.day", {
            day: i + 1,
          }),
          prop: `day${i + 1}`,
          content: "20",
        });
      }
      this.daysList = dayList;
    },
    hide() {
      this.visible = false;
    },
    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
    },
  },
  created() {
    this.show(true)
  }
};
</script>
