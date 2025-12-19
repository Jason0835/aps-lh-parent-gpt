<template>
  <el-dialog
    :title="title"
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
      form: {},
      rules: {
        工厂: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        年月: [
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
          prop: "工厂",
          label: this.$t("工厂"),
          type: "select",
        },
        {
          prop: "年月",
          label: this.$t("年月"),
          type: "date",
          format: "yyyy-MM",
        },
        {
          prop: "订单类型",
          label: this.$t("订单类型"),
          type: "select",
        },
        {
          prop: "NC物料编码",
          label: this.$t("NC物料编码"),
          type:'number'
        },
        {
          prop: "储备数量",
          label: this.$t("储备数量"),
          type:'number'
        },
        {
          prop: "物料描述",
          label: this.$t("物料描述"),
        },
        {
          prop: "产品分类",
          label: this.$t("产品分类"),
        },
        {
          prop: "品牌",
          label: this.$t("品牌"),
        },
        {
          prop: "月均销量",
          label: this.$t("月均销量"),
          type:'number'
        },  {
          prop: "滚动12个月发货频次",
          label: this.$t("滚动12个月发货频次"),
          type:'number'
        },
        {
          prop: "适销区域",
          label: this.$t("适销区域"),
        },
        {
          prop: "超6个月库存",
          label: this.$t("超6个月库存"),
          type:'number'
        },
        {
          prop: "超9个月库存",
          label: this.$t("超9个月库存"),
          type:'number'
        },
        {
          prop: "备库上限",
          label: this.$t("备库上限"),
        },  {
          prop: "备注",
          label: this.$t("备注"),
        },
      ];
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await editProductMoldingLimit(params);
        this.$modal.msgSuccess(res.msg);
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
      if (data) {
        this.isEdit = true;
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
