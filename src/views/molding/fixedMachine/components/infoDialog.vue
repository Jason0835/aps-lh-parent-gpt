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

import materialCodeSelect from "@/views/components/materialCodeSelect.vue";
import structureSelect from "../components/structureSelect.vue";
import formingCapacitySelect from "@/views/components/formingCapacitySelect.vue";
import { editCxMachineFixed } from "@/api/monthplan/mdmCxMachineFixed";

import infoForm from "@/views/components/infoForm.vue";

export default {
  components: {
    infoForm,
    materialCodeSelect,
    structureSelect,
    formingCapacitySelect,
  },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      form: {},
      oldFixedStructure1: [],
      rules: {
        factoryCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        cxMachineCode: [
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
      return this.isEdit
        ? this.$t("common.button.edit")
        : this.$t("common.button.add");
    },
    columns() {
      return [
        {
          label: this.$t("common.factory"),
          prop: "factoryCode",
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
        },
        {
          prop: "cxMachineCode",
          label: this.$t("ui.data.column.workWearInfo.cxMachineCode"),
          render: (form) => {
            return (
              <formingCapacitySelect
                key={form.cxMachineCode}
                multiple={false}
                v-model={form.cxMachineCode}
              />
            );
          },
        },
        {
          prop: "fixedStructure1",
          label: this.$t("ui.data.column.workWearInfo.fixedStructure1"),
          render: (form) => {
            return (
              <structureSelect
                key={form.fixedStructure1}
                multiple={true}
                v-model={form.fixedStructure1}
                oldList={form.fixedStructure1}
              />
            );
          },
          maxlength: 500,
        },
        {
          prop: "fixedStructure2",
          label: this.$t("ui.data.column.workWearInfo.fixedStructure2"),
          render: (form) => {
            return (
              <structureSelect
                key={form.fixedStructure2}
                multiple={true}
                v-model={form.fixedStructure2}
                oldList={form.fixedStructure2}
              />
            );
          },
          maxlength: 500,
        },
        {
          prop: "fixedStructure3",
          label: this.$t("ui.data.column.workWearInfo.fixedStructure3"),
          maxlength: 500,
          render: (form) => {
            return (
              <structureSelect
                key={form.fixedStructure3}
                multiple={true}
                v-model={form.fixedStructure3}
                oldList={form.fixedStructure3}
              />
            );
          },
        },
        {
          prop: "fixedMaterialCode",
          label: this.$t("ui.data.column.workWearInfo.fixedMaterialCode"),
          maxlength: 500,
          render: (form) => {
            return (
              <materialCodeSelect
                key={form.fixedMaterialCode}
                multiple={true}
                v-model={form.fixedMaterialCode}
              />
            );
          },
        },
        {
          prop: "disableStructure",
          label: this.$t("ui.data.column.workWearInfo.disableStructure"),
          maxlength: 500,
          render: (form) => {
            return (
              <structureSelect
                key={form.disableStructure}
                multiple={true}
                v-model={form.disableStructure}
                oldList={form.disableStructure}
              />
            );
          },
        },
        {
          prop: "fixedMaterialDesc",
          label: this.$t("固定物料描述"),
          disabled: true,
        },
        {
          prop: "disableMaterialCode",
          label: this.$t("ui.data.column.workWearInfo.disableMaterialCode"),
          maxlength: 500,
          render: (form) => {
            return (
              <materialCodeSelect
                key={form.disableMaterialCode}
                multiple={true}
                v-model={form.disableMaterialCode}
              />
            );
          },
        },
        {
          prop: "disableMaterialDesc",
          label: this.$t("不可作业物料描述"),
          disabled: true,
        },
      ];
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await editCxMachineFixed(params);
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
    handleStructure1Change(val, row) {
      if (val) {
        // let odlList = this.form.fixedStructure1
        //   ? this.form.fixedStructure1.split(",")
        //   : [];
        // let newList = val.split(",");
        // const merged = [...odlList, ...newList];
        // const unique = [...new Set(merged)];
        // let resultList = unique.join(",");
        this.oldFixedStructure1 = row;
      } else {
        this.oldFixedStructure1 = [];
      }
    },
  },
};
</script>
