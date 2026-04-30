<template>
  <el-dialog
    title="特殊材料生产情况"
    :visible="visible"
    width="800px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :append-to-body="true"
  >
  <el-table :data="tableData1" border style="width: 100%">
      <el-table-column prop="process" label="工序" width="120">
      </el-table-column>
      <el-table-column prop="specialMaterial" label="特殊材料" width="120">
      </el-table-column>
      <el-table-column prop="todayStockAndPlan" label="特殊材料今日库存+今日计划" width="120">
      </el-table-column>
      <el-table-column prop="equivalentTireCount" label="折合轮胎条数" width="120">
      </el-table-column>
      <el-table-column prop="originalStructure" label="原结构" width="120">
      </el-table-column>
      <el-table-column prop="adjustedTireCount" label="调整的轮胎条数" width="120">
      </el-table-column>
    </el-table>

    <el-table :data="tableData2" border style="width: 100%;margin-top: 20px">
      <el-table-column prop="process" label="工序" width="120">
      </el-table-column>
      <el-table-column prop="specialMaterial" label="特殊材料" width="120">
      </el-table-column>
      <el-table-column prop="todayStockAndPlan" label="特殊材料今日库存+今日计划" width="120">
      </el-table-column>
      <el-table-column prop="equivalentTireCount" label="折合轮胎条数" width="120">
      </el-table-column>
      <el-table-column prop="originalSpecification" label="原规格" width="120">
      </el-table-column>
      <el-table-column prop="adjustedTireCount" label="调整的轮胎条数" width="120">
      </el-table-column>
    </el-table>

    <template slot="footer">
      <el-button @click="hide">{{ this.$t("common.button.cancel") }}</el-button>
      <el-button type="primary" :loading="loading" @click="hide">{{
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
      tableData1: [
        {
          process: "压延",
          specialMaterial: "396钢丝",
          todayStockAndPlan: "2000米",
          equivalentTireCount: "1090",
          originalStructure: "12.00R20",
          adjustedTireCount: "1090"
        }
      ],
      tableData2: [
        {
          process: "密炼",
          specialMaterial: "T601",
          todayStockAndPlan: "1.5T",
          equivalentTireCount: "500",
          originalSpecification: "雪地胎",
          adjustedTireCount: "500"
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
          prop: "process",
          label: this.$t("工序"),
          span: 12,
        },
        {
          prop: "specialMaterial",
          label: this.$t("特殊材料"),
          span: 12,
        },
        {
          prop: "todayStockAndPlan",
          label: this.$t("特殊材料今日库存+今日计划"),
          span: 12,
        },
        {
          prop: "equivalentTireCount",
          label: this.$t("折合轮胎条数"),
          span: 12,
        },
        {
          prop: "originalStructure",
          label: this.$t("原结构"),
          span: 12,
        },
        {
          prop: "adjustedTireCount",
          label: this.$t("调整的轮胎条数"),
          span: 12,
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
    },
    hide() {
      this.visible = false;
    },
    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
    },
  },
};
</script>
