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
          label: this.$t("ui.data.column.mdmMaterialInfo.factoryCode"),
          prop: "factoryCode",
          maxlength: "10",
          required: true,
          disabled: true,
          type: "select", //ISORNOT
          dictData: this.parentDict.type.biz_factory_name,
          disabled: true,
        },
        {
          prop: "productTypeName",
          disabled: true,
          label: this.$t("ui.data.column.mdmMaterialInfo.productTypeName"),
        },
        // {
        //   prop: "productCategory",
        //   disabled: true,
        //   label: this.$t("ui.data.column.capsuleChuck.productTypeCode"),
        //   dictData: this.parentDict.type.product_category,
        // },
        {
          label: this.$t("ui.data.column.mdmMaterialInfo.materialCategory"),
          prop: "materialCategory",
          maxlength: "50",
          required: true,
          type: "select", //ISORNOT
          dictData: this.parentDict.type.material_type,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.mdmMaterialInfo.structureName"),
          prop: "structureName",
          maxlength: "20",
          required: true,
        },
        {
          label: this.$t("ui.data.column.mdmMaterialInfo.materialCode"),
          prop: "materialCode",
          maxlength: "50",
          required: true,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.mdmMaterialInfo.mesMaterialCode"),
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
          label: this.$t("ui.data.column.mdmMaterialInfo.materialDesc"),
          prop: "materialDesc",
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.mdmMaterialInfo.specifications"),
          prop: "specifications",
          maxlength: "50",
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.mdmMaterialInfo.mainPattern"),
          prop: "mainPattern",
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.mdmMaterialInfo.pattern"),
          prop: "pattern",
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.mdmMaterialInfo.brand"),
          prop: "brand",
          type: "select", //ISORNOT
          dictData: this.parentDict.type.biz_brand_type,
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.mdmMaterialInfo.speed"),
          prop: "speed",

          disabled: true,
        },

        {
          label: this.$t("ui.data.column.mdmMaterialInfo.hierarchy"),
          prop: "hierarchy",
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.mdmMaterialInfo.proSize"),
          prop: "proSize",
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.mdmMaterialInfo.ability"),
          prop: "ability",
          disabled: true,
        },
        {
          label: this.$t("ui.data.column.mdmMaterialInfo.cantProduce"),
          prop: "cantProduce",
          type: "select", //ISORNOT
          dictData: this.parentDict.type.biz_yes_no,
        },
        {
          prop: "embryoCode",
          disabled: true,
          label: this.$t("ui.data.column.mdmMaterialInfo.embryoCode"),
        },
        {
          prop: "embryoDesc",
          disabled: true,
          label: this.$t("ui.data.column.mdmMaterialInfo.embryoDesc"),
        },
        {
          prop: "sectionWidth",
          disabled: true,
          label: this.$t("ui.data.column.mdmMaterialInfo.sectionWidth"),
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
