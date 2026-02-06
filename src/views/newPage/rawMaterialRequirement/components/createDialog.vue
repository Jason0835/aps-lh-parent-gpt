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
  listMdmProductConstruction,
  generateMdmProductConstruction,
  removeMdmProductConstruction
} from "@/api/maindata/rawMaterialRequirePlan​";
import { values } from "lodash";
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
        factoryCode: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],
        yearMonth: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "change",
          },
        ],

      },
      columns: [
        {
          prop: "factoryCode",
          label: this.$t("common.factory"),
          type: "select",
          dictData: this.parentDict.type.biz_factory_name,
        },
        {
          prop: "yearMonth",
          label: this.$t("ui.data.colume.yearMonth"),
          type: "date",
          dateType: "month",
          valueFormat: "yyyy-MM",
        },
        {
          prop: "isSpringFestivalMonth",
          label: this.$t("ui.data.rawMaterialRequirement.isSpringFestivalMonth"),
          type: "select",
          clearable: false,
          dictData: [{
            label:'T+1',
            value:'0'
          },{
            label:'T+2',
            value:'1'
          },],
        },

      ],
    };
  },
  computed: {
    title: function () {
      return this.$t("ui.data.column.rawMaterial.genger");
    },
  },
  methods: {
    // api
    async save(params) {
      // console.log(params);
      try {
        this.loading = true;
        if (params.yearMonth) {
          const [year, month] = params.yearMonth.split("-");
          params.year = year;
          params.month = month;
        }
        const data = await generateMdmProductConstruction(params);
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
    async show(data) {
      this.visible = true;
      const now = new Date();
      // now.setMonth(now.getMonth() + 1);
      // const year = now.getFullYear();
      // const month = String(now.getMonth() + 1).padStart(2, "0"); // 月份从0开始，需要+1
      const nextMonth = new Date(now.getFullYear(), now.getMonth() + 1, 1);
      const year = nextMonth.getFullYear();
      const month = nextMonth.getMonth() + 1; // 月份从0开始，需要+1
      let defaultParams = {
        factoryCode: "116",
        yearMonth: `${year}-${month}`,
        isSpringFestivalMonth:'0'
      };
      this.form={
        ...defaultParams
      }

    },
    hide() {
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
