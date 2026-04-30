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

import { saveData, savePoData } from "@/api/newPage/salesOrderPool";

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
        orderPriority: [
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
        // {
        //     prop: "factoryCode",
        //     label: this.$t("common.factory"),
        //     type: "select",
        //     disabled: true,
        //     dictData: this.parentDict.type.biz_factory_name,
        //   },

        //   {
        //     prop: "productType",
        //     label: this.$t("ui.data.column.monthplan.productType"),
        //     type: "select",
        //     disabled: true,
        //     dictData: this.parentDict.type.biz_product_type,
        //   },
        {
          prop: "salCodePo",
          label: this.$t("ui.data.column.monthplan.salCodePo"),

        },
        {
          prop: "scmPriority",
          label: this.$t("ui.data.column.monthplan.scmPriority"),
          type: "select",
          dictData: this.parentDict.type.biz_scm_type,
        },
        // {
        //   prop: "area",
        //   label: this.$t("common.area"),
        //   disabled: true,
        // },
        // {
        //   prop: "salCode",
        //   label: this.$t("ui.data.column.monthplan.salCode"),
        //   disabled: true,
        // },
        // {
        //   prop: "salNCode",
        //   label: this.$t("ui.data.column.monthplan.salNCode"),
        //   disabled: true,
        // },
        // {
        //   prop: "natCode",
        //   label: this.$t("ui.data.column.monthplan.natCode"),
        //   disabled: true,
        // },
        // {
        //   prop: "brand",
        //   label: this.$t("common.brand"),
        //   disabled: true,
        // },

        // {
        //   prop: "备注",
        //   label: this.$t("备注"),
        // },
      ];
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;
        let obj = {
          salCodePo: params.salCodePo,
          scmPriority: params.scmPriority,
        };
        const res = await savePoData(obj);
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
