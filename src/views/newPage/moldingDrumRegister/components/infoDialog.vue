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


import {
  saveMdmWorkWearInfo
} from "@/api/monthplan/mdmWorkWearInfo";


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
            trigger: "change",
          },
        ],
        cxMachineBrandCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        cxMachineTypeCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        workWearStatus: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        specificationModel: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        qty: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        unit: [
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
        perimeterMin: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        perimeterMax: [
          {
            required: true,
            message: this.$t("common.rule.input"),
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
      return (
        (this.isEdit
          ? this.$t("common.button.edit")
          : this.$t("common.button.add"))
      );
    },
    columns() {
      return [
           {
          prop: "factoryCode",
          label: this.$t("common.factory"),
          type:'select',
          dictData: this.parentDict.type.biz_factory_name,
        },
        {
          prop: "workWearType",
          label: this.$t("ui.data.column.trialPlan.trialType"),
          type:'select',
          dictData: this.parentDict.type.biz_work_type,
        },
        {
          prop: "workWearStatus",
          label: this.$t("ui.data.column.WorkWearInfo.workWearStatus"),
          type:'select',
          dictData: this.parentDict.type.biz_available_status,
        },
        {
          prop: "workWearName",
          label: this.$t("common.name"),
          maxlength:64
        },
        {
          prop: "cxMachineBrandCode",
          label: this.$t("ui.data.column.docMoldingMachine.moldingMachineClassName"),
          type:'select',
          dictData: this.parentDict.type.biz_machine_brand,
        },
        {
          prop: "cxMachineTypeCode",
          label: this.$t("ui.data.colume.collect.type"),
          type:'select',
          dictData: this.parentDict.type.biz_class_type,
        },
        {
          prop: "perimeterMax",
          label: this.$t("ui.data.column.capsuleChuck.perimeterMax"),
          type:'number',
          max:999999,
          min:1
        },
        {
          prop: "perimeterMin",
          label: this.$t("ui.data.column.capsuleChuck.perimeterMin"),
          type:'number',
          max:999999,
          min:1
        },
        {
          prop: "specificationModel",
          label: this.$t("ui.data.column.specColor.specDesc"),
          maxlength:64
        },
        {
          prop: "qty",
          label: this.$t("common.num"),
          type:'number',
          max:9999
        },
        {
          prop: "unit",
          label: this.$t("common.unit"),
          type:'select',
          dictData: this.parentDict.type.biz_work_unit,
        },
        {
          prop: "usedType",
          label: this.$t("ui.data.column.WorkWearInfo.usedType"),
          maxlength:20
        },
        {
          prop: "remark",
          label: this.$t("common.remark"),
        },
      ];
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await saveMdmWorkWearInfo(params);
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
          factoryCode: "116",
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
