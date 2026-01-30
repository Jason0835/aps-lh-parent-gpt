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
import { editSkuStructure } from "@/api/monthplan/skuStructure";
import structureSelect from "@/views/components/structureSelect.vue";
import materialCodeSelect from "@/views/components/materialCodeSelect.vue";
import infoForm from "@/views/components/infoForm.vue";

export default {
  components: { infoForm, materialCodeSelect,structureSelect },
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
        materialCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        mesMaterialCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        structureName: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        factoryCode: [
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
          prop: "factoryCode",
          label: this.$t("common.factory"),
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
        },
        {
          prop: "mainMaterialDesc",
          label: this.$t("ui.data.rubberMaterial.embryoDesc"),
        },
        // {
        //   prop: "materialCode",
        //   label: this.$t("ui.data.column.monthplan.oriMaterialCode"),
        //   render: (form) => {
        //     return (
        //       <materialCodeSelect
        //         key={form.materialCode}
        //         v-model={form.materialCode}
        //         onChange={this.handleMaterialCodeChange}
        //       />
        //     );
        //   },
        // },
        // {
        //   prop: "materialDesc",
        //   label: this.$t("ui.data.column.scheduleAdjust.productCodeDesc"),
        //   disabled: true,
        // },

        {
          prop: "structureName",
          label: this.$t("ui.data.column.finishStock.structureName"),
          render: (form) => {
            return (
              <structureSelect
                key={form.structureName}
                v-model={form.structureName}
              />
            );
          },

        },
      ];
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await editSkuStructure(params);
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
        this.$set(this.form, "mesMaterialCode", data.mesMaterialCode);
      } else {
        this.form = {
          factoryCode:'116'
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
    handleMaterialCodeChange(val, row) {
      if (val) {
        this.$set(this.form, "materialDesc", row.materialDesc);
      } else {
        this.$set(this.form, "materialDesc", "");
      }
    },
  },
};
</script>
