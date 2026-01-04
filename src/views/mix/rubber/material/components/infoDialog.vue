<template>
  <el-dialog
    :title="title"
    :visible="visible"
    width="600px"
    @close="hide"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
  >
    <info-form
      class="form-item-height"
      ref="form"
      :form="form"
      :rules="rules"
      :columns="columns"
      label-position="right"
      label-width="150px"
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
import infoForm from "@/views/components/infoForm.vue";
import {
  tableListProductinfo,
  listProductinfo,
  editProductinfo,
  removeProductinfo,
  // configurationMould,
} from "@/api/lean/productinfo";
export default {
  components: { infoForm },
  inject: ["parentDict"],
  data() {
    return {
      loading: false,
      visible: false,
      isEdit: false,
      form: {
        classShift: "2",
      },
      rules: {
        proSize: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        brand: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        pattern: [
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
          maxlength: "10",
          required: true,
          disabled: true,
          type: "select", //ISORNOT
          dictData: this.parentDict.type.biz_factory_name,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.facMonthPlanInit.productSpecsName"),
          prop: "materialCategory",
          maxlength: "50",
          required: true,
          type: "select", //ISORNOT
          dictData: this.parentDict.type.material_type,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleAdjust.structureCode"),
          prop: "structureName",
          maxlength: "20",
          required: true,
        },
        {
          label: this.$t("ui.data.colume.wms.unused.productCode"),
          prop: "materialCode",
          maxlength: "50",
          required: true,
          disabled: true,
        },
        {
          label: this.$t("ui.data.defectiveStock.mesMaterialCode"),
          prop: "mesMaterialCode",
          maxlength: "50",
          required: true,
          disabled: true,
        },
        // {
        //   label: this.$t("物料名称"),
        //   prop: "minparkTime",
        //   maxlength: "50",
        //   required: true,
        //   disabled: true,
        // },
        {
          label: this.$t("ui.data.column.scheduleAdjust.productCodeDesc"),
          prop: "materialDesc",
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.reportClassAccuracy.materialCode"),
          prop: "specifications",
          maxlength: "50",
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.moldLedger.mainPattern"),
          prop: "mainPattern",
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.confMinProd.pattern"),
          prop: "pattern",
          disabled: true,
        },
        {
          label: this.$t("ui.data.colume.plan.first.draft.brand"),
          prop: "brand",
          type: "select", //ISORNOT
          dictData: this.parentDict.type.biz_brand_type,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleAdjust.seep"),
          prop: "speed",

          disabled: true,
        },

        {
          label: this.$t("ui.data.column.scheduleAdjust.hierarchy"),
          prop: "hierarchy",
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleAdjust.proSize"),
          prop: "proSize",
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.lean.productinfo.ability"),
          prop: "ability",
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.scheduleAdjust.cantProduce"),
          prop: "cantProduce",
          type: "select", //ISORNOT
          dictData: this.parentDict.type.biz_yes_no,
        },
        {
          prop: "embryoCode",
          disabled: true,
          label: this.$t("ui.data.rubberMaterial.embryoCode"),
        },
        {
          prop: "embryoDesc",
          disabled: true,
          label: this.$t("ui.data.rubberMaterial.embryoDesc"),
        },
        {
          prop: "sectionWidth",
          disabled: true,
          label: this.$t("ui.data.rubberMaterial.sectionWidth"),
        },
        {
          prop: "remark",
          disabled: true,
          label: this.$t("common.remark"),
        },
      ];
    },
  },
  methods: {
    // api
    async save(params) {
      // console.log(params);
      try {
        this.loading = true;
        const data = await editProductinfo(params);
        this.$modal.msgSuccess(data.msg);
        this.$emit("success");
        this.hide();
      } catch (error) {
        console.log(error);
      } finally {
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
        this.form.cantProduce = data.cantProduce + "";
      } else {
        this.form = {
          classShift: "2",
        };
      }
    },
    hide() {
      this.form = { classShift: "2" };
      this.$refs.form.triggerResetForm();
      // this.resetForm("infoForm");
      this.isEdit = false;
      this.visible = false;
    },

    handleConfirm() {
      this.$refs.form.triggerConfirm(this.save);
      // this.$refs.form.validate((valid) => {
      //   if (valid) {
      //     this.save({
      //       ...this.form,
      //     });
      //   }
      // });
    },
  },
};
</script>
