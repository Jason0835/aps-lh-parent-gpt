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
        }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script>
import moment from "moment";
import infoForm from "@/views/components/infoForm.vue";
import areaSelect from '@/views/components/areaSelect.vue'
import {saveAreaCapaInfo} from "@/api/monthplan/mdmAreaCapaAllocation";

export default {
  components: {infoForm,areaSelect},
  inject: ["parentDict"],
  data() {
    const validatePositiveInteger = (rule, value, callback) => {
      if (value === "" || value === null || value === undefined) {
        if (rule.required) {
          return callback(new Error(this.$t("common.rule.noData")));
        }
        return callback();
      }
      const strValue = String(value).trim();

      // 检查是否只包含数字
      if (!/^\d+$/.test(strValue)) {
        return callback(
          new Error(this.$t("common.rule.noPoint"))
        );
      }

      // 转换为数字
      const numValue = Number(strValue);
      if (numValue > 999999) {
        return callback(new Error(this.$t("common.rule.inoutMax")));
      }

      if (!Number.isInteger(numValue)) {
        return callback(new Error(this.$t("common.rule.peleaseInteger")));
      }

      callback();
    };
    return {
      loading: false,
      visible: false,
      isEdit: false,
      editType: null,
      form: {},
      rules: {
        // factoryCode: [
        //   {
        //     required: true,
        //     message: this.$t("common.rule.select"),
        //     trigger: "blur",
        //   },
        // ],
        // customCode: [
        //   {
        //     required: true,
        //     message: this.$t("common.rule.input"),
        //     trigger: "blur",
        //   },
        // ],
        // year: [
        //   {
        //     required: true,
        //     message: this.$t("common.rule.select"),
        //     trigger: "blur",
        //   },
        // ],
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
        month: [
          {
            required: true,
            message: this.$t("common.rule.select"),
            trigger: "blur",
          },
        ],
        areaCode: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
        ],
        capacityAllocation: [
          {
            required: true,
            message: this.$t("common.rule.input"),
            trigger: "change",
          },
          {
            validator: (rule, value, callback) => {
              validatePositiveInteger({ required: true }, value, callback);
            },
            trigger: ["change"],
          },
        ],
        // productCode: [
        //   {
        //     required: true,
        //     message: this.$t("common.rule.input"),
        //     trigger: "blur",
        //   },
        // ],
        // locationType: [
        //   {
        //     required: true,
        //     message: this.$t("common.rule.select"),
        //     trigger: "blur",
        //   },
        // ],
      },
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
    columns() {
      return [
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

        // {
        //   prop: "month",
        //   label: this.$t("ui.data.column.productionMouldConfiguration.month"),
        // },
        // {


        {
          prop: "productTypeCode",
          label: this.$t("ui.data.column.monthplan.productType"),
          type: "select",
          dictData: this.parentDict.type.biz_product_type,
        },
        {
          prop: "areaCode",
          label: this.$t("common.area"),
          render: (form) => {
            return (
              <areaSelect
                key={form.areaCode}
                multiple={true}
                v-model={form.areaCode}
              />
            );
          },
        },
        // {
        //   prop: "mouldCode",
        //   label: this.$t("三月末库存"),
        // },
        // {
        //   prop: "mouldCode",
        //   label: this.$t("四月发货任务"),
        // },
        // {
        //   prop: "mouldCode",
        //   label: this.$t("任务占比"),
        // },
        {
          prop: "capacityAllocation",
          label: this.$t("ui.data.column.regionalCapacityAllocation.fenpei"),
          type: "number",
          max:99999999,
          min:0
        },
      ];
    },
  },
  methods: {
    // api
    async save(params) {
      try {
        this.loading = true;

        const res = await saveAreaCapaInfo(params);
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
          yearMonth: data.year + "-" + data.month,
        };
      }
    },
    hide() {
      this.form = {};
      this.$refs.form.triggerResetForm();
      this.isEdit = false;
      this.visible = false;
    },

    handleConfirm() {
      this.$refs.form.triggerConfirm(async (params) => {
        Object.keys(params).forEach((key) => {
          if (this.isEmpty(params[key])) {
            params[key] = "";
          }
        });

        // 解析ui.data.colume.yearMonth
        let array = params.yearMonth.split("-")
        params.year = array[0]
        params.month = array[1]

        try {
          this.loading = true;
          this.save(params);
        } catch (error) {
          console.error(error);
          this.$modal.msgError(error.message);
          this.loading = false;
        }
      });
    },
  },
};
</script>
