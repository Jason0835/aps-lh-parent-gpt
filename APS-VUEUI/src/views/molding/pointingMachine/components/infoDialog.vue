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
import {editProductMoldingLimit} from "@/api/mdm/productMoldingLimit"

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
        factoryCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        sapCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        embryoCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        machineCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        lineType: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        jobType: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
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
      return (
        (this.isEdit
          ? this.$t("common.button.edit")
          : this.$t("common.button.add")) +
        this.$t("ui.data.column.cx.specifyMachine.modalName")
      );
    },
    columns() {
      return [
        {
          label: this.$t("ui.data.column.specifyMachine.factoryCode"),
          prop: "factoryCode",
          span: 24,
          type: "select", //biz_factory_name
          dictData: this.parentDict.type.biz_factory_name,
          clearable: false,
        },
        {
          label: this.$t("ui.data.column.specifyMachine.sapCode"),
          prop: "sapCode",
          span: 24,
          required: true,
        },
        {
          label: this.$t("ui.data.column.specifyMachine.embryoCode"),
          prop: "embryoCode",
          span: 24,
          required: true,
        },
        {
          label: this.$t("ui.data.column.specifyMachine.machineCode"),
          prop: "machineCode",
          span: 24,
          required: true,
          type: "select",
          dictData: this.moldingMachines,
          labelKey: "moldingMachineCode",
          valueKey: "moldingMachineCode",
          filterable: true,
        },
        // {
        //   label: this.$t("ui.data.column.specifyMachine.lineType"),
        //   prop: "lineType",
        //   span: 24,
        //   type: "select", //LINE_TYPE
        //   dictData: this.parentDict.type.LINE_TYPE,
        // },
        {
          label: this.$t("ui.data.column.specifyMachine.jobType"),
          prop: "jobType",
          span: 24,
          type: "select", //JOB_TYPE
          dictData: this.parentDict.type.JOB_TYPE,
        },
        {
          label: this.$t("ui.data.column.remark"),
          prop: "remark",
          span: 24,
          type: "textarea",
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
          factoryCode: ""
        }
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
