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
import moment from "moment";

import infoForm from "@/views/components/infoForm.vue";
import structureSelect from "../components/structureSelect.vue";
import materialCodeSelect from "../components/materialCodeSelect.vue";

import { editMoldingParams, checkUniqueCxKeyProduct } from "@/api/cx/keyProduct";

export default {
  components: { infoForm,structureSelect,materialCodeSelect },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      form: {},
      rules: {
        structureName: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        embryoCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        closeOutRangeMaximum: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
        rangeValue: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "blur",
          },
        ],
      },
      columns: [
      {
          prop: "structureName",
          align: "center",
          label: this.$t("结构"),
          render: (form) => {
            return (
              <structureSelect
                key={form.structureName}
                v-model={form.structureName}
              />
            );
          }
          // sortable: "custom",
        },
        {
          prop: "embryoCode",
          align: "center",
          label: this.$t("胎胚代码"),
          render: (form) => {
            return (
              <materialCodeSelect
                key={form.embryoCode}
                v-model={form.embryoCode}
                onChange={this.handleEmbryoCodeChange}
              />
            );
          },
        },
        {
          prop: "embryoDesc",
          align: "center",
          label: this.$t("胎胚描述"),
          disabled: true,
        },
        {
          prop: "isActive",
          align: "center",
          label: this.$t("是否启用"),
          type: "select",
          dictData: this.parentDict.type.biz_yes_no,
        },
      ],
    };
  },
  computed: {
    title: function () {
      return (
        (this.isEdit
          ? this.$t("common.button.edit")
          : this.$t("common.button.add"))
      );
    },
  },
  methods: {
    handleEmbryoCodeChange(val,row){
      if (val) {
        this.$set(this.form,'embryoDesc',row.embryoDesc);
      }else{
        this.$set(this.form,'embryoDesc','');

      }

    },
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await editMoldingParams(params);
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
        data.isActive+="";
        this.form = {
          ...data,
        };
      }else{
        this.isEdit = false;
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
      this.$refs.form.triggerConfirm(async (params) => {
        const checkData = {
          structureName: params.structureName,
          embryoCode: params.embryoCode,
          id: params.id || null
        };
        const checkRes = await checkUniqueCxKeyProduct(checkData);
        if (checkRes.data && checkRes.data.exist) {
          this.$modal.msgError("结构加胎胚代码已存在");
          return;
        }
        this.save(params);
      });
    },
  },
};
</script>
